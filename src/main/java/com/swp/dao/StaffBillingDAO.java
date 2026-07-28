package com.swp.dao;

import com.swp.model.dto.CheckoutResult;
import com.swp.model.dto.CheckoutView;
import com.swp.model.dto.InvoiceView;
import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Xử lý dữ liệu checkout và invoice cho Staff/Owner: lấy thông tin trả sân,
 * tính phụ phí quá giờ, tạo hóa đơn, đổi trạng thái booking và gửi notification thanh toán cho Customer.
 */
public class StaffBillingDAO {

    private static final BigDecimal DEFAULT_OVERTIME_FEE_PER_MINUTE = BigDecimal.valueOf(5000);
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_PENDING_CHECKOUT_PAYMENT = "PENDING_CHECKOUT_PAYMENT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String INVOICE_PENDING = "PENDING";
    private static final String INVOICE_PAID = "PAID";
    private static final String INVOICE_ACTIVE_LEGACY = "ACTIVE";
    private static final String PAYMENT_TYPE_CHECKOUT = "CHECKOUT";
    private static final String METHOD_CASH = "CASH";
    private static final String METHOD_ONLINE_REQUEST = "ONLINE_REQUEST";

    /**
     * Lấy thông tin preview checkout cho một booking.
     * Method tính số tiền còn lại ở server để Staff/Owner xem trước trước khi gửi hóa đơn cho Customer.
     */
    public CheckoutView getCheckoutView(long bookingId) throws SQLException {
        // SQL: Lấy dữ liệu booking/sân/Customer để preview checkout và tính số tiền còn lại.
        String sql = """
                SELECT b.booking_id, b.booking_code, b.customer_id, b.complex_id, b.field_id,
                       b.status, b.start_time, b.end_time, b.total_amount, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       fc.complex_name, fc.address, fc.ward, fc.city
                FROM bookings b
                JOIN users u ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                JOIN football_complexes fc ON b.complex_id = fc.complex_id
                WHERE b.booking_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                CheckoutView view = mapCheckoutView(rs);
                enrichCheckoutAmounts(conn, view, getOvertimeFeePerMinute(conn), LocalDateTime.now());
                InvoiceSummary invoice = findLatestCheckoutInvoice(conn, bookingId, false);
                if (invoice != null) {
                    view.setExistingInvoiceId(invoice.invoiceId());
                    view.setExistingInvoiceStatus(invoice.status());
                }
                PaymentRequestSummary paymentRequest = findPendingCheckoutPayment(conn, bookingId);
                if (paymentRequest != null) {
                    view.setPendingPaymentRequestId(paymentRequest.paymentId());
                    view.setPendingPaymentRequestStatus(paymentRequest.status());
                }
                return view;
            }
        }
    }

    /**
     * Lấy invoice mới nhất của booking để Staff/Owner xem chi tiết hoặc Customer mở từ notification.
     */
    public InvoiceView getInvoiceByBookingId(long bookingId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(invoiceViewSql("i.booking_id = ?"))) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapInvoiceView(rs) : null;
            }
        }
    }

    public InvoiceView getInvoiceByInvoiceId(long invoiceId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(invoiceViewSql("i.invoice_id = ?"))) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapInvoiceView(rs) : null;
            }
        }
    }

    public boolean hasPaidInvoice(long bookingId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            InvoiceSummary invoice = findLatestCheckoutInvoice(conn, bookingId, false);
            return invoice != null && isPaidInvoice(invoice.status());
        }
    }

    /**
     * Kiểm tra Staff có ca làm việc đang hoạt động tại complex trước khi thực hiện checkout.
     */
    public boolean canStaffCheckoutComplex(long staffId, long complexId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return hasActiveShiftForComplex(conn, staffId, complexId);
        }
    }

    /**
     * Kiểm tra Staff có phân ca trong ngày tại complex trước khi xem/xuất invoice.
     */
    public boolean canStaffViewComplexToday(long staffId, long complexId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return hasAssignedShiftForComplexToday(conn, staffId, complexId);
        }
    }

    /**
     * Hoàn tất bước Staff/Owner trả sân theo phương thức đã chọn.
     * Booking được khóa, tiền còn lại được tính lại từ DB và mọi ghi nhận invoice/payment/status đi trong cùng transaction.
     */
    public CheckoutResult completeCheckout(
            long bookingId,
            long actorId,
            boolean staffRole,
            String checkoutPaymentMethod
    ) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BookingLock booking = lockBooking(conn, bookingId);
                if (booking == null) {
                    throw new IllegalArgumentException("Khong tim thay lich dat san.");
                }
                // Business Rule BR-12: Staff chỉ được checkout booking tại complex có ca trực đang hoạt động.
                if (staffRole && !hasActiveShiftForComplex(conn, actorId, booking.complexId())) {
                    throw new SecurityException("Ban khong co ca lam viec dang hoat dong tai co so nay.");
                }

                LocalDateTime now = LocalDateTime.now();
                CheckoutAmounts amounts = calculateCheckoutAmounts(conn, booking, now);

                // Nếu đã có invoice thì xử lý tiếp đúng nhánh; không tạo trùng hóa đơn/payment request.
                InvoiceSummary existingInvoice = findLatestCheckoutInvoice(conn, bookingId, true);
                if (existingInvoice != null) {
                    if (isPaidInvoice(existingInvoice.status())) {
                        conn.commit();
                        return new CheckoutResult(existingInvoice.invoiceId(), bookingId, existingInvoice.invoiceCode(),
                                "Hoa don da duoc thanh toan.");
                    }

                    if (amounts.remainingAmount().signum() == 0) {
                        markInvoicePaid(conn, existingInvoice.invoiceId(), amounts, BigDecimal.ZERO);
                        completeBookingAfterCheckout(conn, booking);
                        conn.commit();
                        return new CheckoutResult(existingInvoice.invoiceId(), bookingId, existingInvoice.invoiceCode(),
                                "Booking da duoc thanh toan du, khong can tao them yeu cau.");
                    }

                    String method = requireCheckoutMethod(checkoutPaymentMethod, amounts.remainingAmount());
                    if (METHOD_CASH.equals(method)) {
                        recordCashCheckoutPayment(conn, booking, existingInvoice.invoiceId(), amounts, actorId);
                        completeBookingAfterCheckout(conn, booking);
                        conn.commit();
                        return new CheckoutResult(existingInvoice.invoiceId(), bookingId, existingInvoice.invoiceCode(),
                                "Đã ghi nhận thanh toán tiền mặt " + moneyPlain(amounts.remainingAmount()) + ". Checkout thành công.");
                    }

                    updatePendingInvoiceAmounts(conn, existingInvoice.invoiceId(), amounts);
                    PaymentRequestSummary requestSummary = createOrUpdatePendingCheckoutPaymentRequest(conn, booking, existingInvoice.invoiceId(),
                            amounts.remainingAmount());
                    ensurePendingCheckoutStatus(conn, booking);
                    if (!requestSummary.existing()) {
                        insertCheckoutPaymentNotification(conn, booking, existingInvoice.invoiceId(), amounts.remainingAmount());
                    }
                    conn.commit();
                    String message = requestSummary.existing()
                            ? "Booking này đã có một yêu cầu thanh toán đang chờ khách xử lý."
                            : "Đã gửi yêu cầu thanh toán " + moneyPlain(amounts.remainingAmount()) + " cho khách hàng.";
                    return new CheckoutResult(existingInvoice.invoiceId(), bookingId, existingInvoice.invoiceCode(), message);
                }

                // Business Rule BR-15: Chỉ booking CHECKED_IN mới được checkout.
                if (!STATUS_CHECKED_IN.equals(booking.status())) {
                    throw new IllegalArgumentException("Chi lich da nhan san moi duoc checkout.");
                }
                // TODO Business Rule BR-16: SRS yêu cầu now >= booking.endTime() trước khi checkout;
                // transaction hiện tại mới tính overtime, chưa chặn checkout sớm.

                String invoiceCode = generateInvoiceCode(bookingId);

                // Business Rule BR-19: Nếu không còn tiền phải trả thì invoice PAID và booking COMPLETED ngay.
                if (amounts.remainingAmount().signum() == 0) {
                    // Khi tiền cọc đã đủ bù toàn bộ chi phí, invoice được đánh dấu PAID và booking hoàn tất ngay.
                    long invoiceId = insertInvoice(
                            conn,
                            invoiceCode,
                            booking,
                            actorId,
                            amounts.subtotal(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            INVOICE_PAID,
                            amounts.overtimeMinutes(),
                            amounts.overtimeFee()
                    );
                    completeBookingAfterCheckout(conn, booking);
                    conn.commit();
                    return new CheckoutResult(invoiceId, bookingId, invoiceCode,
                            "Booking không còn số tiền phải thanh toán. Đã hoàn tất checkout.");
                }

                String method = requireCheckoutMethod(checkoutPaymentMethod, amounts.remainingAmount());
                if (METHOD_CASH.equals(method)) {
                    long invoiceId = insertInvoice(
                            conn,
                            invoiceCode,
                            booking,
                            actorId,
                            amounts.subtotal(),
                            amounts.remainingAmount(),
                            BigDecimal.ZERO,
                            INVOICE_PENDING,
                            amounts.overtimeMinutes(),
                            amounts.overtimeFee()
                    );
                    recordCashCheckoutPayment(conn, booking, invoiceId, amounts, actorId);
                    completeBookingAfterCheckout(conn, booking);
                    conn.commit();
                    return new CheckoutResult(invoiceId, bookingId, invoiceCode,
                            "Đã ghi nhận thanh toán tiền mặt " + moneyPlain(amounts.remainingAmount())
                                    + ". Checkout booking thành công.");
                }

                // Business Rule BR-20: Còn tiền phải trả và Staff chọn online thì tạo invoice/payment PENDING.
                long invoiceId = insertInvoice(
                        conn,
                        invoiceCode,
                        booking,
                        actorId,
                        amounts.subtotal(),
                        amounts.remainingAmount(),
                        BigDecimal.ZERO,
                        INVOICE_PENDING,
                        amounts.overtimeMinutes(),
                        amounts.overtimeFee()
                );
                createOrUpdatePendingCheckoutPaymentRequest(conn, booking, invoiceId, amounts.remainingAmount());
                updateBookingStatus(conn, bookingId, STATUS_CHECKED_IN, STATUS_PENDING_CHECKOUT_PAYMENT);
                insertCheckoutPaymentNotification(conn, booking, invoiceId, amounts.remainingAmount());

                conn.commit();
                return new CheckoutResult(invoiceId, bookingId, invoiceCode,
                        "Đã gửi yêu cầu thanh toán " + moneyPlain(amounts.remainingAmount()) + " cho khách hàng.");
            } catch (SQLException | RuntimeException e) {
                    rollback(conn, e);
                throw e;
            }
        }
    }

    public boolean cancelLateNoShowBooking(long bookingId, long staffId) throws SQLException {
        return cancelLateNoShowBooking(bookingId, staffId, true);
    }

    public boolean cancelLateNoShowBooking(long bookingId, long actorId, boolean staffRole) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BookingLock booking = lockBooking(conn, bookingId);
                if (booking == null) {
                    throw new IllegalArgumentException("Khong tim thay lich dat san.");
                }
                // Business Rule BR-14: Chỉ booking CONFIRMED chưa check-in mới được hủy no-show.
                if (!STATUS_CONFIRMED.equals(booking.status())) {
                    throw new IllegalArgumentException("Chi booking da xac nhan va chua check-in moi co the huy no-show.");
                }
                // Business Rule BR-14: Chỉ được hủy no-show sau 30 phút kể từ giờ bắt đầu booking.
                if (booking.startTime() == null || LocalDateTime.now().isBefore(booking.startTime().plusMinutes(30))) {
                    throw new IllegalArgumentException("Booking chua qua 30 phut ke tu gio bat dau.");
                }
                // Business Rule BR-12: Staff hủy no-show cũng phải có ca đang hoạt động tại đúng complex.
                if (staffRole && !hasActiveShiftForComplex(conn, actorId, booking.complexId())) {
                    throw new SecurityException("Ban khong co ca lam viec dang hoat dong tai co so nay.");
                }

                // SQL: Hủy booking CONFIRMED thành CANCELLED khi khách no-show quá 30 phút.
                String update = """
                        UPDATE bookings
                        SET status = 'CANCELLED',
                            cancellation_reason = 'NO_SHOW_LATE_30_MINUTES',
                            cancelled_at = GETDATE(),
                            updated_at = GETDATE()
                        WHERE booking_id = ?
                          AND status = 'CONFIRMED'
                        """;
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setLong(1, bookingId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Khong cap nhat duoc booking no-show.");
                    }
                }

                // Business Rule BR-14: Hủy no-show giải phóng sân.
                releaseField(conn, booking.fieldId());
                insertNotification(conn,
                        booking.customerId(),
                        "Booking đã bị hủy do đến muộn",
                        "Booking " + booking.bookingCode() + " đã bị hủy vì khách chưa check-in sau 30 phút kể từ giờ bắt đầu.",
                        "BOOKING",
                        booking.bookingId());

                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    /**
     * SQL chung để hiển thị invoice, bao gồm thông tin booking, sân, Customer, Staff và payment checkout mới nhất.
     */
    private String invoiceViewSql(String predicate) {
        // SQL: Query view invoice dùng chung cho Staff/Owner và Customer mở invoice checkout.
        return """
                SELECT TOP 1
                       i.invoice_id, i.invoice_code, i.customer_id, i.status AS invoice_status, i.issued_at,
                       i.subtotal, i.total_amount, i.paid_amount,
                       i.overtime_minutes, i.overtime_fee,
                       b.booking_id, b.booking_code, b.status AS booking_status, b.start_time, b.end_time,
                       b.field_id, b.total_amount AS field_fee, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       fc.complex_id, fc.complex_name, fc.address, fc.ward, fc.city,
                       staff.full_name AS staff_name,
                       lp.payment_status,
                       lp.payment_method_name,
                       lp.payment_amount,
                       lp.paid_at,
                       dp.deposit_payment_method_name
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                JOIN users u ON i.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                JOIN football_complexes fc ON b.complex_id = fc.complex_id
                LEFT JOIN users staff ON i.staff_id = staff.user_id
                OUTER APPLY (
                    SELECT TOP 1 p.status AS payment_status,
                           CASE WHEN UPPER(pm.method_code) = 'CASH' THEN N'Tiền mặt' ELSE pm.method_name END AS payment_method_name,
                           p.amount AS payment_amount,
                           p.paid_at
                    FROM payments p
                    LEFT JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = i.booking_id
                      AND p.customer_id = i.customer_id
                      AND p.payment_type = 'CHECKOUT'
                    ORDER BY p.created_at DESC, p.payment_id DESC
                ) lp
                OUTER APPLY (
                    SELECT TOP 1
                           CASE WHEN UPPER(pm.method_code) = 'CASH' THEN N'Tiền mặt' ELSE pm.method_name END AS deposit_payment_method_name
                    FROM payments p
                    LEFT JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = i.booking_id
                      AND p.customer_id = i.customer_id
                      AND p.payment_type = 'DEPOSIT'
                      AND p.status = 'SUCCESS'
                    ORDER BY p.paid_at DESC, p.created_at DESC, p.payment_id DESC
                ) dp
                WHERE %s
                  AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                ORDER BY i.issued_at DESC, i.invoice_id DESC
                """.formatted(predicate);
    }

    /**
     * Khóa booking trong transaction checkout để không có hai Staff/Owner tạo invoice cùng lúc.
     */
    private BookingLock lockBooking(Connection conn, long bookingId) throws SQLException {
        // SQL: Khóa booking bằng UPDLOCK/HOLDLOCK trước khi checkout hoặc hủy no-show.
        String sql = """
                SELECT booking_id, booking_code, customer_id, complex_id, field_id,
                       status, start_time, end_time, total_amount, deposit_amount
                FROM bookings WITH (UPDLOCK, HOLDLOCK)
                WHERE booking_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new BookingLock(
                        rs.getLong("booking_id"),
                        rs.getString("booking_code"),
                        rs.getLong("customer_id"),
                        rs.getLong("complex_id"),
                        rs.getLong("field_id"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("start_time")),
                        toLocalDateTime(rs.getTimestamp("end_time")),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("deposit_amount")
                );
            }
        }
    }

    /**
     * Tìm invoice checkout mới nhất, tùy chọn khóa bản ghi khi chuẩn bị tạo/gửi lại invoice.
     */
    private InvoiceSummary findLatestCheckoutInvoice(Connection conn, long bookingId, boolean forUpdate) throws SQLException {
        String lock = forUpdate ? " WITH (UPDLOCK, HOLDLOCK)" : "";
        // SQL: Lấy invoice checkout mới nhất, có thể kèm lock khi đang xử lý transaction.
        String sql = """
                SELECT TOP 1 invoice_id, invoice_code, status, total_amount
                FROM invoices%s
                WHERE booking_id = ?
                  AND status IN ('PENDING', 'PAID', 'ACTIVE')
                ORDER BY issued_at DESC, invoice_id DESC
                """.formatted(lock);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new InvoiceSummary(
                        rs.getLong("invoice_id"),
                        rs.getString("invoice_code"),
                        rs.getString("status"),
                        safe(rs.getBigDecimal("total_amount"))
                );
            }
        }
    }

    /**
     * Bổ sung các số tiền checkout cho màn hình preview: phụ phí quá giờ, subtotal và số còn phải trả sau khi trừ cọc.
     */
    private void enrichCheckoutAmounts(Connection conn, CheckoutView view, BigDecimal overtimeFeePerMinute, LocalDateTime now)
            throws SQLException {
        view.setCheckoutTime(now);
        view.setOvertimeFeePerMinute(overtimeFeePerMinute);
        // Business Rule BR-17: Preview checkout cũng tính số phút quá giờ theo thời điểm hiện tại.
        long overtimeMinutes = calculateOvertimeMinutes(view.getEndTime(), now);
        BigDecimal overtimeFee = overtimeFeePerMinute.multiply(BigDecimal.valueOf(overtimeMinutes));
        BigDecimal subtotal = safe(view.getFieldFee()).add(overtimeFee);
        BigDecimal paidAmount = getSuccessfulPaidAmountForBooking(conn, view.getBookingId());
        // Business Rule BR-18: Preview số tiền còn lại không được nhỏ hơn 0 và luôn dựa trên payment SUCCESS trong DB.
        BigDecimal finalAmount = maxZero(subtotal.subtract(paidAmount));

        view.setOvertimeMinutes(overtimeMinutes);
        view.setOvertimeFee(overtimeFee);
        view.setSubtotal(subtotal);
        view.setFinalAmount(finalAmount);
        view.setPaidAmountBeforeCheckout(paidAmount);
        view.setCheckoutAllowed(STATUS_CHECKED_IN.equals(view.getStatus()));
        view.setCheckoutBlockedReason(null);
    }

    private CheckoutAmounts calculateCheckoutAmounts(Connection conn, BookingLock booking, LocalDateTime now)
            throws SQLException {
        BigDecimal overtimeFeePerMinute = getOvertimeFeePerMinute(conn);
        long overtimeMinutes = calculateOvertimeMinutes(booking.endTime(), now);
        BigDecimal overtimeFee = overtimeFeePerMinute.multiply(BigDecimal.valueOf(overtimeMinutes));
        BigDecimal subtotal = safe(booking.fieldFee()).add(overtimeFee);
        BigDecimal paidAmount = getSuccessfulPaidAmountForBooking(conn, booking.bookingId());
        BigDecimal remainingAmount = maxZero(subtotal.subtract(paidAmount));
        return new CheckoutAmounts(subtotal, paidAmount, remainingAmount, overtimeMinutes, overtimeFee);
    }

    private BigDecimal getSuccessfulPaidAmountForBooking(Connection conn, Long bookingId) throws SQLException {
        if (bookingId == null) {
            return BigDecimal.ZERO;
        }
        // SQL: Tính tổng payment SUCCESS của booking để xác định số tiền còn phải trả khi checkout.
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS paid_amount
                FROM payments WITH (UPDLOCK, HOLDLOCK)
                WHERE booking_id = ?
                  AND status = 'SUCCESS'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? safe(rs.getBigDecimal("paid_amount")) : BigDecimal.ZERO;
            }
        }
    }

    private String requireCheckoutMethod(String rawMethod, BigDecimal remainingAmount) {
        if (safe(remainingAmount).signum() == 0) {
            return "";
        }
        String method = rawMethod == null ? "" : rawMethod.trim().toUpperCase();
        if (method.isEmpty()) {
            throw new IllegalArgumentException("Vui long chon phuong thuc xu ly thanh toan checkout.");
        }
        if (!METHOD_CASH.equals(method) && !METHOD_ONLINE_REQUEST.equals(method)) {
            throw new IllegalArgumentException("Phuong thuc xu ly checkout khong hop le.");
        }
        return method;
    }

    /**
     * Tính số phút quá giờ, làm tròn lên để chỉ cần quá một phần phút vẫn bị tính một phút.
     */
    private long calculateOvertimeMinutes(LocalDateTime endTime, LocalDateTime now) {
        // Business Rule BR-17: Chưa quá giờ kết thúc thì không phát sinh phụ phí quá giờ.
        if (endTime == null || now == null || !now.isAfter(endTime)) {
            return 0;
        }
        long nanos = Duration.between(endTime, now).toNanos();
        long minuteNanos = 60_000_000_000L;
        return (nanos + minuteNanos - 1) / minuteNanos;
    }

    private BigDecimal getOvertimeFeePerMinute(Connection conn) {
        // SQL: Đọc cấu hình phí quá giờ theo phút cho flow checkout.
        String sql = """
                SELECT setting_value
                FROM system_settings
                WHERE setting_key = 'CHECKOUT_OVERTIME_FEE_PER_MINUTE'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String raw = rs.getString("setting_value");
                if (raw != null && !raw.isBlank()) {
                    return new BigDecimal(raw.trim());
                }
            }
        } catch (Exception ignored) {
            return DEFAULT_OVERTIME_FEE_PER_MINUTE;
        }
        return DEFAULT_OVERTIME_FEE_PER_MINUTE;
    }

    private void updateBookingStatus(Connection conn, long bookingId, String oldStatus, String newStatus)
            throws SQLException {
        // SQL: Chuyển trạng thái booking checkout khi trạng thái hiện tại khớp oldStatus.
        String sql = """
                UPDATE bookings
                SET status = ?,
                    updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status = ?
                """;
        // Business Rule BR-24: Booking chỉ được chuyển trạng thái khi oldStatus khớp trạng thái hiện tại.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, bookingId);
            ps.setString(3, oldStatus);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Booking khong con hop le de checkout.");
            }
        }
    }

    private void releaseField(Connection conn, long fieldId) throws SQLException {
        // SQL: Trả sân về AVAILABLE sau checkout/no-show nếu sân không maintenance/disabled.
        String sql = """
                UPDATE fields
                SET status = 'AVAILABLE',
                    updated_at = GETDATE()
                WHERE field_id = ?
                  AND status NOT IN ('MAINTENANCE', 'DISABLED')
                """;
        // Business Rule BR-12: SQL kiểm tra ca trực cùng ngày, cùng complex và đang nằm trong khoảng giờ làm việc.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fieldId);
            ps.executeUpdate();
        }
    }

    /**
     * Tạo invoice checkout và trả về invoice_id để notification/payment tham chiếu.
     */
    private long insertInvoice(
            Connection conn,
            String invoiceCode,
            BookingLock booking,
            Long staffId,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            String status,
            long overtimeMinutes,
            BigDecimal overtimeFee
    ) throws SQLException {
        // SQL: Tạo invoice checkout và trả invoice_id bằng OUTPUT INSERTED.
        String sql = """
                INSERT INTO invoices (
                    invoice_code,
                    booking_id,
                    customer_id,
                    staff_id,
                    subtotal,
                    discount_amount,
                    total_amount,
                    paid_amount,
                    status,
                    issued_at,
                    overtime_minutes,
                    overtime_fee
                )
                OUTPUT INSERTED.invoice_id
                VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, GETDATE(), ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceCode);
            ps.setLong(2, booking.bookingId());
            ps.setLong(3, booking.customerId());
            if (staffId == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, staffId);
            }
            ps.setBigDecimal(5, safe(subtotal));
            ps.setBigDecimal(6, safe(totalAmount));
            ps.setBigDecimal(7, safe(paidAmount));
            ps.setString(8, status);
            ps.setLong(9, overtimeMinutes);
            ps.setBigDecimal(10, safe(overtimeFee));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Khong the tao hoa don.");
                }
                return rs.getLong(1);
            }
        }
    }

    private void updatePendingInvoiceAmounts(Connection conn, long invoiceId, CheckoutAmounts amounts)
            throws SQLException {
        // SQL: Cập nhật lại số tiền invoice PENDING khi Staff gửi lại yêu cầu checkout.
        String sql = """
                UPDATE invoices
                SET subtotal = ?,
                    total_amount = ?,
                    paid_amount = 0,
                    overtime_minutes = ?,
                    overtime_fee = ?
                WHERE invoice_id = ?
                  AND status = 'PENDING'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amounts.subtotal());
            ps.setBigDecimal(2, amounts.remainingAmount());
            ps.setLong(3, amounts.overtimeMinutes());
            ps.setBigDecimal(4, amounts.overtimeFee());
            ps.setLong(5, invoiceId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc hoa don checkout dang cho.");
            }
        }
    }

    private void markInvoicePaid(
            Connection conn,
            long invoiceId,
            CheckoutAmounts amounts,
            BigDecimal paidAmount
    ) throws SQLException {
        // SQL: Đánh dấu invoice checkout PAID và lưu amount/overtime cuối cùng.
        String sql = """
                UPDATE invoices
                SET status = 'PAID',
                    subtotal = ?,
                    total_amount = ?,
                    paid_amount = ?,
                    overtime_minutes = ?,
                    overtime_fee = ?
                WHERE invoice_id = ?
                  AND status IN ('PENDING', 'ACTIVE')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amounts.subtotal());
            ps.setBigDecimal(2, amounts.remainingAmount());
            ps.setBigDecimal(3, safe(paidAmount));
            ps.setLong(4, amounts.overtimeMinutes());
            ps.setBigDecimal(5, amounts.overtimeFee());
            ps.setLong(6, invoiceId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc trang thai hoa don.");
            }
        }
    }

    private void completeBookingAfterCheckout(
            Connection conn,
            BookingLock booking
    ) throws SQLException {
        // SQL: Hoàn tất booking sau checkout, chỉ khi booking còn đúng trạng thái đang xử lý.
        String updateBooking = """
                UPDATE bookings
                SET status = 'COMPLETED',
                    updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
            ps.setLong(1, booking.bookingId());
            ps.setString(2, booking.status());
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Booking khong con hop le de checkout.");
            }
        }
        releaseField(conn, booking.fieldId());
    }

    private void ensurePendingCheckoutStatus(Connection conn, BookingLock booking)
            throws SQLException {
        if (STATUS_PENDING_CHECKOUT_PAYMENT.equals(booking.status())) {
            return;
        }
        updateBookingStatus(conn, booking.bookingId(), STATUS_CHECKED_IN, STATUS_PENDING_CHECKOUT_PAYMENT);
    }

    private void recordCashCheckoutPayment(
            Connection conn,
            BookingLock booking,
            long invoiceId,
            CheckoutAmounts amounts,
            long staffId
    ) throws SQLException {
        if (amounts.remainingAmount().signum() <= 0) {
            return;
        }
        if (hasSuccessfulCheckoutPayment(conn, booking.bookingId())) {
            throw new IllegalArgumentException("Khoan checkout nay da duoc thanh toan.");
        }

        int cashMethodId = getOrCreatePaymentMethod(conn, METHOD_CASH, "Tiền mặt");
        String transactionRef = generateTransactionRef();
        String gatewayRef = "CASH-" + staffId + "-" + transactionRef;
        // SQL: Ghi nhận payment CHECKOUT SUCCESS cho giao dịch tiền mặt tại quầy.
        String insertPayment = """
                INSERT INTO payments (
                    booking_id,
                    customer_id,
                    payment_method_id,
                    amount,
                    payment_type,
                    status,
                    transaction_ref,
                    gateway_transaction_id,
                    paid_at,
                    created_at
                )
                VALUES (?, ?, ?, ?, 'CHECKOUT', 'SUCCESS', ?, ?, GETDATE(), GETDATE())
                """;
        // Business Rule BR-21: Tiền mặt tại quầy được ghi SUCCESS trước khi invoice/booking hoàn tất checkout.
        try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
            ps.setLong(1, booking.bookingId());
            ps.setLong(2, booking.customerId());
            ps.setInt(3, cashMethodId);
            ps.setBigDecimal(4, amounts.remainingAmount());
            ps.setString(5, transactionRef);
            ps.setString(6, gatewayRef);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong ghi nhan duoc giao dich tien mat.");
            }
        }

        markInvoicePaid(conn, invoiceId, amounts, amounts.remainingAmount());
        failPendingCheckoutPayments(conn, booking.bookingId(), transactionRef);
        insertNotification(conn,
                booking.customerId(),
                "Đã ghi nhận thanh toán tiền mặt",
                "Booking " + booking.bookingCode() + " đã được ghi nhận thanh toán tiền mặt "
                        + moneyPlain(amounts.remainingAmount()) + " tại quầy.",
                "CHECKOUT_PAYMENT_SUCCESS",
                invoiceId);
    }

    private boolean hasSuccessfulCheckoutPayment(Connection conn, long bookingId) throws SQLException {
        // SQL: Kiểm tra booking đã có checkout payment SUCCESS chưa để chống ghi trùng.
        String sql = """
                SELECT TOP 1 1
                FROM payments WITH (UPDLOCK, HOLDLOCK)
                WHERE booking_id = ?
                  AND payment_type = 'CHECKOUT'
                  AND status = 'SUCCESS'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void failPendingCheckoutPayments(Connection conn, long bookingId, String paidTransactionRef)
            throws SQLException {
        // SQL: Đánh dấu các checkout payment PENDING khác là FAILED sau khi một giao dịch đã thanh toán.
        String sql = """
                UPDATE payments
                SET status = 'FAILED'
                WHERE booking_id = ?
                  AND payment_type = 'CHECKOUT'
                  AND status = 'PENDING'
                  AND transaction_ref <> ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            ps.setString(2, paidTransactionRef);
            ps.executeUpdate();
        }
    }

    private PaymentRequestSummary createOrUpdatePendingCheckoutPaymentRequest(
            Connection conn,
            BookingLock booking,
            long invoiceId,
            BigDecimal amount
    ) throws SQLException {
        if (safe(amount).signum() <= 0) {
            throw new IllegalArgumentException("Booking da duoc thanh toan du, khong can tao them yeu cau.");
        }
        if (hasSuccessfulCheckoutPayment(conn, booking.bookingId())) {
            throw new IllegalArgumentException("Khoan checkout nay da duoc thanh toan.");
        }

        int onlineMethodId = getDefaultOnlinePaymentMethodId(conn);
        // SQL: Tìm checkout payment PENDING hiện có để cập nhật amount/method thay vì tạo trùng.
        String selectExisting = """
                SELECT TOP 1 payment_id, status
                FROM payments WITH (UPDLOCK, HOLDLOCK)
                WHERE booking_id = ?
                  AND customer_id = ?
                  AND payment_type = 'CHECKOUT'
                  AND status = 'PENDING'
                ORDER BY created_at DESC, payment_id DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(selectExisting)) {
            ps.setLong(1, booking.bookingId());
            ps.setLong(2, booking.customerId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long paymentId = rs.getLong("payment_id");
                    updatePendingCheckoutPaymentAmount(conn, paymentId, onlineMethodId, amount);
                    return new PaymentRequestSummary(paymentId, rs.getString("status"), true);
                }
            }
        }

        // SQL: Insert payment CHECKOUT PENDING cho yêu cầu Customer thanh toán online.
        String insertPayment = """
                INSERT INTO payments (
                    booking_id,
                    customer_id,
                    payment_method_id,
                    amount,
                    payment_type,
                    status,
                    transaction_ref,
                    created_at
                )
                OUTPUT INSERTED.payment_id
                VALUES (?, ?, ?, ?, 'CHECKOUT', 'PENDING', ?, GETDATE())
                """;
        String transactionRef = generateTransactionRef();
        // Business Rule BR-20: Online request dùng payment PENDING để Customer thanh toán phần checkout còn lại.
        try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
            ps.setLong(1, booking.bookingId());
            ps.setLong(2, booking.customerId());
            ps.setInt(3, onlineMethodId);
            ps.setBigDecimal(4, amount);
            ps.setString(5, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Khong tao duoc yeu cau thanh toan online.");
                }
                return new PaymentRequestSummary(rs.getLong(1), STATUS_PENDING, false);
            }
        }
    }

    private void updatePendingCheckoutPaymentAmount(
            Connection conn,
            long paymentId,
            int paymentMethodId,
            BigDecimal amount
    ) throws SQLException {
        // SQL: Cập nhật amount/method của payment checkout PENDING đang chờ Customer.
        String sql = """
                UPDATE payments
                SET amount = ?,
                    payment_method_id = ?
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, paymentMethodId);
            ps.setLong(3, paymentId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc yeu cau thanh toan checkout.");
            }
        }
    }

    private PaymentRequestSummary findPendingCheckoutPayment(Connection conn, long bookingId) throws SQLException {
        // SQL: Lấy checkout payment PENDING mới nhất của booking để preview trạng thái request.
        String sql = """
                SELECT TOP 1 payment_id, status
                FROM payments
                WHERE booking_id = ?
                  AND payment_type = 'CHECKOUT'
                  AND status = 'PENDING'
                ORDER BY created_at DESC, payment_id DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PaymentRequestSummary(rs.getLong("payment_id"), rs.getString("status"), true);
            }
        }
    }

    private int getDefaultOnlinePaymentMethodId(Connection conn) throws SQLException {
        // SQL: Chọn payment method online mặc định, ưu tiên VNPay rồi SIMULATED.
        String sql = """
                SELECT TOP 1 payment_method_id
                FROM payment_methods
                WHERE status = 'ACTIVE'
                  AND UPPER(method_code) <> 'CASH'
                ORDER BY CASE
                             WHEN UPPER(method_code) = 'VNPAY' THEN 0
                             WHEN UPPER(method_code) = 'SIMULATED' THEN 1
                             ELSE 2
                         END,
                         payment_method_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalArgumentException("Chua cau hinh phuong thuc thanh toan online.");
            }
            return rs.getInt("payment_method_id");
        }
    }

    private int getOrCreatePaymentMethod(Connection conn, String methodCode, String methodName) throws SQLException {
        Integer existing = findPaymentMethodIdByCode(conn, methodCode);
        if (existing != null) {
            return existing;
        }
        // SQL: Tạo payment method CASH/online fallback nếu chưa có cấu hình.
        String sql = """
                INSERT INTO payment_methods (method_code, method_name, status)
                OUTPUT INSERTED.payment_method_id
                VALUES (?, ?, 'ACTIVE')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, methodCode);
            ps.setString(2, methodName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        Integer created = findPaymentMethodIdByCode(conn, methodCode);
        if (created == null) {
            throw new SQLException("Khong cau hinh duoc phuong thuc thanh toan " + methodCode + ".");
        }
        return created;
    }

    private Integer findPaymentMethodIdByCode(Connection conn, String methodCode) throws SQLException {
        // SQL: Lookup payment method theo code với lock để tránh tạo trùng khi fallback insert.
        String sql = """
                SELECT TOP 1 payment_method_id
                FROM payment_methods WITH (UPDLOCK, HOLDLOCK)
                WHERE UPPER(method_code) = UPPER(?)
                ORDER BY payment_method_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, methodCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("payment_method_id") : null;
            }
        }
    }

    /**
     * Gửi notification cho Customer để mở hóa đơn checkout và chọn phương thức thanh toán.
     */
    private void insertCheckoutPaymentNotification(Connection conn, BookingLock booking, long invoiceId, BigDecimal finalAmount)
            throws SQLException {
        insertNotification(conn,
                booking.customerId(),
                "Ban co hoa don can thanh toan",
                "Booking " + booking.bookingCode() + " da duoc tra san. Vui long thanh toan so tien con lai "
                        + moneyPlain(finalAmount) + ".",
                "CHECKOUT_PAYMENT",
                invoiceId);
    }

    private void insertNotification(
            Connection conn,
            long userId,
            String title,
            String message,
            String type,
            long referenceId
    ) throws SQLException {
        // SQL: Insert notification checkout/payment cho Customer hoặc Staff.
        String sql = """
                INSERT INTO notifications (
                    user_id, title, message, notification_type, reference_id, is_read, created_at
                )
                VALUES (?, ?, ?, ?, ?, 0, GETDATE())
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, type);
            ps.setLong(5, referenceId);
            ps.executeUpdate();
        }
    }

    private boolean hasActiveShiftForComplex(Connection conn, long staffId, long complexId) throws SQLException {
        // SQL: Kiểm tra Staff có ca trực đang hoạt động tại complex ở thời điểm hiện tại.
        String sql = """
                SELECT TOP 1 1
                FROM work_shifts ws
                JOIN shift_assignments sa ON ws.shift_id = sa.shift_id
                WHERE sa.staff_id = ?
                  AND ws.complex_id = ?
                  AND ws.shift_date = CAST(GETDATE() AS DATE)
                  AND (sa.status IS NULL OR sa.status <> 'CANCELLED')
                  AND (
                      (
                          CAST(ws.start_time AS TIME) <= CAST(ws.end_time AS TIME)
                          AND CAST(GETDATE() AS TIME) >= CAST(ws.start_time AS TIME)
                          AND CAST(GETDATE() AS TIME) <= CAST(ws.end_time AS TIME)
                      )
                      OR
                      (
                          CAST(ws.start_time AS TIME) > CAST(ws.end_time AS TIME)
                          AND (
                              CAST(GETDATE() AS TIME) >= CAST(ws.start_time AS TIME)
                              OR CAST(GETDATE() AS TIME) <= CAST(ws.end_time AS TIME)
                          )
                      )
                  )
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setLong(2, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasAssignedShiftForComplexToday(Connection conn, long staffId, long complexId) throws SQLException {
        // SQL: Kiểm tra Staff có phân ca trong ngày tại complex để được xem invoice.
        String sql = """
                SELECT TOP 1 1
                FROM work_shifts ws
                JOIN shift_assignments sa ON ws.shift_id = sa.shift_id
                WHERE sa.staff_id = ?
                  AND ws.complex_id = ?
                  AND ws.shift_date = CAST(GETDATE() AS DATE)
                  AND (sa.status IS NULL OR sa.status <> 'CANCELLED')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setLong(2, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private CheckoutView mapCheckoutView(ResultSet rs) throws SQLException {
        CheckoutView view = new CheckoutView();
        view.setBookingId(rs.getLong("booking_id"));
        view.setBookingCode(rs.getString("booking_code"));
        view.setCustomerId(rs.getLong("customer_id"));
        view.setComplexId(rs.getLong("complex_id"));
        view.setFieldId(rs.getLong("field_id"));
        view.setStatus(rs.getString("status"));
        view.setCustomerName(rs.getString("customer_name"));
        view.setCustomerPhone(rs.getString("customer_phone"));
        view.setComplexName(rs.getString("complex_name"));
        view.setComplexAddress(joinAddress(rs));
        view.setFieldName(rs.getString("field_name"));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setFieldFee(rs.getBigDecimal("total_amount"));
        view.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        return view;
    }

    private InvoiceView mapInvoiceView(ResultSet rs) throws SQLException {
        InvoiceView view = new InvoiceView();
        view.setInvoiceId(rs.getLong("invoice_id"));
        view.setInvoiceCode(rs.getString("invoice_code"));
        view.setInvoiceStatus(rs.getString("invoice_status"));
        view.setIssuedAt(toLocalDateTime(rs.getTimestamp("issued_at")));
        view.setBookingId(rs.getLong("booking_id"));
        view.setBookingCode(rs.getString("booking_code"));
        view.setCustomerId(rs.getLong("customer_id"));
        view.setComplexId(rs.getLong("complex_id"));
        view.setFieldId(rs.getLong("field_id"));
        view.setCustomerName(rs.getString("customer_name"));
        view.setCustomerPhone(rs.getString("customer_phone"));
        view.setComplexName(rs.getString("complex_name"));
        view.setComplexAddress(joinAddress(rs));
        view.setFieldName(rs.getString("field_name"));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setFieldFee(rs.getBigDecimal("field_fee"));
        view.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        view.setOvertimeMinutes(rs.getLong("overtime_minutes"));
        view.setOvertimeFee(rs.getBigDecimal("overtime_fee"));
        view.setSubtotal(rs.getBigDecimal("subtotal"));
        view.setTotalAmount(rs.getBigDecimal("total_amount"));
        view.setPaidAmount(rs.getBigDecimal("paid_amount"));
        view.setBookingStatus(rs.getString("booking_status"));
        view.setPaymentStatus(rs.getString("payment_status"));
        view.setPaymentMethodName(rs.getString("payment_method_name"));
        view.setCheckoutPaymentAmount(rs.getBigDecimal("payment_amount"));
        view.setCheckoutPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));
        view.setDepositPaymentMethodName(rs.getString("deposit_payment_method_name"));
        view.setStaffName(rs.getString("staff_name"));
        return view;
    }

    private String joinAddress(ResultSet rs) throws SQLException {
        List<String> parts = new ArrayList<>();
        addPart(parts, rs.getString("address"));
        addPart(parts, rs.getString("ward"));
        addPart(parts, rs.getString("city"));
        return String.join(", ", parts);
    }

    private void addPart(List<String> parts, String value) {
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value.trim());
        }
    }

    private BigDecimal maxZero(BigDecimal value) {
        return safe(value).signum() < 0 ? BigDecimal.ZERO : safe(value);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean isPaidInvoice(String status) {
        return INVOICE_PAID.equals(status) || INVOICE_ACTIVE_LEGACY.equals(status);
    }

    private String moneyPlain(BigDecimal value) {
        return String.format("%,d VND", safe(value).longValue());
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String generateInvoiceCode(long bookingId) {
        return "INV" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + bookingId;
    }

    private String generateTransactionRef() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                .format(LocalDateTime.now());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "TXN" + timestamp + random;
    }

    private void rollback(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private record BookingLock(
            long bookingId,
            String bookingCode,
            long customerId,
            long complexId,
            long fieldId,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal fieldFee,
            BigDecimal depositAmount
    ) {
    }

    private record InvoiceSummary(
            long invoiceId,
            String invoiceCode,
            String status,
            BigDecimal totalAmount
    ) {
    }

    private record CheckoutAmounts(
            BigDecimal subtotal,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            long overtimeMinutes,
            BigDecimal overtimeFee
    ) {
    }

    private record PaymentRequestSummary(
            long paymentId,
            String status,
            boolean existing
    ) {
    }
}

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
    private static final String INVOICE_PENDING = "PENDING";
    private static final String INVOICE_PAID = "PAID";
    private static final String INVOICE_ACTIVE_LEGACY = "ACTIVE";

    /**
     * Lấy thông tin preview checkout cho một booking.
     * Method tính số tiền còn lại ở server để Staff/Owner xem trước trước khi gửi hóa đơn cho Customer.
     */
    public CheckoutView getCheckoutView(long bookingId) throws SQLException {
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
                enrichCheckoutAmounts(view, getOvertimeFeePerMinute(conn), LocalDateTime.now());
                InvoiceSummary invoice = findLatestCheckoutInvoice(conn, bookingId, false);
                if (invoice != null) {
                    view.setExistingInvoiceId(invoice.invoiceId());
                    view.setExistingInvoiceStatus(invoice.status());
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
     * Hoàn tất bước Staff/Owner trả sân.
     * Booking được khóa, tính tiền sân/phụ phí, trừ cọc và tạo invoice trong một transaction.
     */
    public CheckoutResult completeCheckout(long bookingId, long actorId, boolean staffRole) throws SQLException {
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

                // Nếu đã có invoice thì không tạo mới; chỉ gửi lại notification khi invoice còn PENDING.
                InvoiceSummary existingInvoice = findLatestCheckoutInvoice(conn, bookingId, true);
                if (existingInvoice != null) {
                    if (INVOICE_PENDING.equals(existingInvoice.status())) {
                        insertCheckoutPaymentNotification(conn, booking, existingInvoice.invoiceId(), existingInvoice.totalAmount());
                        conn.commit();
                        return new CheckoutResult(existingInvoice.invoiceId(), bookingId, existingInvoice.invoiceCode(),
                                "Da gui lai yeu cau thanh toan cho khach.");
                    }
                    conn.commit();
                    return new CheckoutResult(existingInvoice.invoiceId(), bookingId, existingInvoice.invoiceCode(),
                            "Hoa don da duoc thanh toan.");
                }

                // Business Rule BR-15: Chỉ booking CHECKED_IN mới được checkout.
                if (!STATUS_CHECKED_IN.equals(booking.status())) {
                    throw new IllegalArgumentException("Chi lich da nhan san moi duoc checkout.");
                }

                // Số tiền checkout được tính từ booking trong DB và thời điểm trả sân thực tế.
                LocalDateTime now = LocalDateTime.now();
                BigDecimal overtimeFeePerMinute = getOvertimeFeePerMinute(conn);
                // Business Rule BR-17: Phụ phí quá giờ được tính theo từng phút sau giờ kết thúc booking.
                long overtimeMinutes = calculateOvertimeMinutes(booking.endTime(), now);
                BigDecimal overtimeFee = overtimeFeePerMinute.multiply(BigDecimal.valueOf(overtimeMinutes));
                BigDecimal fieldTotal = safe(booking.fieldFee());
                BigDecimal subtotal = fieldTotal.add(overtimeFee);
                // Business Rule BR-18: Số tiền còn lại = max(tổng tiền sân + phí quá giờ - tiền cọc, 0).
                BigDecimal finalAmount = maxZero(subtotal.subtract(safe(booking.depositAmount())));
                String invoiceCode = generateInvoiceCode(bookingId);

                // Business Rule BR-19: Nếu không còn tiền phải trả thì invoice PAID và booking COMPLETED ngay.
                if (finalAmount.signum() == 0) {
                    // Khi tiền cọc đã đủ bù toàn bộ chi phí, invoice được đánh dấu PAID và booking hoàn tất ngay.
                    long invoiceId = insertInvoice(
                            conn,
                            invoiceCode,
                            booking,
                            actorId,
                            subtotal,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            INVOICE_PAID,
                            overtimeMinutes,
                            overtimeFee
                    );
                    updateBookingStatus(conn, bookingId, STATUS_CHECKED_IN, STATUS_COMPLETED);
                    releaseField(conn, booking.fieldId());
                    insertBookingStatusLog(conn, booking.bookingId(), STATUS_CHECKED_IN, STATUS_COMPLETED,
                            actorId, "Checkout completed with zero amount due.");
                    conn.commit();
                    return new CheckoutResult(invoiceId, bookingId, invoiceCode,
                            "Booking khong con so tien phai thanh toan. Da hoan tat checkout.");
                }

                // Business Rule BR-20: Còn tiền phải trả thì invoice PENDING và booking chuyển sang PENDING_CHECKOUT_PAYMENT.
                // Còn tiền phải trả thì tạo invoice PENDING và chuyển booking sang chờ Customer thanh toán checkout.
                long invoiceId = insertInvoice(
                        conn,
                        invoiceCode,
                        booking,
                        actorId,
                        subtotal,
                        finalAmount,
                        BigDecimal.ZERO,
                        INVOICE_PENDING,
                        overtimeMinutes,
                        overtimeFee
                );
                updateBookingStatus(conn, bookingId, STATUS_CHECKED_IN, STATUS_PENDING_CHECKOUT_PAYMENT);
                insertBookingStatusLog(conn, booking.bookingId(), STATUS_CHECKED_IN, STATUS_PENDING_CHECKOUT_PAYMENT,
                        actorId, "Checkout payment request created.");
                insertCheckoutPaymentNotification(conn, booking, invoiceId, finalAmount);

                conn.commit();
                return new CheckoutResult(invoiceId, bookingId, invoiceCode,
                        "Da gui yeu cau thanh toan cho khach.");
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

                // Business Rule BR-14: Hủy no-show giải phóng sân và ghi log CANCELLED.
                releaseField(conn, booking.fieldId());
                insertBookingStatusLog(conn, booking.bookingId(), STATUS_CONFIRMED, STATUS_CANCELLED,
                        actorId, "NO_SHOW_LATE_30_MINUTES");
                insertNotification(conn,
                        booking.customerId(),
                        "Booking da bi huy do den muon",
                        "Booking " + booking.bookingCode() + " da bi huy vi khach chua check-in sau 30 phut ke tu gio bat dau.",
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
                       lp.payment_method_name
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                JOIN users u ON i.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                JOIN football_complexes fc ON b.complex_id = fc.complex_id
                LEFT JOIN users staff ON i.staff_id = staff.user_id
                OUTER APPLY (
                    SELECT TOP 1 p.status AS payment_status,
                           pm.method_name AS payment_method_name
                    FROM payments p
                    LEFT JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = i.booking_id
                      AND p.customer_id = i.customer_id
                      AND p.payment_type = 'CHECKOUT'
                    ORDER BY p.created_at DESC, p.payment_id DESC
                ) lp
                WHERE %s
                  AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                ORDER BY i.issued_at DESC, i.invoice_id DESC
                """.formatted(predicate);
    }

    /**
     * Khóa booking trong transaction checkout để không có hai Staff/Owner tạo invoice cùng lúc.
     */
    private BookingLock lockBooking(Connection conn, long bookingId) throws SQLException {
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
    private void enrichCheckoutAmounts(CheckoutView view, BigDecimal overtimeFeePerMinute, LocalDateTime now) {
        view.setCheckoutTime(now);
        view.setOvertimeFeePerMinute(overtimeFeePerMinute);
        // Business Rule BR-17: Preview checkout cũng tính số phút quá giờ theo thời điểm hiện tại.
        long overtimeMinutes = calculateOvertimeMinutes(view.getEndTime(), now);
        BigDecimal overtimeFee = overtimeFeePerMinute.multiply(BigDecimal.valueOf(overtimeMinutes));
        BigDecimal subtotal = safe(view.getFieldFee()).add(overtimeFee);
        // Business Rule BR-18: Preview số tiền còn lại không được nhỏ hơn 0 sau khi trừ tiền cọc.
        BigDecimal finalAmount = maxZero(subtotal.subtract(safe(view.getDepositAmount())));

        view.setOvertimeMinutes(overtimeMinutes);
        view.setOvertimeFee(overtimeFee);
        view.setSubtotal(subtotal);
        view.setFinalAmount(finalAmount);
        view.setCheckoutAllowed(STATUS_CHECKED_IN.equals(view.getStatus()));
        view.setCheckoutBlockedReason(null);
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

    private void insertBookingStatusLog(
            Connection conn,
            long bookingId,
            String oldStatus,
            String newStatus,
            Long changedBy,
            String note
    ) throws SQLException {
        String sql = """
                INSERT INTO booking_status_logs (
                    booking_id, old_status, new_status, changed_by, note, created_at
                )
                VALUES (?, ?, ?, ?, ?, GETDATE())
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            ps.setString(2, oldStatus);
            ps.setString(3, newStatus);
            if (changedBy == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, changedBy);
            }
            ps.setString(5, note);
            ps.executeUpdate();
        }
    }

    private boolean hasActiveShiftForComplex(Connection conn, long staffId, long complexId) throws SQLException {
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
}

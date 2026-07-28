package com.swp.dao;

import com.swp.model.Payment;
import com.swp.model.PaymentMethod;
import com.swp.model.User;
import com.swp.model.dto.BookingView;
import com.swp.model.dto.CheckoutPaymentRequestView;
import com.swp.model.dto.InvoiceView;
import com.swp.model.dto.PaymentView;
import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cung cấp các transaction thanh toán cho đặt cọc booking, checkout invoice và membership.
 * DAO luôn lấy amount từ database, khóa payment/booking/invoice khi cập nhật và ghi callback để audit gateway.
 */
public class PaymentDAO {

    private final VoucherDAO voucherDAO = new VoucherDAO();

    private static final String STATUS_HOLD = "HOLD";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_PENDING_CHECKOUT_PAYMENT = "PENDING_CHECKOUT_PAYMENT";
    private static final String PAYMENT_TYPE_DEPOSIT = "DEPOSIT";
    private static final String PAYMENT_TYPE_CHECKOUT = "CHECKOUT";
    private static final String PAYMENT_TYPE_MEMBERSHIP = "MEMBERSHIP";
    private static final String METHOD_CASH = "CASH";
    private static final String GATEWAY_SIMULATED = "SIMULATED";
    private static final String DISTRIBUTION_PUBLIC_CODE = "PUBLIC_CODE";
    private static final String DISTRIBUTION_REWARD_VOUCHER = "REWARD_VOUCHER";

    private final UserDAO userDAO = new UserDAO();

    /**
     * Lấy booking còn hiệu lực để Customer thanh toán tiền cọc.
     * Query giới hạn theo customer_id, trạng thái HOLD, thời gian giữ chỗ và chưa có payment SUCCESS.
     */
    public BookingView getBookingForPayment(long bookingId, long customerId) throws SQLException {
        // SQL: Lấy booking HOLD còn hạn và chưa SUCCESS để Customer thanh toán đặt cọc.
        String sql = """
                SELECT b.booking_id,
                       b.booking_code,
                       b.recurring_group_id,
                       COALESCE(rg.repeat_type, 'NONE') AS repeat_type,
                       grp.recurring_count,
                       b.customer_id,
                       b.complex_id,
                       fa.complex_name,
                       b.field_id,
                       f.field_name,
                       ft.type_name AS field_type_name,
                       b.start_time,
                       b.end_time,
                       grp.total_amount,
                       CASE
                           WHEN COALESCE(rg.repeat_type, 'NONE') = 'MONTHLY'
                           THEN grp.total_amount
                           ELSE grp.deposit_amount
                       END AS deposit_amount,
                       b.status,
                       grp.hold_expires_at,
                       lp.payment_status,
                       lp.payment_method_name,
                       lp.paid_amount,
                       lp.paid_at
                FROM bookings b
                LEFT JOIN booking_recurring_groups rg ON b.recurring_group_id = rg.recurring_group_id
                INNER JOIN football_complexes fa ON b.complex_id = fa.complex_id
                INNER JOIN fields f ON b.field_id = f.field_id
                INNER JOIN field_types ft ON f.field_type_id = ft.field_type_id
                OUTER APPLY (
                    SELECT COUNT(*) AS recurring_count,
                           SUM(COALESCE(sb.final_amount, sb.total_amount)) AS total_amount,
                           SUM(ROUND(COALESCE(sb.final_amount, sb.total_amount) * 0.30, 2)) AS deposit_amount,
                           MIN(sb.hold_expires_at) AS hold_expires_at
                    FROM bookings sb
                    WHERE sb.customer_id = b.customer_id
                      AND (
                          (b.recurring_group_id IS NOT NULL AND sb.recurring_group_id = b.recurring_group_id)
                          OR (b.recurring_group_id IS NULL AND sb.booking_id = b.booking_id)
                      )
                ) grp
                OUTER APPLY (
                    SELECT TOP 1
                           p.status AS payment_status,
                           pm.method_name AS payment_method_name,
                           p.amount AS paid_amount,
                           p.paid_at
                    FROM payments p
                    LEFT JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE EXISTS (
                        SELECT 1
                        FROM bookings paid_booking
                        WHERE paid_booking.booking_id = p.booking_id
                          AND paid_booking.customer_id = b.customer_id
                          AND (
                              (b.recurring_group_id IS NOT NULL AND paid_booking.recurring_group_id = b.recurring_group_id)
                              OR (b.recurring_group_id IS NULL AND paid_booking.booking_id = b.booking_id)
                          )
                    )
                    ORDER BY p.created_at DESC, p.payment_id DESC
                ) lp
                WHERE b.booking_id = ?
                  AND b.customer_id = ?
                  -- Business Rule BR-08: Chỉ booking HOLD còn hạn mới được lấy để thanh toán đặt cọc.
                  AND b.status = 'HOLD'
                  AND grp.hold_expires_at > GETDATE()
                  AND NOT EXISTS (
                      SELECT 1
                      FROM bookings invalid_booking
                      WHERE invalid_booking.customer_id = b.customer_id
                        AND (
                            (b.recurring_group_id IS NOT NULL AND invalid_booking.recurring_group_id = b.recurring_group_id)
                            OR (b.recurring_group_id IS NULL AND invalid_booking.booking_id = b.booking_id)
                        )
                        AND (
                            invalid_booking.status <> 'HOLD'
                            OR invalid_booking.hold_expires_at <= GETDATE()
                        )
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM payments paid
                      WHERE EXISTS (
                          SELECT 1
                          FROM bookings paid_booking
                          WHERE paid_booking.booking_id = paid.booking_id
                            AND paid_booking.customer_id = b.customer_id
                            AND (
                                (b.recurring_group_id IS NOT NULL AND paid_booking.recurring_group_id = b.recurring_group_id)
                                OR (b.recurring_group_id IS NULL AND paid_booking.booking_id = b.booking_id)
                            )
                      )
                        AND paid.status = 'SUCCESS'
                  )
                """;

        // Mở kết nối chỉ để đọc booking đủ điều kiện thanh toán.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            ps.setLong(2, customerId);

            // ResultSet có tối đa một booking vì filter theo booking_id.
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapBookingForPayment(rs) : null;
            }
        }
    }

    public List<PaymentMethod> getActivePaymentMethods() throws SQLException {
        // SQL: Lấy toàn bộ payment method ACTIVE cho các luồng nội bộ.
        String sql = """
                SELECT payment_method_id, method_code, method_name, status
                FROM payment_methods
                WHERE status = 'ACTIVE'
                ORDER BY payment_method_id
        """;
        List<PaymentMethod> methods = new ArrayList<>();

        // Đọc toàn bộ payment method ACTIVE để phục vụ các màn nội bộ cần cả CASH.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Duyệt từng dòng method và map sang model.
            while (rs.next()) {
                methods.add(mapPaymentMethod(rs));
            }
        }

        return methods;
    }

    public List<PaymentMethod> getActiveOnlinePaymentMethods() throws SQLException {
        // SQL: Lấy payment method online cho Customer, loại CASH khỏi form thanh toán.
        String sql = """
                SELECT payment_method_id, method_code, method_name, status
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
        List<PaymentMethod> methods = new ArrayList<>();

        // Customer online chỉ thấy method không phải CASH.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Giữ thứ tự ưu tiên VNPay trước các method online còn lại.
            while (rs.next()) {
                methods.add(mapPaymentMethod(rs));
            }
        }

        return methods;
    }

    public PaymentMethod getPaymentMethodById(int paymentMethodId) throws SQLException {
        // SQL: Lookup payment method để validate trước khi tạo payment.
        String sql = """
                SELECT payment_method_id, method_code, method_name, status
                FROM payment_methods
                WHERE payment_method_id = ?
                """;

        // Lookup method theo id để controller/DAO validate trước khi tạo payment.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentMethodId);
            // Không tìm thấy method thì trả null cho tầng gọi quyết định message lỗi.
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPaymentMethod(rs) : null;
            }
        }
    }

    /**
     * Tạo hoặc tái sử dụng payment PENDING cho tiền cọc booking.
     * Booking/nhóm recurring được khóa để số tiền và trạng thái không đổi trong lúc tạo transactionRef.
     */
    public Payment createPendingDepositPayment(long bookingId, long customerId, int paymentMethodId)
            throws SQLException {
        // SQL: Khóa booking/group HOLD để tính amount đặt cọc và kiểm tra trạng thái.
        String selectBooking = """
                SELECT CASE
                           WHEN COALESCE(rg.repeat_type, 'NONE') = 'MONTHLY'
                           THEN grp.total_amount
                           ELSE grp.deposit_amount
                        END AS deposit_amount,
                        b.voucher_id,
                        b.user_voucher_id,
                        v.distribution_type,
                        b.status,
                       CASE
                           WHEN grp.invalid_booking_count = 0
                                AND grp.hold_expired_count = 0
                           THEN 1
                           ELSE 0
                       END AS hold_valid,
                       CASE
                           WHEN EXISTS (
                               SELECT 1
                               FROM payments p
                               WHERE p.status = 'SUCCESS'
                                 AND p.payment_type = 'DEPOSIT'
                                 AND EXISTS (
                                     SELECT 1
                                     FROM bookings paid_booking
                                     WHERE paid_booking.booking_id = p.booking_id
                                       AND paid_booking.customer_id = b.customer_id
                                       AND (
                                           (b.recurring_group_id IS NOT NULL AND paid_booking.recurring_group_id = b.recurring_group_id)
                                           OR (b.recurring_group_id IS NULL AND paid_booking.booking_id = b.booking_id)
                                       )
                                 )
                           )
                           THEN 1
                           ELSE 0
                       END AS has_success
                FROM bookings b WITH (UPDLOCK, HOLDLOCK)
                LEFT JOIN booking_recurring_groups rg ON b.recurring_group_id = rg.recurring_group_id
                LEFT JOIN vouchers v ON b.voucher_id = v.id
                OUTER APPLY (
                    SELECT SUM(COALESCE(sb.final_amount, sb.total_amount)) AS total_amount,
                           -- Business Rule BR-06: Tiền cọc booking thường bằng 30% final amount.
                           SUM(ROUND(COALESCE(sb.final_amount, sb.total_amount) * 0.30, 2)) AS deposit_amount,
                           SUM(CASE WHEN sb.status <> 'HOLD' THEN 1 ELSE 0
                    END) AS invalid_booking_count,
                           SUM(CASE WHEN sb.hold_expires_at > GETDATE() THEN 0 ELSE 1 END) AS hold_expired_count
                    FROM bookings sb WITH (UPDLOCK, HOLDLOCK)
                    WHERE sb.customer_id = b.customer_id
                      AND (
                          (b.recurring_group_id IS NOT NULL AND sb.recurring_group_id = b.recurring_group_id)
                          OR (b.recurring_group_id IS NULL AND sb.booking_id = b.booking_id)
                      )
                ) grp
                WHERE b.booking_id = ?
                  AND b.customer_id = ?
                """;
        // SQL: Kiểm tra payment method ACTIVE trước khi tạo payment đặt cọc.
        String selectMethod = """
                SELECT payment_method_id, method_code
                FROM payment_methods
                WHERE payment_method_id = ?
                  AND status = 'ACTIVE'
                """;
        // SQL: Tìm payment DEPOSIT PENDING cùng booking/group để reuse transactionRef.
        String selectExistingDepositPayment = """
                SELECT TOP 1 p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.payment_method_id,
                       p.amount,
                       p.payment_type,
                       p.status,
                       p.transaction_ref,
                       p.gateway_transaction_id,
                       p.paid_at,
                       p.created_at
                FROM payments p WITH (UPDLOCK, HOLDLOCK)
                WHERE p.customer_id = ?
                  AND p.payment_type = 'DEPOSIT'
                  AND p.status = 'PENDING'
                  AND EXISTS (
                      SELECT 1
                      FROM bookings source_booking
                      INNER JOIN bookings paid_booking ON paid_booking.booking_id = p.booking_id
                      WHERE source_booking.booking_id = ?
                        AND source_booking.customer_id = ?
                        AND paid_booking.customer_id = source_booking.customer_id
                        AND (
                            (source_booking.recurring_group_id IS NOT NULL AND paid_booking.recurring_group_id = source_booking.recurring_group_id)
                            OR (source_booking.recurring_group_id IS NULL AND paid_booking.booking_id = source_booking.booking_id)
                        )
                  )
                ORDER BY p.created_at DESC, p.payment_id DESC
                """;
        // SQL: Insert payment DEPOSIT PENDING và trả row vừa tạo cho controller.
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
                OUTPUT INSERTED.payment_id,
                       INSERTED.booking_id,
                       INSERTED.customer_id,
                       INSERTED.payment_method_id,
                       INSERTED.amount,
                       INSERTED.payment_type,
                       INSERTED.status,
                       INSERTED.transaction_ref,
                       INSERTED.gateway_transaction_id,
                       INSERTED.paid_at,
                       INSERTED.created_at
                VALUES (?, ?, ?, ?, 'DEPOSIT', 'PENDING', ?, GETDATE())
                """;

        // Dùng transaction để khóa booking và insert/reuse payment một cách nguyên tử.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            // Nếu bất kỳ validate/insert nào lỗi thì rollback toàn bộ payment pending.
            try {
                // Business Rule BR-06: Amount đặt cọc được tính từ booking trong DB, không nhận từ request để tránh chỉnh sửa số tiền ở client.
                BigDecimal amount;
                Integer voucherId;
                String distributionType;
                // Lock booking/recurring group để trạng thái HOLD và amount không đổi giữa lúc tạo payment.
                try (PreparedStatement ps = conn.prepareStatement(selectBooking)) {
                    ps.setLong(1, bookingId);
                    ps.setLong(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Không có booking hợp lệ cho customer hiện tại thì dừng thanh toán.
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y booking h\u1ee3p l\u1ec7.");
                        }
                        // Business Rule BR-08: Payment đặt cọc chỉ hợp lệ khi booking vẫn ở trạng thái HOLD.
                        if (!STATUS_HOLD.equals(rs.getString("status"))) {
                            throw new IllegalArgumentException("Booking kh\u00f4ng c\u00f2n \u1edf tr\u1ea1ng th\u00e1i ch\u1edd thanh to\u00e1n.");
                        }
                        // Business Rule BR-05: HOLD hết hạn thì không được tạo payment và slot sẽ được giải phóng ở flow dọn trạng thái.
                        if (rs.getInt("hold_valid") != 1) {
                            throw new IllegalArgumentException("Th\u1eddi gian gi\u1eef ch\u1ed7 c\u1ee7a booking \u0111\u00e3 h\u1ebft h\u1ea1n.");
                        }
                        // Chặn tạo payment mới nếu booking/group đã có giao dịch thành công.
                        if (rs.getInt("has_success") == 1) {
                            throw new IllegalArgumentException("Booking n\u00e0y \u0111\u00e3 \u0111\u01b0\u1ee3c thanh to\u00e1n.");
                        }
                        int selectedVoucherId = rs.getInt("voucher_id");
                        voucherId = rs.wasNull() ? null : selectedVoucherId;
                        distributionType = rs.getString("distribution_type");
                        amount = rs.getBigDecimal("deposit_amount");
                    }
                }

                // Chỉ PUBLIC_CODE cần kiểm tra lịch sử dùng theo customer/voucher; REWARD_VOUCHER đã khóa bằng user_voucher_id.
                if (voucherId != null
                        && DISTRIBUTION_PUBLIC_CODE.equalsIgnoreCase(distributionType)
                        && voucherDAO.hasCustomerUsedVoucher(voucherId, customerId, conn)) {
                    throw new IllegalArgumentException("B\u1ea1n \u0111\u00e3 s\u1eed d\u1ee5ng m\u00e3 gi\u1ea3m gi\u00e1 n\u00e0y.");
                }

                // Amount null hoặc không dương cho thấy booking/pricing không hợp lệ nên không tạo payment.
                if (amount == null || amount.signum() <= 0) {
                    throw new IllegalArgumentException("S\u1ed1 ti\u1ec1n c\u1ecdc c\u1ee7a booking kh\u00f4ng h\u1ee3p l\u1ec7.");
                }

                // Xác minh payment method ACTIVE ngay trong transaction tạo payment.
                try (PreparedStatement ps = conn.prepareStatement(selectMethod)) {
                    ps.setInt(1, paymentMethodId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Method không tồn tại/không ACTIVE thì dừng trước khi insert.
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Ph\u01b0\u01a1ng th\u1ee9c thanh to\u00e1n kh\u00f4ng h\u1ee3p l\u1ec7.");
                        }
                        // Customer online không được tự tạo payment CASH.
                        if (METHOD_CASH.equalsIgnoreCase(rs.getString("method_code"))) {
                            throw new IllegalArgumentException("Phuong thuc tien mat chi duoc Staff ghi nhan tai quay Check-out.");
                        }
                    }
                }

                // Business Rule BR-23: Nếu đã có payment PENDING cho cùng booking/nhóm, tái sử dụng để callback không bị phân mảnh.
                try (PreparedStatement ps = conn.prepareStatement(selectExistingDepositPayment)) {
                    ps.setLong(1, customerId);
                    ps.setLong(2, bookingId);
                    ps.setLong(3, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Nếu đã có PENDING cùng scope thì trả lại giao dịch cũ thay vì sinh transactionRef mới.
                        if (rs.next()) {
                            // Reuse the live deposit payment for this booking/group instead of inserting a duplicate.
                            Payment existing = mapPayment(rs);
                            conn.commit();
                            return existing;
                        }
                    }
                }

                String transactionRef = generateTransactionRef();
                Payment payment;
                // Insert payment PENDING mới sau khi tất cả validate đều pass.
                try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
                    ps.setLong(1, bookingId);
                    ps.setLong(2, customerId);
                    ps.setInt(3, paymentMethodId);
                    ps.setBigDecimal(4, amount);
                    ps.setString(5, transactionRef);
                    try (ResultSet rs = ps.executeQuery()) {
                        // OUTPUT INSERTED phải trả về payment vừa tạo; không có row là lỗi DB.
                        if (!rs.next()) {
                            throw new SQLException("Khong tao duoc giao dich thanh toan.");
                        }
                        payment = mapPayment(rs);
                    }
                }

                conn.commit();
                return payment;
            } catch (SQLException | RuntimeException e) {
                // Rollback cả validate có side effect và insert payment khi transaction lỗi.
                rollback(conn, e);
                throw e;
            }
        }
    }

    /**
     * Lấy invoice checkout để Customer chọn phương thức thanh toán.
     * Điều kiện customer_id bảo vệ ownership trước khi hiển thị số tiền còn lại.
     */
    public InvoiceView getCheckoutInvoiceForPayment(long invoiceId, long customerId) throws SQLException {
        // SQL: Lấy invoice checkout thuộc Customer để hiển thị số tiền còn lại.
        String sql = """
                SELECT TOP 1
                       i.invoice_id, i.invoice_code, i.customer_id, i.status AS invoice_status, i.issued_at,
                       i.subtotal, i.total_amount, i.paid_amount,
                       i.overtime_minutes, i.overtime_fee,
                       b.booking_id, b.booking_code, b.status AS booking_status, b.start_time, b.end_time,
                       b.field_id, b.total_amount AS field_fee, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       fc.complex_id, fc.complex_name, fc.address, fc.ward, fc.city,
                       lp.payment_status,
                       lp.payment_method_name
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                JOIN users u ON i.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                JOIN football_complexes fc ON b.complex_id = fc.complex_id
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
                WHERE i.invoice_id = ?
                  AND i.customer_id = ?
                  AND i.status IN ('PENDING', 'PAID')
                ORDER BY i.issued_at DESC, i.invoice_id DESC
                """;
        // Query invoice theo customer để chặn xem/thanh toán hóa đơn của tài khoản khác.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ps.setLong(2, customerId);
            // Trả null khi không có invoice checkout hợp lệ cho Customer.
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCheckoutInvoiceView(rs) : null;
            }
        }
    }

    /**
     * Tạo hoặc tái sử dụng payment PENDING cho hóa đơn checkout.
     * Invoice và booking được khóa để tránh Staff/Customer tạo hai giao dịch checkout cùng lúc.
     */
    public Payment createPendingCheckoutPayment(long invoiceId, long customerId, int paymentMethodId)
            throws SQLException {
        // SQL: Khóa invoice và booking checkout để tạo payment phần còn lại an toàn.
        String selectInvoice = """
                SELECT i.invoice_id,
                       i.booking_id,
                       i.customer_id,
                       i.total_amount,
                       i.status AS invoice_status,
                       b.status AS booking_status
                FROM invoices i WITH (UPDLOCK, HOLDLOCK)
                JOIN bookings b WITH (UPDLOCK, HOLDLOCK) ON i.booking_id = b.booking_id
                WHERE i.invoice_id = ?
                  AND i.customer_id = ?
                """;
        // SQL: Kiểm tra payment method ACTIVE và không phải CASH cho flow Customer online.
        String selectMethod = """
                SELECT payment_method_id, method_code
                FROM payment_methods
                WHERE payment_method_id = ?
                  AND status = 'ACTIVE'
                """;
        // SQL: Tìm checkout payment PENDING/SUCCESS để reuse hoặc chặn tạo mới.
        String selectExistingPayment = """
                SELECT TOP 1 p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.payment_method_id,
                       p.amount,
                       p.payment_type,
                       p.status,
                       p.transaction_ref,
                       p.gateway_transaction_id,
                       p.paid_at,
                       p.created_at
                FROM payments p WITH (UPDLOCK, HOLDLOCK)
                WHERE p.booking_id = ?
                  AND p.customer_id = ?
                  AND p.payment_type = 'CHECKOUT'
                  AND p.status IN ('PENDING', 'SUCCESS')
                ORDER BY p.created_at DESC, p.payment_id DESC
                """;
        // SQL: Insert payment CHECKOUT PENDING cho hóa đơn checkout.
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
                OUTPUT INSERTED.payment_id,
                       INSERTED.booking_id,
                       INSERTED.customer_id,
                       INSERTED.payment_method_id,
                       INSERTED.amount,
                       INSERTED.payment_type,
                       INSERTED.status,
                       INSERTED.transaction_ref,
                       INSERTED.gateway_transaction_id,
                       INSERTED.paid_at,
                       INSERTED.created_at
                VALUES (?, ?, ?, ?, 'CHECKOUT', 'PENDING', ?, GETDATE())
                """;

        // Transaction checkout khóa invoice/booking/payment để tránh double submit.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            // Mọi lỗi trong lúc tạo/reuse payment checkout đều rollback.
            try {
                // Amount checkout lấy từ invoice PENDING trong DB; trạng thái booking cũng được xác minh tại đây.
                long bookingId;
                BigDecimal amount;
                // Lock invoice và booking để Staff/Customer không cập nhật song song.
                try (PreparedStatement ps = conn.prepareStatement(selectInvoice)) {
                    ps.setLong(1, invoiceId);
                    ps.setLong(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Invoice phải thuộc đúng Customer và tồn tại trong DB.
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Khong tim thay hoa don checkout hop le.");
                        }
                        // Business Rule BR-20: Chỉ invoice PENDING mới được tạo payment checkout.
                        if (!STATUS_PENDING.equals(rs.getString("invoice_status"))) {
                            throw new IllegalArgumentException("Hoa don nay khong con o trang thai cho thanh toan.");
                        }
                        String bookingStatus = rs.getString("booking_status");
                        // Business Rule BR-20: Booking phải đang chờ thanh toán checkout hoặc còn CHECKED_IN theo luồng hiện tại.
                        if (!STATUS_PENDING_CHECKOUT_PAYMENT.equals(bookingStatus)
                                && !"CHECKED_IN".equals(bookingStatus)) {
                            throw new IllegalArgumentException("Booking khong con hop le de thanh toan checkout.");
                        }
                        bookingId = rs.getLong("booking_id");
                        amount = rs.getBigDecimal("total_amount");
                    }
                }

                // Amount checkout luôn lấy từ invoice; null/không dương là dữ liệu sai.
                if (amount == null || amount.signum() <= 0) {
                    throw new IllegalArgumentException("So tien hoa don checkout khong hop le.");
                }

                // Validate method ACTIVE và không phải CASH trước khi tạo online payment.
                try (PreparedStatement ps = conn.prepareStatement(selectMethod)) {
                    ps.setInt(1, paymentMethodId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Method không hợp lệ thì không insert payment checkout.
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
                        }
                        // CASH chỉ được Staff ghi nhận tại quầy, không qua form online Customer.
                        if (METHOD_CASH.equalsIgnoreCase(rs.getString("method_code"))) {
                            throw new IllegalArgumentException("Phuong thuc tien mat chi duoc Staff ghi nhan tai quay Check-out.");
                        }
                    }
                }

                // Business Rule BR-23: Callback hoặc submit lặp sẽ dùng lại payment PENDING; payment đã SUCCESS thì chặn tạo mới.
                try (PreparedStatement ps = conn.prepareStatement(selectExistingPayment)) {
                    ps.setLong(1, bookingId);
                    ps.setLong(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Reuse payment đang PENDING; nếu đã SUCCESS thì báo invoice đã thanh toán.
                        if (rs.next()) {
                            Payment existing = mapPayment(rs);
                            // Payment checkout SUCCESS nghĩa là invoice đã được xử lý, không tạo lại.
                            if (STATUS_SUCCESS.equals(existing.getStatus())) {
                                throw new IllegalArgumentException("Hoa don checkout da duoc thanh toan.");
                            }
                            updatePendingPaymentMethod(conn, existing.getPaymentId(), paymentMethodId, amount);
                            existing.setPaymentMethodId(paymentMethodId);
                            existing.setAmount(amount);
                            conn.commit();
                            return existing;
                        }
                    }
                }

                String transactionRef = generateTransactionRef();
                Payment payment;
                // Chưa có payment checkout sống thì tạo PENDING mới cho invoice này.
                try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
                    ps.setLong(1, bookingId);
                    ps.setLong(2, customerId);
                    ps.setInt(3, paymentMethodId);
                    ps.setBigDecimal(4, amount);
                    ps.setString(5, transactionRef);
                    try (ResultSet rs = ps.executeQuery()) {
                        // OUTPUT INSERTED không trả row thì insert thất bại.
                        if (!rs.next()) {
                            throw new SQLException("Khong tao duoc giao dich thanh toan checkout.");
                        }
                        payment = mapPayment(rs);
                    }
                }

                conn.commit();
                return payment;
            } catch (SQLException | RuntimeException e) {
                // Checkout payment phải rollback nếu update method/insert gặp lỗi.
                rollback(conn, e);
                throw e;
            }
        }
    }

    private void updatePendingPaymentMethod(
            Connection conn,
            Long paymentId,
            int paymentMethodId,
            BigDecimal amount
    ) throws SQLException {
        // paymentId null nghĩa là không có payment PENDING để cập nhật lại method.
        if (paymentId == null) {
            throw new SQLException("Khong tim thay payment checkout dang cho.");
        }
        // SQL: Cập nhật method/amount cho payment checkout PENDING khi Customer chọn lại phương thức.
        String sql = """
                UPDATE payments
                SET payment_method_id = ?,
                    amount = ?
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;
        // Chỉ update đúng một row PENDING để tránh đổi method của payment đã xử lý.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentMethodId);
            ps.setBigDecimal(2, amount);
            ps.setLong(3, paymentId);
            // executeUpdate khác 1 là payment không còn ở trạng thái được phép sửa.
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc phuong thuc thanh toan checkout.");
            }
        }
    }

    /**
     * Tạo payment PENDING cho gói membership VIP.
     * Nếu Customer đã có payment membership PENDING thì tái sử dụng để không sinh nhiều transaction đang chờ.
     */
    public Payment createPendingMembershipPayment(long customerId, int paymentMethodId, BigDecimal amount) throws SQLException {
        // SQL: Kiểm tra payment method ACTIVE và không phải CASH cho flow membership online.
        String selectMethod = """
                SELECT payment_method_id, method_code
                FROM payment_methods
                WHERE payment_method_id = ?
                  AND status = 'ACTIVE'
                """;
        // SQL: Tìm payment membership PENDING/SUCCESS gần nhất để reuse hoặc quyết định tạo mới.
        String selectExistingPayment = """
                SELECT TOP 1 p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.payment_method_id,
                       p.amount,
                       p.payment_type,
                       p.status,
                       p.transaction_ref,
                       p.gateway_transaction_id,
                       p.paid_at,
                       p.created_at
                FROM payments p WITH (UPDLOCK, HOLDLOCK)
                WHERE p.customer_id = ?
                  AND p.payment_type = 'MEMBERSHIP'
                  AND p.status IN ('PENDING', 'SUCCESS')
                ORDER BY p.created_at DESC, p.payment_id DESC
                """;
        // SQL: Insert payment MEMBERSHIP PENDING không gắn booking.
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
                OUTPUT INSERTED.payment_id,
                       INSERTED.booking_id,
                       INSERTED.customer_id,
                       INSERTED.payment_method_id,
                       INSERTED.amount,
                       INSERTED.payment_type,
                       INSERTED.status,
                       INSERTED.transaction_ref,
                       INSERTED.gateway_transaction_id,
                       INSERTED.paid_at,
                       INSERTED.created_at
                VALUES (NULL, ?, ?, ?, 'MEMBERSHIP', 'PENDING', ?, GETDATE())
                """;

        // Membership payment cũng dùng transaction để reuse PENDING hoặc tạo transactionRef mới.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            // Rollback nếu validate method hoặc insert payment membership lỗi.
            try {
                // Giá membership phải do server truyền vào và lớn hơn 0.
                if (amount == null || amount.signum() <= 0) {
                    throw new IllegalArgumentException("So tien khong hop le.");
                }

                // Chỉ cho dùng payment method ACTIVE và không phải CASH cho flow Customer online.
                try (PreparedStatement ps = conn.prepareStatement(selectMethod)) {
                    ps.setInt(1, paymentMethodId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Method id không tồn tại hoặc inactive thì chặn tạo payment.
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
                        }
                        // CASH không đi qua flow online membership.
                        if (METHOD_CASH.equalsIgnoreCase(rs.getString("method_code"))) {
                            throw new IllegalArgumentException("Phuong thuc tien mat chi duoc Staff ghi nhan tai quay Check-out.");
                        }
                    }
                }

                // Tìm payment membership gần nhất để reuse nếu vẫn đang PENDING.
                try (PreparedStatement ps = conn.prepareStatement(selectExistingPayment)) {
                    ps.setLong(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Có payment cũ thì kiểm tra trạng thái trước khi quyết định tạo mới.
                        if (rs.next()) {
                            Payment existing = mapPayment(rs);
                            // SUCCESS được bỏ qua để Customer có thể mua/gia hạn thêm gói mới.
                            if (STATUS_SUCCESS.equals(existing.getStatus())) {
                                // Already bought membership, user should wait or we just create a new one.
                                // Actually, let's just create a new one since they can extend.
                                // But if it's PENDING, we can reuse it.
                            // PENDING được tái sử dụng để không tạo nhiều giao dịch membership đang chờ.
                            } else if (STATUS_PENDING.equals(existing.getStatus())) {
                                conn.commit();
                                return existing;
                            }
                        }
                    }
                }

                String transactionRef = generateTransactionRef();
                Payment payment;
                // Không có membership PENDING thì tạo payment mới.
                try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
                    ps.setLong(1, customerId);
                    ps.setInt(2, paymentMethodId);
                    ps.setBigDecimal(3, amount);
                    ps.setString(4, transactionRef);
                    try (ResultSet rs = ps.executeQuery()) {
                        // Insert phải trả lại row để controller lấy transactionRef redirect/result.
                        if (!rs.next()) {
                            throw new SQLException("Khong tao duoc giao dich thanh toan membership.");
                        }
                        payment = mapPayment(rs);
                    }
                }

                conn.commit();
                return payment;
            } catch (SQLException | RuntimeException e) {
                // Không để payment membership dở dang nếu bất kỳ bước nào lỗi.
                rollback(conn, e);
                throw e;
            }
        }
    }

    /**
     * Hoàn tất payment membership: cập nhật payment SUCCESS, gia hạn VIP và gửi notification trong cùng transaction.
     */
    private PaymentUpdateResult markMembershipPaymentSuccess(
            Connection conn,
            PaymentLock payment,
            String transactionRef,
            String gatewayTransactionId,
            String rawPayload,
            String signature,
            String gatewayCode
    ) throws SQLException {
        // SQL: Cập nhật payment membership từ PENDING sang SUCCESS khi gateway xác nhận.
        String updatePayment = """
                UPDATE payments
                SET status = 'SUCCESS',
                    gateway_transaction_id = ?,
                    paid_at = GETDATE()
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;
        // Update payment membership từ PENDING sang SUCCESS trước khi gia hạn VIP.
        try (PreparedStatement ps = conn.prepareStatement(updatePayment)) {
            ps.setString(1, gatewayTransactionId);
            ps.setLong(2, payment.paymentId());
            // Nếu không update đúng một row thì payment không còn ở trạng thái hợp lệ.
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc trang thai thanh toan.");
            }
        }

        // Grant 30 days of VIP
        User user = userDAO.getUserById(payment.customerId()).orElse(null);
        // Chỉ gia hạn VIP khi user vẫn tồn tại.
        if (user != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime newExpiration;
            // User còn VIP thì cộng tiếp 30 ngày từ hạn cũ.
            if (user.isVip() && user.getVipValidUntil() != null && user.getVipValidUntil().isAfter(now)) {
                newExpiration = user.getVipValidUntil().plusDays(30);
            } else {
                // User chưa VIP hoặc hết hạn thì bắt đầu 30 ngày từ hiện tại.
                newExpiration = now.plusDays(30);
            }
            userDAO.updateVipStatus(user.getUserId(), newExpiration);

            // Notify customer
            insertNotification(conn, payment.customerId(), "Đăng ký thành công", "Bạn đã đăng ký thành công Gói Hội Viên VIP và nhận 30 ngày sử dụng đặc quyền.", "SYSTEM", null);

            // Notify admins, owners, staff
            // SQL: Lấy danh sách admin/owner/staff active để gửi notification hội viên mới.
            String notifySql = """
                    SELECT u.user_id 
                    FROM users u 
                    INNER JOIN roles r ON u.role_id = r.role_id 
                    WHERE r.role_name IN ('ADMIN', 'OWNER', 'STAFF') 
                      AND u.status = 'ACTIVE'
                    """;
            // Gửi thông báo nội bộ cho các vai trò cần biết có hội viên mới.
            try (PreparedStatement psNotify = conn.prepareStatement(notifySql);
                 ResultSet rsNotify = psNotify.executeQuery()) {
                // Mỗi admin/owner/staff active nhận một notification riêng.
                while (rsNotify.next()) {
                    long staffId = rsNotify.getLong("user_id");
                    insertNotification(conn, staffId, "Hội viên mới", "Khách hàng " + user.getFullName() + " vừa đăng ký mới Gói Hội Viên VIP.", "SYSTEM", null);
                }
            }
        }

        insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
        return PaymentUpdateResult.UPDATED_SUCCESS;
    }

    /**
     * Lấy payment theo transactionRef để IPN đối chiếu số tiền trước khi cập nhật trạng thái.
     */
    public GatewayPaymentView findPaymentByTransactionRef(String transactionRef) throws SQLException {
        // Callback không có transactionRef thì không thể đối chiếu payment.
        if (transactionRef == null || transactionRef.isBlank()) {
            return null;
        }

        // SQL: Lookup payment theo transactionRef để Return/IPN đối chiếu amount và trạng thái.
        String sql = """
                SELECT p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.amount,
                       p.status,
                       p.transaction_ref,
                       p.gateway_transaction_id,
                       b.status AS booking_status
                FROM payments p
                LEFT JOIN bookings b ON p.booking_id = b.booking_id
                WHERE p.transaction_ref = ?
                """;

        // Lookup payment phục vụ IPN kiểm amount/trạng thái trước khi cập nhật.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
                // Không có transactionRef trong DB thì VNPay nhận Order not found.
                if (!rs.next()) {
                    return null;
                }
                return new GatewayPaymentView(
                        rs.getLong("payment_id"),
                        rs.getLong("booking_id"),
                        rs.getLong("customer_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("status"),
                        rs.getString("transaction_ref"),
                        rs.getString("gateway_transaction_id"),
                        rs.getString("booking_status")
                );
            }
        }
    }

    /**
     * Lưu callback gateway kể cả khi chữ ký không hợp lệ, miễn là tìm được transactionRef.
     * Việc lưu payload giúp audit và debug các lần callback lỗi.
     */
    public boolean savePaymentCallbackByTransactionRef(
            String transactionRef,
            String gatewayCode,
            String rawPayload,
            String signature,
            boolean valid
    ) throws SQLException {
        // Không lưu callback nếu không biết payment nào nhận payload này.
        if (transactionRef == null || transactionRef.isBlank()) {
            return false;
        }

        // Lưu callback trong transaction riêng để audit kể cả khi signature sai.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long paymentId = findPaymentIdByTransactionRef(conn, transactionRef);
                // Không có payment thì commit rỗng và báo false cho caller.
                if (paymentId == null) {
                    conn.commit();
                    return false;
                }
                insertCallback(conn, paymentId, rawPayload, signature, valid, gatewayCode);
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                // Lỗi insert callback cũng rollback để không lưu bản ghi thiếu.
                rollback(conn, e);
                throw e;
            }
        }
    }

    public boolean markPaymentSuccessAndConfirmBooking(
            String transactionRef,
            String gatewayTransactionId,
            String rawPayload,
            String signature
    ) throws SQLException {
        PaymentUpdateResult result = markPaymentSuccessAndConfirmBooking(
                transactionRef,
                gatewayTransactionId,
                rawPayload,
                signature,
                GATEWAY_SIMULATED
        );
        return result == PaymentUpdateResult.UPDATED_SUCCESS
                || result == PaymentUpdateResult.ALREADY_SUCCESS;
    }

    /**
     * Xử lý callback thành công cho mọi loại payment.
     * Method idempotent: payment đã SUCCESS/FAILED chỉ ghi thêm callback và trả trạng thái đã xử lý.
     */
    public PaymentUpdateResult markPaymentSuccessAndConfirmBooking(
            String transactionRef,
            String gatewayTransactionId,
            String rawPayload,
            String signature,
            String gatewayCode
    ) throws SQLException {
        // SQL: Khóa payment, booking và invoice liên quan để xử lý callback success idempotent.
        String selectPayment = """
                SELECT p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.amount,
                       p.payment_type,
                       p.status AS payment_status,
                       b.status AS booking_status,
                       b.field_id,
                       b.recurring_group_id,
                       ci.invoice_id,
                       ci.invoice_status,
                       ci.checkout_staff_id,
                       scope.booking_count,
                       CASE
                           WHEN scope.invalid_booking_count = 0
                                AND scope.hold_expired_count = 0
                           THEN 1
                           ELSE 0
                       END AS hold_valid
                FROM payments p WITH (UPDLOCK, HOLDLOCK)
                LEFT JOIN bookings b WITH (UPDLOCK, HOLDLOCK) ON p.booking_id = b.booking_id
                OUTER APPLY (
                    SELECT TOP 1 i.invoice_id,
                           i.status AS invoice_status,
                           i.staff_id AS checkout_staff_id
                    FROM invoices i WITH (UPDLOCK, HOLDLOCK)
                    WHERE i.booking_id = p.booking_id
                      AND i.customer_id = p.customer_id
                      AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                    ORDER BY i.issued_at DESC, i.invoice_id DESC
                ) ci
                OUTER APPLY (
                    SELECT COUNT(*) AS booking_count,
                           SUM(CASE WHEN sb.status <> 'HOLD' THEN 1 ELSE 0 END) AS invalid_booking_count,
                           SUM(CASE WHEN sb.hold_expires_at > GETDATE() THEN 0 ELSE 1 END) AS hold_expired_count
                    FROM bookings sb WITH (UPDLOCK, HOLDLOCK)
                    WHERE sb.customer_id = b.customer_id
                      AND (
                          (b.recurring_group_id IS NOT NULL AND sb.recurring_group_id = b.recurring_group_id)
                          OR (b.recurring_group_id IS NULL AND sb.booking_id = b.booking_id)
                      )
                ) scope
                WHERE p.transaction_ref = ?
                """;
        // SQL: Xác nhận booking HOLD thành CONFIRMED sau payment đặt cọc thành công.
        String updateBooking = """
                UPDATE bookings
                SET status = 'CONFIRMED', updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status = 'HOLD'
                  AND hold_expires_at > GETDATE()
                """;
        // SQL: Cập nhật payment từ PENDING sang SUCCESS và lưu mã giao dịch gateway.
        String updatePayment = """
                UPDATE payments
                SET status = 'SUCCESS',
                    gateway_transaction_id = ?,
                    paid_at = GETDATE()
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;
        // Callback success được xử lý trong transaction để payment/booking/voucher cùng commit.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PaymentLock payment = getPaymentLock(conn, selectPayment, transactionRef);
                // Không thấy payment tương ứng thì trả NOT_FOUND cho Return/IPN.
                if (payment == null) {
                    conn.commit();
                    return PaymentUpdateResult.NOT_FOUND;
                }
                // Business Rule BR-23: Callback thành công lặp lại không đổi trạng thái lần nữa, chỉ lưu payload để audit.
                if (STATUS_SUCCESS.equals(payment.paymentStatus())) {
                    // Callback có thể tới nhiều lần; giữ nguyên trạng thái đã thành công và chỉ lưu payload mới.
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_SUCCESS;
                }
                // Business Rule BR-23: Callback muộn cho payment đã FAILED không được đảo trạng thái booking/invoice.
                if (STATUS_FAILED.equals(payment.paymentStatus())) {
                    // Nếu giao dịch đã thất bại trước đó, không đổi lại booking/invoice bằng một callback muộn.
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_FAILED;
                }
                // Business Rule BR-21: Checkout payment thành công chuyển sang nhánh cập nhật invoice PAID và booking COMPLETED.
                if (PAYMENT_TYPE_CHECKOUT.equals(payment.paymentType())) {
                    PaymentUpdateResult result = markCheckoutPaymentSuccess(
                            conn,
                            payment,
                            transactionRef,
                            gatewayTransactionId,
                            rawPayload,
                            signature,
                            gatewayCode
                    );
                    conn.commit();
                    return result;
                }
                // Membership success không cần booking/invoice mà gia hạn VIP cho Customer.
                if (PAYMENT_TYPE_MEMBERSHIP.equals(payment.paymentType())) {
                    PaymentUpdateResult result = markMembershipPaymentSuccess(
                            conn,
                            payment,
                            transactionRef,
                            gatewayTransactionId,
                            rawPayload,
                            signature,
                            gatewayCode
                    );
                    conn.commit();
                    return result;
                }
                // Business Rule BR-08: Chỉ payment PENDING của booking HOLD còn hạn mới được xác nhận sang CONFIRMED.
                if (!STATUS_PENDING.equals(payment.paymentStatus())
                        || !STATUS_HOLD.equals(payment.bookingStatus())
                        || !payment.holdValid()) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.INVALID_STATE;
                }

                // Business Rule BR-08: Với booking định kỳ, một payment đặt cọc xác nhận toàn bộ booking con còn HOLD trong cùng group.
                List<Long> bookingIds = getScopedBookingIds(conn, payment);
                if (bookingIds.isEmpty() || bookingIds.size() != payment.bookingCount()) {
                    conn.commit();
                    return PaymentUpdateResult.INVALID_STATE;
                }

                // Business Rule BR-08: Thanh toán thành công cập nhật từng booking HOLD sang CONFIRMED.
                for (Long bookingId : bookingIds) {
                    // Update từng booking trong scope; chỉ cần một booking lỗi là rollback toàn bộ.
                    try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
                        ps.setLong(1, bookingId);
                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("Booking khong con hop le de xac nhan thanh toan.");
                        }
                    }
                }
                // Business Rule BR-25: Payment sau callback thành công được chuyển từ PENDING sang SUCCESS.
                try (PreparedStatement ps = conn.prepareStatement(updatePayment)) {
                    ps.setString(1, gatewayTransactionId);
                    ps.setLong(2, payment.paymentId());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Khong cap nhat duoc trang thai thanh toan.");
                    }
                }
                // Business Rule BR-11: Ghi nhận voucher và payment success chung transaction để tránh booking CONFIRMED nhưng voucher chưa trừ lượt.
                recordVoucherUsage(conn, bookingIds, payment);
                insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);

                conn.commit();
                
                // Notification gửi sau commit để lỗi thông báo không rollback payment đã thành công.
                try {
                    com.swp.dao.BookingDAO bookingDAO = new com.swp.dao.BookingDAO();
                    com.swp.dao.NotificationDAO notificationDAO = new com.swp.dao.NotificationDAO();
                    // Gửi notification cho từng booking được xác nhận trong payment này.
                    for (Long id : bookingIds) {
                        com.swp.model.dto.BookingView bv = bookingDAO.getBookingDetailByIdAndCustomerId(id, payment.customerId());
                        // Nếu booking không còn đọc được sau commit thì bỏ qua notification của booking đó.
                        if (bv != null) {
                            String timeStr = bv.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
                            com.swp.model.Notification notif = new com.swp.model.Notification();
                            notif.setUserId(payment.customerId());
                            notif.setTitle("Đặt sân thành công");
                            notif.setMessage("Bạn đã thanh toán thành công và đặt sân " + bv.getFieldName() + " (" + bv.getComplexName() + ") lúc " + timeStr + ". Mã giao dịch: " + transactionRef);
                            notif.setNotificationType("BOOKING");
                            notif.setReferenceId(id);
                            notificationDAO.insertNotification(notif);

                            String staffMsg = "Khách hàng " + bv.getCustomerName() + " đã đặt sân " + bv.getFieldName() + " (" + bv.getComplexName() + ") lúc " + timeStr + ". Mã giao dịch: " + transactionRef;
                            notificationDAO.notifyRole("OWNER", "Có khách hàng đặt sân mới", staffMsg, "BOOKING", id);
                            notificationDAO.notifyRole("STAFF", "Có khách hàng đặt sân mới", staffMsg, "BOOKING", id);
                        }
                    }
                } catch (Exception e) {
                    // Notification lỗi không được làm fail giao dịch đã commit.
                    e.printStackTrace();
                }

                return PaymentUpdateResult.UPDATED_SUCCESS;
            } catch (SQLException | RuntimeException e) {
                // Nếu success flow lỗi trước commit thì rollback payment/booking/voucher.
                rollback(conn, e);
                throw e;
            }
        }
    }

    /**
     * Hoàn tất payment checkout: payment SUCCESS, invoice PAID, booking COMPLETED,
     * trả sân về AVAILABLE và gửi notification cho Customer trong cùng transaction.
     */
    private PaymentUpdateResult markCheckoutPaymentSuccess(
            Connection conn,
            PaymentLock payment,
            String transactionRef,
            String gatewayTransactionId,
            String rawPayload,
            String signature,
            String gatewayCode
    ) throws SQLException {
        // Business Rule BR-21: Chỉ payment checkout PENDING của invoice PENDING mới được hoàn tất.
        if (!STATUS_PENDING.equals(payment.paymentStatus())
                || payment.invoiceId() == null
                || !STATUS_PENDING.equals(payment.invoiceStatus())
                || (!STATUS_PENDING_CHECKOUT_PAYMENT.equals(payment.bookingStatus())
                    && !"CHECKED_IN".equals(payment.bookingStatus()))) {
            insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
            return PaymentUpdateResult.INVALID_STATE;
        }

        // SQL: Cập nhật payment checkout từ PENDING sang SUCCESS.
        String updatePayment = """
                UPDATE payments
                SET status = 'SUCCESS',
                    gateway_transaction_id = ?,
                    paid_at = GETDATE()
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;
        // SQL: Cập nhật invoice checkout thành PAID với số tiền đã thanh toán.
        String updateInvoice = """
                UPDATE invoices
                SET status = 'PAID',
                    paid_amount = ?,
                    total_amount = ?,
                    issued_at = COALESCE(issued_at, GETDATE())
                WHERE invoice_id = ?
                  AND status = 'PENDING'
                """;
        // SQL: Hoàn tất booking sau khi checkout payment thành công.
        String updateBooking = """
                UPDATE bookings
                SET status = 'COMPLETED',
                    updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status IN ('PENDING_CHECKOUT_PAYMENT', 'CHECKED_IN')
                """;
        // SQL: Trả sân về AVAILABLE sau checkout nếu sân không maintenance/disabled.
        String releaseField = """
                UPDATE fields
                SET status = 'AVAILABLE',
                    updated_at = GETDATE()
                WHERE field_id = ?
                  AND status NOT IN ('MAINTENANCE', 'DISABLED')
                """;
        // Business Rule BR-25: Payment checkout thành công được cập nhật sang SUCCESS.
        try (PreparedStatement ps = conn.prepareStatement(updatePayment)) {
            ps.setString(1, gatewayTransactionId);
            ps.setLong(2, payment.paymentId());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc checkout payment.");
            }
        }
        // Business Rule BR-21: Checkout payment thành công cập nhật invoice sang PAID.
        try (PreparedStatement ps = conn.prepareStatement(updateInvoice)) {
            ps.setBigDecimal(1, payment.amount());
            ps.setBigDecimal(2, payment.amount());
            ps.setLong(3, payment.invoiceId());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc hoa don checkout.");
            }
        }
        // Business Rule BR-21: Checkout payment thành công hoàn tất booking và giải phóng sân.
        try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
            ps.setLong(1, payment.bookingId());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Khong cap nhat duoc booking sau checkout payment.");
            }
        }
        // Release sân là best-effort trong transaction, không yêu cầu đúng một row vì sân có thể đang maintenance/disabled.
        try (PreparedStatement ps = conn.prepareStatement(releaseField)) {
            ps.setLong(1, payment.fieldId());
            ps.executeUpdate();
        }
        userDAO.awardRewardPoints(conn, payment.customerId, payment.bookingId);

        insertNotification(conn,
                payment.customerId(),
                "Thanh toán hóa đơn thành công",
                "Hóa đơn checkout của booking đã được thanh toán thành công. Mã giao dịch: " + transactionRef,
                "CHECKOUT_PAYMENT_SUCCESS",
                payment.invoiceId());
        // Nếu Staff là người tạo checkout request thì báo lại khi Customer đã trả tiền.
        if (payment.checkoutStaffId() != null) {
            insertNotification(conn,
                    payment.checkoutStaffId(),
                    "Khách đã thanh toán checkout",
                    "Khách hàng đã thanh toán thành công " + payment.amount()
                            + " cho booking #" + payment.bookingId() + ".",
                    "CHECKOUT_PAYMENT_SUCCESS",
                    payment.invoiceId());
        }
        insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
        return PaymentUpdateResult.UPDATED_SUCCESS;
    }

    public boolean markPaymentFailed(String transactionRef, String rawPayload, String signature)
            throws SQLException {
        PaymentUpdateResult result = markPaymentFailed(
                transactionRef,
                rawPayload,
                signature,
                GATEWAY_SIMULATED
        );
        return result == PaymentUpdateResult.UPDATED_FAILED
                || result == PaymentUpdateResult.ALREADY_FAILED;
    }

    /**
     * Đánh dấu payment thất bại theo transactionRef.
     * Method cũng idempotent để callback thất bại lặp lại không làm thay đổi payment đã xử lý.
     */
    public PaymentUpdateResult markPaymentFailed(
            String transactionRef,
            String rawPayload,
            String signature,
            String gatewayCode
    ) throws SQLException {
        // SQL: Khóa payment theo transactionRef để xử lý callback failed idempotent.
        String selectPayment = """
                SELECT p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.amount,
                       p.payment_type,
                       p.status AS payment_status,
                       b.status AS booking_status,
                       b.field_id,
                       b.recurring_group_id,
                       CAST(NULL AS BIGINT) AS invoice_id,
                       CAST(NULL AS VARCHAR(50)) AS invoice_status,
                       CAST(NULL AS BIGINT) AS checkout_staff_id,
                       1 AS booking_count,
                       CASE WHEN b.hold_expires_at > GETDATE() THEN 1 ELSE 0 END AS hold_valid
                FROM payments p WITH (UPDLOCK, HOLDLOCK)
                LEFT JOIN bookings b ON p.booking_id = b.booking_id
                WHERE p.transaction_ref = ?
                """;
        // SQL: Cập nhật payment PENDING sang FAILED khi gateway báo thất bại.
        String updatePayment = """
                UPDATE payments
                SET status = 'FAILED'
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;

        // Payment failed cũng xử lý trong transaction để callback audit đi kèm status update.
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PaymentLock payment = getPaymentLock(conn, selectPayment, transactionRef);
                // Không có payment thì báo NOT_FOUND cho gateway/caller.
                if (payment == null) {
                    conn.commit();
                    return PaymentUpdateResult.NOT_FOUND;
                }
                // Business Rule BR-23: Callback thất bại lặp lại chỉ lưu callback, không đổi thêm trạng thái.
                if (STATUS_FAILED.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_FAILED;
                }
                // Business Rule BR-23: Payment đã SUCCESS không bị đổi ngược thành FAILED bởi callback muộn.
                if (STATUS_SUCCESS.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_SUCCESS;
                }
                // Chỉ payment PENDING mới được chuyển sang FAILED.
                if (!STATUS_PENDING.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.INVALID_STATE;
                }

                // Business Rule BR-25: Payment thất bại được đánh dấu FAILED khi còn PENDING.
                try (PreparedStatement ps = conn.prepareStatement(updatePayment)) {
                    ps.setLong(1, payment.paymentId());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Khong cap nhat duoc giao dich that bai.");
                    }
                }
                insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);

                conn.commit();
                return PaymentUpdateResult.UPDATED_FAILED;
            } catch (SQLException | RuntimeException e) {
                // Rollback status FAILED và callback nếu update thất bại.
                rollback(conn, e);
                throw e;
            }
        }
    }

    private PaymentMethod mapPaymentMethod(ResultSet rs) throws SQLException {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId(rs.getInt("payment_method_id"));
        method.setMethodCode(rs.getString("method_code"));
        method.setMethodName(rs.getString("method_name"));
        method.setStatus(rs.getString("status"));
        return method;
    }

    public PaymentView getPaymentResult(String transactionRef, long customerId) throws SQLException {
        // SQL: Lấy kết quả payment theo transactionRef và customerId để bảo vệ ownership.
        String sql = paymentViewSql() + """
                WHERE p.transaction_ref = ?
                  AND p.customer_id = ?
                """;

        // Result theo customerId để Customer không xem được giao dịch của người khác.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            ps.setLong(2, customerId);
            // Không có giao dịch hợp lệ thì controller sẽ trả 404.
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPaymentView(rs) : null;
            }
        }
    }

    public PaymentView getPaymentResultByTransactionRef(String transactionRef) throws SQLException {
        // SQL: Lấy kết quả payment theo transactionRef cho trang Return sau gateway redirect.
        String sql = paymentViewSql() + """
                WHERE p.transaction_ref = ?
                """;

        // Lookup theo transactionRef dùng cho VNPay Return sau khi gateway redirect về.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            // Return page có thể được mở ngay sau callback nên không filter session ở DAO này.
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPaymentView(rs) : null;
            }
        }
    }

    public List<PaymentView> getPaymentHistory(long customerId) throws SQLException {
        // SQL: Lấy lịch sử payment của Customer theo thứ tự mới nhất.
        String sql = paymentViewSql() + """
                WHERE p.customer_id = ?
                ORDER BY p.created_at DESC, p.paid_at DESC, p.payment_id DESC
        """;
        List<PaymentView> payments = new ArrayList<>();

        // Lấy lịch sử payment theo customer hiện tại.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                // Map từng giao dịch để JSP render bảng lịch sử.
                while (rs.next()) {
                    payments.add(mapPaymentView(rs));
                }
            }
        }

        return payments;
    }

    public List<CheckoutPaymentRequestView> getPendingCheckoutPaymentRequests(long customerId) throws SQLException {
        // SQL: Lấy các yêu cầu thanh toán checkout đang PENDING của Customer.
        String sql = """
                SELECT p.payment_id,
                       ci.invoice_id,
                       p.booking_id,
                       b.booking_code,
                       fa.complex_name,
                       f.field_name,
                       b.start_time,
                       b.end_time,
                       ci.subtotal AS checkout_total_amount,
                       paid.paid_amount,
                       p.amount AS remaining_amount,
                       p.status,
                       p.created_at
                FROM payments p
                INNER JOIN bookings b ON p.booking_id = b.booking_id
                INNER JOIN football_complexes fa ON b.complex_id = fa.complex_id
                INNER JOIN fields f ON b.field_id = f.field_id
                OUTER APPLY (
                    SELECT TOP 1 i.invoice_id,
                           i.subtotal
                    FROM invoices i
                    WHERE i.booking_id = p.booking_id
                      AND i.customer_id = p.customer_id
                      AND i.status = 'PENDING'
                    ORDER BY i.issued_at DESC, i.invoice_id DESC
                ) ci
                OUTER APPLY (
                    SELECT COALESCE(SUM(prev.amount), 0) AS paid_amount
                    FROM payments prev
                    WHERE prev.booking_id = p.booking_id
                      AND prev.customer_id = p.customer_id
                      AND prev.status = 'SUCCESS'
                ) paid
                WHERE p.customer_id = ?
                  AND p.payment_type = 'CHECKOUT'
                  AND p.status = 'PENDING'
                  AND ci.invoice_id IS NOT NULL
                  AND b.status = 'PENDING_CHECKOUT_PAYMENT'
                ORDER BY p.created_at DESC, p.payment_id DESC
        """;
        List<CheckoutPaymentRequestView> requests = new ArrayList<>();

        // API polling chỉ trả các checkout payment request đang PENDING.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                // Mỗi row là một yêu cầu Customer cần thanh toán online.
                while (rs.next()) {
                    CheckoutPaymentRequestView request = new CheckoutPaymentRequestView();
                    request.setPaymentRequestId(rs.getLong("payment_id"));
                    request.setInvoiceId(rs.getLong("invoice_id"));
                    request.setBookingId(rs.getLong("booking_id"));
                    request.setBookingCode(rs.getString("booking_code"));
                    request.setComplexName(rs.getString("complex_name"));
                    request.setFieldName(rs.getString("field_name"));
                    request.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
                    request.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    request.setCheckoutTotalAmount(rs.getBigDecimal("checkout_total_amount"));
                    request.setPaidAmount(rs.getBigDecimal("paid_amount"));
                    request.setRemainingAmount(rs.getBigDecimal("remaining_amount"));
                    request.setStatus(rs.getString("status"));
                    request.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    requests.add(request);
                }
            }
        }

        return requests;
    }

    private String paymentViewSql() {
        // SQL: Query view payment dùng chung cho result, return và history.
        return """
                SELECT p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.amount,
                       p.payment_type,
                       p.status,
                       p.transaction_ref,
                       p.gateway_transaction_id,
                       p.paid_at,
                       p.created_at,
                       ci.invoice_id,
                       CASE WHEN UPPER(pm.method_code) = 'CASH' THEN N'Tiền mặt' ELSE pm.method_name END AS payment_method_name,
                       b.booking_code,
                       b.status AS booking_status,
                       b.hold_expires_at,
                       b.start_time,
                       b.end_time,
                       fa.complex_name,
                       f.field_name
                FROM payments p
                INNER JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                LEFT JOIN bookings b ON p.booking_id = b.booking_id
                LEFT JOIN football_complexes fa ON b.complex_id = fa.complex_id
                LEFT JOIN fields f ON b.field_id = f.field_id
                OUTER APPLY (
                    SELECT TOP 1 i.invoice_id
                    FROM invoices i
                    WHERE i.booking_id = p.booking_id
                      AND i.customer_id = p.customer_id
                      AND p.payment_type = 'CHECKOUT'
                      AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                    ORDER BY i.issued_at DESC, i.invoice_id DESC
                ) ci
                """;
    }

    private BookingView mapBookingForPayment(ResultSet rs) throws SQLException {
        BookingView view = new BookingView();
        view.setBookingId(rs.getLong("booking_id"));
        view.setBookingCode(rs.getString("booking_code"));
        view.setRecurringGroupId(getLongOrNull(rs, "recurring_group_id"));
        view.setRepeatType(rs.getString("repeat_type"));
        view.setRecurringCount(rs.getInt("recurring_count"));
        view.setCustomerId(rs.getLong("customer_id"));
        view.setComplexId(rs.getLong("complex_id"));
        view.setComplexName(rs.getString("complex_name"));
        view.setFieldId(rs.getLong("field_id"));
        view.setFieldName(rs.getString("field_name"));
        view.setFieldTypeName(rs.getString("field_type_name"));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setTotalAmount(rs.getBigDecimal("total_amount"));
        view.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        view.setStatus(rs.getString("status"));
        view.setHoldExpiresAt(toLocalDateTime(rs.getTimestamp("hold_expires_at")));
        view.setPaymentStatus(rs.getString("payment_status"));
        view.setPaymentMethodName(rs.getString("payment_method_name"));
        view.setPaidAmount(rs.getBigDecimal("paid_amount"));
        view.setPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));
        return view;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getLong("payment_id"));
        payment.setBookingId(getLongOrNull(rs, "booking_id"));
        payment.setCustomerId(rs.getLong("customer_id"));
        payment.setPaymentMethodId(rs.getInt("payment_method_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setPaymentType(rs.getString("payment_type"));
        payment.setStatus(rs.getString("status"));
        payment.setTransactionRef(rs.getString("transaction_ref"));
        payment.setGatewayTransactionId(rs.getString("gateway_transaction_id"));
        payment.setPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));
        payment.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return payment;
    }

    private PaymentView mapPaymentView(ResultSet rs) throws SQLException {
        PaymentView view = new PaymentView();
        view.setPaymentId(rs.getLong("payment_id"));
        view.setInvoiceId(getLongOrNull(rs, "invoice_id"));
        view.setBookingId(getLongOrNull(rs, "booking_id"));
        view.setCustomerId(rs.getLong("customer_id"));
        view.setAmount(rs.getBigDecimal("amount"));
        view.setPaymentType(rs.getString("payment_type"));
        view.setStatus(rs.getString("status"));
        view.setTransactionRef(rs.getString("transaction_ref"));
        view.setGatewayTransactionId(rs.getString("gateway_transaction_id"));
        view.setPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));
        view.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        view.setPaymentMethodName(rs.getString("payment_method_name"));
        view.setBookingCode(rs.getString("booking_code"));
        view.setBookingStatus(rs.getString("booking_status"));
        view.setHoldExpiresAt(toLocalDateTime(rs.getTimestamp("hold_expires_at")));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setComplexName(rs.getString("complex_name"));
        view.setFieldName(rs.getString("field_name"));
        return view;
    }

    private InvoiceView mapCheckoutInvoiceView(ResultSet rs) throws SQLException {
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
        return view;
    }

    private PaymentLock getPaymentLock(Connection conn, String sql, String transactionRef)
            throws SQLException {
        // Lock payment theo transactionRef để callback song song không cập nhật trùng.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
                // Không có payment lock thì flow callback trả NOT_FOUND.
                if (!rs.next()) {
                    return null;
                }
                return new PaymentLock(
                        rs.getLong("payment_id"),
                        rs.getLong("booking_id"),
                        rs.getLong("customer_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("payment_type"),
                        rs.getLong("field_id"),
                        getLongOrNull(rs, "invoice_id"),
                        rs.getString("invoice_status"),
                        getLongOrNull(rs, "recurring_group_id"),
                        rs.getInt("booking_count"),
                        rs.getString("payment_status"),
                        rs.getString("booking_status"),
                        getLongOrNull(rs, "checkout_staff_id"),
                        rs.getInt("hold_valid") == 1
                );
            }
        }
    }

    /**
     * Lấy danh sách booking cần xác nhận cho payment đặt cọc.
     * Booking đơn chỉ có một id, booking recurring lấy toàn bộ id trong cùng group của Customer.
     */
    private List<Long> getScopedBookingIds(Connection conn, PaymentLock payment) throws SQLException {
        // SQL: Sẽ được gán theo booking đơn hoặc recurring group để lấy scope cần xác nhận.
        String sql;
        // Booking đơn chỉ cần query đúng booking_id của payment.
        if (payment.recurringGroupId() == null) {
            // SQL: Lấy booking đơn còn HOLD và còn hạn để xác nhận sau payment đặt cọc.
            sql = """
                    SELECT booking_id
                    FROM bookings WITH (UPDLOCK, HOLDLOCK)
                    WHERE booking_id = ?
                      AND customer_id = ?
                      AND status = 'HOLD'
                      AND hold_expires_at > GETDATE()
                    ORDER BY booking_id
                    """;
        } else {
            // Booking định kỳ xác nhận toàn bộ booking trong cùng recurring group.
            // SQL: Lấy toàn bộ booking trong recurring group còn HOLD và còn hạn để xác nhận.
            sql = """
                    SELECT booking_id
                    FROM bookings WITH (UPDLOCK, HOLDLOCK)
                    WHERE customer_id = ?
                      AND recurring_group_id = ?
                      AND status = 'HOLD'
                      AND hold_expires_at > GETDATE()
                    ORDER BY booking_id
                    """;
        }

        List<Long> bookingIds = new ArrayList<>();
        // Bind tham số khác nhau tùy booking đơn hay recurring group.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (payment.recurringGroupId() == null) {
                ps.setLong(1, payment.bookingId());
                ps.setLong(2, payment.customerId());
            } else {
                ps.setLong(1, payment.customerId());
                ps.setLong(2, payment.recurringGroupId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                // Duyệt toàn bộ booking còn HOLD trong scope để cập nhật success.
                while (rs.next()) {
                    bookingIds.add(rs.getLong("booking_id"));
                }
            }
        }
        return bookingIds;
    }

    /**
     * Ghi nhận voucher cho các booking được payment xác nhận.
     * PUBLIC_CODE tăng used sau payment; REWARD_VOUCHER không tăng used lần nữa vì đã tăng lúc redeem.
     */
    private void recordVoucherUsage(Connection conn, List<Long> bookingIds, PaymentLock payment) throws SQLException {
        Map<String, VoucherBookingApplication> voucherApplications = new LinkedHashMap<>();
        // Gom voucher theo mã/public hoặc userVoucher để tránh ghi usage trùng trong booking recurring.
        for (Long bookingId : bookingIds) {
            VoucherBookingApplication application = getVoucherApplicationForBooking(conn, bookingId);
            // Booking không dùng voucher thì bỏ qua.
            if (application != null) {
                String key = application.userVoucherId() == null
                        ? "PUBLIC:" + application.voucherId()
                        : "OWNED:" + application.userVoucherId();
                voucherApplications.putIfAbsent(key, application);
            }
        }

        // Xử lý từng voucher duy nhất sau khi payment success.
        for (VoucherBookingApplication application : voucherApplications.values()) {
            // REWARD_VOUCHER là voucher đổi điểm của Customer, cần mark user voucher thành USED.
            if (DISTRIBUTION_REWARD_VOUCHER.equalsIgnoreCase(application.distributionType())) {
                // Voucher đổi điểm đã được reserve ở booking HOLD; payment success chỉ chuyển sang USED.
                if (application.userVoucherId() == null
                        || !voucherDAO.markUserVoucherUsed(conn, application.userVoucherId(), payment.customerId())) {
                    throw new SQLException("Voucher cua khach hang khong hop le de ghi nhan thanh toan.");
                }
                VoucherDAO.UsageInsertResult usageResult = voucherDAO.recordUsageIfAbsent(
                        application.voucherId(),
                        application.userVoucherId(),
                        payment.customerId(),
                        application.bookingId(),
                        payment.paymentId(),
                        conn
                );
                // Conflict nghĩa là voucher đã được ghi cho booking/payment khác.
                if (usageResult == VoucherDAO.UsageInsertResult.CONFLICT) {
                    throw new SQLException("Voucher da duoc ghi nhan cho booking khac.");
                }
            } else if (DISTRIBUTION_PUBLIC_CODE.equalsIgnoreCase(application.distributionType())) {
                // Mã công khai chỉ tăng used khi usage mới được insert thành công.
                VoucherDAO.UsageInsertResult usageResult = voucherDAO.recordUsageIfAbsent(
                        application.voucherId(),
                        null,
                        payment.customerId(),
                        application.bookingId(),
                        payment.paymentId(),
                        conn
                );
                // Public code bị conflict nếu Customer đã có usage trước đó.
                if (usageResult == VoucherDAO.UsageInsertResult.CONFLICT) {
                    throw new SQLException("Khach hang da su dung ma giam gia nay.");
                }
                // Chỉ tăng used khi record usage mới được insert, tránh tăng lại khi callback lặp.
                if (usageResult == VoucherDAO.UsageInsertResult.INSERTED
                        && !voucherDAO.incrementUsed(application.voucherId(), conn)) {
                    throw new SQLException("Voucher khong con luot su dung de xac nhan thanh toan.");
                }
            }
        }
    }

    private VoucherBookingApplication getVoucherApplicationForBooking(Connection conn, Long bookingId) throws SQLException {
        // SQL: Lấy voucher đang gắn với booking để ghi nhận usage sau payment success.
        String sql = """
                SELECT b.booking_id,
                       b.voucher_id,
                       b.user_voucher_id,
                       v.distribution_type
                FROM bookings b
                JOIN vouchers v ON b.voucher_id = v.id
                WHERE b.booking_id = ?
                """;
        // Lấy voucher đang gắn với booking để ghi nhận sau khi payment success.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                // Booking không có voucher hoặc không tồn tại thì không cần ghi usage.
                if (!rs.next()) {
                    return null;
                }
                int voucherId = rs.getInt("voucher_id");
                // voucher_id NULL nghĩa là booking không áp voucher.
                if (rs.wasNull()) {
                    return null;
                }
                long userVoucherId = rs.getLong("user_voucher_id");
                return new VoucherBookingApplication(
                        rs.getLong("booking_id"),
                        voucherId,
                        rs.wasNull() ? null : userVoucherId,
                        rs.getString("distribution_type")
                );
            }
        }
    }

    private Long findPaymentIdByTransactionRef(Connection conn, String transactionRef) throws SQLException {
        // SQL: Lookup payment_id theo transactionRef để lưu callback audit.
        String sql = """
                SELECT payment_id
                FROM payments
                WHERE transaction_ref = ?
                """;
        // Helper lookup id để lưu callback audit theo transactionRef.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("payment_id") : null;
            }
        }
    }

    private void insertCallback(
            Connection conn,
            long paymentId,
            String rawPayload,
            String signature,
            boolean valid
    ) throws SQLException {
        insertCallback(conn, paymentId, rawPayload, signature, valid, GATEWAY_SIMULATED);
    }

    private void insertCallback(
            Connection conn,
            long paymentId,
            String rawPayload,
            String signature,
            boolean valid,
            String gatewayCode
    ) throws SQLException {
        // SQL: Lưu raw callback gateway để audit/debug kể cả khi signature không hợp lệ.
        String sql = """
                INSERT INTO payment_callbacks (
                    payment_id, gateway_code, raw_payload, signature, valid, received_at
                )
                VALUES (?, ?, ?, ?, ?, GETDATE())
                """;
        // Lưu raw payload và chữ ký gateway để phục vụ audit/debug sau này.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            ps.setString(2, normalizeGatewayCode(gatewayCode));
            ps.setString(3, rawPayload == null ? "" : rawPayload);
            ps.setString(4, signature == null ? "" : signature);
            ps.setBoolean(5, valid);
            ps.executeUpdate();
        }
    }

    private void insertNotification(
            Connection conn,
            long userId,
            String title,
            String message,
            String type,
            Long referenceId
    ) throws SQLException {
        // SQL: Insert notification cho Customer/Staff liên quan tới payment event.
        String sql = """
                INSERT INTO notifications (
                    user_id, title, message, notification_type, reference_id, is_read, created_at
                )
                VALUES (?, ?, ?, ?, ?, 0, GETDATE())
                """;
        // Notification có thể có hoặc không có referenceId tùy loại payment event.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, type);
            // referenceId null dùng cho thông báo membership/system không gắn invoice/booking cụ thể.
            if (referenceId == null) {
                ps.setNull(5, java.sql.Types.BIGINT);
            } else {
                // Có referenceId thì lưu để UI điều hướng về booking/invoice liên quan.
                ps.setLong(5, referenceId);
            }
            ps.executeUpdate();
        }
    }

    private String normalizeGatewayCode(String gatewayCode) {
        return gatewayCode == null || gatewayCode.isBlank() ? GATEWAY_SIMULATED : gatewayCode.trim();
    }

    private void rollback(Connection conn, Exception original) {
        // Rollback có thể lỗi nếu connection đã đóng; lỗi rollback được gắn vào exception gốc.
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private String generateTransactionRef() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                .format(LocalDateTime.now());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "TXN" + timestamp + random;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String joinAddress(ResultSet rs) throws SQLException {
        List<String> parts = new ArrayList<>();
        addPart(parts, rs.getString("address"));
        addPart(parts, rs.getString("ward"));
        addPart(parts, rs.getString("city"));
        return String.join(", ", parts);
    }

    private void addPart(List<String> parts, String value) {
        // Chỉ thêm phần địa chỉ có nội dung để chuỗi join không có dấu phẩy thừa.
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value.trim());
        }
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record PaymentLock(
            long paymentId,
            long bookingId,
            long customerId,
            BigDecimal amount,
            String paymentType,
            long fieldId,
            Long invoiceId,
            String invoiceStatus,
            Long recurringGroupId,
            int bookingCount,
            String paymentStatus,
            String bookingStatus,
            Long checkoutStaffId,
            boolean holdValid
    ) {
    }

    private record VoucherBookingApplication(
            long bookingId,
            int voucherId,
            Long userVoucherId,
            String distributionType
    ) {
    }

    public record GatewayPaymentView(
            long paymentId,
            long bookingId,
            long customerId,
            BigDecimal amount,
            String status,
            String transactionRef,
            String gatewayTransactionId,
            String bookingStatus
    ) {
    }

    public enum PaymentUpdateResult {
        UPDATED_SUCCESS,
        ALREADY_SUCCESS,
        UPDATED_FAILED,
        ALREADY_FAILED,
        NOT_FOUND,
        INVALID_STATE
    }
}

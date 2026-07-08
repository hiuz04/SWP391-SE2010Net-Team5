package com.swp.dao;

import com.swp.model.Payment;
import com.swp.model.PaymentMethod;
import com.swp.model.dto.BookingView;
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
import java.util.List;
import java.util.UUID;

public class PaymentDAO {

    private static final String STATUS_HOLD = "HOLD";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String GATEWAY_SIMULATED = "SIMULATED";

    public BookingView getBookingForPayment(long bookingId, long customerId) throws SQLException {
        String sql = """
                SELECT b.booking_id,
                       b.booking_code,
                       b.recurring_group_id,
                       COALESCE(rg.repeat_type, 'NONE') AS repeat_type,
                       grp.recurring_count,
                       b.customer_id,
                       b.facility_id,
                       fa.facility_name,
                       b.field_id,
                       f.field_name,
                       ft.type_name AS field_type_name,
                       b.start_time,
                       b.end_time,
                       grp.total_amount,
                       grp.deposit_amount,
                       b.status,
                       grp.hold_expires_at,
                       lp.payment_status,
                       lp.payment_method_name,
                       lp.paid_amount,
                       lp.paid_at
                FROM bookings b
                LEFT JOIN booking_recurring_groups rg ON b.recurring_group_id = rg.recurring_group_id
                INNER JOIN facilities fa ON b.facility_id = fa.facility_id
                INNER JOIN fields f ON b.field_id = f.field_id
                INNER JOIN field_types ft ON f.field_type_id = ft.field_type_id
                OUTER APPLY (
                    SELECT COUNT(*) AS recurring_count,
                           SUM(sb.total_amount) AS total_amount,
                           SUM(sb.deposit_amount) AS deposit_amount,
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

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            ps.setLong(2, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapBookingForPayment(rs) : null;
            }
        }
    }

    public List<PaymentMethod> getActivePaymentMethods() throws SQLException {
        String sql = """
                SELECT payment_method_id, method_code, method_name, status
                FROM payment_methods
                WHERE status = 'ACTIVE'
                ORDER BY payment_method_id
                """;
        List<PaymentMethod> methods = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                methods.add(mapPaymentMethod(rs));
            }
        }

        return methods;
    }

    public PaymentMethod getPaymentMethodById(int paymentMethodId) throws SQLException {
        String sql = """
                SELECT payment_method_id, method_code, method_name, status
                FROM payment_methods
                WHERE payment_method_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentMethodId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPaymentMethod(rs) : null;
            }
        }
    }

    public Payment createPendingDepositPayment(long bookingId, long customerId, int paymentMethodId)
            throws SQLException {
        String selectBooking = """
                SELECT grp.deposit_amount,
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
                OUTER APPLY (
                    SELECT SUM(sb.deposit_amount) AS deposit_amount,
                           SUM(CASE WHEN sb.status <> 'HOLD' THEN 1 ELSE 0 END) AS invalid_booking_count,
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
        String selectMethod = """
                SELECT payment_method_id
                FROM payment_methods
                WHERE payment_method_id = ?
                  AND status = 'ACTIVE'
                """;
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

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal amount;
                try (PreparedStatement ps = conn.prepareStatement(selectBooking)) {
                    ps.setLong(1, bookingId);
                    ps.setLong(2, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Kh\u00f4ng t\u00ecm th\u1ea5y booking h\u1ee3p l\u1ec7.");
                        }
                        if (!STATUS_HOLD.equals(rs.getString("status"))) {
                            throw new IllegalArgumentException("Booking kh\u00f4ng c\u00f2n \u1edf tr\u1ea1ng th\u00e1i ch\u1edd thanh to\u00e1n.");
                        }
                        if (rs.getInt("hold_valid") != 1) {
                            throw new IllegalArgumentException("Th\u1eddi gian gi\u1eef ch\u1ed7 c\u1ee7a booking \u0111\u00e3 h\u1ebft h\u1ea1n.");
                        }
                        if (rs.getInt("has_success") == 1) {
                            throw new IllegalArgumentException("Booking n\u00e0y \u0111\u00e3 \u0111\u01b0\u1ee3c thanh to\u00e1n.");
                        }
                        amount = rs.getBigDecimal("deposit_amount");
                    }
                }

                if (amount == null || amount.signum() <= 0) {
                    throw new IllegalArgumentException("S\u1ed1 ti\u1ec1n c\u1ecdc c\u1ee7a booking kh\u00f4ng h\u1ee3p l\u1ec7.");
                }

                try (PreparedStatement ps = conn.prepareStatement(selectMethod)) {
                    ps.setInt(1, paymentMethodId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Ph\u01b0\u01a1ng th\u1ee9c thanh to\u00e1n kh\u00f4ng h\u1ee3p l\u1ec7.");
                        }
                    }
                }

                String transactionRef = generateTransactionRef();
                Payment payment;
                try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
                    ps.setLong(1, bookingId);
                    ps.setLong(2, customerId);
                    ps.setInt(3, paymentMethodId);
                    ps.setBigDecimal(4, amount);
                    ps.setString(5, transactionRef);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Khong tao duoc giao dich thanh toan.");
                        }
                        payment = mapPayment(rs);
                    }
                }

                conn.commit();
                return payment;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    public GatewayPaymentView findPaymentByTransactionRef(String transactionRef) throws SQLException {
        if (transactionRef == null || transactionRef.isBlank()) {
            return null;
        }

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
                INNER JOIN bookings b ON p.booking_id = b.booking_id
                WHERE p.transaction_ref = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
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

    public boolean savePaymentCallbackByTransactionRef(
            String transactionRef,
            String gatewayCode,
            String rawPayload,
            String signature,
            boolean valid
    ) throws SQLException {
        if (transactionRef == null || transactionRef.isBlank()) {
            return false;
        }

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long paymentId = findPaymentIdByTransactionRef(conn, transactionRef);
                if (paymentId == null) {
                    conn.commit();
                    return false;
                }
                insertCallback(conn, paymentId, rawPayload, signature, valid, gatewayCode);
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
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

    public PaymentUpdateResult markPaymentSuccessAndConfirmBooking(
            String transactionRef,
            String gatewayTransactionId,
            String rawPayload,
            String signature,
            String gatewayCode
    ) throws SQLException {
        String selectPayment = """
                SELECT p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.status AS payment_status,
                       b.status AS booking_status,
                       b.recurring_group_id,
                       scope.booking_count,
                       CASE
                           WHEN scope.invalid_booking_count = 0
                                AND scope.hold_expired_count = 0
                           THEN 1
                           ELSE 0
                       END AS hold_valid
                FROM payments p WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN bookings b WITH (UPDLOCK, HOLDLOCK) ON p.booking_id = b.booking_id
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
        String updateBooking = """
                UPDATE bookings
                SET status = 'CONFIRMED', updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status = 'HOLD'
                  AND hold_expires_at > GETDATE()
                """;
        String updatePayment = """
                UPDATE payments
                SET status = 'SUCCESS',
                    gateway_transaction_id = ?,
                    paid_at = GETDATE()
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;
        String insertStatusLog = """
                INSERT INTO booking_status_logs (
                    booking_id, old_status, new_status, changed_by, note, created_at
                )
                VALUES (?, 'HOLD', 'CONFIRMED', ?, ?, GETDATE())
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PaymentLock payment = getPaymentLock(conn, selectPayment, transactionRef);
                if (payment == null) {
                    conn.commit();
                    return PaymentUpdateResult.NOT_FOUND;
                }
                if (STATUS_SUCCESS.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_SUCCESS;
                }
                if (STATUS_FAILED.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_FAILED;
                }
                if (!STATUS_PENDING.equals(payment.paymentStatus())
                        || !STATUS_HOLD.equals(payment.bookingStatus())
                        || !payment.holdValid()) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.INVALID_STATE;
                }

                List<Long> bookingIds = getScopedBookingIds(conn, payment);
                if (bookingIds.isEmpty() || bookingIds.size() != payment.bookingCount()) {
                    conn.commit();
                    return PaymentUpdateResult.INVALID_STATE;
                }

                for (Long bookingId : bookingIds) {
                    try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
                        ps.setLong(1, bookingId);
                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("Booking khong con hop le de xac nhan thanh toan.");
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(updatePayment)) {
                    ps.setString(1, gatewayTransactionId);
                    ps.setLong(2, payment.paymentId());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Khong cap nhat duoc trang thai thanh toan.");
                    }
                }
                for (Long bookingId : bookingIds) {
                    try (PreparedStatement ps = conn.prepareStatement(insertStatusLog)) {
                        ps.setLong(1, bookingId);
                        ps.setLong(2, payment.customerId());
                        ps.setString(3, "Payment completed: " + transactionRef);
                        ps.executeUpdate();
                    }
                }
                insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);

                conn.commit();
                return PaymentUpdateResult.UPDATED_SUCCESS;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
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

    public PaymentUpdateResult markPaymentFailed(
            String transactionRef,
            String rawPayload,
            String signature,
            String gatewayCode
    ) throws SQLException {
        String selectPayment = """
                SELECT p.payment_id,
                       p.booking_id,
                       p.customer_id,
                       p.status AS payment_status,
                       b.status AS booking_status,
                       b.recurring_group_id,
                       1 AS booking_count,
                       CASE WHEN b.hold_expires_at > GETDATE() THEN 1 ELSE 0 END AS hold_valid
                FROM payments p WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN bookings b ON p.booking_id = b.booking_id
                WHERE p.transaction_ref = ?
                """;
        String updatePayment = """
                UPDATE payments
                SET status = 'FAILED'
                WHERE payment_id = ?
                  AND status = 'PENDING'
                """;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PaymentLock payment = getPaymentLock(conn, selectPayment, transactionRef);
                if (payment == null) {
                    conn.commit();
                    return PaymentUpdateResult.NOT_FOUND;
                }
                if (STATUS_FAILED.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_FAILED;
                }
                if (STATUS_SUCCESS.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.ALREADY_SUCCESS;
                }
                if (!STATUS_PENDING.equals(payment.paymentStatus())) {
                    insertCallback(conn, payment.paymentId(), rawPayload, signature, true, gatewayCode);
                    conn.commit();
                    return PaymentUpdateResult.INVALID_STATE;
                }

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
        String sql = paymentViewSql() + """
                WHERE p.transaction_ref = ?
                  AND p.customer_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPaymentView(rs) : null;
            }
        }
    }

    public List<PaymentView> getPaymentHistory(long customerId) throws SQLException {
        String sql = paymentViewSql() + """
                WHERE p.customer_id = ?
                ORDER BY p.created_at DESC, p.paid_at DESC, p.payment_id DESC
                """;
        List<PaymentView> payments = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapPaymentView(rs));
                }
            }
        }

        return payments;
    }

    private String paymentViewSql() {
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
                       pm.method_name AS payment_method_name,
                       b.booking_code,
                       b.status AS booking_status,
                       b.hold_expires_at,
                       b.start_time,
                       b.end_time,
                       fa.facility_name,
                       f.field_name
                FROM payments p
                INNER JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                INNER JOIN bookings b ON p.booking_id = b.booking_id
                INNER JOIN facilities fa ON b.facility_id = fa.facility_id
                INNER JOIN fields f ON b.field_id = f.field_id
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
        view.setFacilityId(rs.getLong("facility_id"));
        view.setFacilityName(rs.getString("facility_name"));
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
        payment.setBookingId(rs.getLong("booking_id"));
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
        view.setBookingId(rs.getLong("booking_id"));
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
        view.setFacilityName(rs.getString("facility_name"));
        view.setFieldName(rs.getString("field_name"));
        return view;
    }

    private PaymentLock getPaymentLock(Connection conn, String sql, String transactionRef)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PaymentLock(
                        rs.getLong("payment_id"),
                        rs.getLong("booking_id"),
                        rs.getLong("customer_id"),
                        getLongOrNull(rs, "recurring_group_id"),
                        rs.getInt("booking_count"),
                        rs.getString("payment_status"),
                        rs.getString("booking_status"),
                        rs.getInt("hold_valid") == 1
                );
            }
        }
    }

    private List<Long> getScopedBookingIds(Connection conn, PaymentLock payment) throws SQLException {
        String sql;
        if (payment.recurringGroupId() == null) {
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
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (payment.recurringGroupId() == null) {
                ps.setLong(1, payment.bookingId());
                ps.setLong(2, payment.customerId());
            } else {
                ps.setLong(1, payment.customerId());
                ps.setLong(2, payment.recurringGroupId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookingIds.add(rs.getLong("booking_id"));
                }
            }
        }
        return bookingIds;
    }

    private Long findPaymentIdByTransactionRef(Connection conn, String transactionRef) throws SQLException {
        String sql = """
                SELECT payment_id
                FROM payments
                WHERE transaction_ref = ?
                """;
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
        String sql = """
                INSERT INTO payment_callbacks (
                    payment_id, gateway_code, raw_payload, signature, valid, received_at
                )
                VALUES (?, ?, ?, ?, ?, GETDATE())
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            ps.setString(2, normalizeGatewayCode(gatewayCode));
            ps.setString(3, rawPayload == null ? "" : rawPayload);
            ps.setString(4, signature == null ? "" : signature);
            ps.setBoolean(5, valid);
            ps.executeUpdate();
        }
    }

    private String normalizeGatewayCode(String gatewayCode) {
        return gatewayCode == null || gatewayCode.isBlank() ? GATEWAY_SIMULATED : gatewayCode.trim();
    }

    private void rollback(Connection conn, Exception original) {
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

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record PaymentLock(
            long paymentId,
            long bookingId,
            long customerId,
            Long recurringGroupId,
            int bookingCount,
            String paymentStatus,
            String bookingStatus,
            boolean holdValid
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

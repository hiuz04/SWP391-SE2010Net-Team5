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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StaffBillingDAO {

    public CheckoutView getCheckoutView(long bookingId) throws SQLException {
        String sql = """
                SELECT b.booking_id, b.booking_code, b.customer_id, b.facility_id, b.field_id,
                       b.status, b.start_time, b.end_time, b.total_amount, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       fac.facility_name, fac.address, fac.ward, fac.district, fac.city
                FROM bookings b
                JOIN users u ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                JOIN facilities fac ON b.facility_id = fac.facility_id
                WHERE b.booking_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCheckoutView(rs) : null;
            }
        }
    }

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
            return findPaidInvoiceForUpdate(conn, bookingId) != null;
        }
    }

    public boolean canStaffCheckoutFacility(long staffId, long facilityId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return hasActiveShiftForFacility(conn, staffId, facilityId);
        }
    }

    public boolean canStaffViewFacilityToday(long staffId, long facilityId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return hasAssignedShiftForFacilityToday(conn, staffId, facilityId);
        }
    }

    public CheckoutResult completeCheckout(long bookingId, long actorId, boolean staffRole) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BookingLock booking = lockBooking(conn, bookingId);
                if (booking == null) {
                    throw new IllegalArgumentException("Không tìm thấy lịch đặt sân.");
                }

                InvoiceSummary existingInvoice = findPaidInvoiceForUpdate(conn, bookingId);
                if (existingInvoice != null) {
                    conn.commit();
                    return new CheckoutResult(existingInvoice.invoiceId(), bookingId, existingInvoice.invoiceCode());
                }

                if (!"CHECKED_IN".equals(booking.status())) {
                    throw new IllegalArgumentException("Chỉ lịch đã nhận sân mới được trả sân.");
                }
                if (staffRole && !hasActiveShiftForFacility(conn, actorId, booking.facilityId())) {
                    throw new SecurityException("Bạn không có ca làm việc đang hoạt động tại cơ sở này.");
                }

                BigDecimal fieldTotal = safe(booking.fieldFee());
                BigDecimal remainingAmount = calculateRemaining(fieldTotal, booking.depositAmount());

                updateBookingCompleted(conn, bookingId);
                releaseField(conn, booking.fieldId());
                insertBookingStatusLog(conn, booking, actorId, "Trả sân hoàn tất.");

                String invoiceCode = generateInvoiceCode(bookingId);
                long invoiceId = insertInvoice(
                        conn,
                        invoiceCode,
                        booking,
                        actorId,
                        fieldTotal,
                        remainingAmount,
                        remainingAmount
                );

                conn.commit();
                return new CheckoutResult(invoiceId, bookingId, invoiceCode);
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    private String invoiceViewSql(String predicate) {
        return """
                SELECT TOP 1
                       i.invoice_id, i.invoice_code, i.status AS invoice_status, i.issued_at,
                       i.subtotal, i.total_amount, i.paid_amount,
                       b.booking_id, b.booking_code, b.start_time, b.end_time,
                       b.total_amount AS field_fee, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       fac.facility_id, fac.facility_name, fac.address, fac.ward, fac.district, fac.city,
                       staff.full_name AS staff_name
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                JOIN users u ON i.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                JOIN facilities fac ON b.facility_id = fac.facility_id
                LEFT JOIN users staff ON i.staff_id = staff.user_id
                WHERE %s
                  AND i.status IN ('PAID', 'ACTIVE')
                ORDER BY i.issued_at DESC, i.invoice_id DESC
                """.formatted(predicate);
    }

    private BookingLock lockBooking(Connection conn, long bookingId) throws SQLException {
        String sql = """
                SELECT booking_id, booking_code, customer_id, facility_id, field_id,
                       status, total_amount, deposit_amount
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
                        rs.getLong("facility_id"),
                        rs.getLong("field_id"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("deposit_amount")
                );
            }
        }
    }

    private InvoiceSummary findPaidInvoiceForUpdate(Connection conn, long bookingId) throws SQLException {
        String sql = """
                SELECT TOP 1 invoice_id, invoice_code
                FROM invoices WITH (UPDLOCK, HOLDLOCK)
                WHERE booking_id = ?
                  AND status IN ('PAID', 'ACTIVE')
                ORDER BY issued_at DESC, invoice_id DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new InvoiceSummary(rs.getLong("invoice_id"), rs.getString("invoice_code"));
            }
        }
    }

    private void updateBookingCompleted(Connection conn, long bookingId) throws SQLException {
        String sql = """
                UPDATE bookings
                SET status = 'COMPLETED',
                    updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status = 'CHECKED_IN'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Lịch đặt sân không còn hợp lệ để trả sân.");
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
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fieldId);
            ps.executeUpdate();
        }
    }

    private long insertInvoice(
            Connection conn,
            String invoiceCode,
            BookingLock booking,
            Long staffId,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            BigDecimal paidAmount
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
                    issued_at
                )
                OUTPUT INSERTED.invoice_id
                VALUES (?, ?, ?, ?, ?, 0, ?, ?, 'PAID', GETDATE())
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
            ps.setBigDecimal(5, subtotal);
            ps.setBigDecimal(6, totalAmount);
            ps.setBigDecimal(7, paidAmount);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Không thể tạo hóa đơn.");
                }
                return rs.getLong(1);
            }
        }
    }

    private void insertBookingStatusLog(Connection conn, BookingLock booking, Long changedBy, String note)
            throws SQLException {
        String sql = """
                INSERT INTO booking_status_logs (
                    booking_id, old_status, new_status, changed_by, note, created_at
                )
                VALUES (?, 'CHECKED_IN', 'COMPLETED', ?, ?, GETDATE())
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, booking.bookingId());
            if (changedBy == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, changedBy);
            }
            ps.setString(3, note);
            ps.executeUpdate();
        }
    }

    private boolean hasActiveShiftForFacility(Connection conn, long staffId, long facilityId) throws SQLException {
        String sql = """
                SELECT TOP 1 1
                FROM work_shifts ws
                JOIN shift_assignments sa ON ws.shift_id = sa.shift_id
                WHERE sa.staff_id = ?
                  AND ws.facility_id = ?
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
            ps.setLong(2, facilityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasAssignedShiftForFacilityToday(Connection conn, long staffId, long facilityId) throws SQLException {
        String sql = """
                SELECT TOP 1 1
                FROM work_shifts ws
                JOIN shift_assignments sa ON ws.shift_id = sa.shift_id
                WHERE sa.staff_id = ?
                  AND ws.facility_id = ?
                  AND ws.shift_date = CAST(GETDATE() AS DATE)
                  AND (sa.status IS NULL OR sa.status <> 'CANCELLED')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setLong(2, facilityId);
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
        view.setFacilityId(rs.getLong("facility_id"));
        view.setFieldId(rs.getLong("field_id"));
        view.setStatus(rs.getString("status"));
        view.setCustomerName(rs.getString("customer_name"));
        view.setCustomerPhone(rs.getString("customer_phone"));
        view.setFacilityName(rs.getString("facility_name"));
        view.setFacilityAddress(joinAddress(rs));
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
        view.setFacilityId(rs.getLong("facility_id"));
        view.setCustomerName(rs.getString("customer_name"));
        view.setCustomerPhone(rs.getString("customer_phone"));
        view.setFacilityName(rs.getString("facility_name"));
        view.setFacilityAddress(joinAddress(rs));
        view.setFieldName(rs.getString("field_name"));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setFieldFee(rs.getBigDecimal("field_fee"));
        view.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        view.setSubtotal(rs.getBigDecimal("subtotal"));
        view.setTotalAmount(rs.getBigDecimal("total_amount"));
        view.setPaidAmount(rs.getBigDecimal("paid_amount"));
        view.setStaffName(rs.getString("staff_name"));
        return view;
    }

    private String joinAddress(ResultSet rs) throws SQLException {
        List<String> parts = new ArrayList<>();
        addPart(parts, rs.getString("address"));
        addPart(parts, rs.getString("ward"));
        addPart(parts, rs.getString("district"));
        addPart(parts, rs.getString("city"));
        return String.join(", ", parts);
    }

    private void addPart(List<String> parts, String value) {
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value.trim());
        }
    }

    private BigDecimal calculateRemaining(BigDecimal fieldTotal, BigDecimal deposit) {
        BigDecimal remaining = safe(fieldTotal).subtract(safe(deposit));
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
            long facilityId,
            long fieldId,
            String status,
            BigDecimal fieldFee,
            BigDecimal depositAmount
    ) {
    }

    private record InvoiceSummary(long invoiceId, String invoiceCode) {
    }
}

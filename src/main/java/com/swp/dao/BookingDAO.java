package com.swp.dao;

import com.swp.model.Booking;
import com.swp.model.Field;
import com.swp.model.FieldMaintenanceSchedule;
import com.swp.model.dto.BookingView;
import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    public Long getFacilityIdByFieldId(Long fieldId) throws SQLException {
        String sql = """
                SELECT facility_id
                FROM fields
                WHERE field_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, fieldId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("facility_id");
                }
            }
        }

        return null;
    }

    public List<Field> getFieldsByFacility(Long facilityId) throws SQLException {
        String sql = """
                SELECT field_id,
                       facility_id,
                       field_type_id,
                       field_name,
                       description,
                       status,
                       created_at,
                       updated_at
                FROM fields
                WHERE facility_id = ?
                ORDER BY field_name
                """;

        List<Field> fields = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, facilityId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Field field = new Field();

                    field.setFieldId(rs.getLong("field_id"));
                    field.setFacilityId(rs.getLong("facility_id"));
                    field.setFieldTypeId(rs.getInt("field_type_id"));
                    field.setFieldName(rs.getString("field_name"));
                    field.setDescription(rs.getString("description"));
                    field.setStatus(rs.getString("status"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        field.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        field.setUpdatedAt(updatedAt.toLocalDateTime());
                    }

                    fields.add(field);
                }
            }
        }

        return fields;
    }

    public List<Booking> getBookingsByFacilityAndDate(Long facilityId, LocalDate date) throws SQLException {
        String sql = """
                SELECT booking_id,
                       booking_code,
                       customer_id,
                       facility_id,
                       field_id,
                       recurring_group_id,
                       start_time,
                       end_time,
                       original_price,
                       discount_amount,
                       total_amount,
                       deposit_amount,
                       status,
                       hold_expires_at,
                       cancellation_reason,
                       cancelled_at,
                       qr_code,
                       created_at,
                       updated_at
                FROM bookings
                WHERE facility_id = ?
                  AND status NOT IN ('CANCELLED', 'EXPIRED', 'REJECTED')
                  AND start_time < ?
                  AND end_time > ?
                """;

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, facilityId);
            ps.setTimestamp(2, Timestamp.valueOf(dayEnd));
            ps.setTimestamp(3, Timestamp.valueOf(dayStart));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Booking booking = new Booking();

                    booking.setBookingId(rs.getLong("booking_id"));
                    booking.setBookingCode(rs.getString("booking_code"));
                    booking.setCustomerId(rs.getLong("customer_id"));
                    booking.setFacilityId(rs.getLong("facility_id"));
                    booking.setFieldId(rs.getLong("field_id"));

                    Object recurringGroupId = rs.getObject("recurring_group_id");
                    if (recurringGroupId != null) {
                        booking.setRecurringGroupId(rs.getLong("recurring_group_id"));
                    }

                    Timestamp startTime = rs.getTimestamp("start_time");
                    if (startTime != null) {
                        booking.setStartTime(startTime.toLocalDateTime());
                    }

                    Timestamp endTime = rs.getTimestamp("end_time");
                    if (endTime != null) {
                        booking.setEndTime(endTime.toLocalDateTime());
                    }

                    booking.setOriginalPrice(rs.getBigDecimal("original_price"));
                    booking.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                    booking.setTotalAmount(rs.getBigDecimal("total_amount"));
                    booking.setDepositAmount(rs.getBigDecimal("deposit_amount"));
                    booking.setStatus(rs.getString("status"));

                    Timestamp holdExpiresAt = rs.getTimestamp("hold_expires_at");
                    if (holdExpiresAt != null) {
                        booking.setHoldExpiresAt(holdExpiresAt.toLocalDateTime());
                    }

                    booking.setCancellationReason(rs.getString("cancellation_reason"));

                    Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
                    if (cancelledAt != null) {
                        booking.setCancelledAt(cancelledAt.toLocalDateTime());
                    }

                    booking.setQrCode(rs.getString("qr_code"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        booking.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        booking.setUpdatedAt(updatedAt.toLocalDateTime());
                    }

                    bookings.add(booking);
                }
            }
        }

        return bookings;
    }

    public List<FieldMaintenanceSchedule> getMaintenanceByFacilityAndDate(Long facilityId, LocalDate date)
            throws SQLException {

        String sql = """
                SELECT m.maintenance_id,
                       m.field_id,
                       m.start_time,
                       m.end_time,
                       m.reason,
                       m.status,
                       m.created_at
                FROM field_maintenance_schedules m
                INNER JOIN fields f ON m.field_id = f.field_id
                WHERE f.facility_id = ?
                  AND m.status <> 'CANCELLED'
                  AND m.start_time < ?
                  AND m.end_time > ?
                """;

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<FieldMaintenanceSchedule> schedules = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, facilityId);
            ps.setTimestamp(2, Timestamp.valueOf(dayEnd));
            ps.setTimestamp(3, Timestamp.valueOf(dayStart));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FieldMaintenanceSchedule schedule = new FieldMaintenanceSchedule();

                    schedule.setMaintenanceId(rs.getLong("maintenance_id"));
                    schedule.setFieldId(rs.getLong("field_id"));

                    Timestamp startTime = rs.getTimestamp("start_time");
                    if (startTime != null) {
                        schedule.setStartTime(startTime.toLocalDateTime());
                    }

                    Timestamp endTime = rs.getTimestamp("end_time");
                    if (endTime != null) {
                        schedule.setEndTime(endTime.toLocalDateTime());
                    }

                    schedule.setReason(rs.getString("reason"));
                    schedule.setStatus(rs.getString("status"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        schedule.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    schedules.add(schedule);
                }
            }
        }

        return schedules;
    }
    public BookingView getBookingPreviewInfoByFieldId(Long fieldId, Long customerId) throws SQLException {
        String sql = """
                SELECT NULL AS booking_id,
                       NULL AS booking_code,
                       u.user_id AS customer_id,
                       u.full_name AS customer_name,
                       u.phone AS customer_phone,
                       u.email AS customer_email,
                       fa.facility_id,
                       fa.facility_name,
                       fa.address AS facility_address,
                       fa.hotline AS facility_hotline,
                       f.field_id,
                       f.field_name,
                       ft.type_name AS field_type_name,
                       ft.number_of_players,
                       NULL AS start_time,
                       NULL AS end_time,
                       NULL AS original_price,
                       NULL AS discount_amount,
                       NULL AS total_amount,
                       NULL AS deposit_amount,
                       NULL AS status,
                       NULL AS hold_expires_at,
                       NULL AS qr_code,
                       NULL AS created_at,
                       NULL AS updated_at,
                       NULL AS cancellation_reason,
                       NULL AS cancelled_at,
                       NULL AS payment_status,
                       NULL AS payment_method_name,
                       NULL AS paid_amount,
                       NULL AS paid_at
                FROM fields f
                INNER JOIN facilities fa ON f.facility_id = fa.facility_id
                INNER JOIN field_types ft ON f.field_type_id = ft.field_type_id
                INNER JOIN users u ON u.user_id = ?
                WHERE f.field_id = ?
                  AND f.status = 'AVAILABLE'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.setLong(2, fieldId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingView(rs);
                }
            }
        }

        return null;
    }

    public boolean isFieldAvailable(Long fieldId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return isFieldAvailable(conn, fieldId, startTime, endTime);
        }
    }

    private boolean isFieldAvailable(Connection conn, Long fieldId, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {

        String sql = """
                SELECT
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM fields f WITH (UPDLOCK, HOLDLOCK)
                            WHERE f.field_id = ?
                              AND f.status = 'AVAILABLE'
                        )
                        AND NOT EXISTS (
                            SELECT 1
                            FROM bookings b
                            WHERE b.field_id = ?
                              AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'REJECTED')
                              AND b.start_time < ?
                              AND b.end_time > ?
                        )
                        AND NOT EXISTS (
                            SELECT 1
                            FROM field_maintenance_schedules m
                            WHERE m.field_id = ?
                              AND m.status <> 'CANCELLED'
                              AND m.start_time < ?
                              AND m.end_time > ?
                        )
                        THEN 1
                        ELSE 0
                    END AS available
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fieldId);
            ps.setLong(2, fieldId);
            ps.setTimestamp(3, Timestamp.valueOf(endTime));
            ps.setTimestamp(4, Timestamp.valueOf(startTime));
            ps.setLong(5, fieldId);
            ps.setTimestamp(6, Timestamp.valueOf(endTime));
            ps.setTimestamp(7, Timestamp.valueOf(startTime));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("available") == 1;
            }
        }
    }
    public BigDecimal calculatePrice(Long fieldId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        String sql = """
                SELECT TOP 1 pr.price
                FROM price_rules pr
                INNER JOIN fields f ON f.field_id = ?
                WHERE pr.status = 'ACTIVE'
                  AND (pr.field_id = f.field_id OR pr.field_id IS NULL)
                  AND (pr.field_type_id = f.field_type_id OR pr.field_type_id IS NULL)
                  AND (pr.facility_id = f.facility_id OR pr.facility_id IS NULL)
                  AND (pr.specific_date = ? OR pr.specific_date IS NULL)
                  AND (pr.day_of_week = ? OR pr.day_of_week IS NULL)
                  AND pr.start_time <= CAST(? AS time)
                  AND pr.end_time >= CAST(? AS time)
                ORDER BY
                  CASE WHEN pr.field_id = f.field_id THEN 0 ELSE 1 END,
                  CASE WHEN pr.specific_date = ? THEN 0 ELSE 1 END,
                  pr.priority DESC,
                  pr.price_rule_id DESC
                """;

        BigDecimal hourlyPrice = BigDecimal.ZERO;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, fieldId);
            ps.setDate(2, java.sql.Date.valueOf(startTime.toLocalDate()));
            ps.setString(3, startTime.getDayOfWeek().name());
            ps.setString(4, startTime.toLocalTime().toString());
            ps.setString(5, endTime.toLocalTime().toString());
            ps.setDate(6, java.sql.Date.valueOf(startTime.toLocalDate()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal("price") != null) {
                    hourlyPrice = rs.getBigDecimal("price");
                }
            }
        }

        BigDecimal minutes = BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes());
        BigDecimal hours = minutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return hourlyPrice.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
    private BookingView mapBookingView(ResultSet rs) throws SQLException {
        BookingView view = new BookingView();

        view.setBookingId(getLongOrNull(rs, "booking_id"));
        view.setBookingCode(rs.getString("booking_code"));
        view.setCustomerId(getLongOrNull(rs, "customer_id"));
        view.setCustomerName(rs.getString("customer_name"));
        view.setCustomerPhone(rs.getString("customer_phone"));
        view.setCustomerEmail(rs.getString("customer_email"));
        view.setFacilityId(getLongOrNull(rs, "facility_id"));
        view.setFacilityName(rs.getString("facility_name"));
        view.setFacilityAddress(rs.getString("facility_address"));
        view.setFacilityHotline(rs.getString("facility_hotline"));
        view.setFieldId(getLongOrNull(rs, "field_id"));
        view.setFieldName(rs.getString("field_name"));
        view.setFieldTypeName(rs.getString("field_type_name"));
        view.setNumberOfPlayers(getIntegerOrNull(rs, "number_of_players"));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setOriginalPrice(rs.getBigDecimal("original_price"));
        view.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        view.setTotalAmount(rs.getBigDecimal("total_amount"));
        view.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        view.setStatus(rs.getString("status"));
        view.setHoldExpiresAt(toLocalDateTime(rs.getTimestamp("hold_expires_at")));
        view.setQrCode(rs.getString("qr_code"));
        view.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        view.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        view.setCancellationReason(rs.getString("cancellation_reason"));
        view.setCancelledAt(toLocalDateTime(rs.getTimestamp("cancelled_at")));
        view.setPaymentStatus(rs.getString("payment_status"));
        view.setPaymentMethodName(rs.getString("payment_method_name"));
        view.setPaidAmount(rs.getBigDecimal("paid_amount"));
        view.setPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));

        return view;
    }
    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntegerOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
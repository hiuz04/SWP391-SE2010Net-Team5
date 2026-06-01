package com.swp.dao;

import com.swp.model.Booking;
import com.swp.model.Field;
import com.swp.model.FieldMaintenanceSchedule;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
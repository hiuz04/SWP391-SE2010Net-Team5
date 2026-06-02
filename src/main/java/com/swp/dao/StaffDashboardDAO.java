package com.swp.dao;

import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * DAO for the Staff Dashboard – returns raw data maps/lists that the servlet
 * serialises to JSON. All queries target SQL Server syntax.
 */
public class StaffDashboardDAO {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Current shift assigned to this staff today
    // ──────────────────────────────────────────────────────────────────────────
    public Map<String, Object> getCurrentShift(long staffId) {
        String sql = """
                SELECT ws.shift_id, ws.shift_name, ws.shift_date,
                       ws.start_time, ws.end_time,
                       f.facility_id, f.facility_name,
                       sa.status AS assignment_status
                FROM work_shifts ws
                JOIN shift_assignments sa ON ws.shift_id = sa.shift_id
                JOIN facilities f ON ws.facility_id = f.facility_id
                WHERE sa.staff_id = ?
                  AND ws.shift_date = CAST(GETDATE() AS DATE)
                ORDER BY ws.start_time
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> shift = new LinkedHashMap<>();
                    shift.put("shiftId", rs.getLong("shift_id"));
                    shift.put("shiftName", rs.getString("shift_name"));
                    shift.put("shiftDate", rs.getString("shift_date"));
                    shift.put("startTime", rs.getString("start_time"));
                    shift.put("endTime", rs.getString("end_time"));
                    shift.put("facilityId", rs.getLong("facility_id"));
                    shift.put("facilityName", rs.getString("facility_name"));
                    shift.put("assignmentStatus", rs.getString("assignment_status"));
                    return shift;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn ca làm việc: " + e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Bookings for today at the staff's facility, ordered by start_time
    // ──────────────────────────────────────────────────────────────────────────
    public List<Map<String, Object>> getTodayBookings(long facilityId) {
        String sql = """
                SELECT b.booking_id, b.booking_code,
                       b.start_time, b.end_time,
                       b.status, b.total_amount,
                       u.full_name AS customer_name,
                       fi.field_name
                FROM bookings b
                JOIN users u  ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id   = fi.field_id
                WHERE b.facility_id = ?
                  AND CAST(b.start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND b.status NOT IN ('CANCELLED','HOLD')
                ORDER BY b.start_time
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, facilityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("bookingId", rs.getLong("booking_id"));
                    row.put("bookingCode", rs.getString("booking_code"));
                    row.put("startTime", rs.getString("start_time"));
                    row.put("endTime", rs.getString("end_time"));
                    row.put("status", rs.getString("status"));
                    row.put("totalAmount", rs.getBigDecimal("total_amount"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("fieldName", rs.getString("field_name"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn booking hôm nay: " + e.getMessage(), e);
        }
        return list;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. KPI: total cash collected by this staff during their shift window
    // ──────────────────────────────────────────────────────────────────────────
    public Map<String, Object> getCashKpi(long staffId, String shiftDateStr,
            String startTimeStr, String endTimeStr) {
        String sql = """
                SELECT
                    COALESCE(SUM(i.total_amount), 0) AS total_cash,
                    COUNT(*) AS tx_count
                FROM invoices i
                WHERE i.staff_id = ?
                  AND i.status   = 'PAID'
                  AND CAST(i.issued_at AS DATE) = ?
                  AND CAST(i.issued_at AS TIME) BETWEEN ? AND ?
                """;
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalCash", BigDecimal.ZERO);
        kpi.put("txCount", 0);
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setString(2, shiftDateStr);
            ps.setString(3, startTimeStr);
            ps.setString(4, endTimeStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    kpi.put("totalCash", rs.getBigDecimal("total_cash"));
                    kpi.put("txCount", rs.getInt("tx_count"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tính tiền thu: " + e.getMessage(), e);
        }
        return kpi;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. KPI: completed bookings count & total bookings today
    // ──────────────────────────────────────────────────────────────────────────
    public Map<String, Object> getBookingKpi(long facilityId) {
        String sql = """
                SELECT
                    COUNT(*) AS total_bookings,
                    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed
                FROM bookings
                WHERE facility_id = ?
                  AND CAST(start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND status NOT IN ('CANCELLED','HOLD')
                """;
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalBookings", 0);
        kpi.put("completed", 0);
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, facilityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    kpi.put("totalBookings", rs.getInt("total_bookings"));
                    kpi.put("completed", rs.getInt("completed"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi KPI booking: " + e.getMessage(), e);
        }
        return kpi;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. KPI: number of customers waiting for check-in right now
    // ──────────────────────────────────────────────────────────────────────────
    public int getPendingCheckinCount(long facilityId) {
        String sql = """
                SELECT COUNT(*) AS cnt
                FROM bookings
                WHERE facility_id = ?
                  AND CAST(start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND status = 'CONFIRMED'
                  AND start_time <= GETDATE()
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, facilityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm check-in chờ: " + e.getMessage(), e);
        }
        return 0;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Average rating today (from a reviews / ratings table if it exists)
    // Falls back to NULL if the table doesn't exist yet.
    // ──────────────────────────────────────────────────────────────────────────
    public Double getAverageRatingToday(long facilityId) {
        // If you have a reviews table, swap the query here.
        // For now we return null to let the UI show "N/A".
        return null;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Recent activity: last 5 events (check-ins + invoices) at the facility
    // ──────────────────────────────────────────────────────────────────────────
    public List<Map<String, Object>> getRecentActivity(long facilityId) {
        // Union of checkins and invoices, most recent first
        String sql = """
                SELECT TOP 5 *
                FROM (
                    -- Check-in events
                    SELECT
                        'CHECKIN'                        AS activity_type,
                        b.booking_code                   AS ref_code,
                        fi.field_name                    AS field_name,
                        u.full_name                      AS customer_name,
                        ci.checkin_time                  AS event_time,
                        NULL                             AS amount
                    FROM checkins ci
                    JOIN bookings b  ON ci.booking_id   = b.booking_id
                    JOIN fields fi   ON b.field_id      = fi.field_id
                    JOIN users u     ON b.customer_id   = u.user_id
                    WHERE b.facility_id = ?
                      AND CAST(ci.checkin_time AS DATE) = CAST(GETDATE() AS DATE)

                    UNION ALL

                    -- Invoice (checkout/payment) events
                    SELECT
                        'INVOICE'                        AS activity_type,
                        i.invoice_code                   AS ref_code,
                        fi.field_name                    AS field_name,
                        u.full_name                      AS customer_name,
                        i.issued_at                      AS event_time,
                        i.total_amount                   AS amount
                    FROM invoices i
                    JOIN bookings b  ON i.booking_id    = b.booking_id
                    JOIN fields fi   ON b.field_id      = fi.field_id
                    JOIN users u     ON i.customer_id   = u.user_id
                    WHERE b.facility_id = ?
                      AND CAST(i.issued_at AS DATE) = CAST(GETDATE() AS DATE)
                ) combined
                ORDER BY event_time DESC
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, facilityId);
            ps.setLong(2, facilityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("type", rs.getString("activity_type"));
                    row.put("refCode", rs.getString("ref_code"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("eventTime", rs.getString("event_time"));
                    BigDecimal amt = rs.getBigDecimal("amount");
                    row.put("amount", amt != null ? amt : BigDecimal.ZERO);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lịch sử hoạt động: " + e.getMessage(), e);
        }
        return list;
    }
}

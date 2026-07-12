package com.swp.dao;

import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaffDashboardDAO {

    public Map<String, Object> getCurrentShift(long staffId) {
        String sql = """
                SELECT ws.shift_id, ws.shift_name, ws.shift_date,
                       ws.start_time, ws.end_time,
                       f.complex_id, f.complex_name,
                       sa.status AS assignment_status
                FROM work_shifts ws
                JOIN shift_assignments sa ON ws.shift_id = sa.shift_id
                JOIN football_complexes f ON ws.complex_id = f.complex_id
                WHERE sa.staff_id = ?
                  AND ws.shift_date = CAST(GETDATE() AS DATE)
                ORDER BY ws.start_time
                """;
        List<Map<String, Object>> shifts = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> shift = new LinkedHashMap<>();
                    shift.put("shiftId", rs.getLong("shift_id"));
                    shift.put("shiftName", rs.getString("shift_name"));
                    shift.put("shiftDate", rs.getString("shift_date"));
                    shift.put("startTime", rs.getString("start_time"));
                    shift.put("endTime", rs.getString("end_time"));
                    shift.put("complexId", rs.getLong("complex_id"));
                    shift.put("complexName", rs.getString("complex_name"));
                    shift.put("assignmentStatus", rs.getString("assignment_status"));
                    shifts.add(shift);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn ca làm việc: " + e.getMessage(), e);
        }
        return selectBestShift(shifts);
    }

    public List<Map<String, Object>> getTodayBookings(long complexId) {
        String sql = """
                SELECT b.booking_id, b.booking_code,
                       b.start_time, b.end_time,
                       b.status, b.total_amount,
                       u.full_name AS customer_name,
                       fi.field_name,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM invoices i
                           WHERE i.booking_id = b.booking_id
                             AND i.status IN ('PAID', 'ACTIVE')
                       ) THEN 1 ELSE 0 END AS has_invoice
                FROM bookings b
                JOIN users u  ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id   = fi.field_id
                WHERE b.complex_id = ?
                  AND CAST(b.start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND b.status NOT IN ('CANCELLED','HOLD')
                ORDER BY b.start_time
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
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
                    row.put("hasInvoice", rs.getInt("has_invoice") == 1);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn booking hôm nay: " + e.getMessage(), e);
        }
        return list;
    }

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

    public Map<String, Object> getBookingKpi(long complexId) {
        String sql = """
                SELECT
                    COUNT(*) AS total_bookings,
                    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed
                FROM bookings
                WHERE complex_id = ?
                  AND CAST(start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND status NOT IN ('CANCELLED','HOLD')
                """;
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalBookings", 0);
        kpi.put("completed", 0);
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
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

    public int getPendingCheckinCount(long complexId) {
        String sql = """
                SELECT COUNT(*) AS cnt
                FROM bookings
                WHERE complex_id = ?
                  AND CAST(start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND status = 'CONFIRMED'
                  AND start_time <= GETDATE()
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm check-in chờ: " + e.getMessage(), e);
        }
        return 0;
    }

    public Double getAverageRatingToday(long complexId) {
        return null;
    }

    public List<Map<String, Object>> getRecentActivity(long complexId) {
        String sql = """
                SELECT TOP 5 *
                FROM (
                    SELECT
                        'CHECKIN' AS activity_type,
                        b.booking_code AS ref_code,
                        fi.field_name,
                        u.full_name AS customer_name,
                        c.checkin_time AS event_time,
                        CAST(0 AS DECIMAL(18,2)) AS amount
                    FROM checkins c
                    JOIN bookings b ON c.booking_id = b.booking_id
                    JOIN users u ON b.customer_id = u.user_id
                    JOIN fields fi ON b.field_id = fi.field_id
                    WHERE b.complex_id = ?

                    UNION ALL

                    SELECT
                        'INVOICE' AS activity_type,
                        i.invoice_code AS ref_code,
                        fi.field_name,
                        u.full_name AS customer_name,
                        i.issued_at AS event_time,
                        i.total_amount AS amount
                    FROM invoices i
                    JOIN bookings b ON i.booking_id = b.booking_id
                    JOIN users u ON i.customer_id = u.user_id
                    JOIN fields fi ON b.field_id = fi.field_id
                    WHERE b.complex_id = ?
                      AND i.status = 'PAID'
                ) activity
                WHERE CAST(activity.event_time AS DATE) = CAST(GETDATE() AS DATE)
                ORDER BY activity.event_time DESC
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            ps.setLong(2, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("type", rs.getString("activity_type"));
                    row.put("refCode", rs.getString("ref_code"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("eventTime", rs.getString("event_time"));
                    BigDecimal amount = rs.getBigDecimal("amount");
                    row.put("amount", amount != null ? amount : BigDecimal.ZERO);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lịch sử hoạt động: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean checkinBooking(long bookingId, long staffId, String note) {
        String updateSql = """
                UPDATE bookings
                SET status = 'CHECKED_IN', updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status = 'CONFIRMED'
                """;
        String insertSql = "INSERT INTO checkins (booking_id, staff_id, checkin_time, note) VALUES (?, ?, GETDATE(), ?)";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateSql);
                 PreparedStatement ps2 = conn.prepareStatement(insertSql)) {
                ps1.setLong(1, bookingId);
                int updated = ps1.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return false;
                }
                ps2.setLong(1, bookingId);
                ps2.setLong(2, staffId);
                ps2.setString(3, note != null ? note : "");
                ps2.executeUpdate();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi check-in: " + e.getMessage(), e);
        }
    }

    public boolean updateFieldStatus(long fieldId, String status) {
        String sql = "UPDATE fields SET status = ?, updated_at = GETDATE() WHERE field_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, fieldId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật trạng thái sân: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getBookingDetailForCheckin(long bookingId) {
        String sql = """
                SELECT b.booking_id, b.booking_code, b.total_amount, b.deposit_amount,
                       b.status,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       b.start_time, b.end_time, f.field_name, fc.complex_name
                FROM bookings b
                JOIN users u ON b.customer_id = u.user_id
                JOIN fields f ON b.field_id = f.field_id
                JOIN football_complexes fc ON b.complex_id = fc.complex_id
                WHERE b.booking_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookingId", rs.getLong("booking_id"));
                    map.put("bookingCode", rs.getString("booking_code"));
                    map.put("totalAmount", rs.getBigDecimal("total_amount"));
                    map.put("depositAmount", rs.getBigDecimal("deposit_amount"));
                    map.put("status", rs.getString("status"));
                    map.put("customerName", rs.getString("customer_name"));
                    map.put("customerPhone", rs.getString("customer_phone"));
                    map.put("startTime", rs.getString("start_time"));
                    map.put("endTime", rs.getString("end_time"));
                    map.put("fieldName", rs.getString("field_name"));
                    map.put("complexName", rs.getString("complex_name"));
                    return map;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy thông tin check-in: " + e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    public List<Map<String, Object>> getBookingsForDate(long complexId, String dateStr) {
        String sql = """
                SELECT b.booking_id, b.booking_code, b.start_time, b.end_time, b.status, b.total_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_id, fi.field_name,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM invoices i
                           WHERE i.booking_id = b.booking_id
                             AND i.status IN ('PAID', 'ACTIVE')
                       ) THEN 1 ELSE 0 END AS has_invoice
                FROM bookings b
                JOIN users u ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                WHERE b.complex_id = ?
                  AND CAST(b.start_time AS DATE) = ?
                  AND b.status NOT IN ('CANCELLED','HOLD')
                ORDER BY b.start_time
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            ps.setString(2, dateStr);
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
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldId", rs.getLong("field_id"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("hasInvoice", rs.getInt("has_invoice") == 1);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách đặt sân theo ngày: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Map<String, Object>> getFieldsForComplex(long complexId) {
        String sql = "SELECT field_id, field_name, status, description FROM fields WHERE complex_id = ? ORDER BY field_name";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("fieldId", rs.getLong("field_id"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("status", rs.getString("status"));
                    row.put("description", rs.getString("description"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách sân: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Map<String, Object>> searchConfirmedBookings(long complexId, String query) {
        String sql = """
                SELECT b.booking_id, b.booking_code,
                       b.start_time, b.end_time,
                       b.status, b.total_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name
                FROM bookings b
                JOIN users u  ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id   = fi.field_id
                WHERE b.complex_id = ?
                  AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'COMPLETED')
                  AND (
                      b.booking_code LIKE ?
                      OR u.full_name LIKE ?
                      OR u.phone LIKE ?
                  )
                ORDER BY b.start_time
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String likePattern = "%" + query + "%";
            ps.setLong(1, complexId);
            ps.setString(2, likePattern);
            ps.setString(3, likePattern);
            ps.setString(4, likePattern);
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
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldName", rs.getString("field_name"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm booking: " + e.getMessage(), e);
        }
        return list;
    }

    private static Map<String, Object> selectBestShift(List<Map<String, Object>> shifts) {
        if (shifts == null || shifts.isEmpty()) {
            return Collections.emptyMap();
        }
        if (shifts.size() == 1) {
            return shifts.get(0);
        }

        LocalTime now = LocalTime.now();

        for (Map<String, Object> shift : shifts) {
            LocalTime start = parseTime((String) shift.get("startTime"));
            LocalTime end = parseTime((String) shift.get("endTime"));
            if (isTimeInShift(now, start, end)) {
                return shift;
            }
        }

        Map<String, Object> bestUpcoming = null;
        LocalTime minUpcomingStart = null;
        for (Map<String, Object> shift : shifts) {
            LocalTime start = parseTime((String) shift.get("startTime"));
            if (now.isBefore(start) && (minUpcomingStart == null || start.isBefore(minUpcomingStart))) {
                minUpcomingStart = start;
                bestUpcoming = shift;
            }
        }
        if (bestUpcoming != null) {
            return bestUpcoming;
        }

        Map<String, Object> bestCompleted = null;
        LocalTime maxCompletedEnd = null;
        for (Map<String, Object> shift : shifts) {
            LocalTime end = parseTime((String) shift.get("endTime"));
            if (maxCompletedEnd == null || end.isAfter(maxCompletedEnd)) {
                maxCompletedEnd = end;
                bestCompleted = shift;
            }
        }
        return bestCompleted != null ? bestCompleted : shifts.get(0);
    }

    private static boolean isTimeInShift(LocalTime time, LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return false;
        }
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    private static LocalTime parseTime(String s) {
        if (s == null || s.trim().isEmpty()) {
            return LocalTime.MIDNIGHT;
        }
        s = s.trim();
        if (s.contains(" ")) {
            s = s.split(" ")[1];
        }
        if (s.contains(".")) {
            s = s.substring(0, s.indexOf('.'));
        }
        String[] parts = s.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return LocalTime.of(h, m);
    }
}

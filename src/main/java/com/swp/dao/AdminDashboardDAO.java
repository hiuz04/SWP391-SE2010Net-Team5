package com.swp.dao;

import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AdminDashboardDAO {

    public Map<String, Object> getDashboardKPIs() {
        Map<String, Object> kpis = new HashMap<>();
        
        kpis.put("todayRevenue", BigDecimal.ZERO);
        kpis.put("todayBookings", 0);
        kpis.put("newCustomers", 0);
        kpis.put("pendingUsers", 0);

        // 1. Doanh thu hôm nay
        String revSql = "SELECT ISNULL(SUM(CASE WHEN status IN ('CONFIRMED', 'CHECKED_IN', 'PENDING_CHECKOUT_PAYMENT') THEN deposit_amount WHEN status = 'COMPLETED' THEN total_amount ELSE 0 END), 0) FROM bookings WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(revSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                kpis.put("todayRevenue", rs.getBigDecimal(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. Lượt đặt sân hôm nay
        String bookSql = "SELECT COUNT(*) FROM bookings WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE) AND status NOT IN ('CANCELLED', 'HOLD')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(bookSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                kpis.put("todayBookings", rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 3. Khách hàng mới trong tuần (đã đổi thành hôm nay)
        String newCustSql = "SELECT COUNT(*) FROM users u JOIN roles r ON u.role_id = r.role_id WHERE r.role_name = 'Customer' AND CAST(u.created_at AS DATE) = CAST(GETDATE() AS DATE)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(newCustSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                kpis.put("newCustomers", rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 4. Doanh thu 30 ngày qua
        String sevDayRevSql = "SELECT ISNULL(SUM(CASE WHEN status IN ('CONFIRMED', 'CHECKED_IN', 'PENDING_CHECKOUT_PAYMENT') THEN deposit_amount WHEN status = 'COMPLETED' THEN total_amount ELSE 0 END), 0) FROM bookings WHERE CAST(created_at AS DATE) >= CAST(DATEADD(day, -29, GETDATE()) AS DATE)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sevDayRevSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                kpis.put("last30DaysRevenue", rs.getBigDecimal(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 5. Tài khoản chờ duyệt
        String pendingSql = "SELECT COUNT(*) FROM users WHERE status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(pendingSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                kpis.put("pendingUsers", rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 5. Số yêu cầu đặt sân chờ xử lý
        kpis.put("pendingBookings", 0);
        String pendingBookingsSql = "SELECT COUNT(*) FROM bookings WHERE status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(pendingBookingsSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                kpis.put("pendingBookings", rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return kpis;
    }

    public List<Map<String, Object>> getRevenueLast30Days() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                WITH Last30Days AS (
                    SELECT CAST(DATEADD(day, -number, GETDATE()) AS DATE) as d
                    FROM master..spt_values
                    WHERE type = 'P' AND number BETWEEN 0 AND 29
                )
                SELECT l.d as date, ISNULL(SUM(CASE 
                    WHEN b.status IN ('CONFIRMED', 'CHECKED_IN', 'PENDING_CHECKOUT_PAYMENT') THEN b.deposit_amount 
                    WHEN b.status = 'COMPLETED' THEN b.total_amount 
                    ELSE 0 
                END), 0) as total
                FROM Last30Days l
                LEFT JOIN bookings b ON CAST(b.created_at AS DATE) = l.d
                GROUP BY l.d
                ORDER BY l.d ASC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("date", rs.getDate("date").toString());
                map.put("total", rs.getBigDecimal("total"));
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getBookingsByFieldType() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT ft.type_name, COUNT(b.booking_id) as cnt
                FROM bookings b
                JOIN fields f ON b.field_id = f.field_id
                JOIN field_types ft ON f.field_type_id = ft.field_type_id
                WHERE b.status NOT IN ('CANCELLED', 'HOLD')
                AND b.created_at >= DATEADD(day, -30, GETDATE())
                GROUP BY ft.type_name
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("typeName", rs.getString("type_name"));
                map.put("count", rs.getInt("cnt"));
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getRecentBookings() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT TOP 5 b.booking_id, u.full_name as customer_name, u.phone as customer_phone,
                       f.field_name, fc.complex_name, b.start_time, b.end_time, 
                       (CASE 
                           WHEN b.status IN ('CONFIRMED', 'CHECKED_IN', 'PENDING_CHECKOUT_PAYMENT') THEN b.deposit_amount 
                           WHEN b.status = 'COMPLETED' THEN b.total_amount 
                           ELSE 0 
                       END) as total_amount, b.status
                FROM bookings b
                JOIN users u ON b.customer_id = u.user_id
                JOIN fields f ON b.field_id = f.field_id
                JOIN football_complexes fc ON f.complex_id = fc.complex_id
                WHERE b.status IN ('CONFIRMED', 'CHECKED_IN', 'PENDING_CHECKOUT_PAYMENT', 'COMPLETED')
                ORDER BY b.created_at DESC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("bookingId", rs.getLong("booking_id"));
                map.put("customerName", rs.getString("customer_name"));
                map.put("customerPhone", rs.getString("customer_phone"));
                map.put("fieldName", rs.getString("field_name"));
                map.put("complexName", rs.getString("complex_name"));
                map.put("startTime", rs.getTimestamp("start_time").toString());
                map.put("endTime", rs.getTimestamp("end_time").toString());
                map.put("totalAmount", rs.getBigDecimal("total_amount"));
                map.put("status", rs.getString("status"));
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}

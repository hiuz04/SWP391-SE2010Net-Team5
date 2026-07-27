package com.swp.dao;

import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                  AND (
                      ws.shift_date = CAST(GETDATE() AS DATE)
                      OR (
                          ws.shift_date = DATEADD(day, -1, CAST(GETDATE() AS DATE))
                          AND ws.end_time < ws.start_time
                          AND CAST(GETDATE() AS TIME) <= ws.end_time
                      )
                  )
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

    public Map<String, Object> getDefaultComplex(long userId) {
        String sql = """
                SELECT TOP 1 f.complex_id, f.complex_name
                FROM football_complexes f
                ORDER BY f.complex_id
                """;
        Map<String, Object> res = new LinkedHashMap<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                res.put("complexId", rs.getLong("complex_id"));
                res.put("complexName", rs.getString("complex_name"));
            }
        } catch (SQLException e) {
            // Ignored
        }
        return res;
    }

    /**
     * Lấy booking trong ngày cho dashboard Staff, kèm cờ hỗ trợ checkout/invoice.
     * has_invoice giúp UI mở hóa đơn hiện có, checkout_due cho biết booking đã sẵn sàng gửi invoice checkout.
     */
    public List<Map<String, Object>> getTodayBookings(long complexId) {
        String sql = """
                SELECT b.booking_id, b.booking_code,
                       b.start_time, b.end_time,
                       b.status, b.total_amount, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       pm_info.method_name AS payment_method_name,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM invoices i
                           WHERE i.booking_id = b.booking_id
                             AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                       ) THEN 1 ELSE 0 END AS has_invoice,
                       CASE WHEN b.status = 'CHECKED_IN' THEN 1 ELSE 0 END AS checkout_due
                FROM bookings b
                JOIN users u  ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id   = fi.field_id
                OUTER APPLY (
                    SELECT TOP 1 pm.method_name
                    FROM payments p
                    JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = b.booking_id AND p.status = 'SUCCESS'
                    ORDER BY CASE WHEN p.payment_type = 'CHECKOUT' THEN 0 ELSE 1 END, p.payment_id DESC
                ) pm_info
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
                    row.put("depositAmount", rs.getBigDecimal("deposit_amount"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("hasInvoice", rs.getInt("has_invoice") == 1);
                    row.put("checkoutDue", rs.getInt("checkout_due") == 1);

                    String pmName = rs.getString("payment_method_name");
                    String pmDisplay = "Tiền mặt";
                    if (pmName != null) {
                        String mUpper = pmName.toUpperCase();
                        if (mUpper.contains("VNPAY") || mUpper.contains("TRANSFER") || mUpper.contains("BANK") || mUpper.contains("ONLINE") || mUpper.contains("QR")) {
                            pmDisplay = "Chuyển khoản";
                        }
                    }
                    row.put("paymentMethodName", pmDisplay);

                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn booking hôm nay: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Tính KPI tiền thu từ invoice PAID do Staff lập trong khung giờ ca làm việc.
     */
    public Map<String, Object> getCashKpi(long staffId, String shiftDateStr,
                                          String startTimeStr, String endTimeStr) {
        String sql = """
                SELECT
                    COALESCE(SUM(i.total_amount), 0) AS total_cash,
                    COUNT(DISTINCT i.invoice_id) AS tx_count
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                OUTER APPLY (
                    SELECT TOP 1 pm.method_name
                    FROM payments p
                    JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = b.booking_id AND p.status = 'SUCCESS'
                    ORDER BY CASE WHEN p.payment_type = 'CHECKOUT' THEN 0 ELSE 1 END, p.payment_id DESC
                ) pm_info
                WHERE i.staff_id = ?
                  AND i.status   = 'PAID'
                  AND CAST(i.issued_at AS DATE) = ?
                  AND CAST(i.issued_at AS TIME) BETWEEN ? AND ?
                  AND (pm_info.method_name IS NULL OR (
                      UPPER(pm_info.method_name) NOT LIKE '%VNPAY%'
                      AND UPPER(pm_info.method_name) NOT LIKE '%TRANSFER%'
                      AND UPPER(pm_info.method_name) NOT LIKE '%BANK%'
                      AND UPPER(pm_info.method_name) NOT LIKE '%ONLINE%'
                  ))
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

    /**
     * Tính tổng doanh thu thu được từ tất cả phương thức (Tiền mặt + Chuyển khoản/Online) trong ca.
     */
    public Map<String, Object> getTotalRevenueKpi(long complexId, String shiftDateStr,
                                                  String startTimeStr, String endTimeStr) {
        String sql = """
                SELECT
                    COALESCE(SUM(i.total_amount), 0) AS total_revenue,
                    COUNT(*) AS tx_count
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                WHERE b.complex_id = ?
                  AND i.status = 'PAID'
                  AND CAST(i.issued_at AS DATE) = ?
                  AND CAST(i.issued_at AS TIME) BETWEEN ? AND ?
                """;
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalRevenue", BigDecimal.ZERO);
        kpi.put("txCount", 0);
        kpi.put("avgTransaction", BigDecimal.ZERO);
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            ps.setString(2, shiftDateStr);
            ps.setString(3, startTimeStr);
            ps.setString(4, endTimeStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total_revenue");
                    int count = rs.getInt("tx_count");
                    kpi.put("totalRevenue", total);
                    kpi.put("txCount", count);
                    BigDecimal avg = (count > 0)
                            ? total.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    kpi.put("avgTransaction", avg);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tính tổng doanh thu: " + e.getMessage(), e);
        }
        return kpi;
    }

    /**
     * Lấy danh sách hóa đơn đã thanh toán TIỀN MẶT trong ca của Staff.
     */
    public List<Map<String, Object>> getCashTransactions(long staffId, String shiftDateStr,
                                                         String startTimeStr, String endTimeStr) {
        String sql = """
                SELECT i.invoice_id, i.invoice_code, i.booking_id, b.booking_code,
                       i.total_amount, i.issued_at, u.full_name AS customer_name,
                       u.phone AS customer_phone, fi.field_name,
                       pm_info.method_name AS payment_method_name
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                JOIN users u ON i.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                OUTER APPLY (
                    SELECT TOP 1 pm.method_name
                    FROM payments p
                    JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = b.booking_id AND p.status = 'SUCCESS'
                    ORDER BY CASE WHEN p.payment_type = 'CHECKOUT' THEN 0 ELSE 1 END, p.payment_id DESC
                ) pm_info
                WHERE i.staff_id = ?
                  AND i.status   = 'PAID'
                  AND CAST(i.issued_at AS DATE) = ?
                  AND CAST(i.issued_at AS TIME) BETWEEN ? AND ?
                  AND (pm_info.method_name IS NULL OR (
                      UPPER(pm_info.method_name) NOT LIKE '%VNPAY%'
                      AND UPPER(pm_info.method_name) NOT LIKE '%TRANSFER%'
                      AND UPPER(pm_info.method_name) NOT LIKE '%BANK%'
                      AND UPPER(pm_info.method_name) NOT LIKE '%ONLINE%'
                  ))
                ORDER BY i.issued_at DESC
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setString(2, shiftDateStr);
            ps.setString(3, startTimeStr);
            ps.setString(4, endTimeStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("invoiceId", rs.getLong("invoice_id"));
                    row.put("invoiceCode", rs.getString("invoice_code"));
                    row.put("bookingId", rs.getLong("booking_id"));
                    row.put("bookingCode", rs.getString("booking_code"));
                    row.put("totalAmount", rs.getBigDecimal("total_amount"));
                    row.put("issuedAt", rs.getString("issued_at"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("paymentMethodName", "Tiền mặt");
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách giao dịch tiền mặt: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Lấy TẤT CẢ danh sách hóa đơn đã thanh toán (Cả Tiền mặt lẫn Chuyển khoản) trong ca.
     */
    public List<Map<String, Object>> getAllTransactions(long complexId, String shiftDateStr,
                                                       String startTimeStr, String endTimeStr) {
        String sql = """
                SELECT i.invoice_id, i.invoice_code, i.booking_id, b.booking_code,
                       i.total_amount, i.issued_at, u.full_name AS customer_name,
                       u.phone AS customer_phone, fi.field_name,
                       pm_info.method_name AS payment_method_name
                FROM invoices i
                JOIN bookings b ON i.booking_id = b.booking_id
                JOIN users u ON i.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                OUTER APPLY (
                    SELECT TOP 1 pm.method_name
                    FROM payments p
                    JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = b.booking_id AND p.status = 'SUCCESS'
                    ORDER BY CASE WHEN p.payment_type = 'CHECKOUT' THEN 0 ELSE 1 END, p.payment_id DESC
                ) pm_info
                WHERE b.complex_id = ?
                  AND i.status   = 'PAID'
                  AND CAST(i.issued_at AS DATE) = ?
                  AND CAST(i.issued_at AS TIME) BETWEEN ? AND ?
                ORDER BY i.issued_at DESC
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            ps.setString(2, shiftDateStr);
            ps.setString(3, startTimeStr);
            ps.setString(4, endTimeStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("invoiceId", rs.getLong("invoice_id"));
                    row.put("invoiceCode", rs.getString("invoice_code"));
                    row.put("bookingId", rs.getLong("booking_id"));
                    row.put("bookingCode", rs.getString("booking_code"));
                    row.put("totalAmount", rs.getBigDecimal("total_amount"));
                    row.put("issuedAt", rs.getString("issued_at"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldName", rs.getString("field_name"));

                    String pmName = rs.getString("payment_method_name");
                    String pmDisplay = "Tiền mặt";
                    if (pmName != null) {
                        String mUpper = pmName.toUpperCase();
                        if (mUpper.contains("VNPAY") || mUpper.contains("TRANSFER") || mUpper.contains("BANK") || mUpper.contains("ONLINE") || mUpper.contains("QR")) {
                            pmDisplay = "Chuyển khoản";
                        }
                    }
                    row.put("paymentMethodName", pmDisplay);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy tất cả giao dịch: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Tính số booking trong ngày và số booking đã COMPLETED sau checkout/payment.
     */
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
                  AND end_time > GETDATE()
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

    /**
     * Lấy hoạt động gần đây cho dashboard, trong đó nhánh INVOICE phản ánh các hóa đơn checkout đã thanh toán.
     */
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

    /**
     * Check-in booking cho Staff.
     * Method chỉ đổi trạng thái khi booking đang CONFIRMED và ghi bản ghi checkins trong cùng transaction.
     */
    public boolean checkinBooking(long bookingId, long staffId, String note) {
        String updateSql = """
                UPDATE bookings
                SET status = 'CHECKED_IN', updated_at = GETDATE()
                WHERE booking_id = ?
                  AND status = 'CONFIRMED'
                  AND CAST(start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND end_time > GETDATE()
                """;
        String insertSql = "INSERT INTO checkins (booking_id, staff_id, checkin_time, note) VALUES (?, ?, GETDATE(), ?)";
        String insertLogSql = """
                INSERT INTO booking_status_logs (
                    booking_id, old_status, new_status, changed_by, note, created_at
                )
                VALUES (?, 'CONFIRMED', 'CHECKED_IN', ?, ?, GETDATE())
                """;
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateSql);
                 PreparedStatement ps2 = conn.prepareStatement(insertSql);
                 PreparedStatement ps3 = conn.prepareStatement(insertLogSql)) {
                // Business Rule BR-13: Chỉ booking CONFIRMED mới được chuyển sang CHECKED_IN.
                ps1.setLong(1, bookingId);
                int updated = ps1.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return false;
                }
                // Business Rule BR-13: Check-in thành công phải tạo log nhận sân cho booking.
                ps2.setLong(1, bookingId);
                ps2.setLong(2, staffId);
                ps2.setString(3, note != null ? note : "");
                ps2.executeUpdate();

                ps3.setLong(1, bookingId);
                ps3.setLong(2, staffId);
                ps3.setString(3, note != null && !note.isBlank()
                        ? note
                        : "Staff check-in booking.");
                ps3.executeUpdate();
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
        return getBookingDetailForCheckin("b.booking_id = ?", ps -> ps.setLong(1, bookingId));
    }

    public Map<String, Object> getBookingDetailForCheckinByCode(String bookingCode) {
        return getBookingDetailForCheckin("UPPER(b.booking_code) = UPPER(?)", ps -> ps.setString(1, bookingCode));
    }

    private Map<String, Object> getBookingDetailForCheckin(String whereClause, SqlBinder binder) {
        String sql = """
                SELECT b.booking_id, b.booking_code,
                       b.original_price, b.discount_amount, b.total_amount, b.final_amount, b.deposit_amount,
                       b.status, b.complex_id,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       b.start_time, b.end_time, f.field_name, fc.complex_name,
                       dp.status AS deposit_payment_status,
                       dp.amount AS deposit_paid_amount,
                       dp.paid_at AS deposit_paid_at,
                       pm_info.method_name AS payment_method_name,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM invoices i
                           WHERE i.booking_id = b.booking_id
                             AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                       ) THEN 1 ELSE 0 END AS has_invoice,
                       CASE WHEN b.status = 'CHECKED_IN' THEN 1 ELSE 0 END AS checkout_due,
                       CASE WHEN CAST(b.start_time AS DATE) = CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END AS booking_today,
                       CASE WHEN b.end_time > GETDATE() THEN 1 ELSE 0 END AS not_expired
                FROM bookings b
                JOIN users u ON b.customer_id = u.user_id
                JOIN fields f ON b.field_id = f.field_id
                JOIN football_complexes fc ON b.complex_id = fc.complex_id
                OUTER APPLY (
                    SELECT TOP 1 p.status, p.amount, p.paid_at
                    FROM payments p
                    WHERE p.booking_id = b.booking_id
                      AND p.payment_type = 'DEPOSIT'
                    ORDER BY CASE WHEN p.status = 'SUCCESS' THEN 0 ELSE 1 END,
                             p.paid_at DESC,
                             p.payment_id DESC
                ) dp
                OUTER APPLY (
                    SELECT TOP 1 pm.method_name
                    FROM payments p
                    JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = b.booking_id AND p.status = 'SUCCESS'
                    ORDER BY CASE WHEN p.payment_type = 'CHECKOUT' THEN 0 ELSE 1 END, p.payment_id DESC
                ) pm_info
                WHERE
                """ + whereClause;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookingId", rs.getLong("booking_id"));
                    map.put("bookingCode", rs.getString("booking_code"));
                    map.put("originalPrice", rs.getBigDecimal("original_price"));
                    map.put("discountAmount", rs.getBigDecimal("discount_amount"));
                    map.put("totalAmount", rs.getBigDecimal("total_amount"));
                    map.put("finalAmount", rs.getBigDecimal("final_amount"));
                    map.put("depositAmount", rs.getBigDecimal("deposit_amount"));
                    map.put("status", rs.getString("status"));
                    map.put("complexId", rs.getLong("complex_id"));
                    map.put("customerName", rs.getString("customer_name"));
                    map.put("customerPhone", rs.getString("customer_phone"));
                    map.put("startTime", rs.getString("start_time"));
                    map.put("endTime", rs.getString("end_time"));
                    map.put("fieldName", rs.getString("field_name"));
                    map.put("complexName", rs.getString("complex_name"));
                    map.put("paymentStatus", rs.getString("deposit_payment_status"));
                    map.put("paymentPaidAmount", rs.getBigDecimal("deposit_paid_amount"));
                    map.put("paymentPaidAt", rs.getString("deposit_paid_at"));
                    map.put("hasInvoice", rs.getInt("has_invoice") == 1);
                    map.put("checkoutDue", rs.getInt("checkout_due") == 1);
                    map.put("bookingToday", rs.getInt("booking_today") == 1);
                    map.put("notExpired", rs.getInt("not_expired") == 1);

                    String pmName = rs.getString("payment_method_name");
                    String pmDisplay = "Tiền mặt";
                    if (pmName != null) {
                        String mUpper = pmName.toUpperCase();
                        if (mUpper.contains("VNPAY") || mUpper.contains("TRANSFER") || mUpper.contains("BANK") || mUpper.contains("ONLINE") || mUpper.contains("QR")) {
                            pmDisplay = "Chuyển khoản";
                        }
                    }
                    map.put("paymentMethodName", pmDisplay);

                    return map;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy thông tin check-in: " + e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    /**
     * Lấy booking theo ngày cho lịch Staff, kèm cờ invoice/checkout để UI hiển thị nút Checkout hoặc Hóa đơn.
     */
    public List<Map<String, Object>> getBookingsForDate(long complexId, String dateStr) {
        String sql = """
                SELECT b.booking_id, b.booking_code, b.start_time, b.end_time, b.status, b.total_amount, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_id, fi.field_name,
                       pm_info.method_name AS payment_method_name,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM invoices i
                           WHERE i.booking_id = b.booking_id
                             AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                       ) THEN 1 ELSE 0 END AS has_invoice,
                       CASE WHEN b.status = 'CHECKED_IN' THEN 1 ELSE 0 END AS checkout_due
                FROM bookings b
                JOIN users u ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id = fi.field_id
                OUTER APPLY (
                    SELECT TOP 1 pm.method_name
                    FROM payments p
                    JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = b.booking_id AND p.status = 'SUCCESS'
                    ORDER BY CASE WHEN p.payment_type = 'CHECKOUT' THEN 0 ELSE 1 END, p.payment_id DESC
                ) pm_info
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
                    row.put("depositAmount", rs.getBigDecimal("deposit_amount"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldId", rs.getLong("field_id"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("hasInvoice", rs.getInt("has_invoice") == 1);
                    row.put("checkoutDue", rs.getInt("checkout_due") == 1);

                    String pmName = rs.getString("payment_method_name");
                    String pmDisplay = "Tiền mặt";
                    if (pmName != null) {
                        String mUpper = pmName.toUpperCase();
                        if (mUpper.contains("VNPAY") || mUpper.contains("TRANSFER") || mUpper.contains("BANK") || mUpper.contains("ONLINE") || mUpper.contains("QR")) {
                            pmDisplay = "Chuyển khoản";
                        }
                    }
                    row.put("paymentMethodName", pmDisplay);

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
                       b.status, b.total_amount, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       dp.status AS deposit_payment_status,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM invoices i
                           WHERE i.booking_id = b.booking_id
                             AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                       ) THEN 1 ELSE 0 END AS has_invoice,
                       CASE WHEN b.status = 'CHECKED_IN' THEN 1 ELSE 0 END AS checkout_due,
                       CASE WHEN CAST(b.start_time AS DATE) = CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END AS booking_today,
                       CASE WHEN b.end_time > GETDATE() THEN 1 ELSE 0 END AS not_expired
                FROM bookings b
                JOIN users u  ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id   = fi.field_id
                OUTER APPLY (
                    SELECT TOP 1 p.status
                    FROM payments p
                    WHERE p.booking_id = b.booking_id
                      AND p.payment_type = 'DEPOSIT'
                    ORDER BY CASE WHEN p.status = 'SUCCESS' THEN 0 ELSE 1 END,
                             p.paid_at DESC,
                             p.payment_id DESC
                ) dp
                WHERE b.complex_id = ?
                  AND b.status IN ('HOLD', 'CONFIRMED', 'CHECKED_IN', 'PENDING_CHECKOUT_PAYMENT', 'COMPLETED', 'CANCELLED', 'EXPIRED')
                  AND (
                      b.booking_code LIKE ?
                      OR u.full_name LIKE ?
                      OR u.phone LIKE ?
                  )
                ORDER BY CASE WHEN CAST(b.start_time AS DATE) = CAST(GETDATE() AS DATE) THEN 0 ELSE 1 END,
                         b.start_time DESC
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
                    row.put("depositAmount", rs.getBigDecimal("deposit_amount"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("paymentStatus", rs.getString("deposit_payment_status"));
                    row.put("hasInvoice", rs.getInt("has_invoice") == 1);
                    row.put("checkoutDue", rs.getInt("checkout_due") == 1);
                    row.put("bookingToday", rs.getInt("booking_today") == 1);
                    row.put("notExpired", rs.getInt("not_expired") == 1);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm booking: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Map<String, Object>> getPendingCheckinBookings(long complexId) {
        String sql = """
                SELECT b.booking_id, b.booking_code,
                       b.start_time, b.end_time,
                       b.status, b.total_amount, b.deposit_amount,
                       u.full_name AS customer_name, u.phone AS customer_phone,
                       fi.field_name,
                       dp.status AS deposit_payment_status,
                       CASE WHEN EXISTS (
                           SELECT 1
                           FROM invoices i
                           WHERE i.booking_id = b.booking_id
                             AND i.status IN ('PENDING', 'PAID', 'ACTIVE')
                       ) THEN 1 ELSE 0 END AS has_invoice,
                       CASE WHEN b.status = 'CHECKED_IN' THEN 1 ELSE 0 END AS checkout_due,
                       CASE WHEN CAST(b.start_time AS DATE) = CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END AS booking_today,
                       CASE WHEN b.end_time > GETDATE() THEN 1 ELSE 0 END AS not_expired
                FROM bookings b
                JOIN users u  ON b.customer_id = u.user_id
                JOIN fields fi ON b.field_id   = fi.field_id
                OUTER APPLY (
                    SELECT TOP 1 p.status
                    FROM payments p
                    WHERE p.booking_id = b.booking_id
                      AND p.payment_type = 'DEPOSIT'
                    ORDER BY CASE WHEN p.status = 'SUCCESS' THEN 0 ELSE 1 END,
                             p.paid_at DESC,
                             p.payment_id DESC
                ) dp
                WHERE b.complex_id = ?
                  AND b.status IN ('CONFIRMED', 'CHECKED_IN')
                  AND CAST(b.start_time AS DATE) = CAST(GETDATE() AS DATE)
                  AND (b.status <> 'CONFIRMED' OR b.end_time > GETDATE())
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
                    row.put("depositAmount", rs.getBigDecimal("deposit_amount"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("customerPhone", rs.getString("customer_phone"));
                    row.put("fieldName", rs.getString("field_name"));
                    row.put("paymentStatus", rs.getString("deposit_payment_status"));
                    row.put("hasInvoice", rs.getInt("has_invoice") == 1);
                    row.put("checkoutDue", rs.getInt("checkout_due") == 1);
                    row.put("bookingToday", rs.getInt("booking_today") == 1);
                    row.put("notExpired", rs.getInt("not_expired") == 1);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách check-in chờ: " + e.getMessage(), e);
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

    private static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return LocalTime.MIDNIGHT;
        }
        timeStr = timeStr.trim().toUpperCase();

        boolean pm = timeStr.contains("CH") || timeStr.contains("PM");
        boolean am = timeStr.contains("SA") || timeStr.contains("AM");

        if (timeStr.contains(" ")) {
            String[] parts = timeStr.split(" ");
            for (String part : parts) {
                if (part.contains(":")) {
                    timeStr = part;
                    break;
                }
            }
        }
        if (timeStr.contains(".")) {
            timeStr = timeStr.split("\\.")[0];
        }

        String clean = timeStr.replaceAll("[^0-9:]", "").trim();
        if (clean.isEmpty()) return LocalTime.MIDNIGHT;

        String[] parts = clean.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        if (pm) {
            if (hour < 12) hour += 12;
        } else if (am) {
            if (hour == 12) hour = 0;
        }

        return LocalTime.of(hour, min, sec);
    }
}

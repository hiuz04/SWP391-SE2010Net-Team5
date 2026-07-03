<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.swp.util.DBContext" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>Khởi Tạo Dữ Liệu Test | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    body { background: #f1f5f9; font-family: 'Inter', sans-serif; padding-top: 50px; }
    .setup-card { max-width: 700px; margin: auto; background: #fff; border-radius: 20px; border: 1px solid #e2e8f0; box-shadow: 0 10px 30px rgba(15,23,42,.05); padding: 40px; }
    .status-step { padding: 12px 18px; border-radius: 12px; margin-bottom: 12px; border: 1px solid #cbd5e1; background: #f8fafc; font-size: 0.9rem; }
    .status-step.success { background: #dcfce7; border-color: #bbf7d0; color: #16a34a; font-weight: 600; }
    .status-step.error { background: #fee2e2; border-color: #fecaca; color: #dc2626; font-weight: 600; }
  </style>
</head>
<body>
<div class="container">
  <div class="setup-card">
    <div class="text-center mb-4">
      <span class="fs-1">⚽</span>
      <h2 class="fw-bold mt-2">Cài Đặt Dữ Liệu Test Cho Hôm Nay</h2>
      <p class="text-muted">Tạo nhanh ca làm việc, phân ca, lịch đặt sân, check-in và hóa đơn cho ngày hôm nay để test chức năng Staff.</p>
    </div>

    <div class="d-flex flex-column gap-2 mb-4">
      <%
        String ctx = request.getContextPath();
        String todayStr = LocalDate.now().toString();
        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);
            
            // Step 1: Clean up
            out.println("<div class='status-step success'><i class='bi bi-trash3-fill me-2'></i>Đang dọn dẹp các lịch đặt sân, ca làm việc cũ của hôm nay (" + todayStr + ")...</div>");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "IF OBJECT_ID('booking_status_logs', 'U') IS NOT NULL " +
                    "DELETE FROM booking_status_logs WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('booking_promotions', 'U') IS NOT NULL " +
                    "DELETE FROM booking_promotions WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('booking_services', 'U') IS NOT NULL " +
                    "DELETE FROM booking_services WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('booking_details', 'U') IS NOT NULL " +
                    "DELETE FROM booking_details WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('booking_items', 'U') IS NOT NULL " +
                    "DELETE FROM booking_items WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('feedbacks', 'U') IS NOT NULL " +
                    "DELETE FROM feedbacks WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('reviews', 'U') IS NOT NULL " +
                    "DELETE FROM reviews WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('payment_callbacks', 'U') IS NOT NULL " +
                    "DELETE FROM payment_callbacks WHERE payment_id IN (SELECT payment_id FROM payments WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "'))"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('payments', 'U') IS NOT NULL " +
                    "DELETE FROM payments WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "DELETE FROM invoices WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "DELETE FROM checkins WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "DELETE FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "'"
                );
                stmt.executeUpdate(
                    "DELETE FROM shift_assignments WHERE shift_id IN (SELECT shift_id FROM work_shifts WHERE shift_date = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "DELETE FROM work_shifts WHERE shift_date = '" + todayStr + "'"
                );
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã dọn dẹp dữ liệu cũ thành công.</div>");

            // Step 2: Insert work shifts
            long shift1Id = 0;
            long shift2Id = 0;
            long shift3Id = 0;
            String insertShiftSql = "INSERT INTO work_shifts (facility_id, shift_name, shift_date, start_time, end_time, created_at) VALUES (?, ?, ?, ?, ?, GETDATE())";
            try (PreparedStatement ps = conn.prepareStatement(insertShiftSql, Statement.RETURN_GENERATED_KEYS)) {
                // Shift 1
                ps.setLong(1, 1);
                ps.setString(2, "Ca sáng Mỹ Đình");
                ps.setString(3, todayStr);
                ps.setString(4, "00:00:00");
                ps.setString(5, "08:00:00");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) shift1Id = rs.getLong(1);
                }

                // Shift 2
                ps.setLong(1, 1);
                ps.setString(2, "Ca chiều Mỹ Đình");
                ps.setString(3, todayStr);
                ps.setString(4, "08:00:00");
                ps.setString(5, "16:00:00");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) shift2Id = rs.getLong(1);
                }

                // Shift 3
                ps.setLong(1, 1);
                ps.setString(2, "Ca tối Mỹ Đình");
                ps.setString(3, todayStr);
                ps.setString(4, "16:00:00");
                ps.setString(5, "22:00:00");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) shift3Id = rs.getLong(1);
                }
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã tạo 3 ca làm việc hôm nay (Shift 1: " + shift1Id + ", Shift 2: " + shift2Id + ", Shift 3: " + shift3Id + ")</div>");

            // Step 3: Insert assignments
            String insertAssignSql = "INSERT INTO shift_assignments (shift_id, staff_id, status) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertAssignSql)) {
                // Assign staff 4 to Shift 1
                ps.setLong(1, shift1Id);
                ps.setLong(2, 4); // Phạm Minh Staff (role_id = 3)
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();

                // Assign staff 5 to Shift 2
                ps.setLong(1, shift2Id);
                ps.setLong(2, 5); // Đỗ Anh Staff (role_id = 3)
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();

                // Assign staff 5 to Shift 3
                ps.setLong(1, shift3Id);
                ps.setLong(2, 5); // Đỗ Anh Staff (role_id = 3)
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã phân ca cho Phạm Minh Staff (ID: 4) và Đỗ Anh Staff (ID: 5).</div>");

            // Step 4: Insert bookings
            long booking1Id = 0;
            long booking2Id = 0;
            long booking3Id = 0;
            long booking4Id = 0;
            long booking5Id = 0;
            long booking6Id = 0;
            long booking7Id = 0;
            long booking8Id = 0;
            long booking9Id = 0;

            String insertBookingSql = "INSERT INTO bookings (" +
                " booking_code, customer_id, facility_id, field_id, start_time, end_time," +
                " original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
            
            try (PreparedStatement ps = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                // Booking 1: CONFIRMED
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "1");
                ps.setLong(2, 6);
                ps.setLong(3, 1);
                ps.setLong(4, 1);
                ps.setString(5, todayStr + " 07:00:00");
                ps.setString(6, todayStr + " 08:30:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking1Id = rs.getLong(1);
                }

                // Booking 2: CHECKED_IN
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "2");
                ps.setLong(2, 7);
                ps.setLong(3, 1);
                ps.setLong(4, 2);
                ps.setString(5, todayStr + " 09:00:00");
                ps.setString(6, todayStr + " 10:30:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("150000"));
                ps.setString(11, "CHECKED_IN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking2Id = rs.getLong(1);
                }

                // Booking 3: COMPLETED
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "3");
                ps.setLong(2, 8);
                ps.setLong(3, 1);
                ps.setLong(4, 3);
                ps.setString(5, todayStr + " 11:00:00");
                ps.setString(6, todayStr + " 12:30:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "COMPLETED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking3Id = rs.getLong(1);
                }

                // Booking 4: CONFIRMED
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "4");
                ps.setLong(2, 9);
                ps.setLong(3, 1);
                ps.setLong(4, 1);
                ps.setString(5, todayStr + " 13:00:00");
                ps.setString(6, todayStr + " 14:30:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("100000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking4Id = rs.getLong(1);
                }

                // Booking 5: CONFIRMED
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "5");
                ps.setLong(2, 6);
                ps.setLong(3, 1);
                ps.setLong(4, 2);
                ps.setString(5, todayStr + " 15:00:00");
                ps.setString(6, todayStr + " 16:30:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("100000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking5Id = rs.getLong(1);
                }

                // Booking 6: CONFIRMED (Test Checkin at 19:00 today)
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "6");
                ps.setLong(2, 8);
                ps.setLong(3, 1);
                ps.setLong(4, 1);
                ps.setString(5, todayStr + " 19:00:00");
                ps.setString(6, todayStr + " 20:30:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking6Id = rs.getLong(1);
                }

                // Booking 7: CHECKED_IN (Test Checkout at 17:30 - 19:00 today)
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "7");
                ps.setLong(2, 9);
                ps.setLong(3, 1);
                ps.setLong(4, 2);
                ps.setString(5, todayStr + " 17:30:00");
                ps.setString(6, todayStr + " 19:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("150000"));
                ps.setString(11, "CHECKED_IN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking7Id = rs.getLong(1);
                }

                // Booking 8: CONFIRMED (Another Test Checkin at 19:30 today)
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "8");
                ps.setLong(2, 6);
                ps.setLong(3, 1);
                ps.setLong(4, 3);
                ps.setString(5, todayStr + " 19:30:00");
                ps.setString(6, todayStr + " 21:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking8Id = rs.getLong(1);
                }

                // Booking 9: CHECKED_IN (Another Test Checkout at 18:00 - 19:30 today)
                ps.setString(1, "BK" + System.currentTimeMillis() % 100000000L + "9");
                ps.setLong(2, 7);
                ps.setLong(3, 1);
                ps.setLong(4, 1);
                ps.setString(5, todayStr + " 18:00:00");
                ps.setString(6, todayStr + " 19:30:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("100000"));
                ps.setString(11, "CHECKED_IN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking9Id = rs.getLong(1);
                }
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã tạo thành công các lịch đặt sân (bookings) cho hôm nay.</div>");

            // Step 5: Checkins
            String insertCheckinSql = "INSERT INTO checkins (booking_id, staff_id, checkin_time, note) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertCheckinSql)) {
                // Checkin for Booking 2
                ps.setLong(1, booking2Id);
                ps.setLong(2, 4);
                ps.setString(3, todayStr + " 09:05:00");
                ps.setString(4, "Đến đúng giờ");
                ps.executeUpdate();

                // Checkin for Booking 7 (Đỗ Anh Staff check-in)
                ps.setLong(1, booking7Id);
                ps.setLong(2, 5);
                ps.setString(3, todayStr + " 17:35:00");
                ps.setString(4, "Khách đến đúng giờ");
                ps.executeUpdate();

                // Checkin for Booking 9 (Đỗ Anh Staff check-in)
                ps.setLong(1, booking9Id);
                ps.setLong(2, 5);
                ps.setString(3, todayStr + " 18:05:00");
                ps.setString(4, "Khách đến đúng giờ");
                ps.executeUpdate();
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã thực hiện check-in mẫu.</div>");

            // Step 6: Invoices
            String insertInvoiceSql = "INSERT INTO invoices (invoice_code, booking_id, customer_id, staff_id, subtotal, discount_amount, total_amount, paid_amount, status, issued_at)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PAID', ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertInvoiceSql)) {
                ps.setString(1, "INV" + System.currentTimeMillis() % 100000000L);
                ps.setLong(2, booking3Id);
                ps.setLong(3, 8);
                ps.setLong(4, 4);
                ps.setBigDecimal(5, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(6, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(7, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(8, new java.math.BigDecimal("600000"));
                ps.setString(9, todayStr + " 12:35:00");
                ps.executeUpdate();
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã xuất hóa đơn 600,000₫ cho Booking 3.</div>");

            conn.commit();
            out.println("<h4 class='text-success fw-bold text-center mt-4'><i class='bi bi-check-all me-1'></i>Hoàn tất thiết lập dữ liệu test!</h4>");

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            out.println("<div class='status-step error'><i class='bi bi-exclamation-triangle-fill me-2'></i>Lỗi khởi tạo dữ liệu: " + e.getMessage() + "</div>");
            e.printStackTrace(new java.io.PrintWriter(out));
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
      %>
    </div>

    <div class="text-center d-flex gap-2 justify-content-center">
      <a href="<%= ctx %>/staff/dashboard" class="btn btn-primary btn-lg">Đi tới Staff Dashboard</a>
      <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary btn-lg">Xem lịch sân bóng</a>
    </div>
  </div>
</div>
</body>
</html>

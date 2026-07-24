<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.LocalDateTime" %>
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
      <p class="text-muted">Tạo nhanh ca làm việc (08:00 - 12:00), lịch đặt sân, check-in, hóa đơn và dữ liệu tìm đối (matchmaking).</p>
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
            out.println("<div class='status-step success'><i class='bi bi-trash3-fill me-2'></i>Đang dọn dẹp dữ liệu cũ của hôm nay (" + todayStr + ")...</div>");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM matchmaking_post_responses");
                stmt.executeUpdate("DELETE FROM matchmaking_posts");
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
                    "IF OBJECT_ID('voucher_usages', 'U') IS NOT NULL " +
                    "DELETE FROM voucher_usages WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "') " +
                    "OR payment_id IN (SELECT payment_id FROM payments WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "'))"
                );
                stmt.executeUpdate(
                    "IF OBJECT_ID('payments', 'U') IS NOT NULL " +
                    "DELETE FROM payments WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "DELETE FROM invoices WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "'" +
                    " OR staff_id IN (4, 5) AND CAST(issued_at AS DATE) = '" + todayStr + "')"
                );
                stmt.executeUpdate(
                    "DELETE FROM checkins WHERE booking_id IN (SELECT booking_id FROM bookings WHERE CAST(start_time AS DATE) = '" + todayStr + "'" +
                    " OR staff_id IN (4, 5) AND CAST(checkin_time AS DATE) = '" + todayStr + "')"
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

            // Step 2: Insert work shifts (08:00 - 12:00, 12:00 - 18:00, 18:00 - 22:00)
            long shift1Id = 0;
            long shift2Id = 0;
            long shift3Id = 0;
            String insertShiftSql = "INSERT INTO work_shifts (complex_id, shift_name, shift_date, start_time, end_time, created_at) VALUES (?, ?, ?, ?, ?, GETDATE())";
            try (PreparedStatement ps = conn.prepareStatement(insertShiftSql, Statement.RETURN_GENERATED_KEYS)) {
                // Shift 1: Ca sáng 8:00 - 12:00
                ps.setLong(1, 1);
                ps.setString(2, "Ca sáng Mỹ Đình");
                ps.setString(3, todayStr);
                ps.setString(4, "08:00:00");
                ps.setString(5, "12:00:00");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) shift1Id = rs.getLong(1);
                }

                // Shift 2: Ca chiều 12:00 - 18:00
                ps.setLong(1, 1);
                ps.setString(2, "Ca chiều Mỹ Đình");
                ps.setString(3, todayStr);
                ps.setString(4, "12:00:00");
                ps.setString(5, "18:00:00");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) shift2Id = rs.getLong(1);
                }

                // Shift 3: Ca tối 18:00 - 22:00
                ps.setLong(1, 1);
                ps.setString(2, "Ca tối Mỹ Đình");
                ps.setString(3, todayStr);
                ps.setString(4, "18:00:00");
                ps.setString(5, "22:00:00");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) shift3Id = rs.getLong(1);
                }
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã tạo 3 ca trực hôm nay. Ca sáng đặc biệt từ 08:00 đến 12:00 (Shift ID: " + shift1Id + ").</div>");

            // Step 3: Insert assignments
            String insertAssignSql = "INSERT INTO shift_assignments (shift_id, staff_id, status) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertAssignSql)) {
                // Assign BOTH staff 4 and staff 5 to Shift 1 (08:00 - 12:00) so they can both log in and see it
                ps.setLong(1, shift1Id);
                ps.setLong(2, 4); // Phạm Minh Staff (ID: 4)
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();

                ps.setLong(1, shift1Id);
                ps.setLong(2, 5); // Đỗ Anh Staff (ID: 5)
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();

                // Assign to Shift 2 (12:00 - 18:00)
                ps.setLong(1, shift2Id);
                ps.setLong(2, 4);
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();

                ps.setLong(1, shift2Id);
                ps.setLong(2, 5);
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();

                // Assign to Shift 3 (18:00 - 22:00)
                ps.setLong(1, shift3Id);
                ps.setLong(2, 5);
                ps.setString(3, "ASSIGNED");
                ps.executeUpdate();
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã phân công ca trực hôm nay cho Phạm Minh (ID: 4) và Đỗ Anh (ID: 5).</div>");

            // Step 4: Insert bookings for testing during 08:00 - 12:00 shift
            long booking1Id = 0, booking2Id = 0, booking3Id = 0, booking4Id = 0, booking5Id = 0, booking6Id = 0;
            long booking7Id = 0, booking8Id = 0, booking9Id = 0;

            String insertBookingSql = "INSERT INTO bookings (" +
                " booking_code, customer_id, complex_id, field_id, start_time, end_time," +
                " original_price, discount_amount, total_amount, deposit_amount, status, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
            
            try (PreparedStatement ps = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                // Booking 1: CONFIRMED, 07:00 - 09:00 (Sân 1). Hourly slot -> Expired
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "1");
                ps.setLong(2, 6);
                ps.setLong(3, 1);
                ps.setLong(4, 1);
                ps.setString(5, todayStr + " 07:00:00");
                ps.setString(6, todayStr + " 09:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking1Id = rs.getLong(1);
                }

                // Booking 2: CONFIRMED, 08:00 - 11:00 (Sân 2). Hourly slot -> Late 30m
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "2");
                ps.setLong(2, 7);
                ps.setLong(3, 1);
                ps.setLong(4, 2);
                ps.setString(5, todayStr + " 08:00:00");
                ps.setString(6, todayStr + " 11:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("150000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking2Id = rs.getLong(1);
                }

                // Booking 3: CONFIRMED, 10:00 - 11:00 (Sân 3). Hourly slot -> Future
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "3");
                ps.setLong(2, 8);
                ps.setLong(3, 1);
                ps.setLong(4, 3);
                ps.setString(5, todayStr + " 10:00:00");
                ps.setString(6, todayStr + " 11:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking3Id = rs.getLong(1);
                }

                // Booking 4: COMPLETED, 08:00 - 09:00 (Sân 1). Hourly slot -> Completed
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "4");
                ps.setLong(2, 9);
                ps.setLong(3, 1);
                ps.setLong(4, 1);
                ps.setString(5, todayStr + " 08:00:00");
                ps.setString(6, todayStr + " 09:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("100000"));
                ps.setString(11, "COMPLETED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking4Id = rs.getLong(1);
                }

                // Booking 5: CHECKED_IN, 09:00 - 11:00 (Sân 2). Hourly slot -> Currently playing
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "5");
                ps.setLong(2, 6);
                ps.setLong(3, 1);
                ps.setLong(4, 2);
                ps.setString(5, todayStr + " 09:00:00");
                ps.setString(6, todayStr + " 11:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("100000"));
                ps.setString(11, "CHECKED_IN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking5Id = rs.getLong(1);
                }

                // Booking 6: CONFIRMED, 11:00 - 12:00 (Sân 3). Hourly slot -> Future
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "6");
                ps.setLong(2, 7);
                ps.setLong(3, 1);
                ps.setLong(4, 3);
                ps.setString(5, todayStr + " 11:00:00");
                ps.setString(6, todayStr + " 12:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking6Id = rs.getLong(1);
                }

                // Booking 7: CONFIRMED (Shift 2: 13:00 - 15:00) -> Hourly slot
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "7");
                ps.setLong(2, 8);
                ps.setLong(3, 1);
                ps.setLong(4, 1);
                ps.setString(5, todayStr + " 13:00:00");
                ps.setString(6, todayStr + " 15:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("400000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("100000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking7Id = rs.getLong(1);
                }

                // Booking 8: CONFIRMED (Shift 2: 15:00 - 17:00) -> Hourly slot
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "8");
                ps.setLong(2, 9);
                ps.setLong(3, 1);
                ps.setLong(4, 2);
                ps.setString(5, todayStr + " 15:00:00");
                ps.setString(6, todayStr + " 17:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("500000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("150000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking8Id = rs.getLong(1);
                }

                // Booking 9: CONFIRMED (Shift 3: 19:00 - 21:00) -> Hourly slot
                ps.setString(1, "BK" + (System.currentTimeMillis() % 100000000L) + "9");
                ps.setLong(2, 6);
                ps.setLong(3, 1);
                ps.setLong(4, 3);
                ps.setString(5, todayStr + " 19:00:00");
                ps.setString(6, todayStr + " 21:00:00");
                ps.setBigDecimal(7, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(8, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(9, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(10, new java.math.BigDecimal("200000"));
                ps.setString(11, "CONFIRMED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) booking9Id = rs.getLong(1);
                }
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã tạo thành công các lịch đặt sân (bookings) đa dạng trong ca.</div>");

            // Step 5: Checkins
            String insertCheckinSql = "INSERT INTO checkins (booking_id, staff_id, checkin_time, note) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertCheckinSql)) {
                // Checkin for Booking 4 (starts at 08:00, checkin at 08:02)
                ps.setLong(1, booking4Id);
                ps.setLong(2, 4);
                ps.setString(3, todayStr + " 08:02:00");
                ps.setString(4, "Khách mượn 2 bóng, mượn 3 bib tập");
                ps.executeUpdate();

                // Checkin for Booking 5 (starts at 09:00, checkin at 09:02)
                ps.setLong(1, booking5Id);
                ps.setLong(2, 5);
                ps.setString(3, todayStr + " 09:02:00");
                ps.setString(4, "Khách đi đúng giờ, thuê 1 khay nước ngọt");
                ps.executeUpdate();
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã thực hiện các check-in mẫu tương ứng.</div>");

            // Step 6: Invoices (Counted inside Cash KPI of 08:00 - 12:00 shift for Staff 4)
            String insertInvoiceSql = "INSERT INTO invoices (invoice_code, booking_id, customer_id, staff_id, subtotal, discount_amount, total_amount, paid_amount, status, issued_at)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PAID', ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertInvoiceSql)) {
                // Issued at 08:45:00 (inside 8:00-12:00 shift, count in KPI)
                ps.setString(1, "INV" + (System.currentTimeMillis() % 100000000L));
                ps.setLong(2, booking4Id);
                ps.setLong(3, 7);
                ps.setLong(4, 4); // Staff 4 (Phạm Minh)
                ps.setBigDecimal(5, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(6, java.math.BigDecimal.ZERO);
                ps.setBigDecimal(7, new java.math.BigDecimal("600000"));
                ps.setBigDecimal(8, new java.math.BigDecimal("600000"));
                ps.setString(9, todayStr + " 08:45:00");
                ps.executeUpdate();
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã xuất hóa đơn đã thanh toán 600,000₫ lúc 08:45 hôm nay cho Staff 4.</div>");

            // Step 7: Matchmaking Posts & Responses setup
            out.println("<div class='status-step success'><i class='bi bi-people-fill me-2'></i>Đang tạo dữ liệu tìm đối giao hữu (matchmaking)...</div>");
            
            String insertPostSql = "INSERT INTO matchmaking_posts (author_id, post_type, title, description, skill_level, expected_time, complex_id, contact_name, contact_phone, status, created_at, updated_at) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
            
            long post1Id = 0, post2Id = 0, post3Id = 0, post4Id = 0, post5Id = 0, post6Id = 0, post7Id = 0;
            try (PreparedStatement ps = conn.prepareStatement(insertPostSql, Statement.RETURN_GENERATED_KEYS)) {
                // Post 1: FIND_OPPONENT, OPEN, author = 6
                ps.setLong(1, 6);
                ps.setString(2, "FIND_OPPONENT");
                ps.setString(3, "Tìm đối cứng sân 7 tối mai giao lưu cọ xát");
                ps.setString(4, "Đội mình tập hợp các cựu sinh viên thể chất, đá bóng tốc độ, thể lực tốt. Muốn tìm đối cứng để cọ xát nâng cao trình độ. Chia tiền sân 50/50.");
                ps.setString(5, "ADVANCED");
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(1).withHour(19).withMinute(0)));
                ps.setLong(7, 1);
                ps.setString(8, "Nguyễn Thế Anh");
                ps.setString(9, "0987654321");
                ps.setString(10, "OPEN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) post1Id = rs.getLong(1);
                }

                // Post 2: FIND_TEAMMATE, OPEN, author = 7
                ps.setLong(1, 7);
                ps.setString(2, "FIND_TEAMMATE");
                ps.setString(3, "Cần tuyển thêm 2 chân sút văn phòng tối thứ 5 hàng tuần");
                ps.setString(4, "Đội bóng khối văn phòng công nghệ, tinh thần vui vẻ, giao lưu nhẹ nhàng dưỡng sinh là chính. Cần tuyển thêm 2 đồng đội đá lâu dài cố định.");
                ps.setString(5, "INTERMEDIATE");
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(2).withHour(18).withMinute(30)));
                ps.setLong(7, 1);
                ps.setString(8, "Trần Khánh Nam");
                ps.setString(9, "0912345678");
                ps.setString(10, "OPEN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) post2Id = rs.getLong(1);
                }

                // Post 3: FIND_OPPONENT, OPEN, author = 8
                ps.setLong(1, 8);
                ps.setString(2, "FIND_OPPONENT");
                ps.setString(3, "Giao lưu nhẹ nhàng dưỡng sinh sân 5 chiều nay Mỹ Đình");
                ps.setString(4, "Đội sinh viên mới lập, kỹ thuật trung bình yếu. Tìm đối tác mềm, giao hữu không cự cãi, đá vui vẻ giải trí.");
                ps.setString(5, "BEGINNER");
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().withHour(17).withMinute(0)));
                ps.setLong(7, 1);
                ps.setString(8, "Phan Văn Huy");
                ps.setString(9, "0933445566");
                ps.setString(10, "OPEN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) post3Id = rs.getLong(1);
                }

                // Post 4: FIND_OPPONENT, CLOSED, author = 6
                ps.setLong(1, 6);
                ps.setString(2, "FIND_OPPONENT");
                ps.setString(3, "Tìm đối đá sân cỏ nhân tạo cuối tuần này - ĐÃ CÓ ĐỐI");
                ps.setString(4, "Mình đã bắt được đối mềm giao hữu rồi nhé. Cảm ơn các anh em phủi đã liên hệ.");
                ps.setString(5, "INTERMEDIATE");
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(3).withHour(16).withMinute(0)));
                ps.setLong(7, 1);
                ps.setString(8, "Nguyễn Thế Anh");
                ps.setString(9, "0987654321");
                ps.setString(10, "CLOSED");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) post4Id = rs.getLong(1);
                }

                // Post 5: FIND_TEAMMATE, OPEN, author = 9
                ps.setLong(1, 9);
                ps.setString(2, "FIND_TEAMMATE");
                ps.setString(3, "Tìm thủ môn bắt chính cho giải ngành Công nghệ thông tin");
                ps.setString(4, "Đội bóng đang tham gia giải phong trào, thủ môn chính bị chấn thương. Cần tìm khẩn cấp thủ môn bắt sân 7 tốt.");
                ps.setString(5, "ADVANCED");
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(5).withHour(8).withMinute(0)));
                ps.setNull(7, java.sql.Types.BIGINT); // Tự chọn sân khách
                ps.setString(8, "Lê Minh Trí");
                ps.setString(9, "0977889900");
                ps.setString(10, "OPEN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) post5Id = rs.getLong(1);
                }

                // Post 6: FIND_OPPONENT, OPEN, author = 8
                ps.setLong(1, 8);
                ps.setString(2, "FIND_OPPONENT");
                ps.setString(3, "Đội bóng U40 tìm đối giao lưu rèn luyện sức khỏe sáng CN");
                ps.setString(4, "Toàn các anh lớn tuổi đi đá mồ hôi là chính. Sân nhà Mỹ Đình đã đặt sẵn, bóng nước đầy đủ.");
                ps.setString(5, "BEGINNER");
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(4).withHour(7).withMinute(30)));
                ps.setLong(7, 1);
                ps.setString(8, "Phan Văn Huy");
                ps.setString(9, "0933445566");
                ps.setString(10, "OPEN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) post6Id = rs.getLong(1);
                }

                // Post 7: FIND_OPPONENT, OPEN, author = 7
                ps.setLong(1, 7);
                ps.setString(2, "FIND_OPPONENT");
                ps.setString(3, "Cần tìm đối tác cọ xát sân Mỹ Đình tối hôm nay");
                ps.setString(4, "Tìm đối nhẹ nhàng vui vẻ đá kèo nước mía giải nhiệt sau giờ làm việc căng thẳng.");
                ps.setString(5, "INTERMEDIATE");
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().withHour(20).withMinute(0)));
                ps.setLong(7, 1);
                ps.setString(8, "Trần Khánh Nam");
                ps.setString(9, "0912345678");
                ps.setString(10, "OPEN");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) post7Id = rs.getLong(1);
                }
            }

            // Insert responses for matchmaking
            String insertRespSql = "INSERT INTO matchmaking_post_responses (post_id, responder_id, message, status, created_at) " +
                " VALUES (?, ?, ?, 'PENDING', GETDATE())";
            try (PreparedStatement ps = conn.prepareStatement(insertRespSql)) {
                // Responses for Post 1 (Advanced Find Opponent)
                ps.setLong(1, post1Id);
                ps.setLong(2, 7); // Responder 7
                ps.setString(3, "Bên mình đá tốt, muốn xin giao hữu cọ xát cùng đội bạn tối mai. Đã đặt sân chưa?");
                ps.executeUpdate();

                ps.setLong(1, post1Id);
                ps.setLong(2, 8); // Responder 8
                ps.setString(3, "Đội mình cũng phủi cứng, muốn giao lưu học hỏi. Nhắn zalo chốt kèo nha.");
                ps.executeUpdate();

                // Response for Post 2 (Find teammate)
                ps.setLong(1, post2Id);
                ps.setLong(2, 6); // Responder 6
                ps.setString(3, "Mình đá cánh phải hoặc hộ công, kỹ thuật khá ổn, muốn tham gia lâu dài cùng anh em.");
                ps.executeUpdate();

                // Response for Post 4 (Closed post)
                ps.setLong(1, post4Id);
                ps.setLong(2, 9); // Responder 9
                ps.setString(3, "Ok đã chốt kèo nhé bạn.");
                ps.executeUpdate();
            }
            out.println("<div class='status-step success'><i class='bi bi-check-circle-fill me-2'></i>Đã tạo thành công 7 tin tìm đối/đồng đội mẫu và các phản hồi lời nhắn đi kèm.</div>");

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
      <a href="<%= ctx %>/matchmaking" class="btn btn-success btn-lg">Đi tới Tìm đối giao hữu</a>
    </div>
  </div>
</div>
</body>
</html>

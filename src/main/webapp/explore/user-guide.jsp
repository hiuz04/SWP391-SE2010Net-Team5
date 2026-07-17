<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String navRole = (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    com.swp.model.User sessionUser = (com.swp.model.User) session.getAttribute("user");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "Người dùng";
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Hướng dẫn sử dụng | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Hướng dẫn sử dụng"></div>

<main class="py-5">
    <div class="container">
        <h1 class="mb-4">Hướng dẫn sử dụng</h1>
        <div class="card p-4 shadow-sm border-0">
            <p>Các bước cơ bản để đặt sân trên Sport Field Booking:</p>
            <ol>
                <li>Đăng nhập vào tài khoản của bạn hoặc tạo tài khoản mới nếu chưa có.</li>
                <li>Sử dụng chức năng tìm kiếm để lọc sân theo vị trí, ngày và giờ.</li>
                <li>Chọn sân bóng phù hợp và xem chi tiết thông tin.</li>
                <li>Nhấn "Đặt ngay" và hoàn tất các bước xác nhận cũng như thanh toán.</li>
                <li>Đến sân theo đúng thời gian đã đặt và tận hưởng trận đấu!</li>
            </ol>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

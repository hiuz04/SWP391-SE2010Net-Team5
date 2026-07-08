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
    <title>Giới thiệu | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Giới thiệu"></div>

<main class="py-5">
    <div class="container">
        <h1 class="mb-4">Giới thiệu về chúng tôi</h1>
        <div class="card p-4 shadow-sm border-0">
            <p>Chào mừng bạn đến với Sport Field Booking - Hệ thống đặt sân bóng online hàng đầu.</p>
            <p>Chúng tôi cung cấp giải pháp đặt sân nhanh chóng, tiện lợi, giúp bạn dễ dàng tìm kiếm và lựa chọn sân bóng phù hợp với nhu cầu của mình.</p>
            <p>Với Sport Field Booking, bạn có thể xem lịch trống, xác nhận booking trực tuyến và thanh toán linh hoạt bằng nhiều hình thức khác nhau.</p>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

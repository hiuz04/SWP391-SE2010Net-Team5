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
    <title>Điều khoản sử dụng | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Điều khoản sử dụng"></div>

<main class="py-5">
    <div class="container">
        <h1 class="mb-4">Điều khoản sử dụng</h1>
        <div class="card p-4 shadow-sm border-0">
            <p>Khi sử dụng dịch vụ của Sport Field Booking, bạn đồng ý với các điều khoản sau:</p>
            <ul>
                <li>Cung cấp thông tin chính xác khi đăng ký tài khoản.</li>
                <li>Tuân thủ các quy định của sân bóng khi đến tham gia.</li>
                <li>Thực hiện thanh toán đúng hạn và đầy đủ.</li>
                <li>Không sử dụng hệ thống cho các mục đích vi phạm pháp luật.</li>
            </ul>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

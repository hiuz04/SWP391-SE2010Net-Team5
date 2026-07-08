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
    <title>Chính sách quyền riêng tư | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Chính sách quyền riêng tư"></div>

<main class="py-5">
    <div class="container">
        <h1 class="mb-4">Chính sách quyền riêng tư</h1>
        <div class="card p-4 shadow-sm border-0">
            <p>Chúng tôi tôn trọng quyền riêng tư của bạn. Dữ liệu của bạn được bảo mật tuyệt đối.</p>
            <p>Hệ thống không bán hoặc chia sẻ thông tin cá nhân của người dùng cho bên thứ ba ngoại trừ những trường hợp được pháp luật yêu cầu.</p>
            <p>Mọi thắc mắc về quyền riêng tư, vui lòng liên hệ với bộ phận hỗ trợ khách hàng của chúng tôi.</p>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

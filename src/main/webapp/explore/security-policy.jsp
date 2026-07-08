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
    <title>Chính sách bảo mật | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Chính sách bảo mật"></div>

<main class="py-5">
    <div class="container">
        <h1 class="mb-4">Chính sách bảo mật</h1>
        <div class="card p-4 shadow-sm border-0">
            <p>Chính sách bảo mật này mô tả cách chúng tôi thu thập, sử dụng và bảo vệ thông tin cá nhân của bạn.</p>
            <ul>
                <li><strong>Thu thập thông tin:</strong> Chúng tôi chỉ thu thập thông tin cần thiết để cung cấp dịch vụ tốt nhất cho bạn.</li>
                <li><strong>Sử dụng thông tin:</strong> Thông tin của bạn được sử dụng để xác nhận đặt sân, hỗ trợ thanh toán và gửi thông báo quan trọng.</li>
                <li><strong>Bảo vệ thông tin:</strong> Chúng tôi áp dụng các biện pháp bảo mật nghiêm ngặt để đảm bảo an toàn cho dữ liệu của bạn.</li>
            </ul>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

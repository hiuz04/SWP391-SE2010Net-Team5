<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    String fullName = request.getAttribute("fullName") != null ? (String) request.getAttribute("fullName") : "";
    String phone = request.getAttribute("phone") != null ? (String) request.getAttribute("phone") : "";
    String email = request.getAttribute("email") != null ? (String) request.getAttribute("email") : "";
    String role = request.getAttribute("role") != null ? (String) request.getAttribute("role") : "CUSTOMER";
    String error = request.getAttribute("error") != null ? (String) request.getAttribute("error") : "";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Đăng ký | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="guest" data-name="" data-active=""></div>

<main class="py-5">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-5">
                <div class="card soft-card p-4 p-lg-5">
                    <div class="text-center mb-4">
                        <span class="logo-box mb-3">⚽</span>
                        <h2 class="fw-bold">Đăng ký tài khoản</h2>
                        <p class="text-muted">Sport Field Booking</p>
                    </div>

                    <% if (!error.isEmpty()) { %>
                    <div class="alert alert-danger"><%= error %></div>
                    <% } %>

                    <form action="<%= ctx %>/register" method="post">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label" for="fullName">Họ tên</label>
                                <input id="fullName" name="fullName" class="form-control" placeholder="Nguyễn Văn A"
                                       value="<%= fullName %>" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label" for="phone">Số điện thoại</label>
                                <input id="phone" name="phone" class="form-control" placeholder="0901234567"
                                       value="<%= phone %>" required>
                            </div>
                            <div class="col-12">
                                <label class="form-label" for="email">Email</label>
                                <input id="email" name="email" type="email" class="form-control"
                                       placeholder="name@example.com" value="<%= email %>" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label" for="password">Mật khẩu</label>
                                <input id="password" name="password" type="password" class="form-control" minlength="6" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label" for="confirmPassword">Xác nhận mật khẩu</label>
                                <input id="confirmPassword" name="confirmPassword" type="password" class="form-control" required>
                            </div>
                            <div class="col-12">
                                <label class="form-label" for="role">Vai trò</label>
                                <select id="role" name="role" class="form-select" required>
                                    <option value="CUSTOMER" <%= "CUSTOMER".equals(role) ? "selected" : "" %>>Khách hàng</option>
                                    <option value="OWNER" <%= "OWNER".equals(role) ? "selected" : "" %>>Chủ sân</option>
                                </select>
                            </div>
                        </div>
                        <button type="submit" class="btn btn-sf-primary btn-lg w-100 mt-4">Tạo tài khoản</button>
                    </form>

                    <p class="text-center mt-4 mb-0">
                        Đã có tài khoản? <a href="<%= ctx %>/login">Đăng nhập</a>
                    </p>
                </div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

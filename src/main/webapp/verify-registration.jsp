<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
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
    <title>Xác thực đăng ký | Sport Field Booking</title>
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
                        <h2 class="fw-bold">Xác thực tài khoản</h2>
                        <p class="text-muted">Vui lòng kiểm tra email của bạn để lấy mã OTP (Gồm 6 chữ số).</p>
                    </div>

                    <% if (!error.isEmpty()) { %>
                    <div class="alert alert-danger"><%= error %></div>
                    <% } %>

                    <form action="<%= ctx %>/verify-registration" method="post">
                        <div class="mb-4">
                            <label class="form-label" for="otpCode">Mã OTP</label>
                            <input type="text" id="otpCode" name="otpCode" class="form-control form-control-lg text-center fw-bold"
                                   style="letter-spacing: 0.5em;" placeholder="------"
                                   maxlength="6" required autofocus>
                        </div>
                        
                        <button type="submit" class="btn btn-sf-primary btn-lg w-100 mb-3">Xác thực</button>
                    </form>

                    <p class="text-center mt-3 mb-0">
                        Chưa nhận được mã? <a href="<%= ctx %>/register">Đăng ký lại</a>
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

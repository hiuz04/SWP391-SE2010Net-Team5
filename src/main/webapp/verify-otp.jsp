<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    String error = (String) request.getAttribute("error");
    String email = (String) session.getAttribute("resetEmail");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác nhận OTP | Sport Field Booking</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <style>
        .fp-card { border: none; border-radius: 16px; box-shadow: 0 8px 32px rgba(0,0,0,0.10); }
        .fp-icon-wrap { width: 72px; height: 72px; background: linear-gradient(135deg, #1a472a, #2d6a4f); border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 32px; color: #fff; margin-bottom: 16px; }
        .btn-fp { background: linear-gradient(135deg, #1a472a, #2d6a4f); color: #fff; border: none; border-radius: 10px; font-weight: 600; transition: opacity .2s, transform .15s; }
        .btn-fp:hover { opacity: .88; transform: translateY(-1px); color: #fff; }
        .input-fp { border-radius: 10px; border: 1.5px solid #d0d7de; text-align: center; font-size: 24px; letter-spacing: 4px; }
        .input-fp:focus { border-color: #2d6a4f; box-shadow: 0 0 0 3px rgba(45,106,79,.15); }
        .alert-error-fp { background: #fff5f5; border: 1.5px solid #e53e3e; color: #c53030; border-radius: 10px; padding: 16px 20px; }
    </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="guest" data-name="" data-active=""></div>

<main class="py-5 min-vh-100 d-flex align-items-center" style="background:#f8faf9;">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-5 col-md-7">
                <div class="card fp-card p-4 p-lg-5">
                    <div class="text-center mb-4">
                        <div class="fp-icon-wrap"><i class="bi bi-chat-dots-fill"></i></div>
                        <h1 class="fw-bold fs-3 mb-1">Xác nhận OTP</h1>
                        <p class="text-muted mb-0" style="font-size:14px;">
                            Mã OTP đã được gửi tới email <strong><%= email != null ? email : "của bạn" %></strong>. Vui lòng kiểm tra hộp thư.
                        </p>
                    </div>

                    <% if (error != null) { %>
                    <div class="alert-error-fp mb-4 d-flex align-items-start gap-2">
                        <i class="bi bi-exclamation-circle-fill fs-5 mt-1 flex-shrink-0"></i>
                        <span><%= error %></span>
                    </div>
                    <% } %>

                    <form id="otp-form" method="post" action="<%= ctx %>/verify-otp">
                        <div class="mb-4">
                            <label class="form-label fw-semibold" for="otpCode">Mã OTP 6 số</label>
                            <input id="otpCode" name="otpCode" type="text" class="form-control form-control-lg input-fp" placeholder="000000" maxlength="6" required autofocus>
                        </div>

                        <button id="verify-btn" type="submit" class="btn btn-fp btn-lg w-100 mb-3">
                            <i class="bi bi-check-circle me-2"></i>Xác nhận
                        </button>
                    </form>

                    <p class="text-center mt-4 mb-0" style="font-size:14px;">
                        Chưa nhận được mã? 
                        <a href="<%= ctx %>/forgot-password" class="fw-semibold text-decoration-none" style="color:#2d6a4f;">Gửi lại</a>
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

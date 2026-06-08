<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu | Sport Field Booking</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <style>
        .fp-card { border: none; border-radius: 16px; box-shadow: 0 8px 32px rgba(0,0,0,0.10); }
        .fp-icon-wrap { width: 72px; height: 72px; background: linear-gradient(135deg, #1a472a, #2d6a4f); border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 32px; color: #fff; margin-bottom: 16px; }
        .btn-fp { background: linear-gradient(135deg, #1a472a, #2d6a4f); color: #fff; border: none; border-radius: 10px; font-weight: 600; transition: opacity .2s, transform .15s; }
        .btn-fp:hover { opacity: .88; transform: translateY(-1px); color: #fff; }
        .input-fp { border-radius: 10px; border: 1.5px solid #d0d7de; }
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
                        <div class="fp-icon-wrap"><i class="bi bi-key-fill"></i></div>
                        <h1 class="fw-bold fs-3 mb-1">Mật khẩu mới</h1>
                        <p class="text-muted mb-0" style="font-size:14px;">
                            Vui lòng nhập mật khẩu mới cho tài khoản của bạn.
                        </p>
                    </div>

                    <% if (error != null) { %>
                    <div class="alert-error-fp mb-4 d-flex align-items-start gap-2">
                        <i class="bi bi-exclamation-circle-fill fs-5 mt-1 flex-shrink-0"></i>
                        <span><%= error %></span>
                    </div>
                    <% } %>

                    <form id="reset-form" method="post" action="<%= ctx %>/reset-password">
                        <div class="mb-3">
                            <label class="form-label fw-semibold" for="newPassword">Mật khẩu mới</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light"><i class="bi bi-lock"></i></span>
                                <input id="newPassword" name="newPassword" type="password" class="form-control input-fp" required autofocus>
                            </div>
                        </div>

                        <div class="mb-4">
                            <label class="form-label fw-semibold" for="confirmPassword">Xác nhận mật khẩu mới</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light"><i class="bi bi-check2-all"></i></span>
                                <input id="confirmPassword" name="confirmPassword" type="password" class="form-control input-fp" required>
                            </div>
                        </div>

                        <button type="submit" class="btn btn-fp btn-lg w-100 mb-3">
                            <i class="bi bi-save me-2"></i>Lưu mật khẩu
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

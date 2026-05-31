<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Map" %>
<%
    String ctx = request.getContextPath();
    String fullName = request.getAttribute("fullName") != null ? (String) request.getAttribute("fullName") : "";
    String phone = request.getAttribute("phone") != null ? (String) request.getAttribute("phone") : "";
    String email = request.getAttribute("email") != null ? (String) request.getAttribute("email") : "";
    String error = request.getAttribute("error") != null ? (String) request.getAttribute("error") : "";
    Map<String, String> fieldErrors = request.getAttribute("fieldErrors") instanceof Map
            ? (Map<String, String>) request.getAttribute("fieldErrors")
            : Collections.emptyMap();

    String fullNameClass = fieldErrors.containsKey("fullName") ? "is-invalid" : "";
    String phoneClass = fieldErrors.containsKey("phone") ? "is-invalid" : "";
    String emailClass = fieldErrors.containsKey("email") ? "is-invalid" : "";
    String passwordClass = fieldErrors.containsKey("password") ? "is-invalid" : "";
    String confirmClass = fieldErrors.containsKey("confirmPassword") ? "is-invalid" : "";
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
                        <p class="text-muted">Tài khoản khách hàng — Sport Field Booking</p>
                    </div>

                    <% if (!error.isEmpty()) { %>
                    <div class="alert alert-danger"><%= error %></div>
                    <% } %>

                    <form id="registerForm" action="<%= ctx %>/register" method="post" novalidate>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label" for="fullName">Họ tên <span class="text-danger">*</span></label>
                                <input id="fullName" name="fullName" class="form-control <%= fullNameClass %>"
                                       placeholder="Nguyễn Văn A" value="<%= fullName %>"
                                       minlength="2" maxlength="100" required>
                                <div class="invalid-feedback" data-field-error="fullName">
                                    <%= fieldErrors.getOrDefault("fullName", "Họ tên không hợp lệ.") %>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label" for="phone">Số điện thoại <span class="text-danger">*</span></label>
                                <input id="phone" name="phone" class="form-control <%= phoneClass %>"
                                       placeholder="0901234567" value="<%= phone %>"
                                       inputmode="numeric" pattern="0[0-9]{9,10}" required>
                                <div class="invalid-feedback" data-field-error="phone">
                                    <%= fieldErrors.getOrDefault("phone", "Số điện thoại không hợp lệ.") %>
                                </div>
                            </div>
                            <div class="col-12">
                                <label class="form-label" for="email">Email <span class="text-danger">*</span></label>
                                <input id="email" name="email" type="email" class="form-control <%= emailClass %>"
                                       placeholder="name@example.com" value="<%= email %>"
                                       maxlength="100" required>
                                <div class="invalid-feedback" data-field-error="email">
                                    <%= fieldErrors.getOrDefault("email", "Email không hợp lệ.") %>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label" for="password">Mật khẩu <span class="text-danger">*</span></label>
                                <div class="password-toggle-wrap">
                                    <input id="password" name="password" type="password"
                                           class="form-control <%= passwordClass %>"
                                           minlength="6" maxlength="64" required>
                                    <button type="button" class="password-toggle-btn"
                                            aria-label="Hiện mật khẩu" aria-pressed="false">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                </div>
                                <div class="form-text">Ít nhất 6 ký tự, gồm chữ cái và chữ số.</div>
                                <div class="invalid-feedback" data-field-error="password">
                                    <%= fieldErrors.getOrDefault("password", "Mật khẩu không hợp lệ.") %>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label" for="confirmPassword">Xác nhận mật khẩu <span class="text-danger">*</span></label>
                                <div class="password-toggle-wrap">
                                    <input id="confirmPassword" name="confirmPassword" type="password"
                                           class="form-control <%= confirmClass %>" required>
                                    <button type="button" class="password-toggle-btn"
                                            aria-label="Hiện mật khẩu" aria-pressed="false">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                </div>
                                <div class="invalid-feedback" data-field-error="confirmPassword">
                                    <%= fieldErrors.getOrDefault("confirmPassword", "Mật khẩu xác nhận không khớp.") %>
                                </div>
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
<script src="<%= ctx %>/assets/js/password-toggle.js"></script>
<script src="<%= ctx %>/assets/js/register-validation.js"></script>
</body>
</html>

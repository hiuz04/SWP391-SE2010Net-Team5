<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    if (request.getAttribute("googleEnabled") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    String ctx = request.getContextPath();
    String login = request.getAttribute("login") != null ? (String) request.getAttribute("login") : "";
    String error = request.getAttribute("error") != null ? (String) request.getAttribute("error") : "";
    String success = request.getAttribute("success") != null ? (String) request.getAttribute("success") : "";
    Boolean googleEnabledAttr = (Boolean) request.getAttribute("googleEnabled");
    boolean googleEnabled = googleEnabledAttr != null && googleEnabledAttr;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Đăng nhập | Sport Field Booking</title>
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
                        <h2 class="fw-bold">Đăng nhập</h2>
                        <p class="text-muted">Sport Field Booking</p>
                    </div>

                    <% if (!success.isEmpty()) { %>
                    <div class="alert alert-success"><%= success %></div>
                    <% } %>
                    <% if (!error.isEmpty()) { %>
                    <div class="alert alert-danger"><%= error %></div>
                    <% } %>

                    <% if (googleEnabled) { %>
                    <a class="btn btn-outline-secondary btn-lg w-100 mb-3 d-flex align-items-center justify-content-center gap-2"
                       href="<%= ctx %>/auth/google">
                        <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="" width="22" height="22">
                        Đăng nhập bằng Google
                    </a>
                    <div class="text-center text-muted mb-3">hoặc</div>
                    <% } %>

                    <form action="<%= ctx %>/login" method="post">
                        <div class="mb-3">
                            <label class="form-label" for="loginInput">Email / Số điện thoại</label>
                            <input id="loginInput" name="login" type="text" class="form-control form-control-lg"
                                   placeholder="name@example.com hoặc 090..." value="<%= login %>" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="password">Mật khẩu</label>
                            <div class="password-toggle-wrap">
                                <input id="password" name="password" type="password"
                                       class="form-control form-control-lg"
                                       placeholder="••••••••" required>
                                <button type="button" class="password-toggle-btn"
                                        aria-label="Hiện mật khẩu" aria-pressed="false">
                                    <i class="bi bi-eye"></i>
                                </button>
                            </div>
                        </div>
                        <div class="d-flex justify-content-between mb-4">
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" id="remember" name="remember">
                                <label for="remember" class="form-check-label">Ghi nhớ</label>
                            </div>
                            <a href="<%= ctx %>/forgot-password.jsp">Quên mật khẩu?</a>
                        </div>
                        <button type="submit" class="btn btn-sf-primary btn-lg w-100">Đăng nhập</button>
                    </form>

                    <p class="text-center mt-4 mb-0">
                        Chưa có tài khoản? <a href="<%= ctx %>/register">Đăng ký</a>
                    </p>
                </div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script src="<%= ctx %>/assets/js/password-toggle.js"></script>
</body>
</html>

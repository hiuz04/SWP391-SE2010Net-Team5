<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
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
    <title>Quên mật khẩu | Sport Field Booking</title>
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
                        <h2 class="fw-bold">Quên mật khẩu</h2>
                        <p class="text-muted">Sport Field Booking</p>
                    </div>
                    <p class="text-muted">Nhập email/số điện thoại để nhận liên kết đặt lại mật khẩu.</p>
                    <div class="mb-3">
                        <label class="form-label">Email / Số điện thoại</label>
                        <input class="form-control form-control-lg" placeholder="name@example.com">
                    </div>
                    <button type="button" class="btn btn-sf-primary btn-lg w-100"
                            data-demo-alert="Chức năng đặt lại mật khẩu sẽ được cập nhật sau.">
                        Gửi yêu cầu
                    </button>
                    <p class="text-center mt-4 mb-0">
                        <a href="<%= ctx %>/login">Quay lại đăng nhập</a>
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

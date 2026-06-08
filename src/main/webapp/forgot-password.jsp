<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    String error   = (String) request.getAttribute("error");
    String success  = (String) request.getAttribute("success");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Quên mật khẩu – nhận mã OTP qua email tài khoản Sport Field Booking.">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Quên mật khẩu | Sport Field Booking</title>
    <style>
        .fp-card {
            border: none;
            border-radius: 16px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.10);
        }
        .fp-icon-wrap {
            width: 72px; height: 72px;
            background: linear-gradient(135deg, #1a472a, #2d6a4f);
            border-radius: 50%;
            display: inline-flex; align-items: center; justify-content: center;
            font-size: 32px; color: #fff; margin-bottom: 16px;
        }
        .btn-fp {
            background: linear-gradient(135deg, #1a472a, #2d6a4f);
            color: #fff; border: none; border-radius: 10px;
            font-weight: 600; letter-spacing: .3px;
            transition: opacity .2s, transform .15s;
        }
        .btn-fp:hover { opacity: .88; transform: translateY(-1px); color: #fff; }
        .input-fp { border-radius: 10px; border: 1.5px solid #d0d7de; }
        .input-fp:focus { border-color: #2d6a4f; box-shadow: 0 0 0 3px rgba(45,106,79,.15); }
        .alert-success-fp {
            background: #f0faf4; border: 1.5px solid #2d6a4f;
            color: #1a472a; border-radius: 10px; padding: 16px 20px;
        }
        .alert-error-fp {
            background: #fff5f5; border: 1.5px solid #e53e3e;
            color: #c53030; border-radius: 10px; padding: 16px 20px;
        }
        .spinner-border-sm { width: 1rem; height: 1rem; }
    </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="guest" data-name="" data-active=""></div>

<main class="py-5 min-vh-100 d-flex align-items-center" style="background:#f8faf9;">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-5 col-md-7">
                <div class="card fp-card p-4 p-lg-5">

                    <!-- Header -->
                    <div class="text-center mb-4">
                        <div class="fp-icon-wrap">
                            <i class="bi bi-shield-lock-fill"></i>
                        </div>
                        <h1 class="fw-bold fs-3 mb-1">Quên mật khẩu</h1>
                        <p class="text-muted mb-0" style="font-size:14px;">
                            Nhập email hoặc số điện thoại – mã OTP sẽ được gửi tới email của bạn.
                        </p>
                    </div>

                    <!-- Alert: Thành công -->
                    <% if (success != null) { %>
                    <div class="alert-success-fp mb-4 d-flex align-items-start gap-2" id="alert-success">
                        <i class="bi bi-check-circle-fill fs-5 mt-1 flex-shrink-0" style="color:#2d6a4f;"></i>
                        <span><%= success %></span>
                    </div>
                    <% } %>

                    <!-- Alert: Lỗi -->
                    <% if (error != null) { %>
                    <div class="alert-error-fp mb-4 d-flex align-items-start gap-2" id="alert-error">
                        <i class="bi bi-exclamation-circle-fill fs-5 mt-1 flex-shrink-0"></i>
                        <span><%= error %></span>
                    </div>
                    <% } %>

                    <!-- Form -->
                    <form id="fp-form" method="post" action="<%= ctx %>/forgot-password" novalidate>
                        <div class="mb-4">
                            <label class="form-label fw-semibold" for="contact">
                                <i class="bi bi-person me-1"></i>Email / Số điện thoại
                            </label>
                            <div class="input-group">
                                <span class="input-group-text" style="border-radius:10px 0 0 10px; border:1.5px solid #d0d7de; border-right:0; background:#f8faf9;">
                                    <i class="bi bi-envelope"></i>
                                </span>
                                <input id="contact"
                                       name="contact"
                                       type="text"
                                       class="form-control form-control-lg input-fp"
                                       style="border-radius:0 10px 10px 0;"
                                       placeholder="name@example.com hoặc 0901234567"
                                       autocomplete="email"
                                       required
                                       autofocus>
                            </div>
                            <div class="invalid-feedback" id="contact-error" style="display:none;">
                                Vui lòng nhập email hoặc số điện thoại.
                            </div>
                        </div>

                        <button id="fp-btn" type="submit" class="btn btn-fp btn-lg w-100 mb-3">
                            <span id="btn-text"><i class="bi bi-send me-2"></i>Nhận mã OTP</span>
                            <span id="btn-loading" class="d-none">
                                <span class="spinner-border spinner-border-sm me-2" role="status"></span>
                                Đang xử lý…
                            </span>
                        </button>
                    </form>

                    <p class="text-center mt-4 mb-0" style="font-size:14px;">
                        Nhớ mật khẩu rồi?
                        <a href="<%= ctx %>/login" class="fw-semibold text-decoration-none" style="color:#2d6a4f;">
                            Quay lại đăng nhập
                        </a>
                    </p>
                </div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
    (function () {
        var form    = document.getElementById('fp-form');
        var input   = document.getElementById('contact');
        var errEl   = document.getElementById('contact-error');
        var btnText = document.getElementById('btn-text');
        var btnLoad = document.getElementById('btn-loading');

        if (!form) return;

        form.addEventListener('submit', function (e) {
            var val = input ? input.value.trim() : '';
            if (!val) {
                e.preventDefault();
                if (input) input.classList.add('is-invalid');
                if (errEl)  errEl.style.display = 'block';
                return;
            }
            // Hiện loading state
            if (btnText) btnText.classList.add('d-none');
            if (btnLoad) btnLoad.classList.remove('d-none');
            if (document.getElementById('fp-btn')) {
                document.getElementById('fp-btn').disabled = true;
            }
        });

        if (input) {
            input.addEventListener('input', function () {
                input.classList.remove('is-invalid');
                if (errEl) errEl.style.display = 'none';
            });
        }
    })();
</script>
</body>
</html>

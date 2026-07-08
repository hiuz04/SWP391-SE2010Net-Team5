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
    <title>Liên Hệ | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Liên Hệ"></div>

<main class="py-5">
    <div class="container">
        <h1 class="mb-4">Liên Hệ</h1>
        <div class="row g-4">
            <div class="col-md-6">
                <div class="card p-4 shadow-sm border-0 h-100">
                    <h4>Thông tin liên lạc</h4>
                    <p class="mt-3"><i class="bi bi-geo-alt me-2 text-success"></i>Hoà Lạc, Thạch Thất, Hà Nội, Việt Nam</p>
                    <p><i class="bi bi-envelope me-2 text-success"></i>tranbaolong.280904@gmail.com</p>
                    <p><i class="bi bi-telephone me-2 text-success"></i>+84 123 456 789</p>
                </div>
            </div>
            <div class="col-md-6">
                <div class="card p-4 shadow-sm border-0 h-100">
                    <h4>Gửi tin nhắn cho chúng tôi</h4>
                    <form class="mt-3">
                        <div class="mb-3">
                            <label class="form-label">Họ và tên</label>
                            <input type="text" class="form-control" placeholder="Nhập họ và tên">
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Email</label>
                            <input type="email" class="form-control" placeholder="Nhập địa chỉ email">
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Nội dung</label>
                            <textarea class="form-control" rows="4" placeholder="Nhập nội dung tin nhắn"></textarea>
                        </div>
                        <button type="button" class="btn btn-sf-primary" onclick="alert('Đã gửi liên hệ thành công!')">Gửi liên hệ</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

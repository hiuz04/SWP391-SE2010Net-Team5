<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% String ctx = request.getContextPath(); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>404 - Không tìm thấy trang</title>
</head>
<body class="bg-light">
<div class="d-flex align-items-center justify-content-center vh-100">
    <div class="text-center p-5 bg-white shadow rounded-4" style="max-width: 500px;">
        <div class="display-1 text-warning mb-3"><i class="bi bi-exclamation-triangle"></i></div>
        <h1 class="fw-bold mb-3">Lỗi 404</h1>
        <p class="fs-5 text-muted mb-4">Xin lỗi, trang bạn đang tìm kiếm không tồn tại hoặc đã bị gỡ bỏ.</p>
        <a href="<%= ctx %>/" class="btn btn-primary btn-lg">Về trang chủ</a>
    </div>
</div>
</body>
</html>

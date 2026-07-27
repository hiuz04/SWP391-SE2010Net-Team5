<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hệ thống đang bảo trì - Sport Field Booking</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <style>
        .maintenance-container {
            height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: #f8f9fa;
        }
        .maintenance-card {
            max-width: 600px;
            text-align: center;
            padding: 3rem;
            border: none;
            border-radius: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.08);
            background: #fff;
        }
        .maintenance-icon {
            font-size: 5rem;
            color: #0d6efd;
            margin-bottom: 1.5rem;
            animation: float 3s ease-in-out infinite;
        }
        @keyframes float {
            0% { transform: translateY(0px); }
            50% { transform: translateY(-15px); }
            100% { transform: translateY(0px); }
        }
    </style>
</head>
<body>

<div class="maintenance-container">
    <div class="maintenance-card">
        <i class="bi bi-tools maintenance-icon"></i>
        <h2 class="fw-bold text-dark mb-3">Hệ thống đang được bảo trì!</h2>
        <p class="text-secondary fs-5 mb-4">
            Chúng tôi đang tiến hành nâng cấp hệ thống để mang lại trải nghiệm tốt nhất cho bạn. 
            Mọi tính năng đặt sân hiện đang tạm ngưng. Vui lòng quay lại sau ít phút!
        </p>
        <p class="text-muted small mb-0">Xin lỗi vì sự bất tiện này.</p>
        <div class="mt-4 pt-3 border-top">
            <a href="<%= ctx %>/login" class="btn btn-outline-secondary btn-sm">Dành cho Quản trị viên (Admin)</a>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

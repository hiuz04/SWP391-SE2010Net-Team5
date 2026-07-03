<!--
* Module: Owner Dashboard
* File: dashboard.html
* Description: Hiển thị các chỉ số vận hành chính (lượt đặt sân trong ngày, doanh thu, số sân đang hoạt động, đánh giá),
*              biểu đồ doanh thu tuần và các nút điều hướng đến các trang quản lý chức năng.
*
* Author: Dương Hải Anh
* Version: 1.0
* Created date: 31/05/2026
-->
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
    <title>Owner Dashboard | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="owner" data-name="Owner A" data-active="Dashboard"></div>
<main class="dashboard-shell">
    <div class="container"><h1 class="section-title">Owner Dashboard</h1>
        <p class="text-muted">Theo dõi hiệu quả kinh doanh cơ sở sân.</p>
        <div class="row g-4 mb-4">
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">Booking hôm nay</div>
                    <h3 class="fw-bold">24</h3><span class="text-success small">+12%</span></div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">Doanh thu</div>
                    <h3 class="fw-bold">8.4M</h3><span class="text-success small">+8%</span></div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">Sân hoạt động</div>
                    <h3 class="fw-bold">12</h3><span class="text-muted small">/15 sân</span></div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">Đánh giá</div>
                    <h3 class="fw-bold">4.8</h3><span class="text-warning small">★★★★★</span></div>
            </div>
        </div>
        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card soft-card p-4"><h5>Doanh thu 7 ngày</h5>
                    <div class="d-flex align-items-end gap-3" style="height:220px">
                        <div class="bg-sf-primary rounded-top flex-fill" style="height:35%"></div>
                        <div class="bg-sf-primary rounded-top flex-fill" style="height:55%"></div>
                        <div class="bg-sf-primary rounded-top flex-fill" style="height:45%"></div>
                        <div class="bg-sf-primary rounded-top flex-fill" style="height:75%"></div>
                        <div class="bg-sf-primary rounded-top flex-fill" style="height:62%"></div>
                        <div class="bg-sf-primary rounded-top flex-fill" style="height:85%"></div>
                        <div class="bg-sf-primary rounded-top flex-fill" style="height:70%"></div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card soft-card p-4"><h5>Quản lý nhanh</h5>
                    <div class="d-grid gap-2">
                        <a class="btn btn-outline-success" href="<%= ctx %>/owner/facility-list">Cơ sở</a>
                        <a class="btn btn-outline-success" href="<%= ctx %>/owner/field-list">Sân bóng</a>
                        <a class="btn btn-outline-success" href="<%= ctx %>/owner/dashboard">Bảng giá</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>
<div id="footer" data-root="../../"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

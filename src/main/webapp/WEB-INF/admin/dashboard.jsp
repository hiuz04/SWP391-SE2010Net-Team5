<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Admin";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Admin Dashboard | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= esc(currentName) %>" data-active="Dashboard"></div>

<main class="dashboard-shell">
    <div class="container">
        <h1 class="section-title">Admin Dashboard</h1>
        <p class="text-muted">Tong quan toan he thong.</p>

        <div class="row g-4 mb-4">
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">Booking hom nay</div>
                    <h3 class="fw-bold">24</h3>
                    <span class="text-success small">+12%</span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">Doanh thu</div>
                    <h3 class="fw-bold">8.4M</h3>
                    <span class="text-success small">+8%</span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">San hoat dong</div>
                    <h3 class="fw-bold">12</h3>
                    <span class="text-muted small">/15 san</span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4">
                    <div class="text-muted">Danh gia</div>
                    <h3 class="fw-bold">4.8</h3>
                    <span class="text-warning small">*****</span>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <div class="col-lg-6">
                <div class="card soft-card p-4">
                    <h5>Yeu cau cho duyet</h5>
                    <p class="display-6 fw-bold text-warning">7</p>
                    <a href="#" class="btn btn-outline-success">Xem danh sach</a>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card soft-card p-4">
                    <h5>Nguoi dung moi</h5>
                    <p class="display-6 fw-bold text-success">128</p>
                    <a href="#" class="btn btn-outline-success">Quan ly nguoi dung</a>
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

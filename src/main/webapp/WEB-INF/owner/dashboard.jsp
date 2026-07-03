<%@ page import="com.swp.model.User" %>
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
    User sessionUser = (User) request.getAttribute("sessionUser");
    if (sessionUser == null) sessionUser = (User) session.getAttribute("user");
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
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
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Dashboard"></div>
<main class="dashboard-shell">
    <div class="container"><h1 class="section-title">Owner Dashboard</h1>
        <p class="text-muted">Theo dõi hiệu quả kinh doanh cơ sở sân.</p>
        <div class="row g-4 mb-4">

            <div class="col-md-4">
                <div class="stat-card p-4">
                    <div class="text-muted">Booking hôm nay</div>
                    <h3 id="todayBooking" class="fw-bold">0</h3>
                    <span id="bookingDifference" class="text-success small">+0</span>
                </div>
            </div>

            <div class="col-md-4">
                <div class="stat-card p-4">
                    <div class="text-muted">
                        Doanh thu tháng này
                    </div>
                    <h3 id="monthRevenue" class="fw-bold">0</h3>
                    <span id="revenueGrowth" class="text-success small">+0%</span>
                </div>
            </div>

            <div class="col-md-4">
                <div class="stat-card p-4">
                    <div class="text-muted">
                        Sân hoạt động
                    </div>
                    <h3 id="activeFields" class="fw-bold">0</h3>
                    <span id="totalFields" class="text-muted small">/0 sân</span>
                </div>
            </div>

<%--            <div class="col-md-3">--%>
<%--                <div class="stat-card p-4">--%>
<%--                    <div class="text-muted">Đánh giá</div>--%>
<%--                    <h3 class="fw-bold">4.8</h3><span class="text-warning small">★★★★★</span></div>--%>
<%--            </div>--%>
        </div>
        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card soft-card p-4"><h5>Doanh thu 7 ngày</h5>
                    <div id="revenueChart">
                        <div class="d-flex align-items-end gap-3" style="height:220px">
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card soft-card p-4"><h5>Quản lý nhanh</h5>
                    <div class="d-grid gap-2">
                        <a class="btn btn-outline-success" href="<%= ctx %>/owner/facility">Cơ sở</a>
                        <a class="btn btn-outline-success" href="<%= ctx %>/owner/field">Sân bóng</a>
                        <a class="btn btn-outline-success" href="<%= ctx %>/owner/price-rules">Bảng giá</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>
<div id="footer" data-root="../../"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script src="<%= ctx %>/assets/js/owner/dashboard.js"></script>
<script>loadData();</script>
</body>
</html>

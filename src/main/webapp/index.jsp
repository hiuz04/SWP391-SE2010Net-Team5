<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.TopFieldSummary" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Collections" %>
<%
    User sessionUser = (User) request.getAttribute("sessionUser");
    if (sessionUser == null) sessionUser = (User) session.getAttribute("user");
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String ctx = request.getContextPath();

    @SuppressWarnings("unchecked")
    List<TopFieldSummary> topFields = (List<TopFieldSummary>) request.getAttribute("topFields");
    if (topFields == null) topFields = Collections.emptyList();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Trang chủ | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Trang chủ"></div>

<header class="hero">
    <div class="container">
        <div class="row align-items-center g-5">
            <div class="col-lg-8">
                <span class="badge text-bg-light text-success mb-3 px-3 py-2">Hệ thống đặt sân bóng online</span>
                <h1 class="display-4 fw-bold mb-3">Đặt sân bóng đá dễ dàng, nhanh chóng</h1>
                <p class="lead mb-4 text-white-50">Tìm sân, chọn khung giờ, thanh toán và quản lý lịch đặt chỉ trong vài thao tác.</p>
                <div class="card hero-card p-3 p-lg-4">
                    <form class="row g-3 align-items-end" action="<%= ctx %>/search" method="get">
                        <div class="col-md-3">
                            <label class="form-label text-dark fw-semibold">Tỉnh / Thành phố</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-geo-alt"></i></span>
                                <select class="form-select" name="province">
                                    <option value="" selected>Tất cả</option>
                                    <%
                                        @SuppressWarnings("unchecked")
                                        java.util.List<String> cities = (java.util.List<String>) request.getAttribute("cities");
                                        if (cities != null) {
                                            for (String c : cities) {
                                    %>
                                    <option value="<%= c %>"><%= c %></option>
                                    <%
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label text-dark fw-semibold">Phường / Xã</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-geo-alt-fill"></i></span>
                                <select class="form-select" name="ward">
                                    <option value="" selected>Tất cả</option>
                                    <%
                                        @SuppressWarnings("unchecked")
                                        java.util.List<String> wards = (java.util.List<String>) request.getAttribute("wards");
                                        if (wards != null) {
                                            for (String w : wards) {
                                    %>
                                    <option value="<%= w %>"><%= w %></option>
                                    <%
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label text-dark fw-semibold">Loại sân</label>
                            <select class="form-select" name="type">
                                <option value="" selected>Tất cả</option>
                                <option value="1">Sân 5</option>
                                <option value="2">Sân 7</option>
                                <option value="3">Sân 11</option>
                            </select>
                        </div>
                        <div class="col-md-3 d-grid">
                            <button type="submit" class="btn btn-sf-accent btn-lg">
                                <i class="bi bi-search me-1"></i>Tìm sân
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</header>

<main>
    <section class="py-5 bg-white">
        <div class="container">
            <div class="row g-4 text-center">
                <div class="col-md-4">
                    <div class="soft-card p-4 h-100">
                        <i class="bi bi-search display-5 text-sf-primary"></i>
                        <h5 class="mt-3">Tìm kiếm dễ dàng</h5>
                        <p class="text-muted mb-0">Lọc theo vị trí, loại sân, khung giờ và mức giá.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="soft-card p-4 h-100">
                        <i class="bi bi-calendar-check display-5 text-sf-primary"></i>
                        <h5 class="mt-3">Đặt sân nhanh</h5>
                        <p class="text-muted mb-0">Xem lịch trống và xác nhận booking trực tuyến.</p>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="soft-card p-4 h-100">
                        <i class="bi bi-credit-card display-5 text-sf-primary"></i>
                        <h5 class="mt-3">Thanh toán linh hoạt</h5>
                        <p class="text-muted mb-0">Hỗ trợ chuyển khoản, ví điện tử và thanh toán tại sân.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section class="py-5">
        <div class="container">
            <div class="d-flex justify-content-between align-items-end mb-4">
                <div>
                    <h2 class="section-title">Sân bóng nổi bật</h2>
                    <p class="text-muted mb-0">Danh sách các sân nổi bật do chủ sân đề xuất.</p>
                </div>
            </div>
            <div class="row g-4">
                <% if (topFields.isEmpty()) { %>
                    <div class="col-12 text-center text-muted py-5">
                        <i class="bi bi-calendar-x display-4"></i>
                        <p class="mt-3">Chưa có dữ liệu sân nổi bật.</p>
                    </div>
                <% } else { %>
                    <% for (TopFieldSummary field : topFields) { %>
                    <div class="col-md-6 col-xl-4">
                        <div class="card soft-card h-100 overflow-hidden">
                            <img class="field-img" src="https://images.unsplash.com/photo-1529900681758-f4b84c32ddda?w=900&q=80" alt="Sân bóng">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-start mb-2">
                                    <h5 class="card-title mb-0"><%= field.getFieldName() %></h5>
                                    <%
                                        String badgeClass = "AVAILABLE".equalsIgnoreCase(field.getStatus())
                                            ? "badge-soft-success" : "badge-soft-danger";
                                        String badgeText = "AVAILABLE".equalsIgnoreCase(field.getStatus())
                                            ? "Còn trống" : "Không khả dụng";
                                    %>
                                    <span class="badge <%= badgeClass %>"><%= badgeText %></span>
                                </div>
                                <p class="text-muted small mb-2">
                                    <i class="bi bi-building me-1"></i><%= field.getFacilityName() != null ? field.getFacilityName() : "" %>
                                </p>
                                <p class="text-muted small mb-2">
                                    <i class="bi bi-geo-alt me-1"></i><%= field.getFullLocation() %>
                                </p>
                                <div class="d-flex gap-3 small mb-3">
                                    <span><i class="bi bi-trophy-fill text-warning"></i> <%= field.getBookingCount() %> lượt đặt</span>
                                    <% if (field.getFieldTypeName() != null && !field.getFieldTypeName().isBlank()) { %>
                                        <span><%= field.getFieldTypeName() %></span>
                                    <% } %>
                                </div>
                                <div class="d-flex justify-content-end">
                                    <a href="<%= ctx %>/booking?facilityId=<%= field.getFacilityId() %>" class="btn btn-sf-primary">Đặt ngay</a>
                                </div>
                            </div>
                        </div>
                    </div>
                    <% } %>
                <% } %>
            </div>
        </div>
    </section>

    <% if ("guest".equalsIgnoreCase(navRole) || "customer".equalsIgnoreCase(navRole)) { %>
    <section class="py-5 bg-sf-primary text-white">
        <div class="container text-center">
            <h2 class="fw-bold">Khuyến mãi đặc biệt</h2>
            <p class="lead text-white-50">Giảm 20% cho lần đặt sân đầu tiên.</p>
            <% if (sessionUser == null) { %>
            <a class="btn btn-light btn-lg" href="<%= ctx %>/register">Đăng ký ngay</a>
            <% } else { %>
            <a class="btn btn-light btn-lg" href="#">Nhận voucher</a>
            <% } %>
        </div>
    </section>
    <% } %>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

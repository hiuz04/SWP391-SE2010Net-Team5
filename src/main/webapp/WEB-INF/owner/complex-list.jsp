<%@ page import="com.swp.model.User" %>
<!--
* Module: Complex Management
* File: complex-list.html
* Description: Trang hiển thị danh sách tổng quan của các cơ sở gồm: tên cơ sở, địa chỉ,
*              số lượng sân, trạng thái hoạt động và các công cụ quản lý đi kèm.
*
* Author: Dương Hải Anh
* Version: 1.0
* Created Date: 04/06/2026
-->
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/owner/complex.css" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
  <title>Quản lý cơ sở | Sport Field Booking</title>
</head>
<body>
<div class="owner-layout">
    <aside class="owner-sidebar" id="owner-sidebar"></aside>

    <main class="main-wrapper owner-content">
        <div class="topbar" id="topbar"></div>

        <!-- Page Header -->
        <div class="page-header">
            <div class="page-header-left">
                <h1><i class="bi bi-buildings me-2 text-success"></i>Quản lý cơ sở</h1>
                <p>Quản lý danh sách các cơ sở bóng đá của bạn.</p>
            </div>
            <button class="btn btn-success px-4 py-2"
                    onclick="navigateComplexForm()">
                <i class="bi bi-plus-lg me-1"></i>
                Thêm cơ sở mới
            </button>
        </div>

        <!-- Filter Card -->
        <div class="filter-card">
            <div class="row g-3 align-items-center">

                <div class="col-lg-5">
                    <div class="input-group">
                        <span class="input-group-text">
                            <i class="bi bi-search"></i>
                        </span>
                        <input
                                id="keyword"
                                class="form-control"
                                type="text"
                                placeholder="Tìm theo tên cơ sở..."
                                oninput="scheduleLoadData()">
                    </div>
                </div>

                <div class="col-lg-4">
                    <select
                            id="status"
                            class="form-select"
                            onchange="scheduleLoadData()"
                    >
                        <option value="">Tất cả trạng thái</option>
                        <option value="ACTIVE">✅ Hoạt động</option>
                        <option value="INACTIVE">🔴 Ngừng hoạt động</option>
                        <option value="PENDING">🟡 Đang thiết lập</option>
                        <option value="MAINTENANCE">🔵 Bảo trì</option>
                    </select>
                </div>

                <div class="col-lg-3 text-end">
                    <span class="stat-pill" id="complex-count">
                        <i class="bi bi-buildings"></i>
                        <strong>0</strong> cơ sở
                    </span>
                </div>

            </div>
        </div>

        <!-- Data container -->
        <div id="complex-data-container"></div>

    </main>
</div>
<div id="footer" data-root="../../"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
    display_name = '<%= displayName %>';
    current_role = '<%= navRole %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/owner/dashboard.js"></script>
<script src="<%= ctx %>/assets/js/owner/complex.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
    loadData();
    renderTopbar({
        title: "Thêm / Sửa cụm sân",
        subtitle: "Điền thông tin để thêm mới hoặc cập nhật cụm sân."
    });
</script>
</body>
</html>

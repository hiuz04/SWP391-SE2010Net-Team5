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
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/owner/complex.css" rel="stylesheet">
  <title>Quản lý cơ sở | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Cơ sở"></div>
<main class="py-5 main-wrapper">
  <div class="container">
    <div class="card shadow-sm border-0 mb-4">
        <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <h1 class="section-title mb-1">Quản lý cơ sở</h1>
                    <p class="text-muted mb-0">
                        Quản lý danh sách các cơ sở bóng đá.
                    </p>
                </div>

                <button class="btn btn-success"
                        onclick="navigateComplexForm()">
                    <i class="bi bi-plus-lg me-1"></i>
                    Thêm cơ sở mới
                </button>
            </div>

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

                <div class="col-lg-3">
                    <select
                            id="status"
                            class="form-select"
                            onchange="scheduleLoadData()"
                    >
                        <option value="">Tất cả trạng thái</option>
                        <option value="ACTIVE">Hoạt động</option>
                        <option value="INACTIVE">Ngừng hoạt động</option>
                        <option value="PENDING">Đang chờ</option>
                        <option value="MAINTENANCE">Bảo trì</option>
                    </select>
                </div>

                <div class="col-lg-4 text-end">
                    <span class="text-muted" id="complex-count"></span>
                </div>
            </div>
        </div>
    </div>
    <!-- Hiển thị danh sách cơ sở -->
    <div class="border-0 shadow-sm px-4 data-container py-3" id="complex-data-container"></div>
  </div>
</main>
<div id="footer" data-root="../../"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/owner/complex.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>loadData()</script>
</body>
</html>

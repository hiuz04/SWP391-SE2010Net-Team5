<%@ page import="com.swp.model.User" %>
<!--
* Module: Field Management
* File: field-list.jsp
* Version: 2.0
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
<%@ include file="/WEB-INF/owner/field-form.jsp" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Quản lý sân bóng - Owner Panel">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/field.css" rel="stylesheet">
    <title>Quản lý sân bóng | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Sân bóng"></div>

<main class="main-wrapper owner-content">

        <!-- Page Header -->
        <div class="page-header">
            <div class="page-header-left">
                <h1><i class="bi bi-dribbble me-2 text-success"></i>Quản lý sân bóng</h1>
                <div class="d-flex align-items-center gap-2 mt-1">
                    <span class="text-muted" style="font-size:.875rem;font-weight:500">Đang quản lý:</span>
                    <select id="complexSelect"
                            class="form-select form-select-sm"
                            style="width:280px;border-radius:9px;font-size:.85rem"
                            onchange="changeComplex(this.value)">
                    </select>
                </div>
            </div>

            <button class="btn btn-success px-4 py-2"
                    id="btn-add-field"
                    onclick="openModal()">
                <i class="bi bi-plus-lg me-1"></i>
                Thêm sân mới
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
                        <input class="form-control"
                               id="keyword"
                               oninput="scheduleLoadData()"
                               placeholder="Tìm theo tên sân...">
                    </div>
                </div>

                <div class="col-lg-3">
                    <select class="form-select" id="status" onchange="scheduleLoadData()">
                        <option value="">Tất cả trạng thái</option>
                        <option value="AVAILABLE">✅ Có sẵn</option>
                        <option value="INACTIVE">🔴 Ngừng hoạt động</option>
                        <option value="MAINTENANCE">🔵 Bảo trì</option>
                    </select>
                </div>

                <div class="col-lg-2">
                    <select class="form-select" id="fieldType" onchange="scheduleLoadData()">
                        <option value="">Tất cả loại sân</option>
                    </select>
                </div>

                <div class="col-lg-2 text-end">
                    <span class="stat-pill" id="field-count">
                        <i class="bi bi-dribbble"></i>
                        <strong>0</strong> sân
                    </span>
                </div>

            </div>
        </div>

        <!-- Data container -->
        <div id="field-data-container"></div>

        <!-- Modal container -->
        <div id="modal"></div>
    </main>
<div id="footer" data-root="../../"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
    display_name = '<%= displayName %>';
    current_role = '<%= navRole %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/owner/field.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
    getComplex();
    loadFieldTypeDataForSearch();
    loadData();
</script>
</body>
</html>

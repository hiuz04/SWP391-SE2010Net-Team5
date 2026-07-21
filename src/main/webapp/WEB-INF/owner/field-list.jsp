<%@ page import="com.swp.model.User" %>
<!--
* Module: Field Management
* File: field-list.jsp
* Description: Trang hiển thị danh sách thông tin tổng quát sân gồm: tên sân, loại sân, cơ sở sở hữu, mô tả sân và trạng thái .
*
* Author: Duong Hai Anh
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
<%@ include file="/WEB-INF/owner/field-form.jsp" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/field.css" rel="stylesheet">
    <title>Quản lý sân bóng | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Sân bóng"></div>
<div class="container">
    <main class="py-5 main-wrapper">
        <div class="container">
            <div class="card shadow-sm border-0 mb-4">
                <div class="card-body">

                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <h1 class="section-title mb-2">Quản lý sân bóng</h1>

                            <div class="d-flex align-items-center gap-2">
                                <span class="text-muted fw-semibold">
                                    Đang quản lý:
                                </span>

                                <select id="complexSelect"
                                        class="form-select form-select-sm"
                                        style="width:320px"
                                        onchange="changeComplex(this.value)">

                                    <c:forEach items="${complexList}" var="c">
                                        <option value="${c.complexId}"
                                                ${c.complexId == currentComplexId ? "selected" : ""}>
                                            🏟️ ${c.complexName}
                                        </option>
                                    </c:forEach>

                                </select>
                            </div>
                        </div>

                        <button class="btn btn-success px-4"
                                onclick="openModal()">
                            <i class="bi bi-plus-lg me-1"></i>
                            Thêm sân mới
                        </button>
                    </div>

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
                                <option value="AVAILABLE">Có sẵn</option>
                                <option value="INACTIVE">Ngừng hoạt động</option>
                                <option value="MAINTENANCE">Bảo trì</option>
                            </select>
                        </div>

                        <div class="col-lg-2">
                            <select class="form-select" id="fieldType" onchange="scheduleLoadData()">
                                <option value="">Tất cả loại sân</option>
                            </select>
                        </div>

                        <div class="col-lg-2 text-end" id="field-count">
                        </div>

                    </div>

                </div>
            </div>
            <!-- Hiển thị danh sách sân -->
            <div class="border-0 shadow-sm px-4 data-container py-3" id="field-data-container"></div>

            <!-- Hiển thị form modal dành cho field -->
            <div id="modal"></div>
        </div>
    </main>
</div>
<div id="footer" data-root="../../"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
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

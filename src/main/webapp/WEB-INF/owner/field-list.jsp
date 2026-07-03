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
<div id="navbar" data-root="<%= ctx %>/" data-role="owner" data-name="Owner A" data-active="Cơ sở"></div>
<div class="container">
    <main class="py-5 main-wrapper">
        <div class="container">
            <div class="d-flex justify-content-between mb-4">
                <h1 class="section-title">Quản lý sân bóng</h1>
                <button class="btn btn-sf-primary" style="height: 40px; background: rgb(5, 150, 105);" onclick="openModal()">✛ Thêm sân mới</button>
            </div>
            <!-- Hiển thị danh sách sân -->
            <div class="border-0 shadow-sm px-4 data-container pt-3" id="field-data-container"></div>

            <!-- Hiển thị form modal dành cho field -->
            <div id="modal"></div>
        </div>
    </main>
</div>
<div id="footer" data-root="../../"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/owner/field.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>loadData();</script>
</body>
</html>

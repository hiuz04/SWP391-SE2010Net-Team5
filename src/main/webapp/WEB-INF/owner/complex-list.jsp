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
  <link href="<%= ctx %>/assets/css/owner/styles.css" rel="stylesheet">
  <title>Quản lý cơ sở | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Cơ sở"></div>
<main class="py-5 main-wrapper">
  <div class="container">
    <div class="d-flex justify-content-between mb-4">
      <h1 class="section-title">Quản lý cơ sở</h1>
      <button class="btn btn-sf-primary" style="height: 40px; background: rgb(5, 150, 105);" onclick="navigateComplexForm()">✛ Thêm cơ sở mới</button>
    </div>
    <!-- Hiển thị danh sách cơ sở -->
    <div class="border-0 shadow-sm px-4 data-container pt-3" id="complex-data-container"></div>
  </div>
</main>
<div id="footer" data-root="../../"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/owner/complex.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>loadData()</script>
</body>
</html>

<%@ page import="com.swp.model.User" %><%--
  Created by IntelliJ IDEA.
  User: duong
  Date: 6/7/2026
  Time: 6:32 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) {
        navRole = "guest";
    }
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
    <link href="<%= ctx %>/assets/css/customer/search.css" rel="stylesheet">

    <title>Tìm sân | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Tìm sân"></div>

<main class="py-4">
    <div class="container">
        <div class="row g-4">
            <aside class="col-lg-3">
                <div class="card soft-card p-3 sidebar-card">
                    <h5>Bộ lọc tìm kiếm</h5>

                    <label for="complexName" class="form-label">
                        Tên sân bóng
                    </label>
                    <input type="text" class="form-control" id="complexName" placeholder="Nhập tên sân bóng..." oninput="scheduleLoadData()">

                    <label class="form-label mt-3" for="type">Loại sân</label>
                    <select class="form-select" id="fieldType" onchange="scheduleLoadData()">
                        <option value="">-- Chọn loại sân --</option>
                        <option value="1">Sân 5</option>
                        <option value="2">Sân 7</option>
                        <option value="3">Sân 11</option>
                        <option value="4">Sân futsal</option>
                    </select>

                    <div class="d-grid mt-3">
                        <button class="btn btn-sf-primary" onclick="clearAll()">Clear</button>
                    </div>
                </div>
            </aside>
            <section class="col-lg-9">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div><h1 class="section-title">Tìm sân bóng</h1>
                        <p class="text-muted mb-0" id="fieldCount"></p></div>
                    <select class="form-select w-auto" id="sortOrder" onchange="searchData()">
                        <option value="">Sắp xếp: Mặc định</option>
                        <option value="price_asc">Giá: Thấp đến Cao</option>
                        <option value="price_desc">Giá: Cao đến Thấp</option>
                    </select></div>
                <div class="row g-4" id="list-container"></div>
            </section>
        </div>
    </div>
</main>
<div id="footer" data-root="../../"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/customer/search.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>


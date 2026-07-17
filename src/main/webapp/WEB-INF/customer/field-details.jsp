<%@ page import="com.swp.model.User" %><%--
  Created by IntelliJ IDEA.
  User: duong
  Date: 6/7/2026
  Time: 3:58 PM
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
    <link href="<%= ctx %>/assets/css/customer/field-detail.css" rel="stylesheet">

    <title>Chi tiết sân | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Tìm sân"></div>
<main class="py-4">
    <div class="container">
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="<%= ctx %>/index.jsp">Trang chủ</a></li>
                <li class="breadcrumb-item"><a href="<%= ctx %>/search">Tìm sân</a></li>
                <li class="breadcrumb-item active">Sân bóng Thể Thao A</li>
            </ol>
        </nav>
        <div>
            <img class="rounded-4 w-100 mb-4" style="height:420px;object-fit:cover"
                 src="https://res.cloudinary.com/du02dvkx7/image/upload/v1780892069/court_a6at9n.webp"
                 alt="Sân bóng">
            <div class="row g-4">
                <div class="col-lg-8">
                    <div class="card soft-card p-4">
                        <h2 class="section-title" id="field-name"></h2>

                        <p class="text-muted" id="address"></p>
                        
                        <h4 class="text-success fw-bold mt-2 mb-3" id="currentPrice"></h4>

                        <div class="d-flex gap-4">
                            <span>
                                <i class="bi bi-clock"></i>
                                Mở cửa: <strong id="workingTime"></strong>
                            </span>
                            <span>
                                <i class="bi bi-telephone-fill me-1"></i>
                                Hotline:
                                <strong class="hotline"></strong>
                            </span>
                            <span>
                                <i class="bi bi-grid-3x3-gap"></i>
                                Số lượng sân:
                                <strong id="fieldCount"></strong>
                            </span>
                        </div>

                        <div class="mt-4">
                            <h5>Mô tả</h5>
                            <p id="description"></p>
                        </div>

                        <div>
                            <h5><i class="table-cells"></i><strong>Danh sách sân</strong></h5>
                            <p id="fields" class="d-flex gap-3"></p>
                        </div>

                    </div>
                    <div class="card soft-card p-4">
                        <h2 class="section-title"><i class="bi bi-tag-fill me-2"></i> Bảng giá thuê sân</h2>
                        <div id="price-rule-list" class="mt-3">
                            <!-- Bảng giá sẽ render ở đây -->
                        </div>
                    </div>
                    <div class="card soft-card p-4 mt-3">
                        <h2 class="section-title">Đánh giá</h2>

                        <div id="feedbackContainer">
                            <!-- Feedback sẽ được render tại đây -->
                        </div>
                    </div>
                </div>
                <aside class="col-lg-4">
                    <div class="card soft-card p-4 sidebar-card"><h4 class="fw-bold">Đặt sân</h4>
                        <a class="btn btn-sf-primary btn-lg w-100" id="bookingUrl" href="<%= ctx %>/booking"><i
                                class="bi bi-calendar-check me-2" style="color: white"></i> Đặt sân ngay</a>
                        <a class="btn-sf-secondary btn-lg w-100"><i
                                class="bi bi-telephone-fill me-1"></i> Gọi: <span class="hotline"></span></a>
                    </div>
                </aside>
            </div>
        </div>
    </div>
</main>
<div id="footer" data-root="../../"></div>
<script>
    window.APP_CTX = '<%= ctx %>';
    const currentRole = "<%= navRole %>";
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script src="<%= ctx %>/assets/js/customer/field-detail.js"></script>
</body>
</html>

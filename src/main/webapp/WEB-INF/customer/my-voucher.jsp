<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
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
    <link href="<%= ctx %>/assets/css/customer/voucher.css" rel="stylesheet">

    <title>Voucher Của Tôi | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Voucher của tôi"></div>
<div class="container py-4">
<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="fw-bold mb-1">Voucher Của Tôi</h2>
        <p class="text-muted mb-0">
            Bạn đang sở hữu <span id="voucherCount">0</span> voucher | Điểm thưởng hiện có: <strong id="userPointDisplay" class="text-success"><%= sessionUser != null ? sessionUser.getRewardPoints() : 0 %></strong> điểm
        </p>
    </div>
    <div>
        <a href="<%= ctx %>/vouchers?to=center" class="btn btn-outline-success">
            <i class="bi bi-ticket-perforated me-1"></i> Đổi thêm voucher
        </a>
    </div>
</div>

<!-- Filter -->
<ul class="nav nav-pills mb-4" id="voucherFilter">
    <li class="nav-item">
        <button class="nav-link active" data-status="ALL">
            Tất cả
        </button>
    </li>

    <li class="nav-item">
        <button class="nav-link" data-status="AVAILABLE">
            Có thể sử dụng
        </button>
    </li>

    <li class="nav-item">
        <button class="nav-link" data-status="USED">
            Đã sử dụng
        </button>
    </li>

    <li class="nav-item">
        <button class="nav-link" data-status="EXPIRED">
            Hết hạn
        </button>
    </li>
</ul>

<div class="row g-4" id="myVoucherList">
</div>

</div>
<div id="footer" data-root="../../"></div>
<script>
    window.APP_CTX = '<%= ctx %>';
    const currentRole = "<%= navRole %>";
    const userPoint = "<%= sessionUser.getRewardPoints() %>";
    const userId = "<%= sessionUser.getUserId() %>"
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script src="<%= ctx %>/assets/js/customer/voucher.js"></script>
<script>
    loadMyVoucher("ALL");
</script>
</body>
</html>
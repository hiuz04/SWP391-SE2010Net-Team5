<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.time.LocalDateTime" %>
<%
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    // Fallback navRole để navbar vẫn render nếu session thiếu role hiển thị.
    if (navRole == null) {
        navRole = "guest";
    }
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    // VIP chỉ tính khi còn hạn, không chỉ dựa vào cờ isVip.
    boolean isVip = sessionUser != null
            && sessionUser.isVip()
            && sessionUser.getVipValidUntil() != null
            && sessionUser.getVipValidUntil().isAfter(LocalDateTime.now());
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

    <title>Kho Voucher | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Kho voucher"></div>
<div class="container py-5">
    <!-- HEADER -->
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 class="fw-bold mb-1">
                <i class="bi bi-ticket-perforated-fill text-success"></i>
                Đổi điểm lấy Voucher
            </h2>
            <p class="text-muted mb-0">
                Sử dụng điểm thưởng để đổi các voucher ưu đãi hấp dẫn
            </p>
        </div>
    </div>

    <!-- POINT CARD -->
    <div class="point-card mb-5">
        <div class="row align-items-center">
            <div class="col-md-8">
                <h5>
                    <i class="bi bi-stars"></i>
                    Điểm thưởng hiện có
                </h5>
                <div class="point-value" id="available-point">
                    <%= sessionUser.getRewardPoints() %>
                </div>
                <small> Mỗi 100.000đ chi tiêu = 100 điểm thưởng </small>
            </div>
            <div class="col-md-4 text-md-end mt-3 mt-md-0">
                <span class="badge bg-warning text-dark fs-6 px-3 py-2">
                    MEMBER
                </span>
            </div>
        </div>
    </div>

    <!-- SEARCH -->
    <div class="row mb-4">
        <div class="col-lg-6">
            <div class="input-group">
                <span class="input-group-text bg-white">
                    <i class="bi bi-search"></i>
                </span>

                <input type="text" id="voucherSearchInput" class="form-control search-box" placeholder="Tìm voucher..." >
            </div>
        </div>
    </div>

    <%-- Chỉ Customer VIP còn hạn mới thấy filter voucher hội viên. --%>
    <% if (isVip) { %>
    <div class="mb-4" id="voucherTypeFilter">
        <button class="btn btn-success filter-btn me-2 active" data-type="ALL_TYPE">
            Tất cả
        </button>

        <button class="btn btn-outline-success filter-btn me-2" data-type="ALL">
            Voucher thường
        </button>

        <button class="btn btn-outline-warning filter-btn" data-type="MEMBER">
            Voucher hội viên
        </button>
    </div>
    <% } %>

    <div class="row g-4" id="voucher-list"></div>

    </div>
</div>
<div id="footer" data-root="../../"></div>

<!-- CONFIRM MODAL -->
<%-- Modal xác nhận trước khi POST đổi voucher bằng điểm. --%>
<div class="modal fade" id="exchangeModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"> Xác nhận đổi voucher </h5>
            <button
                type="button"
                class="btn-close"
                data-bs-dismiss="modal"
            >
            </button>
        </div>

        <div class="modal-body">
            <p id="voucherName"></p>

            <div class="alert alert-info">
                <div>
                    Điểm hiện có:
                    <strong id="currentPoint"><%= sessionUser.getRewardPoints() %></strong>
                </div>

                <div>
                    Điểm cần:
                    <strong id="exchangePoint"></strong>
                </div>

                <div>
                    Sau khi đổi:
                    <strong id="remainPoint"></strong>
                </div>
            </div>
        </div>

        <div class="modal-footer">
            <button
                class="btn btn-secondary"
                data-bs-dismiss="modal">
                Hủy
            </button>

            <button
                class="btn btn-success"
                id="confirmExchangeBtn">
                Xác nhận đổi
            </button>
        </div>
    </div>
</div>

<script>
    window.APP_CTX = '<%= ctx %>';
    const currentRole = "<%= navRole %>";
    const userPoint = "<%= sessionUser.getRewardPoints() %>";
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script src="<%= ctx %>/assets/js/customer/voucher.js"></script>
<script>
    // Mặc định load toàn bộ voucher Customer được phép thấy.
    loadVoucherStock("ALL_TYPE");
</script>
</body>
</html>

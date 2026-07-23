<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.swp.model.dto.BookingView" %>
<%@ page import="com.swp.model.User" %>
<%
    String ctx = request.getContextPath();
    List<BookingView> bookingList = (List<BookingView>) request.getAttribute("bookingList");
    NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    int currentPage = request.getAttribute("currentPage") != null ? (Integer) request.getAttribute("currentPage") : 1;
    int totalPages = request.getAttribute("totalPages") != null ? (Integer) request.getAttribute("totalPages") : 1;
    
    String searchVal = request.getAttribute("search") != null ? (String) request.getAttribute("search") : "";
    String filterVal = request.getAttribute("filter") != null ? (String) request.getAttribute("filter") : "";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Lịch Đặt Sân - Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
</head>
<body class="bg-light">

<%
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Admin";
%>
<div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= currentName %>" data-active="Lịch đặt sân"></div>

<main class="dashboard-shell py-4">
    <div class="container">
    <!-- Header Page -->
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 class="fw-bold mb-0">Quản lý Lịch Đặt Sân</h2>
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb mb-0">
                    <li class="breadcrumb-item"><a href="<%= ctx %>/admin/dashboard" class="text-decoration-none text-muted">Dashboard</a></li>
                    <li class="breadcrumb-item active" aria-current="page">Lịch đặt sân</li>
                </ol>
            </nav>
        </div>
    </div>

    <!-- Main Content -->
    <div class="card shadow-sm border-0 rounded-3">
        <!-- Filters & Toolbar -->
        <div class="card-header bg-white border-bottom-0 pt-4 pb-0 px-4">
            <form method="GET" action="<%= ctx %>/admin/bookings">
                <div class="row g-3 mb-3">
                    <div class="col-md-5">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" name="search" value="<%= searchVal.replace("\"", "&quot;") %>" class="form-control border-start-0 ps-0" placeholder="Tìm theo mã booking, tên, SĐT...">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <select name="filter" class="form-select">
                            <option value="">Tất cả</option>
                            <option value="revenue_today" <%= "revenue_today".equals(filterVal) ? "selected" : "" %>>Doanh thu hôm nay (Paid Invoices)</option>
                            <option value="revenue_7days" <%= "revenue_7days".equals(filterVal) ? "selected" : "" %>>Doanh thu 7 ngày qua</option>
                            <option value="bookings_today" <%= "bookings_today".equals(filterVal) ? "selected" : "" %>>Lượt đặt sân hôm nay</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <button type="submit" class="btn btn-primary w-100"><i class="bi bi-funnel me-1"></i> Lọc</button>
                    </div>
                </div>
            </form>
        </div>

        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light text-muted" style="font-size: 0.85rem; text-transform: uppercase;">
                        <tr>
                            <th class="ps-4">Mã Booking</th>
                            <th>Khách hàng</th>
                            <th>Sân</th>
                            <th>Thời gian</th>
                            <th>Tổng tiền</th>
                            <th>Trạng thái</th>
                            <th class="text-end pe-4">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
<%
    if (bookingList != null && !bookingList.isEmpty()) {
        for (BookingView b : bookingList) {
            String badgeClass = "bg-secondary";
            if ("COMPLETED".equals(b.getStatus())) badgeClass = "bg-success";
            else if ("PENDING".equals(b.getStatus())) badgeClass = "bg-warning text-dark";
            else if ("HOLD".equals(b.getStatus())) badgeClass = "bg-info text-dark";
            else if ("CANCELLED".equals(b.getStatus()) || "REJECTED".equals(b.getStatus())) badgeClass = "bg-danger";
%>
                        <tr>
                            <td class="ps-4 fw-medium">#<%= b.getBookingCode() %></td>
                            <td>
                                <div><%= b.getCustomerName() %></div>
                                <small class="text-muted"><%= b.getCustomerPhone() %></small>
                            </td>
                            <td>
                                <div><%= b.getFieldName() %> - <%= b.getFieldTypeName() %></div>
                                <small class="text-muted"><%= b.getComplexName() %></small>
                            </td>
                            <td>
                                <div><%= b.getStartTime() != null ? b.getStartTime().format(dtf) : "" %></div>
                                <small class="text-muted">Đến <%= b.getEndTime() != null ? b.getEndTime().format(dtf) : "" %></small>
                            </td>
                            <td class="fw-bold text-success">
                                <%= currencyFormat.format(b.getFinalAmount() != null ? b.getFinalAmount() : 0) %>₫
                            </td>
                            <td>
                                <span class="badge <%= badgeClass %>"><%= b.getStatus() %></span>
                            </td>
                            <td class="text-end pe-4">
                                <a href="<%= ctx %>/admin/booking-detail?id=<%= b.getBookingId() %>" class="btn btn-sm btn-outline-primary" title="Xem chi tiết">
                                    <i class="bi bi-eye"></i> Chi tiết
                                </a>
                            </td>
                        </tr>
<%
        }
    } else {
%>
                        <tr>
                            <td colspan="7" class="text-center py-5 text-muted">
                                <i class="bi bi-inbox fs-1 d-block mb-3"></i>
                                Không có lượt đặt sân nào.
                            </td>
                        </tr>
<%
    }
%>
                    </tbody>
                </table>
            </div>
        </div>
        
<%
    String searchParam = searchVal != null && !searchVal.isEmpty() ? "&search=" + searchVal.replace("\"", "&quot;") : "";
    String filterParam = filterVal != null && !filterVal.isEmpty() ? "&filter=" + filterVal.replace("\"", "&quot;") : "";
    String queryParams = searchParam + filterParam;
%>
        <!-- Pagination -->
        <div class="card-footer bg-white border-top p-3">
            <div class="d-flex justify-content-between align-items-center">
                <span class="text-muted small">
                    Trang <%= currentPage %> / <%= totalPages %>
                </span>
                <% if (totalPages > 1) { %>
                <nav aria-label="Page navigation">
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item <%= (currentPage == 1) ? "disabled" : "" %>">
                            <a class="page-link" href="<%= ctx %>/admin/bookings?page=<%= currentPage - 1 %><%= queryParams %>"><i class="bi bi-chevron-left"></i></a>
                        </li>
                        <% for (int i = 1; i <= totalPages; i++) { %>
                        <li class="page-item <%= (currentPage == i) ? "active" : "" %>">
                            <a class="page-link" href="<%= ctx %>/admin/bookings?page=<%= i %><%= queryParams %>"><%= i %></a>
                        </li>
                        <% } %>
                        <li class="page-item <%= (currentPage == totalPages) ? "disabled" : "" %>">
                            <a class="page-link" href="<%= ctx %>/admin/bookings?page=<%= currentPage + 1 %><%= queryParams %>"><i class="bi bi-chevron-right"></i></a>
                        </li>
                    </ul>
                </nav>
                <% } %>
            </div>
        </div>
    </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

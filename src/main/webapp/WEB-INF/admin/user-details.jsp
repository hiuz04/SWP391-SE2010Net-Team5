<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.BookingView" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Admin";
            
    User userDetail = (User) request.getAttribute("userDetail");
    List<BookingView> recentBookings = (List<BookingView>) request.getAttribute("recentBookings");
    
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Chi tiết người dùng | Sport Field Booking</title>
    <style>
        .avatar-lg {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32px;
            font-weight: bold;
            color: white;
            background: linear-gradient(135deg, #6c757d, #adb5bd);
        }
    </style>
</head>
<body class="bg-light">
<div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= esc(currentName) %>" data-active="Người dùng"></div>

<main class="py-4">
    <div class="container">
        <div class="mb-4">
            <a href="<%= ctx %>/admin/users" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left me-1"></i> Quay lại</a>
        </div>
        
        <div class="row g-4">
            <!-- User Profile Card -->
            <div class="col-lg-4">
                <div class="card soft-card border-0 shadow-sm">
                    <div class="card-body text-center p-4">
                        <%
                            String firstLetter = userDetail.getFullName() != null && userDetail.getFullName().length() > 0 ? userDetail.getFullName().substring(0, 1).toUpperCase() : "U";
                        %>
                        <div class="d-flex justify-content-center mb-3">
                            <div class="avatar-lg"><%= esc(firstLetter) %></div>
                        </div>
                        <h4 class="fw-bold mb-1"><%= esc(userDetail.getFullName()) %></h4>
                        <p class="text-muted mb-3"><%= esc(userDetail.getEmail()) %></p>
                        
                        <div class="d-flex justify-content-center gap-2 mb-4">
                            <%
                                String roleBadgeClass = "bg-secondary";
                                String roleName = userDetail.getRoleName() != null ? userDetail.getRoleName() : "Khách hàng";
                                if ("Admin".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-danger";
                                else if ("Customer".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-primary";
                                else if ("Owner".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-warning text-dark";
                                else if ("Staff".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-info";
                    
                                String statusBadgeClass = "bg-secondary";
                                String statusText = userDetail.getStatus() != null ? userDetail.getStatus() : "ACTIVE";
                                if ("ACTIVE".equalsIgnoreCase(statusText)) {
                                    statusBadgeClass = "bg-success";
                                    statusText = "Hoạt động";
                                } else if ("PENDING".equalsIgnoreCase(statusText)) {
                                    statusBadgeClass = "bg-warning text-dark";
                                    statusText = "Chờ duyệt";
                                } else if ("BANNED".equalsIgnoreCase(statusText)) {
                                    statusBadgeClass = "bg-danger";
                                    statusText = "Đã khóa";
                                }
                            %>
                            <span class="badge <%= roleBadgeClass %>"><%= esc(roleName) %></span>
                            <span class="badge <%= statusBadgeClass %>"><%= statusText %></span>
                        </div>
                        
                        <ul class="list-group list-group-flush text-start">
                            <li class="list-group-item px-0 py-3 d-flex align-items-center">
                                <i class="bi bi-telephone text-muted me-3 fs-5"></i>
                                <div>
                                    <div class="small text-muted">Số điện thoại</div>
                                    <div class="fw-semibold"><%= esc(userDetail.getPhone()) %></div>
                                </div>
                            </li>
                            <li class="list-group-item px-0 py-3 d-flex align-items-center">
                                <i class="bi bi-person-badge text-muted me-3 fs-5"></i>
                                <div>
                                    <div class="small text-muted">ID Người dùng</div>
                                    <div class="fw-semibold">#USR-<%= String.format("%03d", userDetail.getUserId()) %></div>
                                </div>
                            </li>
                            <li class="list-group-item px-0 py-3 d-flex align-items-center">
                                <i class="bi bi-calendar3 text-muted me-3 fs-5"></i>
                                <div>
                                    <div class="small text-muted">Ngày tham gia</div>
                                    <div class="fw-semibold"><%= userDetail.getCreatedAt() != null ? userDetail.getCreatedAt().format(dateFmt) : "" %></div>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
            
            <!-- User Booking History -->
            <div class="col-lg-8">
                <div class="card soft-card border-0 shadow-sm h-100">
                    <div class="card-header bg-white border-0 pt-4 pb-0">
                        <h5 class="fw-bold mb-0">Lịch sử đặt sân</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>Mã đặt sân</th>
                                        <th>Sân / Cơ sở</th>
                                        <th>Thời gian đá</th>
                                        <th>Tổng tiền</th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                <%
                                    if (recentBookings != null && !recentBookings.isEmpty()) {
                                        for (BookingView b : recentBookings) {
                                            String st = b.getStatus();
                                            String badgeClass = "bg-secondary";
                                            String stText = st;
                                            if ("CONFIRMED".equalsIgnoreCase(st)) { badgeClass = "bg-success bg-opacity-10 text-success border border-success border-opacity-25"; stText = "Đã xác nhận"; }
                                            else if ("PENDING".equalsIgnoreCase(st)) { badgeClass = "bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25"; stText = "Chờ thanh toán"; }
                                            else if ("CHECKED_IN".equalsIgnoreCase(st)) { badgeClass = "bg-info bg-opacity-10 text-info border border-info border-opacity-25"; stText = "Đang đá"; }
                                            else if ("COMPLETED".equalsIgnoreCase(st)) { badgeClass = "bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25"; stText = "Hoàn thành"; }
                                            else if ("CANCELLED".equalsIgnoreCase(st)) { badgeClass = "bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25"; stText = "Đã hủy"; }
                                %>
                                    <tr>
                                        <td><strong><%= esc(b.getBookingCode()) %></strong></td>
                                        <td>
                                            <div class="fw-semibold"><%= esc(b.getFieldName()) %></div>
                                            <small class="text-muted"><%= esc(b.getComplexName()) %></small>
                                        </td>
                                        <td>
                                            <div><%= b.getStartTime() != null ? b.getStartTime().format(dtf) : "" %></div>
                                            <small class="text-muted">đến <%= b.getEndTime() != null ? b.getEndTime().format(dtf).substring(11) : "" %></small>
                                        </td>
                                        <td class="fw-semibold text-success"><%= currencyFormat.format(b.getTotalAmount()) %>₫</td>
                                        <td><span class="badge rounded-pill <%= badgeClass %> px-2"><%= stText %></span></td>
                                    </tr>
                                <%
                                        }
                                    } else {
                                %>
                                    <tr>
                                        <td colspan="5" class="text-center py-5 text-muted">Người dùng này chưa có lượt đặt sân nào.</td>
                                    </tr>
                                <%
                                    }
                                %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

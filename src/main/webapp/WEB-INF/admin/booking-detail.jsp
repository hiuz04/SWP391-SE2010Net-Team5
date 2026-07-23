<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.swp.model.dto.BookingView" %>
<%@ page import="com.swp.model.User" %>
<%
    String ctx = request.getContextPath();
    BookingView b = (BookingView) request.getAttribute("booking");
    NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    
    if (b == null) {
        response.sendRedirect(ctx + "/admin/bookings");
        return;
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi tiết Lượt đặt sân #<%= b.getBookingCode() %> - Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <style>
        .detail-card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            margin-bottom: 24px;
        }
        .detail-header {
            background-color: #f8f9fa;
            border-bottom: 1px solid #edf2f9;
            border-radius: 12px 12px 0 0;
            padding: 16px 24px;
        }
        .detail-body {
            padding: 24px;
        }
        .info-label {
            color: #6e84a3;
            font-size: 0.875rem;
            margin-bottom: 4px;
        }
        .info-value {
            font-weight: 500;
            color: #12263f;
        }
    </style>
</head>
<body class="bg-light">

<%
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Admin";
%>
<div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= currentName %>" data-active="Lịch đặt sân"></div>

<main class="dashboard-shell py-4">
    <div class="container" style="max-width: 900px;">
    <!-- Header Page -->
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 class="fw-bold mb-1">Chi tiết Lượt đặt sân</h2>
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb mb-0">
                    <li class="breadcrumb-item"><a href="<%= ctx %>/admin/dashboard" class="text-decoration-none text-muted">Dashboard</a></li>
                    <li class="breadcrumb-item"><a href="<%= ctx %>/admin/bookings" class="text-decoration-none text-muted">Lịch đặt sân</a></li>
                    <li class="breadcrumb-item active" aria-current="page">#<%= b.getBookingCode() %></li>
                </ol>
            </nav>
        </div>
        <a href="<%= ctx %>/admin/bookings" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left"></i> Quay lại
        </a>
    </div>

    <!-- Booking Summary Card -->
    <div class="card detail-card">
        <div class="detail-header d-flex justify-content-between align-items-center">
            <h5 class="mb-0 fw-bold"><i class="bi bi-info-circle text-primary me-2"></i>Thông tin chung</h5>
            <%
                String badgeClass = "bg-secondary";
                if ("COMPLETED".equals(b.getStatus())) badgeClass = "bg-success";
                else if ("PENDING".equals(b.getStatus())) badgeClass = "bg-warning text-dark";
                else if ("HOLD".equals(b.getStatus())) badgeClass = "bg-info text-dark";
                else if ("CANCELLED".equals(b.getStatus()) || "REJECTED".equals(b.getStatus())) badgeClass = "bg-danger";
            %>
            <span class="badge <%= badgeClass %> fs-6 px-3 py-2"><%= b.getStatus() %></span>
        </div>
        <div class="detail-body row g-4">
            <div class="col-md-6">
                <div class="info-label">Mã Booking</div>
                <div class="info-value fs-5">#<%= b.getBookingCode() %></div>
            </div>
            <div class="col-md-6">
                <div class="info-label">Ngày tạo</div>
                <div class="info-value"><%= b.getCreatedAt() != null ? b.getCreatedAt().format(dtf) : "N/A" %></div>
            </div>
            <% if ("CANCELLED".equals(b.getStatus()) || "REJECTED".equals(b.getStatus())) { %>
            <div class="col-12 mt-3 p-3 bg-danger bg-opacity-10 rounded border border-danger border-opacity-25">
                <div class="d-flex">
                    <i class="bi bi-exclamation-triangle text-danger me-2 mt-1"></i>
                    <div>
                        <div class="fw-bold text-danger">Đã bị hủy vào <%= b.getCancelledAt() != null ? b.getCancelledAt().format(dtf) : "N/A" %></div>
                        <div class="text-danger small mt-1">Lý do: <%= b.getCancellationReason() != null ? b.getCancellationReason() : "Không có lý do" %></div>
                    </div>
                </div>
            </div>
            <% } %>
        </div>
    </div>

    <!-- Details Grid -->
    <div class="row g-4">
        <!-- Khách hàng & Sân -->
        <div class="col-md-6">
            <div class="card detail-card h-100">
                <div class="detail-header">
                    <h5 class="mb-0 fw-bold"><i class="bi bi-person text-success me-2"></i>Khách hàng & Sân</h5>
                </div>
                <div class="detail-body">
                    <div class="mb-3">
                        <div class="info-label">Khách hàng</div>
                        <div class="info-value"><%= b.getCustomerName() %></div>
                        <div class="text-muted small mt-1"><i class="bi bi-telephone me-1"></i><%= b.getCustomerPhone() %></div>
                        <div class="text-muted small"><i class="bi bi-envelope me-1"></i><%= b.getCustomerEmail() != null ? b.getCustomerEmail() : "Không có" %></div>
                    </div>
                    <hr>
                    <div class="mb-3">
                        <div class="info-label">Khu vực sân</div>
                        <div class="info-value"><%= b.getComplexName() %></div>
                        <div class="text-muted small"><i class="bi bi-geo-alt me-1"></i><%= b.getComplexAddress() %></div>
                    </div>
                    <div>
                        <div class="info-label">Sân thi đấu</div>
                        <div class="info-value"><%= b.getFieldName() %> <span class="badge bg-light text-dark ms-2 border"><%= b.getFieldTypeName() %></span></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Thời gian & Thanh toán -->
        <div class="col-md-6">
            <div class="card detail-card h-100">
                <div class="detail-header">
                    <h5 class="mb-0 fw-bold"><i class="bi bi-clock text-warning me-2"></i>Thời gian & Thanh toán</h5>
                </div>
                <div class="detail-body">
                    <div class="mb-3">
                        <div class="info-label">Thời gian bắt đầu</div>
                        <div class="info-value text-primary fw-bold"><%= b.getStartTime() != null ? b.getStartTime().format(dtf) : "N/A" %></div>
                    </div>
                    <div class="mb-3">
                        <div class="info-label">Thời gian kết thúc</div>
                        <div class="info-value text-danger fw-bold"><%= b.getEndTime() != null ? b.getEndTime().format(dtf) : "N/A" %></div>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Tổng tiền:</span>
                        <span class="fw-bold"><%= currencyFormat.format(b.getFinalAmount() != null ? b.getFinalAmount() : 0) %>₫</span>
                    </div>
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Đã thanh toán:</span>
                        <span class="fw-bold text-success"><%= currencyFormat.format(b.getPaidAmount() != null ? b.getPaidAmount() : 0) %>₫</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="text-muted">Trạng thái TT:</span>
                        <% if ("SUCCESS".equals(b.getPaymentStatus())) { %>
                            <span class="badge bg-success">Đã thanh toán</span>
                        <% } else { %>
                            <span class="badge bg-warning text-dark"><%= b.getPaymentStatus() != null ? b.getPaymentStatus() : "Chưa thanh toán" %></span>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>
    </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

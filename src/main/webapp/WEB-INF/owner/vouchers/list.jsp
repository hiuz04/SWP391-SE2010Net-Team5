<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.Voucher" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String money(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(value);
    }

    private String dateTime(LocalDateTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String statusBadge(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) return "bg-success";
        return "bg-secondary";
    }

    private String discountTypeText(String discountType) {
        if ("PERCENT".equalsIgnoreCase(discountType)) return "Theo phần trăm";
        if ("FIXED".equalsIgnoreCase(discountType)) return "Số tiền cố định";
        return esc(discountType);
    }

    private String statusText(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) return "Đang hoạt động";
        if ("DISABLED".equalsIgnoreCase(status)) return "Tạm tắt";
        return esc(status);
    }
%>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Owner";
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = currentUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    List<Voucher> vouchers = (List<Voucher>) request.getAttribute("vouchers");
    String successMessage = (String) session.getAttribute("successMessage");
    String errorMessage = (String) session.getAttribute("errorMessage");
    if (successMessage != null) session.removeAttribute("successMessage");
    if (errorMessage != null) session.removeAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
    <title>Quản lý mã giảm giá | Sport Field Booking</title>
</head>
<body class="bg-light">
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= esc(currentName) %>" data-active="Mã giảm giá"></div>

<main class="owner-content">
    <div class="page-header">
        <div class="page-header-left">
            <h1><i class="bi bi-ticket-perforated me-2"></i>Quản lý mã giảm giá</h1>
            <p>Tạo, sửa và bật/tắt mã giảm giá cho luồng đặt sân.</p>
        </div>
        <a class="btn btn-success px-4 py-2" href="<%= ctx %>/owner/vouchers?action=create">
            <i class="bi bi-plus-circle me-1"></i>Thêm mã giảm giá
        </a>
    </div>

    <% if (successMessage != null) { %>
    <div class="alert alert-success alert-dismissible fade show" role="alert">
        <%= esc(successMessage) %>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
    </div>
    <% } %>
    <% if (errorMessage != null) { %>
    <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <%= esc(errorMessage) %>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
    </div>
    <% } %>

            <div class="card soft-card border-0 shadow-sm">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                        <tr>
                            <th class="ps-4">Mã</th>
                            <th>Tên mã giảm giá</th>
                            <th>Loại giảm</th>
                            <th>Giá trị giảm</th>
                            <th>Đơn tối thiểu</th>
                            <th>Số lượng</th>
                            <th>Đã dùng</th>
                            <th>Ngày bắt đầu</th>
                            <th>Ngày kết thúc</th>
                            <th>Trạng thái</th>
                            <th class="text-end pe-4">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% if (vouchers != null && !vouchers.isEmpty()) {
                            for (Voucher voucher : vouchers) {
                                boolean active = "ACTIVE".equalsIgnoreCase(voucher.getStatus());
                        %>
                        <tr>
                            <td class="ps-4 fw-bold"><%= esc(voucher.getCode()) %></td>
                            <td><%= esc(voucher.getName()) %></td>
                            <td><span class="badge bg-info"><%= discountTypeText(voucher.getDiscountType()) %></span></td>
                            <td><%= "PERCENT".equalsIgnoreCase(voucher.getDiscountType())
                                    ? esc(voucher.getDiscountValue() + "%")
                                    : money(voucher.getDiscountValue()) %></td>
                            <td><%= money(voucher.getMinOrder()) %></td>
                            <td><%= voucher.getQuantity() %></td>
                            <td><%= voucher.getUsed() %> / <%= voucher.getQuantity() %></td>
                            <td><%= dateTime(voucher.getStartDate()) %></td>
                            <td><%= dateTime(voucher.getEndDate()) %></td>
                            <td><span class="badge <%= statusBadge(voucher.getStatus()) %>"><%= statusText(voucher.getStatus()) %></span></td>
                            <td class="text-end pe-4">
                                <%-- Business Rule BR-39: Manage Voucher chỉ cho sửa và bật/tắt, không có thao tác xóa vĩnh viễn. --%>
                                <a class="btn btn-sm btn-outline-primary" href="<%= ctx %>/owner/vouchers?action=edit&id=<%= voucher.getId() %>">
                                    <i class="bi bi-pencil"></i>
                                </a>
                                <form method="post" action="<%= ctx %>/owner/vouchers" class="d-inline">
                                    <input type="hidden" name="action" value="toggle-status">
                                    <input type="hidden" name="id" value="<%= voucher.getId() %>">
                                    <button type="submit" class="btn btn-sm <%= active ? "btn-outline-secondary" : "btn-outline-success" %>">
                                        <%= active ? "Tắt" : "Bật" %>
                                    </button>
                                </form>
                            </td>
                        </tr>
                        <%  }
                        } else { %>
                        <tr>
                            <td colspan="11" class="text-center text-muted py-4">Chưa có mã giảm giá nào.</td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
    display_name = '<%= currentName %>';
    current_role = '<%= navRole %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

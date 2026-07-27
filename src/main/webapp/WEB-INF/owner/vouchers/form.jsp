<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.Voucher" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String decimalValue(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String dateTimeValue(LocalDateTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
%>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Owner";
    Voucher voucher = (Voucher) request.getAttribute("voucher");
    if (voucher == null) voucher = new Voucher();
    String mode = (String) request.getAttribute("mode");
    if (mode == null || mode.isBlank()) mode = voucher.getId() > 0 ? "edit" : "create";
    boolean edit = "edit".equals(mode);
    String error = (String) request.getAttribute("error");
    String discountType = voucher.getDiscountType() == null ? "PERCENT" : voucher.getDiscountType();
    String status = voucher.getStatus() == null ? "ACTIVE" : voucher.getStatus();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title><%= edit ? "Sửa mã giảm giá" : "Tạo mã giảm giá" %> | Sport Field Booking</title>
</head>
<body class="bg-light">
<div id="navbar" data-root="<%= ctx %>/" data-role="owner" data-name="<%= esc(currentName) %>" data-active="Mã giảm giá"></div>

<main class="py-4">
    <div class="container">
        <div class="mb-3">
            <a class="btn btn-outline-secondary" href="<%= ctx %>/owner/vouchers">
                <i class="bi bi-arrow-left"></i> Quay l&#7841;i
            </a>
        </div>

        <div class="card soft-card border-0 shadow-sm">
            <div class="card-body p-4">
                <h1 class="section-title mb-3"><%= edit ? "Sửa mã giảm giá" : "Tạo mã giảm giá" %></h1>
                <% if (error != null && !error.isBlank()) { %>
                <div class="alert alert-danger"><%= esc(error) %></div>
                <% } %>

                <form method="post" action="<%= ctx %>/owner/vouchers?action=<%= edit ? "edit" : "create" %>">
                    <% if (edit) { %>
                    <input type="hidden" name="id" value="<%= voucher.getId() %>">
                    <% } %>
                    <div class="row g-3">
                        <div class="col-md-4">
                            <label class="form-label fw-semibold" for="code">Mã giảm giá <span class="text-danger">*</span></label>
                            <%-- Business Rule BR-31: UI bắt buộc code và giới hạn 50 ký tự trước khi servlet chuẩn hóa uppercase/unique. --%>
                            <input class="form-control text-uppercase" id="code" name="code" maxlength="50"
                                   value="<%= esc(voucher.getCode()) %>" required>
                        </div>
                        <div class="col-md-8">
                            <label class="form-label fw-semibold" for="name">Tên mã giảm giá <span class="text-danger">*</span></label>
                            <%-- Business Rule BR-31: UI bắt buộc name trước khi submit form voucher. --%>
                            <%-- Business Rule BR-32: UI giới hạn tên voucher tối đa 255 ký tự. --%>
                            <input class="form-control" id="name" name="name" maxlength="255"
                                   value="<%= esc(voucher.getName()) %>" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold" for="discountType">Loại giảm giá</label>
                            <select class="form-select" id="discountType" name="discountType">
                                <option value="PERCENT" <%= "PERCENT".equalsIgnoreCase(discountType) ? "selected" : "" %>>Giảm theo phần trăm</option>
                                <option value="FIXED" <%= "FIXED".equalsIgnoreCase(discountType) ? "selected" : "" %>>Giảm số tiền cố định</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold" for="discountValue">Giá trị giảm</label>
                            <%-- Business Rule BR-33: Giá trị giảm phải lớn hơn 0; servlet kiểm tra thêm giới hạn PERCENT <= 100. --%>
                            <input class="form-control" id="discountValue" name="discountValue" type="number"
                                   step="0.01" min="0.01" value="<%= decimalValue(voucher.getDiscountValue()) %>" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold" for="minOrder">Đơn tối thiểu</label>
                            <%-- Business Rule BR-34: Đơn tối thiểu không được âm. --%>
                            <input class="form-control" id="minOrder" name="minOrder" type="number"
                                   step="0.01" min="0" value="<%= voucher.getMinOrder() == null ? "0" : decimalValue(voucher.getMinOrder()) %>">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold" for="quantity">Số lượng</label>
                            <%-- Business Rule BR-34/BR-37: Quantity phải dương và khi edit không được thấp hơn lượt đã dùng. --%>
                            <input class="form-control" id="quantity" name="quantity" type="number"
                                   min="1" value="<%= voucher.getQuantity() > 0 ? voucher.getQuantity() : 1 %>" required>
                        </div>
                        <% if (edit) { %>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold" for="usedDisplay">Lượt đã dùng</label>
                            <input class="form-control" id="usedDisplay" type="text"
                                   value="<%= Math.max(voucher.getUsed(), 0) %> / <%= Math.max(voucher.getQuantity(), 0) %>" readonly>
                        </div>
                        <% } %>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold" for="status">Trạng thái</label>
                            <%-- Business Rule BR-38: Voucher status chỉ nằm trong ACTIVE hoặc DISABLED. --%>
                            <select class="form-select" id="status" name="status">
                                <option value="ACTIVE" <%= "ACTIVE".equalsIgnoreCase(status) ? "selected" : "" %>>Đang hoạt động</option>
                                <option value="DISABLED" <%= "DISABLED".equalsIgnoreCase(status) ? "selected" : "" %>>Tạm tắt</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold" for="startDate">Ngày bắt đầu</label>
                            <%-- Business Rule BR-35: Servlet chặn start date sau end date khi lưu. --%>
                            <input class="form-control" id="startDate" name="startDate" type="datetime-local"
                                   value="<%= dateTimeValue(voucher.getStartDate()) %>" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold" for="endDate">Ngày kết thúc</label>
                            <input class="form-control" id="endDate" name="endDate" type="datetime-local"
                                   value="<%= dateTimeValue(voucher.getEndDate()) %>" required>
                        </div>
                    </div>

                    <div class="d-flex justify-content-end gap-2 mt-4">
                        <a class="btn btn-light" href="<%= ctx %>/owner/vouchers">H&#7911;y</a>
                        <button class="btn btn-success" type="submit">
                            <i class="bi bi-save me-2"></i>Lưu mã giảm giá
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

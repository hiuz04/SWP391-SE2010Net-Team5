<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.PaymentView" %>
<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
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
%>

<%
    String ctx = request.getContextPath();
    PaymentView payment = (PaymentView) request.getAttribute("payment");
    if (payment == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay giao dich.");
        return;
    }
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Nguoi dung";
    boolean success = "SUCCESS".equals(payment.getStatus());
    boolean failed = "FAILED".equals(payment.getStatus());
    boolean checkoutPayment = "CHECKOUT".equals(payment.getPaymentType());
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>K&#7871;t qu&#7843; thanh to&#225;n | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(currentName) %>" data-active="Thanh to&#225;n"></div>

<main class="py-5">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card soft-card p-4 p-md-5">
                    <div class="text-center">
                        <% if (success) { %>
                        <i class="bi bi-check-circle-fill text-success display-1"></i>
                        <h1 class="fw-bold mt-3">Thanh to&#225;n th&#224;nh c&#244;ng</h1>
                        <p class="text-muted"><%= checkoutPayment ? "Hóa đơn checkout đã được thanh toán. Booking đã hoàn tất." : "Thanh toán thành công. Booking đã được xác nhận." %></p>
                        <% } else if (failed) { %>
                        <i class="bi bi-x-circle-fill text-danger display-1"></i>
                        <h1 class="fw-bold mt-3">Thanh to&#225;n th&#7845;t b&#7841;i</h1>
                        <p class="text-muted"><%= checkoutPayment ? "Thanh toán thất bại. Hóa đơn vẫn chờ thanh toán và bạn có thể thử lại." : "Thanh toán thất bại. Bạn có thể thử lại nếu booking còn thời gian giữ chỗ." %></p>
                        <% } else { %>
                        <i class="bi bi-hourglass-split text-warning display-1"></i>
                        <h1 class="fw-bold mt-3">Giao d&#7883;ch &#273;ang x&#7917; l&#253;</h1>
                        <% } %>
                    </div>

                    <div class="border rounded p-3 p-md-4 mt-4">
                        <div class="row g-3">
                            <div class="col-md-6"><span class="text-muted d-block small">M&#227; giao d&#7883;ch</span><strong><%= esc(payment.getTransactionRef()) %></strong></div>
                            <div class="col-md-6"><span class="text-muted d-block small">Booking</span><strong><%= esc(payment.getBookingCode()) %></strong></div>
                            <div class="col-md-6"><span class="text-muted d-block small">C&#417; s&#7903; / s&#226;n</span><strong><%= esc(payment.getFacilityName()) %> - <%= esc(payment.getFieldName()) %></strong></div>
                            <div class="col-md-6"><span class="text-muted d-block small">Th&#7901;i gian</span><strong><%= dateTime(payment.getStartTime()) %> - <%= dateTime(payment.getEndTime()) %></strong></div>
                            <div class="col-md-6"><span class="text-muted d-block small">S&#7889; ti&#7873;n</span><strong><%= money(payment.getAmount()) %></strong></div>
                            <div class="col-md-6"><span class="text-muted d-block small">Ph&#432;&#417;ng th&#7913;c</span><strong><%= esc(payment.getPaymentMethodName()) %></strong></div>
                            <div class="col-md-6"><span class="text-muted d-block small">Lo&#7841;i thanh to&#225;n</span><strong><%= esc(payment.getPaymentType()) %></strong></div>
                            <div class="col-md-6"><span class="text-muted d-block small">Th&#7901;i &#273;i&#7875;m</span><strong><%= dateTime(payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt()) %></strong></div>
                        </div>
                    </div>

                    <div class="d-flex flex-wrap gap-2 justify-content-center mt-4">
                        <% if (payment.isRetryAllowed()) { %>
                        <% if (checkoutPayment && payment.getInvoiceId() != null) { %>
                        <a class="btn btn-danger" href="<%= ctx %>/payment?action=method&type=checkout&invoiceId=<%= payment.getInvoiceId() %>">Th&#7917; thanh to&#225;n l&#7841;i</a>
                        <% } else { %>
                        <a class="btn btn-danger" href="<%= ctx %>/payment?action=method&bookingId=<%= payment.getBookingId() %>">Th&#7917; thanh to&#225;n l&#7841;i</a>
                        <% } %>
                        <% } %>
                        <% if (checkoutPayment && payment.getInvoiceId() != null) { %>
                        <a class="btn btn-sf-primary" href="<%= ctx %>/customer/checkout-invoice?id=<%= payment.getInvoiceId() %>">Xem h&#243;a &#273;&#417;n</a>
                        <% } else { %>
                        <a class="btn btn-sf-primary" href="<%= ctx %>/booking?action=detail&id=<%= payment.getBookingId() %>">Xem booking</a>
                        <% } %>
                        <a class="btn btn-outline-success" href="<%= ctx %>/booking?action=history">L&#7883;ch s&#7917; booking</a>
                        <a class="btn btn-outline-secondary" href="<%= ctx %>/">V&#7873; trang ch&#7911;</a>
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

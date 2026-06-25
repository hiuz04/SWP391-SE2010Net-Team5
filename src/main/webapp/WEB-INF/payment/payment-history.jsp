<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.PaymentView" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.ArrayList" %>
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

    private String statusLabel(String status) {
        if ("SUCCESS".equals(status)) return "Th&#224;nh c&#244;ng";
        if ("FAILED".equals(status)) return "Th&#7845;t b&#7841;i";
        if ("PENDING".equals(status)) return "Ch&#7901; x&#7917; l&#253;";
        return esc(status);
    }

    private String statusClass(String status) {
        if ("SUCCESS".equals(status)) return "badge-soft-success";
        if ("FAILED".equals(status)) return "badge-soft-danger";
        return "badge-soft-warning";
    }
%>

<%
    String ctx = request.getContextPath();
    List<PaymentView> payments = (List<PaymentView>) request.getAttribute("payments");
    if (payments == null) payments = new ArrayList<>();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Nguoi dung";
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>L&#7883;ch s&#7917; giao d&#7883;ch | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(currentName) %>" data-active="L&#7883;ch s&#7917; giao d&#7883;ch"></div>

<main class="py-5">
    <div class="container">
        <div class="d-flex justify-content-between align-items-end mb-4">
            <div>
                <h1 class="section-title">L&#7883;ch s&#7917; giao d&#7883;ch</h1>
                <p class="text-muted mb-0">C&#225;c giao d&#7883;ch thanh to&#225;n c&#7911;a t&#224;i kho&#7843;n hi&#7879;n t&#7841;i.</p>
            </div>
            <a class="btn btn-outline-success" href="<%= ctx %>/booking?action=history">L&#7883;ch s&#7917; booking</a>
        </div>

        <% if (payments.isEmpty()) { %>
        <div class="card soft-card p-5 text-center">
            <i class="bi bi-receipt display-4 text-muted"></i>
            <h5 class="mt-3">Ch&#432;a c&#243; giao d&#7883;ch n&#224;o</h5>
            <p class="text-muted mb-0">Giao d&#7883;ch s&#7869; xu&#7845;t hi&#7879;n sau khi b&#7841;n th&#7921;c hi&#7879;n thanh to&#225;n booking.</p>
        </div>
        <% } else { %>
        <div class="card table-card border-0 shadow-sm">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>M&#227; giao d&#7883;ch</th>
                        <th>Booking</th>
                        <th>C&#417; s&#7903; / s&#226;n</th>
                        <th>S&#7889; ti&#7873;n</th>
                        <th>Lo&#7841;i</th>
                        <th>Ph&#432;&#417;ng th&#7913;c</th>
                        <th>Tr&#7841;ng th&#225;i</th>
                        <th>Th&#7901;i gian</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (PaymentView payment : payments) { %>
                    <tr>
                        <td><strong><%= esc(payment.getTransactionRef()) %></strong></td>
                        <td><%= esc(payment.getBookingCode()) %></td>
                        <td>
                            <div class="fw-semibold"><%= esc(payment.getFacilityName()) %></div>
                            <div class="text-muted small"><%= esc(payment.getFieldName()) %></div>
                        </td>
                        <td><%= money(payment.getAmount()) %></td>
                        <td><%= esc(payment.getPaymentType()) %></td>
                        <td><%= esc(payment.getPaymentMethodName()) %></td>
                        <td><span class="badge <%= statusClass(payment.getStatus()) %>"><%= statusLabel(payment.getStatus()) %></span></td>
                        <td><%= dateTime(payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt()) %></td>
                        <td>
                            <a class="btn btn-sm btn-outline-success" href="<%= ctx %>/booking?action=detail&id=<%= payment.getBookingId() %>">Chi ti&#7871;t</a>
                        </td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </div>
        <% } %>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

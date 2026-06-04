<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.BookingView" %>
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
        if (status == null) return "Kh&#244;ng r&#245;";
        switch (status) {
            case "HOLD":
                return "Ch&#7901; thanh to&#225;n";
            case "CONFIRMED":
                return "&#272;&#227; x&#225;c nh&#7853;n";
            case "CHECKED_IN":
                return "&#272;&#227; check-in";
            case "COMPLETED":
                return "Ho&#224;n t&#7845;t";
            case "CANCELLED":
                return "&#272;&#227; h&#7911;y";
            case "EXPIRED":
                return "H&#7871;t h&#7841;n";
            case "REJECTED":
                return "T&#7915; ch&#7889;i";
            default:
                return esc(status);
        }
    }

    private String statusBadgeClass(String status) {
        if (status == null) return "badge-soft-secondary";
        switch (status) {
            case "CONFIRMED":
            case "COMPLETED":
            case "CHECKED_IN":
                return "badge-soft-success";
            case "HOLD":
                return "badge-soft-warning";
            case "CANCELLED":
            case "EXPIRED":
            case "REJECTED":
                return "badge-soft-danger";
            default:
                return "badge-soft-info";
        }
    }

    private String paymentLabel(String status) {
        if (status == null || status.isBlank()) return "Ch&#432;a thanh to&#225;n";
        switch (status) {
            case "SUCCESS":
                return "&#272;&#227; thanh to&#225;n";
            case "PENDING":
                return "Ch&#7901; thanh to&#225;n";
            case "FAILED":
                return "Thanh to&#225;n l&#7895;i";
            case "REFUNDED":
                return "&#272;&#227; ho&#224;n ti&#7873;n";
            default:
                return esc(status);
        }
    }
%>

<%
    String ctx = request.getContextPath();
    List<BookingView> bookings = (List<BookingView>) request.getAttribute("bookings");
    if (bookings == null) bookings = new ArrayList<>();
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
    <title>L&#7883;ch s&#7917; &#273;&#7863;t s&#226;n | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(currentName) %>" data-active="L&#7883;ch s&#7917; &#273;&#7863;t s&#226;n"></div>

<main class="py-5">
    <div class="container">
        <div class="d-flex justify-content-between align-items-end mb-4">
            <div>
                <h1 class="section-title">L&#7883;ch s&#7917; &#273;&#7863;t s&#226;n</h1>
                <p class="text-muted mb-0">Theo d&#245;i c&#225;c booking &#273;&#227;, &#273;ang v&#224; s&#7855;p di&#7877;n ra.</p>
            </div>
            <a class="btn btn-sf-primary" href="<%= ctx %>/">&#272;&#7863;t s&#226;n m&#7899;i</a>
        </div>

        <% if (bookings.isEmpty()) { %>
        <div class="card soft-card p-5 text-center">
            <h5>Ch&#432;a c&#243; booking n&#224;o</h5>
            <p class="text-muted">B&#7841;n c&#243; th&#7875; t&#236;m s&#226;n v&#224; t&#7841;o booking &#273;&#7847;u ti&#234;n.</p>
            <div><a class="btn btn-sf-primary" href="<%= ctx %>/">V&#7873; trang ch&#7911;</a></div>
        </div>
        <% } else { %>
        <div class="card table-card shadow-sm border-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>M&#227;</th>
                        <th>C&#417; s&#7903; / s&#226;n</th>
                        <th>Th&#7901;i gian</th>
                        <th>Tr&#7841;ng th&#225;i</th>
                        <th>Thanh to&#225;n</th>
                        <th>T&#7893;ng ti&#7873;n</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (BookingView booking : bookings) { %>
                    <tr>
                        <td><strong><%= esc(booking.getBookingCode()) %></strong></td>
                        <td>
                            <div class="fw-semibold"><%= esc(booking.getFacilityName()) %></div>
                            <div class="text-muted small"><%= esc(booking.getFieldName()) %></div>
                        </td>
                        <td><%= dateTime(booking.getStartTime()) %> - <%= dateTime(booking.getEndTime()) %></td>
                        <td><span class="badge <%= statusBadgeClass(booking.getStatus()) %>"><%= statusLabel(booking.getStatus()) %></span></td>
                        <td><%= paymentLabel(booking.getPaymentStatus()) %></td>
                        <td><%= money(booking.getTotalAmount()) %></td>
                        <td>
                            <a class="btn btn-sm btn-outline-success"
                               href="<%= ctx %>/booking?action=detail&id=<%= booking.getBookingId() %>">Chi ti&#7871;t</a>
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

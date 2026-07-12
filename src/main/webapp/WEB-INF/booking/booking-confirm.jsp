<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.Booking" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.BookingView" %>
<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.time.LocalDate" %>
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
    BookingView bookingInfo = (BookingView) request.getAttribute("bookingInfo");
    Booking bookingPreview = (Booking) request.getAttribute("bookingPreview");
    String startTimeValue = (String) request.getAttribute("startTimeValue");
    String endTimeValue = (String) request.getAttribute("endTimeValue");
    String repeatType = (String) request.getAttribute("repeatType");
    Integer recurringCount = (Integer) request.getAttribute("recurringCount");

    if (repeatType == null || repeatType.isBlank()) repeatType = "NONE";
    if (recurringCount == null) recurringCount = 1;

    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Nguoi dung";

    if (bookingInfo == null || bookingPreview == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thieu thong tin xac nhan booking.");
        return;
    }
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>X&#225;c nh&#7853;n &#273;&#7863;t s&#226;n | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(currentName) %>" data-active="T&#236;m s&#226;n"></div>

<main class="py-5">
    <div class="container">
        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card soft-card p-4">
                    <h1 class="section-title mb-3">X&#225;c nh&#7853;n &#273;&#7863;t s&#226;n</h1>
                    <div class="row g-3">
                        <div class="col-md-6">
                            <div class="text-muted small">C&#417; s&#7903;</div>
                            <div class="fw-semibold"><%= esc(bookingInfo.getComplexName()) %></div>
                            <div class="text-muted"><%= esc(bookingInfo.getComplexAddress()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">S&#226;n</div>
                            <div class="fw-semibold"><%= esc(bookingInfo.getFieldName()) %></div>
                            <div class="text-muted"><%= esc(bookingInfo.getFieldTypeName()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Th&#7901;i gian</div>
                            <div class="fw-semibold"><%= dateTime(bookingPreview.getStartTime()) %> - <%= dateTime(bookingPreview.getEndTime()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Kh&#225;ch h&#224;ng</div>
                            <div class="fw-semibold"><%= esc(bookingInfo.getCustomerName()) %></div>
                            <div class="text-muted"><%= esc(bookingInfo.getCustomerPhone()) %> <%= esc(bookingInfo.getCustomerEmail()) %></div>
                        </div>
                    </div>
                </div>
            </div>

            <aside class="col-lg-4">
                <div class="card soft-card p-4 sidebar-card">
                    <h5 class="mb-3">Chi ph&#237;</h5>
                    <div class="d-flex justify-content-between mb-2">
                        <span>Gi&#225; g&#7889;c</span>
                        <strong><%= money(bookingPreview.getOriginalPrice()) %></strong>
                    </div>
                    <div class="d-flex justify-content-between mb-2">
                        <span><%= "MONTHLY".equals(repeatType) ? "Thanh to&#225;n to&#224;n b&#7897; (100%)" : "Ti&#7873;n c&#7885;c c&#7847;n thanh to&#225;n (30%)" %></span>
                        <strong class="text-primary"><%= money(bookingPreview.getDepositAmount()) %></strong>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between fs-5 mb-3">
                        <span>T&#7893;ng ti&#7873;n s&#226;n</span>
                        <strong class="text-success"><%= money(bookingPreview.getTotalAmount()) %></strong>
                    </div>
                    <p class="text-muted small">Booking s&#7869; &#273;&#432;&#7907;c gi&#7919; trong 15 ph&#250;t, &#273;&#7871;n <%= dateTime(bookingPreview.getHoldExpiresAt()) %>.</p>
                    <div class="alert alert-info py-2 small">
                        <strong>Lo&#7841;i thu&#234;:</strong>
                        <%= "MONTHLY".equals(repeatType) ? "Thu&#234; theo th&#225;ng" : "Thu&#234; &#273;&#417;n l&#7867;" %>.
                        H&#7879; th&#7889;ng s&#7869; t&#7841;o <%= recurringCount %> booking trong th&#225;ng n&#224;y v&#224; th&#225;ng sau n&#7871;u c&#225;c khung gi&#7901; &#273;&#7873;u c&#242;n tr&#7889;ng.
                    </div>

                    <form method="post" action="<%= ctx %>/booking">
                        <input type="hidden" name="action" value="confirm">
                        <input type="hidden" name="fieldId" value="<%= bookingPreview.getFieldId() %>">
                        <input type="hidden" name="startTime" value="<%= esc(startTimeValue) %>">
                        <input type="hidden" name="endTime" value="<%= esc(endTimeValue) %>">
                        <input type="hidden" name="repeatType" value="<%= esc(repeatType) %>">
                        <button type="submit" class="btn btn-sf-primary w-100">T&#7841;o booking</button>
                    </form>
                    <a href="<%= ctx %>/booking?action=create&complexId=<%= bookingPreview.getComplexId() %>&date=<%= bookingPreview.getStartTime().toLocalDate() %>"
                       class="btn btn-outline-secondary w-100 mt-2">Quay l&#7841;i ch&#7885;n gi&#7901;</a>
                </div>
            </aside>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

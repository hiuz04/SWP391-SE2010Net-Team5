<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.PaymentMethod" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.BookingView" %>
<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
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

    private String timeOnly(LocalDateTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String dayOfWeek(LocalDateTime value) {
        if (value == null) return "";
        switch (value.getDayOfWeek().getValue()) {
            case 1:
                return "Th&#7913; 2";
            case 2:
                return "Th&#7913; 3";
            case 3:
                return "Th&#7913; 4";
            case 4:
                return "Th&#7913; 5";
            case 5:
                return "Th&#7913; 6";
            case 6:
                return "Th&#7913; 7";
            default:
                return "Ch&#7911; nh&#7853;t";
        }
    }

    private boolean isMonthlyBooking(BookingView booking) {
        return booking != null && "MONTHLY".equals(booking.getRepeatType());
    }

    private String bookingTimeLabel(BookingView booking) {
        if (booking == null) return "";
        if (isMonthlyBooking(booking)) {
            return dayOfWeek(booking.getStartTime()) + ", "
                    + timeOnly(booking.getStartTime()) + " - " + timeOnly(booking.getEndTime());
        }
        return dateTime(booking.getStartTime()) + " - " + timeOnly(booking.getEndTime());
    }
%>

<%
    String ctx = request.getContextPath();
    BookingView booking = (BookingView) request.getAttribute("booking");
    if (booking == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay booking.");
        return;
    }
    List<PaymentMethod> paymentMethods = (List<PaymentMethod>) request.getAttribute("paymentMethods");
    if (paymentMethods == null) paymentMethods = new ArrayList<>();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Nguoi dung";
    String error = (String) request.getAttribute("error");
    boolean holdValid = "HOLD".equals(booking.getStatus())
            && booking.getHoldExpiresAt() != null
            && booking.getHoldExpiresAt().isAfter(LocalDateTime.now());
    boolean monthly = isMonthlyBooking(booking);
    Integer vnpayMethodId = null;
    for (PaymentMethod method : paymentMethods) {
        if ("VNPAY".equalsIgnoreCase(method.getMethodCode())) {
            vnpayMethodId = method.getPaymentMethodId();
            break;
        }
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
    <title>Thanh to&#225;n | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(currentName) %>" data-active="Thanh to&#225;n"></div>

<main class="py-5">
    <div class="container">
        <div class="mb-3">
            <a class="btn btn-outline-secondary" href="<%= ctx %>/booking?action=detail&id=<%= booking.getBookingId() %>">
                <i class="bi bi-arrow-left"></i> Quay l&#7841;i booking
            </a>
        </div>

        <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger"><%= esc(error) %></div>
        <% } %>
        <% if (!holdValid) { %>
        <div class="alert alert-warning">
            Booking kh&#244;ng c&#242;n trong th&#7901;i gian gi&#7919; ch&#7895;. Kh&#244;ng th&#7875; t&#7841;o giao d&#7883;ch m&#7899;i.
        </div>
        <% } %>

        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card soft-card p-4">
                    <h1 class="section-title">Ch&#7885;n ph&#432;&#417;ng th&#7913;c thanh to&#225;n</h1>
                    <p class="text-muted">Booking <strong><%= esc(booking.getBookingCode()) %></strong></p>

                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <div class="text-muted small">C&#417; s&#7903;</div>
                            <div class="fw-semibold"><%= esc(booking.getComplexName()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">S&#226;n / lo&#7841;i s&#226;n</div>
                            <div class="fw-semibold"><%= esc(booking.getFieldName()) %> - <%= esc(booking.getFieldTypeName()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Th&#7901;i gian</div>
                            <div class="fw-semibold"><%= bookingTimeLabel(booking) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Lo&#7841;i booking</div>
                            <div class="fw-semibold">
                                <%= monthly ? "Thu&#234; theo th&#225;ng" : "Thu&#234; &#273;&#417;n l&#7867;" %>
                                <% if (monthly && booking.getRecurringCount() != null) { %>
                                <span class="text-muted">(<%= booking.getRecurringCount() %> bu&#7893;i)</span>
                                <% } %>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Gi&#7919; ch&#7895; &#273;&#7871;n</div>
                            <div class="fw-semibold text-danger"><%= dateTime(booking.getHoldExpiresAt()) %></div>
                        </div>
                    </div>

                    <% if (paymentMethods.isEmpty()) { %>
                    <div class="alert alert-info mb-0">Hi&#7879;n kh&#244;ng c&#243; ph&#432;&#417;ng th&#7913;c thanh to&#225;n &#273;ang ho&#7841;t &#273;&#7897;ng.</div>
                    <% } else { %>
                    <form method="post" action="<%= ctx %>/payment">
                        <input type="hidden" name="action" value="pay">
                        <input type="hidden" name="bookingId" value="<%= booking.getBookingId() %>">
                        <h5 class="mb-3">Ph&#432;&#417;ng th&#7913;c</h5>
                        <div class="vstack gap-2">
                            <% for (int i = 0; i < paymentMethods.size(); i++) {
                                PaymentMethod method = paymentMethods.get(i); %>
                            <label class="border rounded p-3 d-flex gap-3 align-items-center">
                                <input class="form-check-input m-0" type="radio" name="paymentMethodId"
                                       value="<%= method.getPaymentMethodId() %>" <%= i == 0 ? "checked" : "" %>>
                                <i class="bi bi-credit-card fs-4 text-success"></i>
                                <span>
                                    <strong><%= esc(method.getMethodName()) %></strong>
                                    <span class="d-block text-muted small"><%= esc(method.getMethodCode()) %></span>
                                </span>
                            </label>
                            <% } %>
                        </div>

                        <div class="d-flex flex-wrap gap-2 mt-4">
                            <button class="btn btn-outline-primary" type="submit" name="paymentMode" value="VNPAY"
                                    id="vnpayButton"
                                    data-vnpay-method-id="<%= vnpayMethodId == null ? "" : vnpayMethodId %>"
                                    <%= holdValid && vnpayMethodId != null ? "" : "disabled" %>>
                                <i class="bi bi-bank"></i> Thanh to&#225;n qua VNPay
                            </button>
                            <button class="btn btn-sf-primary" type="submit" name="simulateStatus" value="SUCCESS"
                                    <%= holdValid ? "" : "disabled" %>>
                                <i class="bi bi-shield-check"></i> Thanh to&#225;n th&#224;nh c&#244;ng
                            </button>
                            <button class="btn btn-outline-danger" type="submit" name="simulateStatus" value="FAILED"
                                    <%= holdValid ? "" : "disabled" %>>
                                Gi&#7843; l&#7853;p th&#7845;t b&#7841;i
                            </button>
                        </div>
                        <div class="text-muted small mt-2">Ch&#7871; &#273;&#7897; m&#244; ph&#7887;ng d&#224;nh cho demo Inter 2.</div>
                        <div class="text-muted small mt-1">VNPay Sandbox y&#234;u c&#7847;u return/ipn URL public HTTPS; khi ch&#7841;y local c&#243; th&#7875; d&#249;ng ngrok.</div>
                    </form>
                    <% } %>
                </div>
            </div>

            <aside class="col-lg-4">
                <div class="card soft-card p-4 sidebar-card">
                    <h5>T&#243;m t&#7855;t thanh to&#225;n</h5>
                    <div class="d-flex justify-content-between mt-3">
                        <span>T&#7893;ng ti&#7873;n</span>
                        <strong><%= money(booking.getTotalAmount()) %></strong>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between fs-5">
                        <span><%= monthly ? "Thanh to&#225;n to&#224;n b&#7897;" : "Ti&#7873;n c&#7885;c" %></span>
                        <strong class="text-success"><%= money(booking.getDepositAmount()) %></strong>
                    </div>
                    <p class="text-muted small mt-3 mb-0">S&#7889; ti&#7873;n &#273;&#432;&#7907;c l&#7845;y tr&#7921;c ti&#7871;p t&#7915; booking trong c&#417; s&#7903; d&#7919; li&#7879;u.</p>
                </div>
            </aside>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
    const vnpayButton = document.getElementById('vnpayButton');
    if (vnpayButton) {
        vnpayButton.addEventListener('click', () => {
            const methodId = vnpayButton.dataset.vnpayMethodId;
            const radio = document.querySelector('input[name="paymentMethodId"][value="' + methodId + '"]');
            if (radio) {
                radio.checked = true;
            }
        });
    }
</script>
</body>
</html>

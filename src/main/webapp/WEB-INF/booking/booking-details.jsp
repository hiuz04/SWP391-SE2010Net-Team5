<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.BookingView" %>
<%@ page import="com.swp.model.dto.SkippedBookingSlot" %>
<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.LocalTime" %>
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

    private String dateOnly(LocalDateTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String dateOnly(LocalDate value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private BigDecimal vipDiscount(BigDecimal originalPrice, BigDecimal voucherDiscount, BigDecimal finalAmount) {
        BigDecimal original = originalPrice == null ? BigDecimal.ZERO : originalPrice;
        BigDecimal voucher = voucherDiscount == null ? BigDecimal.ZERO : voucherDiscount;
        BigDecimal payable = finalAmount == null ? BigDecimal.ZERO : finalAmount;
        BigDecimal discount = original.subtract(voucher).subtract(payable);
        return discount.compareTo(BigDecimal.ZERO) > 0 ? discount : BigDecimal.ZERO;
    }

    private String timeOnly(LocalDateTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String timeOnly(LocalTime value) {
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

    private boolean monthly(BookingView booking) {
        return booking != null && "MONTHLY".equals(booking.getRepeatType());
    }

    private String bookingTimeLabel(BookingView booking) {
        if (booking == null) return "";
        if (monthly(booking)) {
            String countText = booking.getRecurringCount() == null
                    ? ""
                    : " (" + booking.getRecurringCount() + " bu&#7893;i)";
            return dayOfWeek(booking.getStartTime()) + ", "
                    + timeOnly(booking.getStartTime()) + " - " + timeOnly(booking.getEndTime())
                    + countText;
        }
        return dateTime(booking.getStartTime()) + " - " + timeOnly(booking.getEndTime());
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
            case "PENDING_CHECKOUT_PAYMENT":
                return "Ch&#7901; thanh to&#225;n h&#243;a &#273;&#417;n";
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
            case "PENDING_CHECKOUT_PAYMENT":
                return "badge-soft-warning";
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
    BookingView booking = (BookingView) request.getAttribute("booking");
    List<BookingView> recurringBookings = (List<BookingView>) request.getAttribute("recurringBookings");
    List<SkippedBookingSlot> recurringSkippedSlots = (List<SkippedBookingSlot>) request.getAttribute("recurringSkippedSlots");
    String recurringSuccessMessage = (String) request.getAttribute("recurringSuccessMessage");
    if (booking == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay booking.");
        return;
    }
    if (recurringBookings == null) recurringBookings = new ArrayList<>();
    if (recurringSkippedSlots == null) recurringSkippedSlots = new ArrayList<>();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Nguoi dung";
    boolean created = "created".equals(request.getAttribute("success"));
    boolean cancelled = "cancelled".equals(request.getAttribute("success"));
    String error = request.getParameter("error");
    String qrText = booking.getQrCode() != null && !booking.getQrCode().isBlank()
            ? booking.getQrCode()
            : booking.getBookingCode();
    boolean paymentSuccess = "SUCCESS".equals(booking.getPaymentStatus());
    boolean holdActive = "HOLD".equals(booking.getStatus())
            && booking.getHoldExpiresAt() != null
            && booking.getHoldExpiresAt().isAfter(LocalDateTime.now());
    BigDecimal discountAmount = booking.getDiscountAmount() == null ? BigDecimal.ZERO : booking.getDiscountAmount();
    BigDecimal finalAmount = booking.getFinalAmount() != null ? booking.getFinalAmount() : booking.getTotalAmount();
    BigDecimal vipDiscountAmount = vipDiscount(booking.getOriginalPrice(), discountAmount, finalAmount);
    boolean hasVoucher = booking.getVoucherCode() != null && !booking.getVoucherCode().isBlank();
    boolean hasDiscount = discountAmount.compareTo(BigDecimal.ZERO) > 0;
    boolean hasVipDiscount = vipDiscountAmount.compareTo(BigDecimal.ZERO) > 0;
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Chi ti&#7871;t booking | Sport Field Booking</title>
    <style>
        .qr-text {
            border: 1px dashed #16a34a;
            border-radius: 8px;
            padding: 18px;
            font-weight: 700;
            letter-spacing: .04em;
            word-break: break-word;
            background: #f8fff8;
        }
    </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(currentName) %>" data-active="L&#7883;ch s&#7917; &#273;&#7863;t s&#226;n"></div>

<main class="py-5">
        <div class="container">
        <% if (created) { %>
        <div class="alert alert-success">
            <%= recurringSuccessMessage != null && !recurringSuccessMessage.isBlank()
                    ? esc(recurringSuccessMessage)
                    : "&#272;&#227; t&#7841;o booking v&#224; gi&#7919; s&#226;n th&#224;nh c&#244;ng. Vui l&#242;ng ho&#224;n t&#7845;t thanh to&#225;n trong th&#7901;i gian gi&#7919; ch&#7895;." %>
            <% if (!recurringSkippedSlots.isEmpty()) { %>
            <div class="mt-2">
                <% for (SkippedBookingSlot skipped : recurringSkippedSlots) { %>
                <div>
                    <%= dateOnly(skipped.getDate()) %>, <%= timeOnly(skipped.getStartTime()) %>-<%= timeOnly(skipped.getEndTime()) %>
                    - <%= esc(skipped.getReason()) %>.
                </div>
                <% } %>
            </div>
            <% } %>
        </div>
        <% } %>
        <% if (cancelled) { %>
        <div class="alert alert-success">&#272;&#227; h&#7911;y booking th&#224;nh c&#244;ng.</div>
        <% } %>
        <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger"><%= esc(error) %></div>
        <% } %>

        <div class="mb-3">
            <a class="btn btn-outline-secondary" href="<%= ctx %>/booking?action=history">
                <i class="bi bi-arrow-left"></i> Quay l&#7841;i l&#7883;ch s&#7917;
            </a>
        </div>

        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card soft-card p-4">
                    <div class="d-flex justify-content-between gap-3 flex-wrap">
                        <div>
                            <h1 class="section-title">Booking <%= esc(booking.getBookingCode()) %></h1>
                            <p class="text-muted mb-0"><%= esc(booking.getComplexName()) %> - <%= esc(booking.getFieldName()) %></p>
                        </div>
                        <span class="badge <%= statusBadgeClass(booking.getStatus()) %> align-self-start"><%= statusLabel(booking.getStatus()) %></span>
                    </div>

                    <div class="row g-3 mt-3">
                        <div class="col-md-6">
                            <div class="text-muted small"><%= monthly(booking) ? "L&#7883;ch c&#7889; &#273;&#7883;nh" : "Gi&#7901; b&#7855;t &#273;&#7847;u / kick-off" %></div>
                            <div class="fw-semibold"><%= bookingTimeLabel(booking) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Lo&#7841;i s&#226;n</div>
                            <div class="fw-semibold"><%= esc(booking.getFieldTypeName()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">&#272;&#7883;a ch&#7881;</div>
                            <div class="fw-semibold"><%= esc(booking.getComplexAddress()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Hotline</div>
                            <div class="fw-semibold"><%= esc(booking.getComplexHotline()) %></div>
                        </div>
                    </div>

                    <div class="timeline mt-4">
                        <div class="timeline-item">
                            <strong>T&#7841;o &#273;&#417;n &#273;&#7863;t s&#226;n</strong>
                            <p class="text-muted mb-0"><%= dateTime(booking.getCreatedAt()) %></p>
                        </div>
                        <% if ("HOLD".equals(booking.getStatus())) { %>
                        <div class="timeline-item">
                            <strong>Ch&#7901; thanh to&#225;n</strong>
                            <p class="text-muted mb-0">Gi&#7919; ch&#7895; &#273;&#7871;n <%= dateTime(booking.getHoldExpiresAt()) %>.</p>
                        </div>
                        <% } else { %>
                        <div class="timeline-item">
                            <strong>Thanh to&#225;n</strong>
                            <p class="text-muted mb-0"><%= paymentLabel(booking.getPaymentStatus()) %><% if (booking.getPaidAt() != null) { %> - <%= dateTime(booking.getPaidAt()) %><% } %></p>
                        </div>
                        <% } %>
                        <% if (booking.getCancelledAt() != null) { %>
                        <div class="timeline-item">
                            <strong>&#272;&#227; h&#7911;y</strong>
                            <p class="text-muted mb-0"><%= dateTime(booking.getCancelledAt()) %> <%= esc(booking.getCancellationReason()) %></p>
                        </div>
                        <% } %>
                    </div>
                </div>

                <% if (monthly(booking) && !recurringBookings.isEmpty()) { %>
                <div class="card soft-card p-4 mt-4">
                    <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
                        <div>
                            <h5 class="mb-1">L&#7883;ch &#273;&#7863;t s&#226;n theo th&#225;ng</h5>
                            <p class="text-muted mb-0">To&#224;n b&#7897; booking trong c&#249;ng nh&#243;m, s&#7855;p x&#7871;p theo ng&#224;y t&#259;ng d&#7847;n.</p>
                        </div>
                        <span class="badge bg-success-subtle text-success border border-success-subtle">
                            <%= recurringBookings.size() %> bu&#7893;i
                        </span>
                    </div>
                    <div class="table-responsive">
                        <table class="table table-sm align-middle mb-0">
                            <thead class="table-light">
                            <tr>
                                <th>STT</th>
                                <th>M&#227; booking</th>
                                <th>Ng&#224;y &#273;&#7863;t</th>
                                <th>Th&#7901;i gian</th>
                                <th>Gi&#225; ti&#7873;n</th>
                                <th>Tr&#7841;ng th&#225;i booking</th>
                                <th>Tr&#7841;ng th&#225;i thanh to&#225;n</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (int i = 0; i < recurringBookings.size(); i++) {
                                BookingView item = recurringBookings.get(i);
                                BigDecimal itemAmount = item.getFinalAmount() != null ? item.getFinalAmount() : item.getTotalAmount();
                            %>
                            <tr>
                                <td><%= i + 1 %></td>
                                <td><strong><%= esc(item.getBookingCode()) %></strong></td>
                                <td><%= dateOnly(item.getStartTime()) %></td>
                                <td><%= timeOnly(item.getStartTime()) %>-<%= timeOnly(item.getEndTime()) %></td>
                                <td><%= money(itemAmount) %></td>
                                <td><span class="badge <%= statusBadgeClass(item.getStatus()) %>"><%= statusLabel(item.getStatus()) %></span></td>
                                <td><%= paymentLabel(item.getPaymentStatus()) %></td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
                <% } %>
            </div>

            <aside class="col-lg-4">
                <div class="card soft-card p-4 text-center sidebar-card">
                    <h5>M&#227; QR check-in</h5>
                    <div class="qr-text my-4"><%= esc(qrText) %></div>
                    <p class="text-muted">&#272;&#432;a m&#227; n&#224;y cho nh&#226;n vi&#234;n s&#226;n &#273;&#7875; check-in.</p>
                    <hr>
                    <div class="d-flex justify-content-between mb-2">
                        <span>Gi&#225; g&#7889;c</span>
                        <strong><%= money(booking.getOriginalPrice()) %></strong>
                    </div>
                    <% if (hasVoucher) { %>
                    <div class="d-flex justify-content-between mb-2">
                        <span>Mã giảm giá</span>
                        <strong><%= esc(booking.getVoucherCode()) %></strong>
                    </div>
                    <% } %>
                    <% if (hasDiscount) { %>
                    <div class="d-flex justify-content-between mb-2 text-success">
                        <span>Gi&#7843;m voucher</span>
                        <strong>-<%= money(discountAmount) %></strong>
                    </div>
                    <% } %>
                    <% if (hasVipDiscount) { %>
                    <div class="d-flex justify-content-between mb-2 text-success">
                        <span>&#431;u &#273;&#227;i h&#7897;i vi&#234;n (5%)</span>
                        <strong>-<%= money(vipDiscountAmount) %></strong>
                    </div>
                    <% } %>
                    <div class="d-flex justify-content-between mb-2">
                        <span>T&#7893;ng sau gi&#7843;m</span>
                        <strong><%= money(finalAmount) %></strong>
                    </div>
                    <div class="d-flex justify-content-between mb-2">
                        <span>S&#7889; ti&#7873;n c&#7885;c ph&#7843;i &#273;&#243;ng</span>
                        <strong><%= money(booking.getDepositAmount()) %></strong>
                    </div>
                    <div class="d-flex justify-content-between mb-2">
                        <span>&#272;&#227; thanh to&#225;n</span>
                        <strong><%= money(booking.getPaidAmount()) %></strong>
                    </div>
                    <div class="d-flex justify-content-between">
                        <span>Thanh to&#225;n</span>
                        <strong><%= paymentLabel(booking.getPaymentStatus()) %></strong>
                    </div>
                    <% if (booking.getPaymentMethodName() != null) { %>
                    <div class="text-muted small mt-2"><%= esc(booking.getPaymentMethodName()) %></div>
                    <% } %>
                    <% if (paymentSuccess || "CONFIRMED".equals(booking.getStatus())) { %>
                    <div class="alert alert-success mt-3 mb-0">
                        <i class="bi bi-check-circle"></i> &#272;&#227; thanh to&#225;n / booking &#273;&#227; x&#225;c nh&#7853;n.
                    </div>
                    <% } else if (holdActive) { %>
                    <a class="btn btn-sf-primary w-100 mt-3"
                       href="<%= ctx %>/payment?action=method&bookingId=<%= booking.getBookingId() %>">
                        <i class="bi bi-credit-card"></i> Thanh to&#225;n ngay
                    </a>
                    <% } else if ("HOLD".equals(booking.getStatus())) { %>
                    <div class="alert alert-warning mt-3 mb-0">Th&#7901;i gian gi&#7919; ch&#7895; &#273;&#227; h&#7871;t h&#7841;n.</div>
                    <% } %>
                    <hr>
                    <% if (booking.isCanCancel()) { %>
                    <form method="post" action="<%= ctx %>/booking" class="text-start">
                        <input type="hidden" name="action" value="cancel">
                        <input type="hidden" name="id" value="<%= booking.getBookingId() %>">
                        <label class="form-label small text-muted" for="cancelReason">L&#253; do h&#7911;y</label>
                        <input class="form-control mb-2" id="cancelReason" name="reason" maxlength="255"
                               value="Kh&#225;ch h&#224;ng h&#7911;y booking">
                        <button type="submit" class="btn btn-outline-danger w-100">H&#7911;y booking</button>
                    </form>
                    <% } else { %>
                    <button type="button" class="btn btn-outline-secondary w-100" disabled>Kh&#244;ng th&#7875; h&#7911;y booking</button>
                    <div class="text-muted small mt-2"><%= esc(booking.getCancelReasonMessage()) %></div>
                    <% } %>
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

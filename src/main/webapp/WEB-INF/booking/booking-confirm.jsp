<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.Booking" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.BookingSlotPreview" %>
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

    private String dateOnly(LocalDate value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String timeOnly(LocalTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String dayOfWeek(LocalDate value) {
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

    private BigDecimal vipDiscount(BigDecimal originalPrice, BigDecimal voucherDiscount, BigDecimal finalAmount) {
        BigDecimal original = originalPrice == null ? BigDecimal.ZERO : originalPrice;
        BigDecimal voucher = voucherDiscount == null ? BigDecimal.ZERO : voucherDiscount;
        BigDecimal payable = finalAmount == null ? BigDecimal.ZERO : finalAmount;
        BigDecimal discount = original.subtract(voucher).subtract(payable);
        return discount.compareTo(BigDecimal.ZERO) > 0 ? discount : BigDecimal.ZERO;
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
    String voucherCode = (String) request.getAttribute("voucherCode");
    String voucherError = (String) request.getAttribute("voucherError");
    String voucherMessage = (String) request.getAttribute("voucherMessage");
    String creationError = (String) request.getAttribute("creationError");
    List<BookingSlotPreview> slotPreviews = (List<BookingSlotPreview>) request.getAttribute("slotPreviews");
    List<SkippedBookingSlot> creationSkippedSlots = (List<SkippedBookingSlot>) request.getAttribute("creationSkippedSlots");
    Integer totalExpectedSlots = (Integer) request.getAttribute("totalExpectedSlots");
    Integer validSlotCount = (Integer) request.getAttribute("validSlotCount");
    Integer skippedSlotCount = (Integer) request.getAttribute("skippedSlotCount");

    if (repeatType == null || repeatType.isBlank()) repeatType = "NONE";
    if (recurringCount == null) recurringCount = 1;
    if (voucherCode == null) voucherCode = "";
    if (slotPreviews == null) slotPreviews = new ArrayList<>();
    if (creationSkippedSlots == null) creationSkippedSlots = new ArrayList<>();
    if (totalExpectedSlots == null) totalExpectedSlots = slotPreviews.isEmpty() ? recurringCount : slotPreviews.size();
    if (validSlotCount == null) validSlotCount = Math.max(0, recurringCount);
    if (skippedSlotCount == null) skippedSlotCount = Math.max(0, totalExpectedSlots - validSlotCount);

    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Nguoi dung";

    if (bookingInfo == null || bookingPreview == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thieu thong tin xac nhan booking.");
        return;
    }
    BigDecimal discountAmount = bookingPreview.getDiscountAmount() == null ? BigDecimal.ZERO : bookingPreview.getDiscountAmount();
    BigDecimal finalAmount = bookingPreview.getFinalAmount() != null ? bookingPreview.getFinalAmount() : bookingPreview.getTotalAmount();
    BigDecimal vipDiscountAmount = vipDiscount(bookingPreview.getOriginalPrice(), discountAmount, finalAmount);
    boolean hasVoucherPreview = voucherCode != null && !voucherCode.isBlank();
    boolean hasDiscount = discountAmount.compareTo(BigDecimal.ZERO) > 0;
    boolean hasVipDiscount = vipDiscountAmount.compareTo(BigDecimal.ZERO) > 0;
    boolean hasAnyDiscount = hasDiscount || hasVipDiscount;
    boolean monthly = "MONTHLY".equals(repeatType);
    boolean noAvailableMonthlySlot = monthly && validSlotCount == 0;
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

                <% if (monthly) { %>
                <div class="card soft-card p-4 mt-4">
                    <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
                        <div>
                            <h5 class="mb-1">L&#7883;ch &#273;&#7863;t s&#226;n theo th&#225;ng</h5>
                            <p class="text-muted mb-0">C&#225;c bu&#7893;i kh&#7843; d&#7909;ng s&#7869; &#273;&#432;&#7907;c t&#7841;o khi b&#7841;n x&#225;c nh&#7853;n.</p>
                        </div>
                        <span class="badge bg-success-subtle text-success border border-success-subtle">
                            <%= validSlotCount %>/<%= totalExpectedSlots %> kh&#7843; d&#7909;ng
                        </span>
                    </div>

                    <% if (creationError != null && !creationError.isBlank()) { %>
                    <div class="alert alert-warning"><%= esc(creationError) %></div>
                    <% } %>

                    <% if (!creationSkippedSlots.isEmpty()) { %>
                    <div class="alert alert-info">
                        <div class="fw-semibold mb-2">C&#225;c bu&#7893;i v&#7915;a b&#7883; b&#7887; qua khi x&#225;c nh&#7853;n:</div>
                        <% for (SkippedBookingSlot skipped : creationSkippedSlots) { %>
                        <div>
                            <%= dateOnly(skipped.getDate()) %>, <%= timeOnly(skipped.getStartTime()) %>-<%= timeOnly(skipped.getEndTime()) %>
                            - <%= esc(skipped.getReason()) %>
                        </div>
                        <% } %>
                    </div>
                    <% } %>

                    <div class="table-responsive">
                        <table class="table table-sm align-middle mb-0">
                            <thead class="table-light">
                            <tr>
                                <th>STT</th>
                                <th>Ng&#224;y</th>
                                <th>Th&#7913;</th>
                                <th>Gi&#7901; b&#7855;t &#273;&#7847;u</th>
                                <th>Gi&#7901; k&#7871;t th&#250;c</th>
                                <th>Gi&#225; bu&#7893;i</th>
                                <th>Tr&#7841;ng th&#225;i</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (int i = 0; i < slotPreviews.size(); i++) {
                                BookingSlotPreview slot = slotPreviews.get(i);
                                boolean available = slot.isAvailable();
                            %>
                            <tr class="<%= available ? "" : "table-warning" %>">
                                <td><%= i + 1 %></td>
                                <td><%= dateOnly(slot.getDate()) %></td>
                                <td><%= dayOfWeek(slot.getDate()) %></td>
                                <td><%= timeOnly(slot.getStartTime()) %></td>
                                <td><%= timeOnly(slot.getEndTime()) %></td>
                                <td><%= money(available ? slot.getPrice() : BigDecimal.ZERO) %></td>
                                <td>
                                    <% if (available) { %>
                                    <span class="badge bg-success-subtle text-success border border-success-subtle">Kh&#7843; d&#7909;ng</span>
                                    <% } else { %>
                                    <span class="badge bg-warning-subtle text-warning text-dark border border-warning-subtle">
                                        B&#7887; qua - <%= esc(slot.getReason()) %>
                                    </span>
                                    <% } %>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
                <% } %>
            </div>

            <aside class="col-lg-4">
                <div class="card soft-card p-4 sidebar-card">
                    <h5 class="mb-3">Chi ph&#237;</h5>
                    <div class="d-flex justify-content-between mb-2">
                        <span>Gi&#225; g&#7889;c</span>
                        <strong><%= money(bookingPreview.getOriginalPrice()) %></strong>
                    </div>
                    <% if (hasVoucherPreview) { %>
                    <div class="d-flex justify-content-between mb-2">
                        <span>Mã giảm giá</span>
                        <strong><%= esc(voucherCode) %></strong>
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
                    <% if (hasAnyDiscount) { %>
                    <div class="d-flex justify-content-between mb-2">
                        <span>T&#7893;ng sau gi&#7843;m</span>
                        <strong><%= money(finalAmount) %></strong>
                    </div>
                    <% } %>
                    <div class="d-flex justify-content-between mb-2">
                        <span><%= monthly ? "S&#7889; ti&#7873;n c&#7847;n thanh to&#225;n (100%)" : "Ti&#7873;n c&#7885;c c&#7847;n thanh to&#225;n (30%)" %></span>
                        <strong class="text-primary"><%= money(bookingPreview.getDepositAmount()) %></strong>
                    </div>
                    <hr>
                    <% if (monthly) { %>
                    <div class="d-flex justify-content-between mb-2">
                        <span>S&#7889; bu&#7893;i d&#7921; ki&#7871;n</span>
                        <strong><%= totalExpectedSlots %></strong>
                    </div>
                    <div class="d-flex justify-content-between mb-2 text-success">
                        <span>S&#7889; bu&#7893;i kh&#7843; d&#7909;ng</span>
                        <strong><%= validSlotCount %></strong>
                    </div>
                    <div class="d-flex justify-content-between mb-2 text-warning">
                        <span>S&#7889; bu&#7893;i b&#7883; b&#7887; qua</span>
                        <strong><%= skippedSlotCount %></strong>
                    </div>
                    <hr>
                    <% } %>
                    <div class="d-flex justify-content-between fs-5 mb-3">
                        <span>T&#7893;ng ti&#7873;n s&#226;n</span>
                        <strong class="text-success"><%= money(finalAmount) %></strong>
                    </div>
                    <p class="text-muted small">Booking s&#7869; &#273;&#432;&#7907;c gi&#7919; trong 15 ph&#250;t, &#273;&#7871;n <%= dateTime(bookingPreview.getHoldExpiresAt()) %>.</p>
                    <div class="alert alert-info py-2 small">
                        <strong>Lo&#7841;i thu&#234;:</strong>
                        <%= monthly ? "Thu&#234; theo th&#225;ng" : "Thu&#234; &#273;&#417;n l&#7867;" %>.
                        <% if (monthly) { %>
                        H&#7879; th&#7889;ng ch&#7881; t&#7841;o c&#225;c bu&#7893;i kh&#7843; d&#7909;ng trong c&#249;ng th&#225;ng v&#7899;i ng&#224;y &#273;&#7847;u ti&#234;n.
                        <% } else { %>
                        H&#7879; th&#7889;ng s&#7869; t&#7841;o booking cho khung gi&#7901; &#273;&#227; ch&#7885;n.
                        <% } %>
                    </div>

                    <form method="post" action="<%= ctx %>/booking">
                        <input type="hidden" name="action" value="confirm">
                        <input type="hidden" name="fieldId" value="<%= bookingPreview.getFieldId() %>">
                        <input type="hidden" name="startTime" value="<%= esc(startTimeValue) %>">
                        <input type="hidden" name="endTime" value="<%= esc(endTimeValue) %>">
                        <input type="hidden" name="repeatType" value="<%= esc(repeatType) %>">
                        <div class="mb-3">
                            <label class="form-label small fw-semibold" for="voucherCode">Mã giảm giá</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-ticket-perforated"></i></span>
                                <input class="form-control text-uppercase" id="voucherCode" name="voucherCode"
                                       maxlength="50" value="<%= esc(voucherCode) %>" placeholder="SALE20">
                            </div>
                            <% if (voucherError != null && !voucherError.isBlank()) { %>
                            <div class="text-danger small mt-2"><%= esc(voucherError) %></div>
                            <% } else if (voucherMessage != null && !voucherMessage.isBlank()) { %>
                            <div class="text-success small mt-2"><%= esc(voucherMessage) %></div>
                            <% } %>
                        </div>
                        <% if (noAvailableMonthlySlot) { %>
                        <div class="alert alert-warning py-2 small">
                            Kh&#244;ng c&#243; bu&#7893;i n&#224;o kh&#7843; d&#7909;ng trong th&#225;ng &#273;&#227; ch&#7885;n.
                        </div>
                        <% } %>
                        <button type="submit" class="btn btn-sf-primary w-100" <%= noAvailableMonthlySlot ? "disabled" : "" %>>T&#7841;o booking</button>
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

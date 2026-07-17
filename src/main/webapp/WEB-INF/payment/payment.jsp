<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.PaymentMethod" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.BookingView" %>
<%@ page import="com.swp.model.dto.InvoiceView" %>
<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>

<%!
    private String esc(Object value) {
        if (value == null) return "";
        return value.toString().replace("&", "&amp;").replace("<", "&lt;")
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
            case 1: return "Thứ 2";
            case 2: return "Thứ 3";
            case 3: return "Thứ 4";
            case 4: return "Thứ 5";
            case 5: return "Thứ 6";
            case 6: return "Thứ 7";
            default: return "Chủ nhật";
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
    String paymentContext = (String) request.getAttribute("paymentContext");
    boolean checkoutContext = "CHECKOUT".equalsIgnoreCase(paymentContext);
    boolean membershipContext = "MEMBERSHIP".equalsIgnoreCase(paymentContext);
    BookingView booking = (BookingView) request.getAttribute("booking");
    InvoiceView invoice = (InvoiceView) request.getAttribute("invoice");

    if (!checkoutContext && !membershipContext && booking == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay booking.");
        return;
    }
    if (checkoutContext && invoice == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay hoa don.");
        return;
    }

    List<PaymentMethod> paymentMethods = (List<PaymentMethod>) request.getAttribute("paymentMethods");
    if (paymentMethods == null) paymentMethods = new ArrayList<>();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Nguoi dung";
    String error = (String) request.getAttribute("error");

    boolean holdValid = !checkoutContext && !membershipContext
            && "HOLD".equals(booking.getStatus())
            && booking.getHoldExpiresAt() != null
            && booking.getHoldExpiresAt().isAfter(LocalDateTime.now());
    boolean canPay = membershipContext ? true : (checkoutContext ? "PENDING".equals(invoice.getInvoiceStatus()) : holdValid);
    boolean monthly = !checkoutContext && !membershipContext && isMonthlyBooking(booking);
    
    BigDecimal amountToPay = (BigDecimal) request.getAttribute("amountToPay");
    if (amountToPay == null) {
        amountToPay = checkoutContext ? invoice.getTotalAmount() : booking.getDepositAmount();
    }

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
    <title>Thanh toán | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(currentName) %>" data-active="Thanh toán"></div>

<main class="py-5">
    <div class="container">
        <div class="mb-3">
            <% if (checkoutContext) { %>
            <a class="btn btn-outline-secondary" href="<%= ctx %>/customer/checkout-invoice?id=<%= invoice.getInvoiceId() %>">
                <i class="bi bi-arrow-left"></i> Quay lại hóa đơn
            </a>
            <% } else if (membershipContext) { %>
            <a class="btn btn-outline-secondary" href="<%= ctx %>/profile">
                <i class="bi bi-arrow-left"></i> Quay lại hồ sơ
            </a>
            <% } else { %>
            <a class="btn btn-outline-secondary" href="<%= ctx %>/booking?action=detail&id=<%= booking.getBookingId() %>">
                <i class="bi bi-arrow-left"></i> Quay lại booking
            </a>
            <% } %>
        </div>

        <% if (error != null && !error.isBlank()) { %>
        <div class="alert alert-danger"><%= esc(error) %></div>
        <% } %>
        <% if (!checkoutContext && !membershipContext && !holdValid) { %>
        <div class="alert alert-warning">
            Booking không còn trong thời gian giữ chỗ. Không thể tạo giao dịch mới.
        </div>
        <% } %>
        <% if (checkoutContext && !canPay) { %>
        <div class="alert alert-info">
            Hóa đơn này không còn ở trạng thái chờ thanh toán.
        </div>
        <% } %>

        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card soft-card p-4">
                    <h1 class="section-title">
                        <%= checkoutContext ? "Thanh toán hóa đơn trả sân" : (membershipContext ? "Thanh toán Gói Hội Viên VIP" : "Chọn phương thức thanh toán") %>
                    </h1>
                    <p class="text-muted">
                        <% if (checkoutContext) { %>
                        Hóa đơn <strong>#<%= esc(invoice.getInvoiceCode()) %></strong> cho booking <strong><%= esc(invoice.getBookingCode()) %></strong>
                        <% } else if (membershipContext) { %>
                        Gói Hội Viên VIP (30 ngày)
                        <% } else { %>
                        Booking <strong><%= esc(booking.getBookingCode()) %></strong>
                        <% } %>
                    </p>

                    <% if (!membershipContext) { %>
                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <div class="text-muted small">Cụm sân</div>
                            <div class="fw-semibold"><%= esc(checkoutContext ? invoice.getComplexName() : booking.getComplexName()) %></div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Sân</div>
                            <div class="fw-semibold">
                                <% if (checkoutContext) { %>
                                <%= esc(invoice.getFieldName()) %>
                                <% } else { %>
                                <%= esc(booking.getFieldName()) %> - <%= esc(booking.getFieldTypeName()) %>
                                <% } %>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Thời gian</div>
                            <div class="fw-semibold">
                                <%= checkoutContext
                                        ? dateTime(invoice.getStartTime()) + " - " + timeOnly(invoice.getEndTime())
                                        : bookingTimeLabel(booking) %>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small"><%= checkoutContext ? "Trạng thái hóa đơn" : "Loại booking" %></div>
                            <div class="fw-semibold">
                                <% if (checkoutContext) { %>
                                <span class="badge bg-warning-subtle text-warning text-dark"><%= esc(invoice.getInvoiceStatus()) %></span>
                                <% } else { %>
                                <%= monthly ? "Thuê theo tháng" : "Thuê đơn lẻ" %>
                                <% if (monthly && booking.getRecurringCount() != null) { %>
                                <span class="text-muted">(<%= booking.getRecurringCount() %> buổi)</span>
                                <% } %>
                                <% } %>
                            </div>
                        </div>
                        <% if (!checkoutContext) { %>
                        <div class="col-md-6">
                            <div class="text-muted small">Giữ chỗ đến</div>
                            <div class="fw-semibold text-danger"><%= dateTime(booking.getHoldExpiresAt()) %></div>
                        </div>
                        <% } else { %>
                        <div class="col-md-6">
                            <div class="text-muted small">Phút quá giờ</div>
                            <div class="fw-semibold"><%= invoice.getOvertimeMinutes() %> phút</div>
                        </div>
                        <div class="col-md-6">
                            <div class="text-muted small">Phụ thu quá giờ</div>
                            <div class="fw-semibold"><%= money(invoice.getOvertimeFee()) %></div>
                        </div>
                        <% } %>
                    </div>
                    <% } else { %>
                    <div class="row g-3 mb-4">
                        <div class="col-md-12">
                            <div class="text-muted small">Chi tiết gói</div>
                            <div class="fw-semibold">Gói VIP 30 ngày sử dụng đặc quyền. Sau khi thanh toán thành công, trạng thái VIP của bạn sẽ được kích hoạt hoặc gia hạn thêm 30 ngày.</div>
                        </div>
                    </div>
                    <% } %>

                    <% if (paymentMethods.isEmpty()) { %>
                    <div class="alert alert-info mb-0">Hiện không có phương thức thanh toán đang hoạt động.</div>
                    <% } else { %>
                    <form method="post" action="<%= ctx %>/payment">
                        <input type="hidden" name="action" value="pay">
                        <% if (checkoutContext) { %>
                        <input type="hidden" name="paymentType" value="CHECKOUT">
                        <input type="hidden" name="invoiceId" value="<%= invoice.getInvoiceId() %>">
                        <% } else if (membershipContext) { %>
                        <input type="hidden" name="paymentType" value="MEMBERSHIP">
                        <% } else { %>
                        <input type="hidden" name="paymentType" value="DEPOSIT">
                        <input type="hidden" name="bookingId" value="<%= booking.getBookingId() %>">
                        <% } %>
                        <h5 class="mb-3">Phương thức</h5>
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
                                    <%= canPay && vnpayMethodId != null ? "" : "disabled" %>>
                                <i class="bi bi-bank"></i> Thanh toán qua VNPay
                            </button>
                            <button class="btn btn-sf-primary" type="submit" name="simulateStatus" value="SUCCESS"
                                    <%= canPay ? "" : "disabled" %>>
                                <i class="bi bi-shield-check"></i> Thanh toán thành công
                            </button>
                            <button class="btn btn-outline-danger" type="submit" name="simulateStatus" value="FAILED"
                                    <%= canPay ? "" : "disabled" %>>
                                Giả lập thất bại
                            </button>
                        </div>
                        <div class="text-muted small mt-2">Chế độ mô phỏng dành cho demo.</div>
                        <div class="text-muted small mt-1">VNPay Sandbox yêu cầu return/ipn URL public HTTPS; khi chạy local có thể dùng ngrok.</div>
                    </form>
                    <% } %>
                </div>
            </div>

            <aside class="col-lg-4">
                <div class="card soft-card p-4 sidebar-card">
                    <h5>Tóm tắt thanh toán</h5>
                    <% if (checkoutContext) { %>
                    <div class="d-flex justify-content-between mt-3">
                        <span>Tổng tiền sân</span>
                        <strong><%= money(invoice.getFieldFee()) %></strong>
                    </div>
                    <div class="d-flex justify-content-between mt-2">
                        <span>Phụ thu quá giờ</span>
                        <strong><%= money(invoice.getOvertimeFee()) %></strong>
                    </div>
                    <div class="d-flex justify-content-between mt-2 text-success">
                        <span>Tiền cọc đã thanh toán</span>
                        <strong>- <%= money(invoice.getDepositAmount()) %></strong>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between fs-5">
                        <span>Cần thanh toán</span>
                        <strong class="text-success"><%= money(amountToPay) %></strong>
                    </div>
                    <% } else if (membershipContext) { %>
                    <div class="d-flex justify-content-between mt-3">
                        <span>Giá trị gói</span>
                        <strong><%= money(amountToPay) %></strong>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between fs-5">
                        <span>Cần thanh toán</span>
                        <strong class="text-success"><%= money(amountToPay) %></strong>
                    </div>
                    <% } else { %>
                    <div class="d-flex justify-content-between mt-3">
                        <span>Tổng tiền</span>
                        <strong><%= money(booking.getTotalAmount()) %></strong>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between fs-5">
                        <span><%= monthly ? "Thanh toán toàn bộ" : "Tiền cọc" %></span>
                        <strong class="text-success"><%= money(amountToPay) %></strong>
                    </div>
                    <% } %>
                    <p class="text-muted small mt-3 mb-0">Số tiền được lấy trực tiếp từ cơ sở dữ liệu.</p>
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

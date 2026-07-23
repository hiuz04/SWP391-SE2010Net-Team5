<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.CheckoutView" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%!
  private String esc(Object value) {
    if (value == null) return "";
    return value.toString()
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private String money(BigDecimal amount) {
    if (amount == null) amount = BigDecimal.ZERO;
    return String.format("%,d VND", amount.longValue());
  }

  private String dateTime(LocalDateTime value) {
    return value == null ? "" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
  }

  private String time(LocalDateTime value) {
    return value == null ? "" : value.format(DateTimeFormatter.ofPattern("HH:mm"));
  }
%>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";

    CheckoutView checkout = (CheckoutView) request.getAttribute("checkout");
    String error = (String) request.getAttribute("error");
    boolean canConfirm = checkout != null && checkout.isCheckoutAllowed();
    BigDecimal remainingAmount = checkout == null ? BigDecimal.ZERO : checkout.getFinalAmount();
    boolean requiresPaymentMethod = remainingAmount != null && remainingAmount.signum() > 0;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Trả sân | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; font-family: 'Inter', sans-serif; }
    .checkout-shell { max-width: 1120px; margin: 0 auto; }
    .panel {
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(15,23,42,.04);
    }
    .summary-line { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
    .summary-total {
      border-top: 1px solid #e2e8f0;
      padding-top: 18px;
      margin-top: 18px;
      font-size: 1.2rem;
      font-weight: 800;
    }
    .btn-sf-primary { background-color: #16a34a; color: #fff; }
    .btn-sf-primary:hover { background-color: #15803d; color: #fff; }
    .readonly-money { background: #f8fafc; font-weight: 700; color: #0f172a; }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= esc(displayName) %>" data-active="Lịch trong ngày"></div>

<main class="py-5">
  <div class="container checkout-shell">
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-3">
      <div>
        <h1 class="fw-bold mb-1">Trả sân</h1>
        <p class="text-muted mb-0">Tính phụ thu quá giờ, ghi nhận tiền mặt hoặc gửi yêu cầu thanh toán online cho khách.</p>
      </div>
      <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i>Quay lại lịch
      </a>
    </div>

    <% if (error != null) { %>
      <div class="panel p-5 text-center">
        <i class="bi bi-exclamation-triangle-fill display-4 text-danger"></i>
        <h4 class="fw-bold mt-3">Không thể trả sân</h4>
        <p class="text-muted mb-4"><%= esc(error) %></p>
        <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary px-4">Xem lịch sân</a>
      </div>
    <% } else if (checkout == null) { %>
      <div class="panel p-5 text-center">
        <i class="bi bi-calendar2-week display-4 text-muted"></i>
        <h4 class="fw-bold mt-3">Chọn lịch cần trả sân</h4>
        <p class="text-muted mb-4">Vui lòng chọn một lịch đang sử dụng từ lịch trong ngày.</p>
        <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary px-4">Xem lịch sân</a>
      </div>
    <% } else { %>
      <% if (!canConfirm) { %>
        <div class="alert alert-warning border-0 shadow-sm">
          <i class="bi bi-clock-history me-2"></i>Chưa đến giờ kết thúc trận, chưa thể trả sân.
        </div>
      <% } %>

      <div class="row g-4 align-items-start">
        <div class="col-lg-8">
          <div class="panel p-4">
            <div class="d-flex align-items-center justify-content-between gap-3 flex-wrap mb-4">
              <div>
                <span class="badge bg-info-subtle text-info fw-bold mb-2">Đang sử dụng</span>
                <h4 class="fw-bold mb-0">#<%= esc(checkout.getBookingCode()) %></h4>
              </div>
              <span class="text-muted small"><%= esc(dateTime(checkout.getStartTime())) %> - <%= esc(time(checkout.getEndTime())) %></span>
            </div>

            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Mã booking</label>
                <input class="form-control" value="<%= esc(checkout.getBookingCode()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Khách hàng</label>
                <input class="form-control" value="<%= esc(checkout.getCustomerName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Số điện thoại</label>
                <input class="form-control" value="<%= esc(checkout.getCustomerPhone()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Cụm sân</label>
                <input class="form-control" value="<%= esc(checkout.getComplexName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Sân</label>
                <input class="form-control" value="<%= esc(checkout.getFieldName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Giờ bắt đầu</label>
                <input class="form-control" value="<%= esc(dateTime(checkout.getStartTime())) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Giờ kết thúc dự kiến</label>
                <input class="form-control" value="<%= esc(dateTime(checkout.getEndTime())) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Giờ checkout thực tế</label>
                <input class="form-control" value="<%= esc(dateTime(checkout.getCheckoutTime())) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Tổng tiền sân</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getFieldFee()) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Phút quá giờ</label>
                <input class="form-control readonly-money" value="<%= checkout.getOvertimeMinutes() %> phút" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Đơn giá phụ thu/phút</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getOvertimeFeePerMinute()) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Tổng phụ thu quá giờ</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getOvertimeFee()) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Đã thanh toán trước</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getPaidAmountBeforeCheckout()) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Số tiền còn lại</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getFinalAmount()) %>" readonly>
              </div>
            </div>
          </div>
        </div>

        <aside class="col-lg-4">
          <div class="panel p-4">
            <h5 class="fw-bold mb-4"><i class="bi bi-receipt-cutoff text-success me-2"></i>Tổng kết</h5>
            <div class="summary-line">
              <span class="text-muted">Tiền sân</span>
              <strong><%= money(checkout.getFieldFee()) %></strong>
            </div>
            <div class="summary-line">
              <span class="text-muted">Phụ thu quá giờ</span>
              <strong><%= money(checkout.getOvertimeFee()) %></strong>
            </div>
            <div class="summary-line">
              <span class="text-muted">Tạm tính</span>
              <strong><%= money(checkout.getSubtotal()) %></strong>
            </div>
            <div class="summary-line text-success">
              <span>Đã thanh toán trước</span>
              <strong>- <%= money(checkout.getPaidAmountBeforeCheckout()) %></strong>
            </div>
            <div class="summary-line summary-total">
              <span>Số tiền còn lại</span>
              <span class="text-success"><%= money(checkout.getFinalAmount()) %></span>
            </div>

            <% if (requiresPaymentMethod) { %>
            <div class="mt-4">
              <div class="small fw-bold text-muted mb-2">Phương thức xử lý</div>
              <label class="border rounded p-3 d-flex gap-3 align-items-start mb-2 checkout-method-option">
                <input class="form-check-input mt-1" type="radio" name="checkoutPaymentMethod" value="CASH">
                <span>
                  <strong>Thanh toán tiền mặt</strong>
                  <span class="d-block text-muted small">Xác nhận đã nhận đủ <%= money(checkout.getFinalAmount()) %> tại quầy.</span>
                </span>
              </label>
              <label class="border rounded p-3 d-flex gap-3 align-items-start checkout-method-option">
                <input class="form-check-input mt-1" type="radio" name="checkoutPaymentMethod" value="ONLINE_REQUEST">
                <span>
                  <strong>Gửi yêu cầu thanh toán online cho khách</strong>
                  <span class="d-block text-muted small">Khách sẽ thấy popup và chọn phương thức online để thanh toán.</span>
                </span>
              </label>
            </div>
            <% } else { %>
            <div class="alert alert-info border-0 mt-4 mb-0">
              Booking đã được thanh toán đủ, có thể hoàn tất checkout mà không cần chọn phương thức thanh toán.
            </div>
            <% } %>

            <div id="checkout-error" class="alert alert-danger d-none mt-4 mb-0"></div>
            <div id="checkout-success" class="alert alert-success d-none mt-4 mb-0"></div>

            <button type="button" id="confirmCheckoutBtn" class="btn btn-sf-primary btn-lg w-100 mt-4" <%= canConfirm ? "" : "disabled" %>>
              <i class="bi bi-check2-circle me-2"></i><%= requiresPaymentMethod ? "Chọn phương thức xử lý" : "Hoàn tất checkout" %>
            </button>
            <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary w-100 mt-2">Quay lại</a>
          </div>
        </aside>
      </div>
    <% } %>
  </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
  const confirmBtn = document.getElementById('confirmCheckoutBtn');
  const requiresPaymentMethod = <%= requiresPaymentMethod ? "true" : "false" %>;
  const remainingAmountText = '<%= money(checkout != null ? checkout.getFinalAmount() : BigDecimal.ZERO) %>';
  const defaultButtonHtml = '<i class="bi bi-check2-circle me-2"></i><%= requiresPaymentMethod ? "Chọn phương thức xử lý" : "Hoàn tất checkout" %>';

  function showCheckoutMessage(id, message) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = message;
    el.classList.remove('d-none');
  }

  function selectedCheckoutMethod() {
    return document.querySelector('input[name="checkoutPaymentMethod"]:checked')?.value || '';
  }

  function refreshCheckoutButton() {
    if (!confirmBtn) return;
    if (!requiresPaymentMethod) {
      confirmBtn.innerHTML = '<i class="bi bi-check2-circle me-2"></i>Hoàn tất checkout';
      return;
    }
    const method = selectedCheckoutMethod();
    if (method === 'CASH') {
      confirmBtn.innerHTML = '<i class="bi bi-cash-coin me-2"></i>Xác nhận đã nhận ' + remainingAmountText;
    } else if (method === 'ONLINE_REQUEST') {
      confirmBtn.innerHTML = '<i class="bi bi-send-check me-2"></i>Gửi yêu cầu thanh toán ' + remainingAmountText;
    } else {
      confirmBtn.innerHTML = defaultButtonHtml;
    }
  }

  async function submitCheckout() {
    if (!confirmBtn) return;
    document.getElementById('checkout-error')?.classList.add('d-none');
    document.getElementById('checkout-success')?.classList.add('d-none');

    const method = selectedCheckoutMethod();
    if (requiresPaymentMethod && !method) {
      showCheckoutMessage('checkout-error', 'Vui lòng chọn thanh toán tiền mặt hoặc gửi yêu cầu thanh toán online.');
      return;
    }

    if (method === 'CASH') {
      const accepted = window.confirm('Xác nhận đã nhận ' + remainingAmountText + ' tiền mặt từ khách?');
      if (!accepted) return;
    }

    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang gửi...';

    try {
      const params = new URLSearchParams();
      params.append('bookingId', '<%= checkout != null ? checkout.getBookingId() : "" %>');
      if (method) {
        params.append('checkoutPaymentMethod', method);
      }

      // API server sẽ khóa booking và tự tính lại số tiền còn lại; client không gửi amount làm nguồn dữ liệu.
      const res = await fetch('<%= ctx %>/api/staff/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });
      const data = await res.json().catch(() => ({}));

      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Không thể gửi yêu cầu thanh toán.');
      }

      showCheckoutMessage('checkout-success', data.message || 'Đã gửi yêu cầu thanh toán cho khách.');
      window.setTimeout(() => {
        window.location.href = data.redirectUrl || ('<%= ctx %>/staff/schedule');
      }, 900);
    } catch (err) {
      showCheckoutMessage('checkout-error', err.message || 'Không thể gửi yêu cầu thanh toán.');
      confirmBtn.disabled = false;
      refreshCheckoutButton();
    }
  }

  if (confirmBtn) {
    confirmBtn.addEventListener('click', submitCheckout);
  }
  document.querySelectorAll('input[name="checkoutPaymentMethod"]').forEach(input => {
    input.addEventListener('change', refreshCheckoutButton);
  });
  refreshCheckoutButton();
</script>
</body>
</html>

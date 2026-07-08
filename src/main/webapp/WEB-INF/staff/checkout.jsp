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
    return String.format("%,d ₫", amount.longValue());
  }

  private String dateTime(LocalDateTime value) {
    return value == null ? "" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
  }
%>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";

    CheckoutView checkout = (CheckoutView) request.getAttribute("checkout");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Checkout | Sport Field Booking</title>
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
      border-radius: 20px;
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
        <h1 class="fw-bold mb-1">Checkout & Billing</h1>
        <p class="text-muted mb-0">Xác nhận thanh toán còn lại cho booking đã check-in.</p>
      </div>
      <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i>Quay lại lịch
      </a>
    </div>

    <% if (error != null) { %>
      <div class="panel p-5 text-center">
        <i class="bi bi-exclamation-triangle-fill display-4 text-danger"></i>
        <h4 class="fw-bold mt-3">Không thể checkout</h4>
        <p class="text-muted mb-4"><%= esc(error) %></p>
        <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary px-4">Xem lịch sân</a>
      </div>
    <% } else if (checkout == null) { %>
      <div class="panel p-5 text-center">
        <i class="bi bi-calendar2-week display-4 text-muted"></i>
        <h4 class="fw-bold mt-3">Chọn booking cần checkout</h4>
        <p class="text-muted mb-4">Vui lòng chọn một booking đang chơi từ lịch trong ngày.</p>
        <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary px-4">Xem lịch sân</a>
      </div>
    <% } else { %>
      <div class="row g-4 align-items-start">
        <div class="col-lg-8">
          <div class="panel p-4">
            <div class="d-flex align-items-center justify-content-between gap-3 flex-wrap mb-4">
              <div>
                <span class="badge bg-info-subtle text-info fw-bold mb-2">Đang chơi</span>
                <h4 class="fw-bold mb-0">#<%= esc(checkout.getBookingCode()) %></h4>
              </div>
              <span class="text-muted small"><%= esc(dateTime(checkout.getStartTime())) %> - <%= esc(dateTime(checkout.getEndTime())) %></span>
            </div>

            <div class="row g-3 mb-4">
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Khách hàng</label>
                <input class="form-control" value="<%= esc(checkout.getCustomerName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Số điện thoại</label>
                <input class="form-control" value="<%= esc(checkout.getCustomerPhone()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Cơ sở</label>
                <input class="form-control" value="<%= esc(checkout.getFacilityName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Sân</label>
                <input class="form-control" value="<%= esc(checkout.getFieldName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Tiền thuê sân</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getFieldFee()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Tiền cọc đã trả</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getDepositAmount()) %>" readonly>
              </div>
            </div>

            <form id="checkout-form">
              <input type="hidden" id="bookingId" value="<%= checkout.getBookingId() %>">
              <input type="hidden" id="fieldFee" value="<%= checkout.getFieldFee().toPlainString() %>">
              <input type="hidden" id="depositAmount" value="<%= checkout.getDepositAmount().toPlainString() %>">

              <div class="row g-3">
                <div class="col-md-6">
                  <label for="surchargeAmount" class="form-label small fw-bold text-muted">Phụ phí phát sinh</label>
                  <input type="number" class="form-control form-control-lg" id="surchargeAmount" min="0" step="1000" value="0">
                </div>
                <div class="col-md-6">
                  <label for="discountAmount" class="form-label small fw-bold text-muted">Giảm giá</label>
                  <input type="number" class="form-control form-control-lg" id="discountAmount" min="0" step="1000" value="0">
                </div>
                <div class="col-12">
                  <label for="surchargeReason" class="form-label small fw-bold text-muted">Lý do phụ phí</label>
                  <textarea class="form-control" id="surchargeReason" rows="3" maxlength="500" placeholder="Ví dụ: thuê thêm dụng cụ, nước uống, bồi hoàn..."></textarea>
                </div>
                <div class="col-12">
                  <label for="note" class="form-label small fw-bold text-muted">Ghi chú</label>
                  <textarea class="form-control" id="note" rows="2" maxlength="500"></textarea>
                </div>
              </div>
            </form>
          </div>
        </div>

        <aside class="col-lg-4">
          <div class="panel p-4">
            <h5 class="fw-bold mb-4"><i class="bi bi-receipt-cutoff text-success me-2"></i>Tổng kết</h5>
            <div class="summary-line">
              <span class="text-muted">Tiền thuê sân</span>
              <strong id="summaryFieldFee"><%= money(checkout.getFieldFee()) %></strong>
            </div>
            <div class="summary-line">
              <span class="text-muted">Phụ phí</span>
              <strong id="summarySurcharge">0 ₫</strong>
            </div>
            <div class="summary-line text-success">
              <span>Tiền cọc</span>
              <strong>- <%= money(checkout.getDepositAmount()) %></strong>
            </div>
            <div class="summary-line text-success">
              <span>Giảm giá</span>
              <strong id="summaryDiscount">- 0 ₫</strong>
            </div>
            <div class="summary-line summary-total">
              <span>Cần thanh toán</span>
              <span class="text-success" id="summaryTotal"><%= money(checkout.getBaseRemainingAmount()) %></span>
            </div>

            <div id="checkout-error" class="alert alert-danger d-none mt-4 mb-0"></div>

            <button type="button" id="confirmCheckoutBtn" class="btn btn-sf-primary btn-lg w-100 mt-4">
              <i class="bi bi-check-circle me-2"></i>Xác nhận checkout
            </button>
            <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary w-100 mt-2">Hủy</a>
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
  const formEl = document.getElementById('checkout-form');
  const confirmBtn = document.getElementById('confirmCheckoutBtn');

  function money(value) {
    return Math.max(0, Number(value || 0)).toLocaleString('vi-VN') + ' ₫';
  }

  function amount(id) {
    const el = document.getElementById(id);
    return el ? Math.max(0, Number(el.value || 0)) : 0;
  }

  function recalculate() {
    const fieldFee = amount('fieldFee');
    const deposit = amount('depositAmount');
    const surcharge = amount('surchargeAmount');
    const discount = amount('discountAmount');
    const total = Math.max(0, fieldFee + surcharge - deposit - discount);

    const surchargeEl = document.getElementById('summarySurcharge');
    const discountEl = document.getElementById('summaryDiscount');
    const totalEl = document.getElementById('summaryTotal');
    if (surchargeEl) surchargeEl.textContent = money(surcharge);
    if (discountEl) discountEl.textContent = '- ' + money(discount);
    if (totalEl) totalEl.textContent = money(total);
  }

  function showError(message) {
    const errorEl = document.getElementById('checkout-error');
    if (!errorEl) return;
    errorEl.textContent = message;
    errorEl.classList.remove('d-none');
  }

  async function submitCheckout() {
    if (!confirmBtn) return;
    const errorEl = document.getElementById('checkout-error');
    if (errorEl) {
      errorEl.classList.add('d-none');
      errorEl.textContent = '';
    }

    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';

    try {
      const params = new URLSearchParams();
      params.append('bookingId', document.getElementById('bookingId').value);
      params.append('surchargeAmount', String(amount('surchargeAmount')));
      params.append('discountAmount', String(amount('discountAmount')));
      params.append('surchargeReason', document.getElementById('surchargeReason').value.trim());
      params.append('note', document.getElementById('note').value.trim());

      const res = await fetch('<%= ctx %>/api/staff/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });
      const data = await res.json().catch(() => ({}));

      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Checkout không thành công');
      }

      window.location.href = '<%= ctx %>/staff/invoice?id=' + data.bookingId;
    } catch (err) {
      showError(err.message || 'Không thể checkout');
      confirmBtn.disabled = false;
      confirmBtn.innerHTML = '<i class="bi bi-check-circle me-2"></i>Xác nhận checkout';
    }
  }

  if (formEl) {
    formEl.addEventListener('input', recalculate);
  }
  if (confirmBtn) {
    confirmBtn.addEventListener('click', submitCheckout);
  }
</script>
</body>
</html>

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
    return String.format("%,d đ", amount.longValue());
  }

  private String date(LocalDateTime value) {
    return value == null ? "" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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
        <h1 class="fw-bold mb-1">Xác nhận trả sân</h1>
        <p class="text-muted mb-0">Hoàn tất lịch đã nhận sân và tạo hóa đơn cho số tiền còn lại.</p>
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
        <p class="text-muted mb-4">Vui lòng chọn một lịch đang chơi từ lịch trong ngày.</p>
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
              <span class="text-muted small"><%= esc(date(checkout.getStartTime())) %> · <%= esc(time(checkout.getStartTime())) %> - <%= esc(time(checkout.getEndTime())) %></span>
            </div>

            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Mã đặt sân</label>
                <input class="form-control" value="<%= esc(checkout.getBookingCode()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Tên khách hàng</label>
                <input class="form-control" value="<%= esc(checkout.getCustomerName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Số điện thoại</label>
                <input class="form-control" value="<%= esc(checkout.getCustomerPhone()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Tên cơ sở</label>
                <input class="form-control" value="<%= esc(checkout.getComplexName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Tên sân</label>
                <input class="form-control" value="<%= esc(checkout.getFieldName()) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Ngày đặt sân</label>
                <input class="form-control" value="<%= esc(date(checkout.getStartTime())) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Giờ bắt đầu</label>
                <input class="form-control" value="<%= esc(time(checkout.getStartTime())) %>" readonly>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Giờ kết thúc</label>
                <input class="form-control" value="<%= esc(time(checkout.getEndTime())) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Tổng tiền thuê sân</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getFieldFee()) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Tiền cọc đã thanh toán</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getDepositAmount()) %>" readonly>
              </div>
              <div class="col-md-4">
                <label class="form-label small fw-bold text-muted">Số tiền còn lại</label>
                <input class="form-control readonly-money" value="<%= money(checkout.getBaseRemainingAmount()) %>" readonly>
              </div>
            </div>
          </div>
        </div>

        <aside class="col-lg-4">
          <div class="panel p-4">
            <h5 class="fw-bold mb-4"><i class="bi bi-receipt-cutoff text-success me-2"></i>Tổng kết</h5>
            <div class="summary-line">
              <span class="text-muted">Tiền thuê sân</span>
              <strong><%= money(checkout.getFieldFee()) %></strong>
            </div>
            <div class="summary-line text-success">
              <span>Tiền cọc đã thanh toán</span>
              <strong>- <%= money(checkout.getDepositAmount()) %></strong>
            </div>
            <div class="summary-line summary-total">
              <span>Cần thanh toán</span>
              <span class="text-success"><%= money(checkout.getBaseRemainingAmount()) %></span>
            </div>

            <div id="checkout-error" class="alert alert-danger d-none mt-4 mb-0"></div>

            <button type="button" id="confirmCheckoutBtn" class="btn btn-sf-primary btn-lg w-100 mt-4">
              <i class="bi bi-check-circle me-2"></i>Xác nhận trả sân
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
      params.append('bookingId', '<%= checkout != null ? checkout.getBookingId() : "" %>');

      const res = await fetch('<%= ctx %>/api/staff/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });
      const data = await res.json().catch(() => ({}));

      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Không thể xác nhận trả sân.');
      }

      window.location.href = data.redirectUrl || ('<%= ctx %>/staff/invoice?id=' + data.bookingId);
    } catch (err) {
      showError(err.message || 'Không thể xác nhận trả sân.');
      confirmBtn.disabled = false;
      confirmBtn.innerHTML = '<i class="bi bi-check-circle me-2"></i>Xác nhận trả sân';
    }
  }

  if (confirmBtn) {
    confirmBtn.addEventListener('click', submitCheckout);
  }
</script>
</body>
</html>

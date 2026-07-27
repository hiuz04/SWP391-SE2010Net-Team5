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
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= esc(displayName) %>" data-active="Check-in/out"></div>

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
      <%
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> checkedInBookings =
            (java.util.List<java.util.Map<String, Object>>) request.getAttribute("checkedInBookings");
      %>
      <% if (checkedInBookings != null && !checkedInBookings.isEmpty()) { %>
        <div class="panel p-4 mb-4">
          <h4 class="fw-bold mb-3"><i class="bi bi-box-arrow-right text-success me-2"></i>Danh sách sân đang sử dụng — Chờ trả sân (Checkout)</h4>
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="bg-light">
                <tr>
                  <th>Khung giờ</th>
                  <th>Mã đặt sân</th>
                  <th>Sân bóng</th>
                  <th>Khách hàng</th>
                  <th>Số điện thoại</th>
                  <th class="text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <% for (java.util.Map<String, Object> b : checkedInBookings) { %>
                  <tr class="booking-row" onclick="showBookingDetailsFromRow(this, event)" style="cursor:pointer;" title="Nhấn để xem chi tiết đặt sân"
                      data-code="<%= esc(b.get("bookingCode")) %>"
                      data-name="<%= esc(b.get("customerName")) %>"
                      data-phone="<%= esc(b.get("customerPhone")) %>"
                      data-field="<%= esc(b.get("fieldName")) %>"
                      data-start="<%= b.get("startTime") != null ? b.get("startTime").toString() : "" %>"
                      data-end="<%= b.get("endTime") != null ? b.get("endTime").toString() : "" %>"
                      data-total="<%= b.get("totalAmount") != null ? b.get("totalAmount").toString() : "0" %>"
                      data-deposit="<%= b.get("depositAmount") != null ? b.get("depositAmount").toString() : "0" %>"
                      data-id="<%= b.get("bookingId") %>">
                    <td><span class="badge bg-light text-dark border"><%= esc(b.get("startTime")) != null ? b.get("startTime").toString().substring(0, Math.min(16, b.get("startTime").toString().length())) : "" %> - <%= esc(b.get("endTime")) != null ? b.get("endTime").toString().substring(0, Math.min(16, b.get("endTime").toString().length())) : "" %></span></td>
                    <td><strong><%= esc(b.get("bookingCode")) %></strong></td>
                    <td><strong><%= esc(b.get("fieldName")) %></strong></td>
                    <td><%= esc(b.get("customerName")) %></td>
                    <td><%= esc(b.get("customerPhone")) %></td>
                    <td class="text-center">
                      <a href="<%= ctx %>/staff/checkout?id=<%= b.get("bookingId") %>" class="btn btn-sm btn-success px-3">
                        <i class="bi bi-box-arrow-right me-1"></i>Checkout
                      </a>
                    </td>
                  </tr>
                <% } %>
              </tbody>
            </table>
          </div>
        </div>
      <% } else { %>
        <div class="panel p-5 text-center">
          <i class="bi bi-check-circle-fill display-4 text-success"></i>
          <h4 class="fw-bold mt-3">Không có sân nào chờ trả sân</h4>
          <p class="text-muted mb-4">Hiện tại không có lịch đặt sân nào đang trong trạng thái nhận sân (`CHECKED_IN`) tại cơ sở này.</p>
          <a href="<%= ctx %>/staff/dashboard" class="btn btn-sf-primary px-4 me-2">Về Dashboard</a>
          <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary px-4">Xem lịch sân</a>
        </div>
      <% } %>
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

<!-- Booking Details Modal -->
<div class="modal fade" id="bookingDetailModal" tabindex="-1" aria-labelledby="bookingDetailModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content border-0 shadow-lg" style="border-radius: 24px; overflow: hidden; background: #ffffff;">
      <div class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-circle bg-success-subtle p-2 text-success d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-color: #dcfce7;">
            <i class="bi bi-calendar-check-fill fs-4" style="color: #16a34a;"></i>
          </div>
          <div>
            <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="bookingDetailModalLabel">Chi tiết lịch đặt sân</h5>
            <span class="text-muted small" style="font-size: 0.8rem;">Mã đặt sân: <strong class="text-success" id="det-code">—</strong></span>
          </div>
        </div>
        <button type="button" class="btn-close bg-light rounded-circle p-2" data-bs-dismiss="modal" aria-label="Close" style="font-size: 0.8rem;"></button>
      </div>

      <div class="modal-body p-4">
        <div class="row g-4">
          <div class="col-md-7">
            <div class="mb-4">
              <h6 class="fw-bold text-muted uppercase small mb-3 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-person-fill me-2" style="color: #16a34a;"></i>THÔNG TIN KHÁCH HÀNG</h6>
              <div class="p-3 bg-light rounded-4 border border-light-subtle" style="background-color: #f8fafc !important;">
                <div class="mb-2">
                  <span class="text-muted small d-block" style="font-size: 0.75rem;">Tên khách hàng</span>
                  <span class="fw-bold text-dark fs-6" id="det-name">—</span>
                </div>
                <div>
                  <span class="text-muted small d-block" style="font-size: 0.75rem;">Số điện thoại</span>
                  <span class="fw-bold text-success fs-6" id="det-phone">—</span>
                </div>
              </div>
            </div>

            <div>
              <h6 class="fw-bold text-muted uppercase small mb-3 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-heptagon-fill me-2" style="color: #16a34a;"></i>THÔNG TIN TRẬN ĐẤU</h6>
              <div class="p-3 bg-light rounded-4 border border-light-subtle" style="background-color: #f8fafc !important;">
                <div class="row g-3">
                  <div class="col-6">
                    <span class="text-muted small d-block" style="font-size: 0.75rem;">Sân bóng</span>
                    <span class="fw-bold text-dark" id="det-field">—</span>
                  </div>
                  <div class="col-6">
                    <span class="text-muted small d-block" style="font-size: 0.75rem;">Trạng thái</span>
                    <div id="det-status-badge">—</div>
                  </div>
                  <div class="col-12">
                    <span class="text-muted small d-block" style="font-size: 0.75rem;">Khung giờ sử dụng</span>
                    <span class="fw-bold text-dark fs-6"><i class="bi bi-clock me-2 text-muted"></i><span id="det-time">—</span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="col-md-5">
            <h6 class="fw-bold text-muted uppercase small mb-3 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-receipt-cutoff me-2" style="color: #16a34a;"></i>CHI TIẾT THANH TOÁN</h6>
            <div class="p-4 rounded-4 shadow-sm border border-success-subtle d-flex flex-column justify-content-between" style="background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%); border-color: #bbf7d0 !important; border: 1px solid; min-height: 230px;">
              <div>
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <span class="text-muted small" style="font-size: 0.8rem;">Giá gốc sân:</span>
                  <span class="fw-bold text-dark" id="det-orig-price">—</span>
                </div>
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <span class="text-muted small">Đã đặt cọc trước:</span>
                  <span class="fw-bold text-danger" id="det-deposit">—</span>
                </div>
                <hr class="my-3 border-secondary-subtle">
              </div>
              <div class="text-center py-2">
                <span class="text-muted small d-block mb-1" style="font-size: 0.72rem; letter-spacing: 0.05em; font-weight: 700;">CẦN THANH TOÁN CÒN LẠI</span>
                <span class="fw-bold text-success display-6" style="font-weight: 800; font-size: 1.8rem;" id="det-total-amount">—</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 pt-0 d-flex justify-content-end gap-2" id="det-modal-footer">
        <button type="button" class="btn btn-light px-4 py-2 rounded-3" data-bs-dismiss="modal">Đóng</button>
      </div>
    </div>
  </div>
</div>

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

  let detailModalInstance = null;

  function showBookingDetailsFromRow(row, event) {
    if (event.target.closest('a') || event.target.closest('button')) return;

    const code = row.getAttribute('data-code') || '—';
    const name = row.getAttribute('data-name') || '—';
    const phone = row.getAttribute('data-phone') || 'Không có SĐT';
    const field = row.getAttribute('data-field') || '—';
    const start = row.getAttribute('data-start') || '';
    const end = row.getAttribute('data-end') || '';
    const total = parseFloat(row.getAttribute('data-total') || '0');
    const deposit = parseFloat(row.getAttribute('data-deposit') || '0');
    const bookingId = row.getAttribute('data-id');

    document.getElementById('det-code').textContent = code;
    document.getElementById('det-name').textContent = name;
    document.getElementById('det-phone').textContent = phone;
    document.getElementById('det-field').textContent = field;

    let timeStr = '—';
    if (start) {
      let tStart = start.includes(' ') ? start.split(' ')[1] : start;
      let tEnd = end.includes(' ') ? end.split(' ')[1] : end;
      timeStr = tStart.substring(0, 5) + ' - ' + tEnd.substring(0, 5);
    }
    document.getElementById('det-time').textContent = timeStr;
    document.getElementById('det-status-badge').innerHTML = '<span class="badge" style="background:#e0f2fe;color:#0369a1;"><i class="bi bi-play-circle me-1"></i>Đang chơi</span>';

    document.getElementById('det-orig-price').textContent = Number(total).toLocaleString('vi-VN') + ' ₫';
    document.getElementById('det-deposit').textContent = Number(deposit).toLocaleString('vi-VN') + ' ₫';
    const rem = total - deposit;
    document.getElementById('det-total-amount').textContent = Number(rem >= 0 ? rem : 0).toLocaleString('vi-VN') + ' ₫';

    const footer = document.getElementById('det-modal-footer');
    footer.innerHTML = '<button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button>' +
                       '<a href="<%= ctx %>/staff/checkout?id=' + bookingId + '" class="btn btn-success px-4">Checkout</a>';

    const modalEl = document.getElementById('bookingDetailModal');
    if (modalEl) {
      if (!detailModalInstance) detailModalInstance = new bootstrap.Modal(modalEl);
      detailModalInstance.show();
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

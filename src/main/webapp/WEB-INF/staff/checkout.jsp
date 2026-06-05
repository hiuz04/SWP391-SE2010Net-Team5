<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.Map" %>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";

    Map<String, Object> booking = (Map<String, Object>) request.getAttribute("booking");
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
    .checkout-card { border-radius: 24px; border: 1px solid #e2e8f0; background: #fff; box-shadow: 0 10px 30px rgba(15,23,42,.04); padding: 32px; }
    .summary-box { border-radius: 20px; border: 1px solid #cbd5e1; background: #f8fafc; padding: 24px; }
    .btn-sf-primary {
      background-color: #16a34a;
      color: #ffffff;
    }
    .btn-sf-primary:hover {
      background-color: #15803d;
      color: #ffffff;
    }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Checkout"></div>

<main class="py-5">
  <div class="container">
    
    <% if (error != null) { %>
      <div class="alert alert-danger rounded-4 border-0 p-4 text-center">
        <i class="bi bi-exclamation-triangle-fill display-5 mb-2 d-block"></i>
        <h4 class="fw-bold">Lỗi Checkout</h4>
        <p class="mb-0"><%= error %></p>
        <a href="<%= ctx %>/staff/schedule" class="btn btn-secondary mt-3">Quay lại lịch sân</a>
      </div>
    <% } else if (booking == null || booking.isEmpty()) { %>
      <div class="card p-5 text-center shadow-sm border-0 rounded-4">
        <i class="bi bi-search display-3 text-muted"></i>
        <h3 class="mt-4 fw-bold">Nhập thông tin Checkout</h3>
        <p class="text-muted">Vui lòng chọn booking đang chơi từ Lịch sân hoặc Dashboard để thực hiện checkout.</p>
        <div class="mt-4">
          <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary">Xem lịch sân</a>
        </div>
      </div>
    <% } else {
        java.math.BigDecimal fieldCost = (java.math.BigDecimal) booking.get("totalAmount");
        java.math.BigDecimal deposit = (java.math.BigDecimal) booking.get("depositAmount");
        if (deposit == null) deposit = java.math.BigDecimal.ZERO;
    %>
      <div class="row g-4">
        <div class="col-lg-8">
          <div class="checkout-card">
            <h1 class="fw-bold mb-2">Checkout sân bóng</h1>
            <p class="text-muted mb-4">Mã lịch đặt: <strong class="text-dark">#<%= booking.get("bookingCode") %></strong></p>

            <div class="row g-3 mb-4">
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Khách hàng</label>
                <input class="form-control rounded-3" value="<%= booking.get("customerName") %>" readonly style="background:#f1f5f9;">
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold text-muted">Sân bóng</label>
                <input class="form-control rounded-3" value="<%= booking.get("fieldName") %>" readonly style="background:#f1f5f9;">
              </div>
            </div>

            <form id="checkout-form">
              <input type="hidden" id="bookingId" value="<%= booking.get("bookingId") %>">
              <input type="hidden" id="fieldCost" value="<%= fieldCost %>">
              <input type="hidden" id="depositAmount" value="<%= deposit %>">

              <div class="row g-3">
                <div class="col-md-6">
                  <label for="surcharge" class="form-label small fw-bold text-muted">Phụ phí phát sinh (₫)</label>
                  <input type="number" class="form-control form-control-lg rounded-3" id="surcharge" value="0" min="0" step="5000" oninput="calculateTotal()">
                </div>
                <div class="col-md-6">
                  <label for="note" class="form-label small fw-bold text-muted">Lý do phụ phí / Ghi chú</label>
                  <input type="text" class="form-control form-control-lg rounded-3" id="note" placeholder="Ví dụ: Thuê thêm bóng, nước uống...">
                </div>
              </div>
            </form>
          </div>
        </div>

        <aside class="col-lg-4">
          <div class="summary-box shadow-sm">
            <h5 class="fw-bold mb-4 pb-2 border-bottom"><i class="bi bi-wallet2 text-success me-2"></i>Tổng kết thanh toán</h5>
            
            <div class="d-flex justify-content-between mb-3">
              <span class="text-muted">Tiền thuê sân:</span>
              <strong class="text-dark"><%= String.format("%,d ₫", fieldCost.longValue()) %></strong>
            </div>
            
            <div class="d-flex justify-content-between mb-3 text-warning">
              <span>Đã cọc trước:</span>
              <strong>- <%= String.format("%,d ₫", deposit.longValue()) %></strong>
            </div>

            <div class="d-flex justify-content-between mb-3 text-danger">
              <span>Phụ phí phát sinh:</span>
              <strong id="summary-surcharge">0 ₫</strong>
            </div>

            <hr class="my-4">

            <div class="d-flex justify-content-between fs-5 fw-bold mb-4">
              <span>Cần thanh toán:</span>
              <span class="text-sf-primary" id="summary-total"><%= String.format("%,d ₫", fieldCost.subtract(deposit).longValue()) %></span>
            </div>

            <button type="button" class="btn btn-sf-primary btn-lg w-100 rounded-3 py-3" onclick="submitCheckout()">
              <i class="bi bi-check-circle me-2"></i>Hoàn tất & Tạo Hóa đơn
            </button>
            <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary w-100 rounded-3 mt-2">Hủy bỏ</a>
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
  function calculateTotal() {
    const cost = parseFloat(document.getElementById('fieldCost').value) || 0;
    const deposit = parseFloat(document.getElementById('depositAmount').value) || 0;
    const surcharge = parseFloat(document.getElementById('surcharge').value) || 0;

    const summarySurcharge = document.getElementById('summary-surcharge');
    const summaryTotal = document.getElementById('summary-total');

    summarySurcharge.textContent = surcharge.toLocaleString('vi-VN') + ' ₫';
    
    const remainingToPay = cost + surcharge - deposit;
    summaryTotal.textContent = Math.max(0, remainingToPay).toLocaleString('vi-VN') + ' ₫';
  }

  async function submitCheckout() {
    const bookingId = document.getElementById('bookingId').value;
    const cost = parseFloat(document.getElementById('fieldCost').value) || 0;
    const deposit = parseFloat(document.getElementById('depositAmount').value) || 0;
    const surcharge = parseFloat(document.getElementById('surcharge').value) || 0;
    const note = document.getElementById('note').value;

    const subtotal = cost + surcharge;
    const totalAmount = subtotal - deposit;
    const paidAmount = totalAmount;

    try {
      const params = new URLSearchParams();
      params.append('bookingId', bookingId);
      params.append('subtotal', subtotal.toString());
      params.append('discountAmount', '0');
      params.append('totalAmount', totalAmount.toString());
      params.append('paidAmount', paidAmount.toString());
      params.append('note', note);

      const res = await fetch('<%= ctx %>/api/staff/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });

      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();

      if (data.success) {
        alert('Checkout thành công!');
        window.location.href = '<%= ctx %>/staff/invoice?id=' + bookingId;
      } else {
        alert('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'));
      }
    } catch (err) {
      alert('Không thể thực hiện checkout: ' + err.message);
    }
  }
</script>
</body>
</html>

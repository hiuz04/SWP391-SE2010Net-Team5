<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.Map" %>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";

    Map<String, Object> invoice = (Map<String, Object>) request.getAttribute("invoice");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hóa đơn thanh toán | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; font-family: 'Inter', sans-serif; }
    .invoice-card { border-radius: 24px; border: 1px solid #e2e8f0; background: #fff; box-shadow: 0 10px 30px rgba(15,23,42,.04); padding: 48px; max-width: 800px; margin: auto; }
    .receipt-header { border-bottom: 2px dashed #cbd5e1; padding-bottom: 24px; margin-bottom: 24px; }
    .receipt-footer { border-top: 2px dashed #cbd5e1; padding-top: 24px; margin-top: 24px; text-align: center; }
    .btn-sf-primary {
      background-color: #16a34a;
      color: #ffffff;
    }
    .btn-sf-primary:hover {
      background-color: #15803d;
      color: #ffffff;
    }
    
    @media print {
      body { background: #fff !important; color: #000 !important; font-size: 12pt; }
      #navbar, #footer, .btn, .no-print { display: none !important; }
      .invoice-card { border: none !important; box-shadow: none !important; padding: 0 !important; margin: 0 !important; max-width: 100% !important; }
      .container { width: 100% !important; max-width: 100% !important; padding: 0 !important; margin: 0 !important; }
    }
  </style>
</head>
<body>
<div id="navbar" class="no-print" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active=""></div>

<main class="py-5">
  <div class="container">
    
    <% if (invoice == null || invoice.isEmpty()) { %>
      <div class="card p-5 text-center shadow-sm border-0 rounded-4 no-print">
        <i class="bi bi-receipt display-3 text-muted"></i>
        <h3 class="mt-4 fw-bold">Không tìm thấy hóa đơn</h3>
        <p class="text-muted">Vui lòng cung cấp mã đặt sân hợp lệ đã hoàn thành thanh toán.</p>
        <div class="mt-4">
          <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary">Lịch sân bóng</a>
        </div>
      </div>
    <% } else {
        java.math.BigDecimal subtotal = (java.math.BigDecimal) invoice.get("subtotal");
        java.math.BigDecimal discount = (java.math.BigDecimal) invoice.get("discountAmount");
        java.math.BigDecimal total = (java.math.BigDecimal) invoice.get("totalAmount");
        java.math.BigDecimal paid = (java.math.BigDecimal) invoice.get("paidAmount");
        
        if (discount == null) discount = java.math.BigDecimal.ZERO;
        if (subtotal == null) subtotal = total;
    %>
      <div class="invoice-card">
        
        <!-- Header -->
        <div class="d-flex justify-content-between align-items-start receipt-header">
          <div>
            <h2 class="fw-bold mb-1" style="font-weight:800;letter-spacing:-0.03em;"><span class="text-success">⚽</span> SPORT FIELD</h2>
            <p class="text-muted small mb-0">Hệ thống đặt sân bóng online tiện lợi</p>
          </div>
          <div class="text-end">
            <h4 class="fw-bold mb-1">HÓA ĐƠN</h4>
            <span class="badge bg-success-subtle text-success fw-bold px-3 py-2" style="border-radius:8px;">
              #<%= invoice.get("invoiceCode") %>
            </span>
          </div>
        </div>

        <!-- Details -->
        <div class="row g-3 mb-4">
          <div class="col-sm-6">
            <span class="text-muted small d-block">ĐƠN VỊ CUNG CẤP</span>
            <strong>Cơ sở: <%= invoice.get("facilityName") %></strong>
            <p class="text-muted small mb-0 mt-1">Hoà Lạc, Thạch Thất, Hà Nội</p>
          </div>
          <div class="col-sm-6 text-sm-end">
            <span class="text-muted small d-block">KHÁCH HÀNG</span>
            <strong><%= invoice.get("customerName") %></strong>
            <%
              Object issuedAtObj = invoice.get("issuedAt");
              String issuedAtStr = issuedAtObj != null ? issuedAtObj.toString() : "";
              if (issuedAtStr.length() > 16) issuedAtStr = issuedAtStr.substring(0, 16);
            %>
            <p class="text-muted small mb-0 mt-1">Ngày lập: <%= issuedAtStr %></p>
          </div>
        </div>

        <div class="table-responsive">
          <table class="table table-borderless align-middle">
            <thead class="table-light">
              <tr class="text-muted uppercase small">
                <th>Nội dung thanh toán</th>
                <th class="text-end">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <strong>Thuê sân bóng: <%= invoice.get("fieldName") %></strong>
                  <div class="text-muted small">Chi phí thuê sân và các phụ dịch phát sinh trong ca</div>
                </td>
                <td class="text-end fw-bold"><%= String.format("%,d ₫", subtotal.longValue()) %></td>
              </tr>
              <% if (discount.compareTo(java.math.BigDecimal.ZERO) > 0) { %>
                <tr class="text-success">
                  <td>Khuyến mãi / Giảm giá:</td>
                  <td class="text-end fw-bold">- <%= String.format("%,d ₫", discount.longValue()) %></td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>

        <hr class="my-4" style="border-top: 1px solid #cbd5e1;">

        <div class="row justify-content-end">
          <div class="col-md-5">
            <div class="d-flex justify-content-between mb-2">
              <span class="text-muted">Tạm tính:</span>
              <strong class="text-dark"><%= String.format("%,d ₫", subtotal.longValue()) %></strong>
            </div>
            <% if (discount.compareTo(java.math.BigDecimal.ZERO) > 0) { %>
              <div class="d-flex justify-content-between mb-2 text-success">
                <span>Khuyến mãi:</span>
                <strong>- <%= String.format("%,d ₫", discount.longValue()) %></strong>
              </div>
            <% } %>
            <div class="d-flex justify-content-between mb-2 fs-5 fw-bold text-sf-primary border-top pt-2">
              <span>Tổng thanh toán:</span>
              <span><%= String.format("%,d ₫", total.longValue()) %></span>
            </div>
          </div>
        </div>

        <!-- Receipt Footer -->
        <div class="receipt-footer">
          <p class="fw-bold mb-1">Cảm ơn quý khách đã sử dụng dịch vụ của chúng tôi!</p>
          <p class="text-muted small mb-0">Hóa đơn điện tử được xác thực và bảo mật.</p>
        </div>

        <div class="text-center mt-5 no-print d-flex justify-content-center gap-2">
          <button class="btn btn-sf-primary btn-lg px-4" onclick="window.print()">
            <i class="bi bi-printer me-2"></i>In hóa đơn
          </button>
          <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary btn-lg px-4">
            Quay lại lịch sân
          </a>
        </div>

      </div>
    <% } %>

  </div>
</main>

<div id="footer" class="no-print" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

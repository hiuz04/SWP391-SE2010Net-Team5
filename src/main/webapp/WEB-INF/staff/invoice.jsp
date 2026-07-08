<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.InvoiceView" %>
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

    InvoiceView invoice = (InvoiceView) request.getAttribute("invoice");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hóa đơn | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; font-family: 'Inter', sans-serif; }
    .invoice-card {
      max-width: 880px;
      margin: 0 auto;
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 20px;
      box-shadow: 0 8px 24px rgba(15,23,42,.04);
      padding: 42px;
    }
    .dash-line { border-top: 2px dashed #cbd5e1; }
    .ledger-row { display: flex; justify-content: space-between; gap: 20px; padding: 10px 0; }
    .btn-sf-primary { background-color: #16a34a; color: #fff; }
    .btn-sf-primary:hover { background-color: #15803d; color: #fff; }
    @media print {
      body { background: #fff !important; }
      #navbar, #footer, .no-print { display: none !important; }
      main { padding: 0 !important; }
      .container { max-width: none !important; padding: 0 !important; margin: 0 !important; }
      .invoice-card {
        border: 0 !important;
        box-shadow: none !important;
        border-radius: 0 !important;
        max-width: none !important;
        padding: 0 !important;
      }
    }
  </style>
</head>
<body>
<div id="navbar" class="no-print" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= esc(displayName) %>" data-active="Lịch trong ngày"></div>

<main class="py-5">
  <div class="container">
    <% if (error != null || invoice == null) { %>
      <div class="invoice-card text-center no-print">
        <i class="bi bi-receipt display-4 text-muted"></i>
        <h4 class="fw-bold mt-3">Không tìm thấy hóa đơn</h4>
        <p class="text-muted mb-4"><%= esc(error != null ? error : "Không tìm thấy hóa đơn") %></p>
        <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary px-4">Quay lại lịch sân</a>
      </div>
    <% } else { %>
      <div class="invoice-card" id="invoice-card">
        <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">
          <div>
            <h2 class="fw-bold mb-1"><span class="text-success">⚽</span> Sport Field Booking</h2>
            <div class="text-muted">Phiếu thanh toán dịch vụ sân bóng</div>
          </div>
          <div class="text-end">
            <div class="text-muted small fw-bold">HÓA ĐƠN</div>
            <h4 class="fw-bold mb-2">#<%= esc(invoice.getInvoiceCode()) %></h4>
            <span class="badge bg-success-subtle text-success fw-bold px-3 py-2"><%= esc(invoice.getInvoiceStatus()) %></span>
          </div>
        </div>

        <div class="dash-line my-4"></div>

        <div class="row g-4 mb-4">
          <div class="col-md-6">
            <div class="text-muted small fw-bold mb-1">CƠ SỞ</div>
            <div class="fw-bold"><%= esc(invoice.getFacilityName()) %></div>
            <div class="text-muted small"><%= esc(invoice.getFacilityAddress()) %></div>
          </div>
          <div class="col-md-6 text-md-end">
            <div class="text-muted small fw-bold mb-1">KHÁCH HÀNG</div>
            <div class="fw-bold"><%= esc(invoice.getCustomerName()) %></div>
            <div class="text-muted small"><%= esc(invoice.getCustomerPhone()) %></div>
          </div>
        </div>

        <div class="row g-4 mb-4">
          <div class="col-md-6">
            <div class="text-muted small fw-bold mb-1">BOOKING</div>
            <div class="fw-bold">#<%= esc(invoice.getBookingCode()) %></div>
            <div class="text-muted small"><%= esc(invoice.getFieldName()) %></div>
          </div>
          <div class="col-md-6 text-md-end">
            <div class="text-muted small fw-bold mb-1">THỜI GIAN</div>
            <div class="fw-bold"><%= esc(dateTime(invoice.getStartTime())) %></div>
            <div class="text-muted small">Đến <%= esc(dateTime(invoice.getEndTime())) %></div>
          </div>
        </div>

        <div class="table-responsive mb-4">
          <table class="table align-middle">
            <thead class="table-light">
              <tr>
                <th>Nội dung</th>
                <th class="text-end">Số tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <strong>Tiền thuê sân</strong>
                  <div class="text-muted small"><%= esc(invoice.getFieldName()) %></div>
                </td>
                <td class="text-end fw-bold"><%= money(invoice.getFieldFee()) %></td>
              </tr>
              <% if (invoice.getSurchargeAmount().compareTo(BigDecimal.ZERO) > 0) { %>
                <tr>
                  <td><strong>Phụ phí / dịch vụ phát sinh</strong></td>
                  <td class="text-end fw-bold"><%= money(invoice.getSurchargeAmount()) %></td>
                </tr>
              <% } %>
              <tr class="text-success">
                <td>Tiền cọc đã nhận</td>
                <td class="text-end fw-bold">- <%= money(invoice.getDepositAmount()) %></td>
              </tr>
              <% if (invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) { %>
                <tr class="text-success">
                  <td>Giảm giá</td>
                  <td class="text-end fw-bold">- <%= money(invoice.getDiscountAmount()) %></td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>

        <div class="row justify-content-end">
          <div class="col-md-6 col-lg-5">
            <div class="ledger-row">
              <span class="text-muted">Tạm tính</span>
              <strong><%= money(invoice.getSubtotal()) %></strong>
            </div>
            <div class="ledger-row text-success">
              <span>Cọc + giảm giá</span>
              <strong>- <%= money(invoice.getDepositAmount().add(invoice.getDiscountAmount())) %></strong>
            </div>
            <div class="ledger-row fs-5 fw-bold border-top mt-2 pt-3">
              <span>Đã thanh toán</span>
              <span class="text-success"><%= money(invoice.getPaidAmount()) %></span>
            </div>
          </div>
        </div>

        <div class="dash-line my-4"></div>

        <div class="d-flex justify-content-between flex-wrap gap-3 small text-muted">
          <div>Ngày lập: <strong class="text-dark"><%= esc(dateTime(invoice.getIssuedAt())) %></strong></div>
          <div>Nhân viên: <strong class="text-dark"><%= esc(invoice.getStaffName()) %></strong></div>
        </div>

        <div class="text-center mt-5 no-print d-flex justify-content-center gap-2 flex-wrap">
          <button type="button" class="btn btn-sf-primary btn-lg px-4" onclick="window.print()">
            <i class="bi bi-printer me-2"></i>In hóa đơn
          </button>
          <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary btn-lg px-4">Quay lại lịch</a>
          <a href="<%= ctx %>/staff/dashboard" class="btn btn-outline-secondary btn-lg px-4">Dashboard</a>
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

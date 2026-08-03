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
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    InvoiceView invoice = (InvoiceView) request.getAttribute("invoice");
    boolean pending = invoice != null && "PENDING".equals(invoice.getInvoiceStatus());
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hóa đơn trả sân | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; }
    .invoice-shell { max-width: 920px; margin: 0 auto; }
    .invoice-card {
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(15,23,42,.04);
      padding: 36px;
    }
    .ledger-row { display: flex; justify-content: space-between; gap: 20px; padding: 10px 0; }
    .dash-line { border-top: 2px dashed #cbd5e1; }
    .btn-sf-primary { background-color: #16a34a; color: #fff; }
    .btn-sf-primary:hover { background-color: #15803d; color: #fff; }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="customer" data-name="<%= esc(displayName) %>" data-active="Lịch sử đặt sân"></div>

<main class="py-5">
  <div class="container invoice-shell">
    <% if (invoice == null) { %>
      <div class="invoice-card text-center">
        <i class="bi bi-receipt display-4 text-muted"></i>
        <h4 class="fw-bold mt-3">Không tìm thấy hóa đơn</h4>
        <a class="btn btn-sf-primary mt-3" href="<%= ctx %>/booking?action=history">Lịch sử booking</a>
      </div>
    <% } else { %>
      <div class="invoice-card">
        <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">
          <div>
            <h2 class="fw-bold mb-1">Hóa đơn trả sân</h2>
            <div class="text-muted">Booking <strong>#<%= esc(invoice.getBookingCode()) %></strong></div>
          </div>
          <div class="text-end">
            <div class="text-muted small fw-bold">MÃ HÓA ĐƠN</div>
            <h4 class="fw-bold mb-2">#<%= esc(invoice.getInvoiceCode()) %></h4>
            <span class="badge <%= pending ? "bg-warning-subtle text-warning text-dark" : "bg-success-subtle text-success" %> fw-bold px-3 py-2">
              <%= pending ? "Chờ thanh toán" : "Đã thanh toán" %>
            </span>
          </div>
        </div>

        <div class="dash-line my-4"></div>

        <div class="row g-4 mb-4">
          <div class="col-md-6">
            <div class="text-muted small fw-bold mb-1">CỤM SÂN</div>
            <div class="fw-bold"><%= esc(invoice.getComplexName()) %></div>
            <div class="text-muted small"><%= esc(invoice.getComplexAddress()) %></div>
          </div>
          <div class="col-md-6 text-md-end">
            <div class="text-muted small fw-bold mb-1">SÂN</div>
            <div class="fw-bold"><%= esc(invoice.getFieldName()) %></div>
            <div class="text-muted small"><%= esc(dateTime(invoice.getStartTime())) %> - <%= esc(time(invoice.getEndTime())) %></div>
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
                  <strong>Tổng tiền sân</strong>
                  <div class="text-muted small"><%= esc(invoice.getFieldName()) %></div>
                </td>
                <td class="text-end fw-bold"><%= money(invoice.getFieldFee()) %></td>
              </tr>
              <tr>
                <td>
                  <strong>Phụ thu quá giờ</strong>
                  <div class="text-muted small"><%= invoice.getOvertimeMinutes() %> phút</div>
                </td>
                <td class="text-end fw-bold"><%= money(invoice.getOvertimeFee()) %></td>
              </tr>
              <tr class="text-success">
                <td>Tiền cọc đã thanh toán</td>
                <td class="text-end fw-bold">- <%= money(invoice.getDepositAmount()) %></td>
              </tr>
              <tr>
                <td class="fw-bold">Số tiền cần thanh toán</td>
                <td class="text-end fw-bold text-success"><%= money(invoice.getTotalAmount()) %></td>
              </tr>
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
              <span>Đã cọc</span>
              <strong>- <%= money(invoice.getDepositAmount()) %></strong>
            </div>
            <div class="ledger-row fs-5 fw-bold border-top mt-2 pt-3">
              <span>Còn lại</span>
              <span class="text-success"><%= money(invoice.getAmountDue()) %></span>
            </div>
          </div>
        </div>

        <div class="dash-line my-4"></div>

        <div class="row g-3">
          <div class="col-md-6">
            <div class="text-muted small fw-bold mb-1">TRẠNG THÁI THANH TOÁN</div>
            <div class="fw-bold"><%= pending ? "Đang chờ thanh toán" : "Đã thanh toán" %></div>
            <% if (invoice.getPaymentStatus() != null) { %>
            <div class="text-muted small">Giao dịch checkout: <%= esc(invoice.getPaymentStatus()) %></div>
            <% } %>
          </div>
          <div class="col-md-6 text-md-end">
            <div class="text-muted small fw-bold mb-1">PHƯƠNG THỨC</div>
            <div class="fw-bold"><%= esc(invoice.getPaymentMethodName() != null ? invoice.getPaymentMethodName() : (pending ? "Thanh toán online" : "")) %></div>
            <% if (invoice.getCheckoutPaidAt() != null) { %>
            <div class="text-muted small"><%= esc(dateTime(invoice.getCheckoutPaidAt())) %></div>
            <% } %>
          </div>
          <div class="col-12">
            <div class="table-responsive">
              <table class="table table-sm align-middle mb-0">
                <tbody>
                  <tr>
                    <td>Cọc<% if (invoice.getDepositPaymentMethodName() != null) { %> qua <%= esc(invoice.getDepositPaymentMethodName()) %><% } %></td>
                    <td class="text-end fw-bold"><%= money(invoice.getDepositAmount()) %></td>
                  </tr>
                  <tr>
                    <td><%= pending ? "Phần còn lại cần thanh toán online" : "Phần còn lại" %><% if (!pending && invoice.getPaymentMethodName() != null) { %> bằng <%= esc(invoice.getPaymentMethodName()) %><% } %></td>
                    <td class="text-end fw-bold"><%= money(pending ? invoice.getAmountDue() : invoice.getCheckoutPaymentAmount()) %></td>
                  </tr>
                  <tr class="table-light">
                    <td class="fw-bold">Tổng đã thanh toán</td>
                    <td class="text-end fw-bold text-success"><%= money(invoice.getDepositAmount().add(invoice.getPaidAmount())) %></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="dash-line my-4"></div>

        <div class="d-flex justify-content-between flex-wrap gap-3 small text-muted">
          <div>Ngày lập: <strong class="text-dark"><%= esc(dateTime(invoice.getIssuedAt())) %></strong></div>
          <div>Trạng thái booking: <strong class="text-dark"><%= esc(invoice.getBookingStatus()) %></strong></div>
        </div>

        <div class="text-center mt-5 d-flex justify-content-center gap-2 flex-wrap">
          <% if (pending) { %>
          <a href="<%= ctx %>/payment?action=method&type=checkout&invoiceId=<%= invoice.getInvoiceId() %>" class="btn btn-sf-primary btn-lg px-4">
            <i class="bi bi-credit-card me-2"></i>Thanh toán
          </a>
          <% } else { %>
          <span class="btn btn-outline-success btn-lg px-4 disabled"><i class="bi bi-check-circle me-2"></i>Đã thanh toán</span>
          <% } %>
          <a href="<%= ctx %>/booking?action=detail&id=<%= invoice.getBookingId() %>" class="btn btn-outline-secondary btn-lg px-4">Xem booking</a>
        </div>
      </div>
    <% } %>
  </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>

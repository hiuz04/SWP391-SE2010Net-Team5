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
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";

    InvoiceView invoice = (InvoiceView) request.getAttribute("invoice");
    String error = (String) request.getAttribute("error");
    boolean pending = invoice != null && "PENDING".equals(invoice.getInvoiceStatus());
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
      max-width: 920px;
      margin: 0 auto;
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(15,23,42,.04);
      padding: 42px;
    }
    .dash-line { border-top: 2px dashed #cbd5e1; }
    .ledger-row { display: flex; justify-content: space-between; gap: 20px; padding: 10px 0; }
    .btn-sf-primary { background-color: #16a34a; color: #fff; }
    .btn-sf-primary:hover { background-color: #15803d; color: #fff; }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= esc(displayName) %>" data-active="Lịch trong ngày"></div>

<main class="py-5">
  <div class="container">
    <% if (error != null || invoice == null) { %>
      <div class="invoice-card text-center">
        <i class="bi bi-receipt display-4 text-muted"></i>
        <h4 class="fw-bold mt-3">Không tìm thấy hóa đơn</h4>
        <p class="text-muted mb-4"><%= esc(error != null ? error : "Không tìm thấy hóa đơn") %></p>
        <a href="<%= ctx %>/staff/schedule" class="btn btn-sf-primary px-4">Quay lại lịch sân</a>
      </div>
    <% } else { %>
      <div class="invoice-card">
        <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">
          <div>
            <h2 class="fw-bold mb-1">Sport Field Booking</h2>
            <div class="text-muted">Hóa đơn trả sân</div>
          </div>
          <div class="text-end">
            <div class="text-muted small fw-bold">HÓA ĐƠN</div>
            <h4 class="fw-bold mb-2">#<%= esc(invoice.getInvoiceCode()) %></h4>
            <span class="badge <%= pending ? "bg-warning-subtle text-warning text-dark" : "bg-success-subtle text-success" %> fw-bold px-3 py-2">
              <%= pending ? "PENDING" : esc(invoice.getInvoiceStatus()) %>
            </span>
          </div>
        </div>

        <% if (pending) { %>
        <div class="alert alert-warning border-0">
          <i class="bi bi-hourglass-split me-2"></i>Đã gửi yêu cầu thanh toán cho khách. Booking sẽ hoàn tất sau khi customer thanh toán thành công.
        </div>
        <% } %>

        <div class="dash-line my-4"></div>

        <div class="row g-4 mb-4">
          <div class="col-md-6">
            <div class="text-muted small fw-bold mb-1">KHÁCH HÀNG</div>
            <div class="fw-bold"><%= esc(invoice.getCustomerName()) %></div>
            <div class="text-muted small"><%= esc(invoice.getCustomerPhone()) %></div>
          </div>
          <div class="col-md-6 text-md-end">
            <div class="text-muted small fw-bold mb-1">CƠ SỞ</div>
            <div class="fw-bold"><%= esc(invoice.getComplexName()) %></div>
            <div class="text-muted small"><%= esc(invoice.getComplexAddress()) %></div>
          </div>
        </div>

        <div class="row g-4 mb-4">
          <div class="col-md-4">
            <div class="text-muted small fw-bold mb-1">MÃ BOOKING</div>
            <div class="fw-bold">#<%= esc(invoice.getBookingCode()) %></div>
          </div>
          <div class="col-md-4">
            <div class="text-muted small fw-bold mb-1">SÂN</div>
            <div class="fw-bold"><%= esc(invoice.getFieldName()) %></div>
          </div>
          <div class="col-md-4 text-md-end">
            <div class="text-muted small fw-bold mb-1">THỜI GIAN</div>
            <div class="fw-bold"><%= esc(dateTime(invoice.getStartTime())) %></div>
            <div class="text-muted small"><%= esc(time(invoice.getStartTime())) %> - <%= esc(time(invoice.getEndTime())) %></div>
          </div>
        </div>

        <div class="table-responsive mb-4">
          <table class="table align-middle">
            <thead class="table-light">
              <tr>
                <th>Tổng kết thanh toán</th>
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
                <td>Số tiền customer cần thanh toán</td>
                <td class="text-end fw-bold"><%= money(invoice.getTotalAmount()) %></td>
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
              <span>Tiền cọc</span>
              <strong>- <%= money(invoice.getDepositAmount()) %></strong>
            </div>
            <div class="ledger-row fs-5 fw-bold border-top mt-2 pt-3">
              <span><%= pending ? "Còn chờ thanh toán" : "Đã thanh toán" %></span>
              <span class="text-success"><%= money(pending ? invoice.getAmountDue() : invoice.getPaidAmount()) %></span>
            </div>
          </div>
        </div>

        <div class="dash-line my-4"></div>

        <div class="d-flex justify-content-between flex-wrap gap-3 small text-muted">
          <div>Ngày lập: <strong class="text-dark"><%= esc(dateTime(invoice.getIssuedAt())) %></strong></div>
          <div>Nhân viên: <strong class="text-dark"><%= esc(invoice.getStaffName()) %></strong></div>
        </div>

        <div class="text-center mt-5 d-flex justify-content-center gap-2 flex-wrap">
          <% if (!pending) { %>
          <a href="<%= ctx %>/staff/invoice/export?id=<%= invoice.getBookingId() %>" class="btn btn-sf-primary btn-lg px-4">
            <i class="bi bi-file-earmark-arrow-down me-2"></i>Xuất hóa đơn
          </a>
          <% } %>
          <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary btn-lg px-4">Quay lại lịch</a>
          <a href="<%= ctx %>/staff/dashboard" class="btn btn-outline-secondary btn-lg px-4">Dashboard</a>
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

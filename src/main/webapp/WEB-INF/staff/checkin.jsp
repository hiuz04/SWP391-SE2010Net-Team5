<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.Map" %>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";

    Map<String, Object> booking = (Map<String, Object>) request.getAttribute("booking");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Check-in | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; font-family: 'Inter', sans-serif; }
    .checkin-card { border-radius: 24px; border: 1px solid #e2e8f0; background: #fff; box-shadow: 0 10px 30px rgba(15,23,42,.04); padding: 32px; }
    .qr-scanner-sim { background: #0f172a; border-radius: 18px; height: 200px; display: flex; align-items: center; justify-content: center; color: #38bdf8; border: 2px dashed #38bdf8; font-family: monospace; }
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
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Check-in"></div>

<main class="py-5">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-lg-6">
        <div class="checkin-card">
          <h2 class="fw-bold mb-1 text-center">Xác nhận Check-in khách</h2>
          <p class="text-muted text-center mb-4">Điền thông tin hoặc quét mã QR để xác nhận khách nhận sân.</p>

          <% if (booking != null && !booking.isEmpty()) { %>
            <!-- Booking details if check-in from link -->
            <div class="alert alert-info rounded-4 border-0 p-3 mb-4">
              <h5 class="fw-bold mb-2">Thông tin lịch đặt</h5>
              <div class="row g-2 small">
                <div class="col-6 text-muted">Mã lịch đặt:</div>
                <div class="col-6 fw-bold text-dark">#<%= booking.get("bookingCode") %></div>
                <div class="col-6 text-muted">Khách hàng:</div>
                <div class="col-6 fw-bold text-dark"><%= booking.get("customerName") %></div>
                <div class="col-6 text-muted">Sân bóng:</div>
                <div class="col-6 fw-bold text-dark"><%= booking.get("fieldName") %></div>
                <div class="col-6 text-muted">Cơ sở:</div>
                <div class="col-6 fw-bold text-dark"><%= booking.get("facilityName") %></div>
              </div>
            </div>

            <form id="checkin-form">
              <input type="hidden" name="bookingId" id="bookingId" value="<%= booking.get("bookingId") %>">
              <div class="mb-3">
                <label for="note" class="form-label small fw-bold text-muted">Ghi chú check-in</label>
                <textarea class="form-control rounded-3" id="note" name="note" rows="3" placeholder="Ví dụ: Khách thuê thêm 2 áo tập, mượn 1 quả bóng..."></textarea>
              </div>
              <div class="d-grid gap-2">
                <button type="button" class="btn btn-sf-primary btn-lg" onclick="submitCheckin()">Xác nhận nhận sân</button>
                <a href="<%= ctx %>/staff/schedule" class="btn btn-light">Quay lại lịch sân</a>
              </div>
            </form>
          <% } else { %>
            <!-- Manual input lookup code -->
            <div class="mb-4">
              <label for="lookupCode" class="form-label small fw-bold text-muted">Nhập mã đặt sân (Booking Code)</label>
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-hash"></i></span>
                <input type="text" class="form-control form-control-lg" id="lookupCode" placeholder="BK001">
                <button class="btn btn-sf-primary px-4" type="button" onclick="lookupBooking()">Tìm</button>
              </div>
            </div>

            <!-- Simulated QR scanner -->
            <div class="mb-4">
              <label class="form-label small fw-bold text-muted d-block text-center">Hoặc Quét QR Code</label>
              <div class="qr-scanner-sim">
                <div class="text-center">
                  <i class="bi bi-qr-code-scan display-4 d-block mb-2"></i>
                  <span>[ SIMULATED QR CAMERA ]</span>
                </div>
              </div>
            </div>

            <div class="text-center">
              <a href="<%= ctx %>/staff/schedule" class="btn btn-light w-100">Xem lịch sân trong ngày</a>
            </div>
          <% } %>

        </div>
      </div>
    </div>
  </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
  async function submitCheckin() {
    const bookingId = document.getElementById('bookingId').value;
    const note = document.getElementById('note').value;

    try {
      const params = new URLSearchParams();
      params.append('bookingId', bookingId);
      params.append('note', note);

      const res = await fetch('<%= ctx %>/api/staff/checkin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });

      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();

      if (data.success) {
        alert('Check-in thành công!');
        window.location.href = '<%= ctx %>/staff/schedule';
      } else {
        alert('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'));
      }
    } catch (err) {
      alert('Không thể thực hiện check-in: ' + err.message);
    }
  }

  function lookupBooking() {
    const code = document.getElementById('lookupCode').value.trim();
    if (!code) {
      alert('Vui lòng nhập mã đặt sân.');
      return;
    }
    alert('Chức năng tìm kiếm mã đang được đồng bộ. Vui lòng bấm Check-in từ Lịch sân hoặc Dashboard để thực hiện nhanh.');
  }
</script>
</body>
</html>

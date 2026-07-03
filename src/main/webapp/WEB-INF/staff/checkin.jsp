<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
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
  <title>Check-in Khách Hàng | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; font-family: 'Inter', sans-serif; }
    .checkin-container { max-width: 900px; margin: auto; }
    .search-card {
      border-radius: 20px;
      border: 1px solid #e2e8f0;
      background: #fff;
      box-shadow: 0 10px 30px rgba(15,23,42,.03);
      padding: 32px;
    }
    .booking-result-card {
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      background: #fff;
      box-shadow: 0 4px 15px rgba(15,23,42,.02);
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .booking-result-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(15,23,42,.06);
    }
    .btn-sf-primary {
      background-color: #16a34a;
      color: #ffffff;
    }
    .btn-sf-primary:hover {
      background-color: #15803d;
      color: #ffffff;
    }
    .time-badge {
      font-size: .85rem;
      font-weight: 700;
      background: #f0fdf4;
      color: #16a34a;
      border-radius: 8px;
      padding: 4px 10px;
    }
    .code-badge {
      font-size: .85rem;
      font-weight: 700;
      background: #f1f5f9;
      color: #475569;
      border-radius: 8px;
      padding: 4px 10px;
    }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Check-in"></div>

<main class="py-5">
  <div class="container checkin-container">
    
    <!-- Tiêu đề trang -->
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-3">
      <div>
        <h1 class="fw-bold mb-1"><i class="bi bi-person-check-fill text-success me-2"></i>Check-in Khách Nhận Sân</h1>
        <p class="text-muted mb-0">Tìm kiếm lịch đặt sân và làm thủ tục check-in nhanh chóng.</p>
      </div>
      <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary">
        <i class="bi bi-calendar2-week me-1"></i>Lịch sân hôm nay
      </a>
    </div>

    <!-- Search Form Card -->
    <div class="search-card mb-4">
      <h5 class="fw-bold mb-3">Tra cứu thông tin đặt sân</h5>
      <form id="search-form" onsubmit="handleSearchSubmit(event)">
        <div class="row g-3">
          <div class="col-md-9">
            <div class="input-group input-group-lg">
              <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
              <input type="text" class="form-control form-control-lg border-start-0 ps-0" id="searchQuery" 
                     placeholder="Nhập mã đặt sân (VD: BK001), tên khách hàng hoặc số điện thoại..." required>
            </div>
          </div>
          <div class="col-md-3">
            <button type="submit" class="btn btn-sf-primary btn-lg w-100 h-100 rounded-3">
              <i class="bi bi-funnel-fill me-1"></i>Tìm kiếm
            </button>
          </div>
        </div>
      </form>
    </div>

    <!-- Search Results Section -->
    <h5 class="fw-bold mb-3 d-none" id="results-title">Kết quả tìm kiếm</h5>
    <div id="loading-state" class="text-center py-4 d-none">
      <div class="spinner-border text-success" role="status"></div>
      <p class="mt-2 text-muted small">Đang tìm kiếm lịch đặt...</p>
    </div>
    <div id="results-container" class="d-flex flex-column gap-3">
      <!-- Results will be rendered here dynamically -->
    </div>

  </div>
</main>

<!-- Bootstrap Check-in Modal -->
<div class="modal fade" id="checkinModal" tabindex="-1" aria-labelledby="checkinModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content rounded-4 border-0 shadow">
      <div class="modal-header border-bottom-0 pb-0">
        <h5 class="modal-title fw-bold" id="checkinModalLabel">Xác nhận nhận sân</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body py-3">
        <div class="alert alert-success rounded-4 border-0 p-3 mb-3">
          <div class="row g-2 small">
            <div class="col-4 text-muted">Mã lịch đặt:</div>
            <div class="col-8 fw-bold text-dark" id="modal-code">#...</div>
            <div class="col-4 text-muted">Khách hàng:</div>
            <div class="col-8 fw-bold text-dark" id="modal-name">...</div>
            <div class="col-4 text-muted">Số điện thoại:</div>
            <div class="col-8 fw-bold text-dark" id="modal-phone">...</div>
            <div class="col-4 text-muted">Sân bóng:</div>
            <div class="col-8 fw-bold text-dark" id="modal-field">...</div>
            <div class="col-4 text-muted">Khung giờ:</div>
            <div class="col-8 fw-bold text-dark" id="modal-time">...</div>
          </div>
        </div>
        <form id="checkin-submit-form">
          <input type="hidden" id="modal-booking-id">
          <div class="mb-3">
            <label for="checkin-note" class="form-label small fw-bold text-muted">Ghi chú check-in</label>
            <textarea class="form-control rounded-3" id="checkin-note" rows="3" 
                      placeholder="Ví dụ: Khách thuê thêm 2 áo tập, mượn 1 quả bóng..."></textarea>
          </div>
        </form>
      </div>
      <div class="modal-footer border-top-0 pt-0">
        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button>
        <button type="button" class="btn btn-sf-primary px-4" onclick="submitCheckin()">Xác nhận nhận sân</button>
      </div>
    </div>
  </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
  let checkinModalInstance = null;

  document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('checkinModal');
    if (modalEl) {
      checkinModalInstance = new bootstrap.Modal(modalEl);
    }
    
    // Check if initial booking is pre-selected from URL parameter
    <% if (booking != null && !booking.isEmpty()) { %>
      const preSelectedBooking = {
        bookingId: <%= booking.get("bookingId") %>,
        bookingCode: "<%= booking.get("bookingCode") %>",
        customerName: "<%= booking.get("customerName") %>",
        customerPhone: "<%= booking.get("customerPhone") != null ? booking.get("customerPhone") : "" %>",
        fieldName: "<%= booking.get("fieldName") %>",
        startTime: "<%= booking.get("startTime") %>",
        endTime: "<%= booking.get("endTime") %>",
        totalAmount: <%= booking.get("totalAmount") %>
      };
      displayBookings([preSelectedBooking]);
    <% } %>
  });

  function handleSearchSubmit(event) {
    event.preventDefault();
    const query = document.getElementById('searchQuery').value.trim();
    if (!query) return;
    performSearch(query);
  }

  async function performSearch(query) {
    const resultsContainer = document.getElementById('results-container');
    const loadingState = document.getElementById('loading-state');
    const resultsTitle = document.getElementById('results-title');

    resultsContainer.innerHTML = '';
    loadingState.classList.remove('d-none');
    resultsTitle.classList.remove('d-none');

    try {
      const res = await fetch(`<%= ctx %>/api/staff/checkin/search?query=${encodeURIComponent(query)}`, {
        credentials: 'include'
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      
      const data = await res.json();
      loadingState.classList.add('d-none');

      if (data.error) {
        resultsContainer.innerHTML = `<div class="alert alert-danger rounded-4">${data.error}</div>`;
        return;
      }

      displayBookings(data);
    } catch (err) {
      loadingState.classList.add('d-none');
      resultsContainer.innerHTML = `<div class="alert alert-danger rounded-4">Lỗi hệ thống: ${err.message}</div>`;
    }
  }  function formatTime(dateTimeStr) {
    if (!dateTimeStr) return '';
    let t = dateTimeStr;
    if (t.includes(' ')) t = t.split(' ')[1];
    if (t.includes('.')) t = t.split('.')[0];
    return t.substring(0, 5);
  }

  function formatDate(dateTimeStr) {
    if (!dateTimeStr) return '';
    try {
      const parts = dateTimeStr.split(' ')[0].split('-');
      if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
      }
    } catch (e) {}
    return dateTimeStr.split(' ')[0] || '';
  }

  function formatMoney(amount) {
    if (amount == null) return '0 ₫';
    return Number(amount).toLocaleString('vi-VN') + ' ₫';
  }

  function isBookingExpired(endTimeStr) {
    if (!endTimeStr) return false;
    try {
      const isoStr = endTimeStr.replace(' ', 'T').substring(0, 19);
      const endDt = new Date(isoStr);
      const now = new Date();
      return endDt < now;
    } catch (e) {
      return false;
    }
  }

  let currentSearchResults = [];

  function displayBookings(bookings) {
    const resultsContainer = document.getElementById('results-container');
    const resultsTitle = document.getElementById('results-title');
    
    resultsTitle.classList.remove('d-none');
    currentSearchResults = bookings || [];
    
    if (!bookings || bookings.length === 0) {
      resultsContainer.innerHTML = `
        <div class="card border-0 rounded-4 p-5 text-center shadow-sm">
          <i class="bi bi-calendar-x display-4 text-muted"></i>
          <h5 class="mt-3 fw-bold">Không tìm thấy lịch đặt nào</h5>
          <p class="text-muted small mb-0">Không có lịch đặt sân nào ở trạng thái "Chờ check-in" khớp với thông tin tìm kiếm.</p>
        </div>`;
      return;
    }

    resultsContainer.innerHTML = bookings.map((b, index) => {
      const isExpired = isBookingExpired(b.endTime);
      let statusBadgeHtml = '';
      let actionBtnHtml = '';

      if (b.status === 'CHECKED_IN') {
        statusBadgeHtml = '<span class="badge bg-info-subtle text-info fw-bold"><i class="bi bi-play-circle me-1"></i>Đang chơi</span>';
        actionBtnHtml = `
          <button class="btn btn-outline-info btn-lg px-4 rounded-3" disabled style="min-width: 150px;">
            <i class="bi bi-check-all me-1"></i>Đã check-in
          </button>
        `;
      } else if (b.status === 'COMPLETED') {
        statusBadgeHtml = '<span class="badge bg-success-subtle text-success fw-bold"><i class="bi bi-check-circle me-1"></i>Đã xong</span>';
        actionBtnHtml = `
          <button class="btn btn-outline-secondary btn-lg px-4 rounded-3" disabled style="min-width: 150px;">
            <i class="bi bi-file-earmark-check me-1"></i>Hoàn thành
          </button>
        `;
      } else {
        if (isExpired) {
          statusBadgeHtml = '<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Quá giờ</span>';
          actionBtnHtml = `
            <button class="btn btn-secondary btn-lg px-4 rounded-3" disabled style="min-width: 150px;">
              <i class="bi bi-exclamation-circle me-1"></i>Quá giờ nhận
            </button>
          `;
        } else {
          statusBadgeHtml = '<span class="badge bg-warning-subtle text-warning fw-bold text-dark"><i class="bi bi-hourglass-split me-1"></i>Chờ check-in</span>';
          actionBtnHtml = `
            <button class="btn btn-sf-primary btn-lg px-4 rounded-3" onclick="openCheckinModalByIndex(${index})" style="min-width: 150px;">
              <i class="bi bi-person-check me-1"></i>Check-in
            </button>
          `;
        }
      }

      return `
        <div class="booking-result-card p-4">
          <div class="row align-items-center g-3">
            <div class="col-md-8">
              <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
                <span class="code-badge">${b.bookingCode}</span>
                <span class="badge bg-light text-dark border"><i class="bi bi-calendar3 me-1"></i>${formatDate(b.startTime)}</span>
                <span class="time-badge"><i class="bi bi-clock me-1"></i>${formatTime(b.startTime)} - ${formatTime(b.endTime)}</span>
                ${statusBadgeHtml}
              </div>
              <h5 class="fw-bold mb-1 text-dark">${b.fieldName}</h5>
              <div class="text-muted small">
                <span class="me-3"><i class="bi bi-person me-1"></i><strong>${b.customerName}</strong></span>
                <span><i class="bi bi-telephone me-1"></i>${b.customerPhone || 'Không có SĐT'}</span>
              </div>
              <div class="text-muted small mt-1">
                <span><i class="bi bi-cash me-1"></i>Tổng tiền: <strong class="text-success">${formatMoney(b.totalAmount)}</strong></span>
              </div>
            </div>
            <div class="col-md-4 text-md-end">
              ${actionBtnHtml}
            </div>
          </div>
        </div>`;
    }).join('');
  }
  function openCheckinModalByIndex(index) {
    const booking = currentSearchResults[index];
    if (booking) {
      openCheckinModal(booking);
    }
  }

  function openCheckinModal(booking) {
    document.getElementById('modal-booking-id').value = booking.bookingId;
    document.getElementById('modal-code').textContent = booking.bookingCode;
    document.getElementById('modal-name').textContent = booking.customerName;
    document.getElementById('modal-phone').textContent = booking.customerPhone || '—';
    document.getElementById('modal-field').textContent = booking.fieldName;
    document.getElementById('modal-time').textContent = `${formatTime(booking.startTime)} - ${formatTime(booking.endTime)}`;
    document.getElementById('checkin-note').value = '';
    
    if (checkinModalInstance) {
      checkinModalInstance.show();
    }
  }

  async function submitCheckin() {
    const bookingId = document.getElementById('modal-booking-id').value;
    const note = document.getElementById('checkin-note').value.trim();

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
        if (checkinModalInstance) checkinModalInstance.hide();
        alert('Check-in thành công!');
        window.location.href = '<%= ctx %>/staff/schedule';
      } else {
        alert('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'));
      }
    } catch (err) {
      alert('Không thể thực hiện check-in: ' + err.message);
    }
  }
</script>
</body>
</html>

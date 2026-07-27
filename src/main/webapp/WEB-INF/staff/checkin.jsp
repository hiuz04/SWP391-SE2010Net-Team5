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
  <title>Check-in/out | Sport Field Booking</title>
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
    .qr-video-wrap {
      position: relative;
      overflow: hidden;
      border-radius: 16px;
      background: #020617;
      aspect-ratio: 4 / 3;
    }
    #qr-video {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .qr-frame {
      position: absolute;
      inset: 14%;
      border: 3px solid rgba(34, 197, 94, .95);
      border-radius: 18px;
      box-shadow: 0 0 0 999px rgba(2, 6, 23, .42);
      pointer-events: none;
    }
    .qr-status {
      min-height: 24px;
      font-weight: 600;
    }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Check-in/out"></div>

<main class="py-5">
  <div class="container checkin-container">
    
    <!-- Tiêu đề trang -->
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-3">
      <div>
        <h1 class="fw-bold mb-1"><i class="bi bi-arrow-left-right text-success me-2"></i>Check-in/out</h1>
        <p class="text-muted mb-0">Tìm booking để nhận sân, trả sân hoặc xem hóa đơn theo trạng thái hiện tại.</p>
      </div>
      <a href="<%= ctx %>/staff/schedule" class="btn btn-outline-secondary">
        <i class="bi bi-calendar2-week me-1"></i>Lịch sân hôm nay
      </a>
    </div>

    <!-- Search Form Card -->
    <div class="search-card mb-4">
      <h5 class="fw-bold mb-3">Tra cứu booking Check-in/out</h5>
      <form id="search-form" onsubmit="handleSearchSubmit(event)">
        <div class="row g-3">
          <div class="col-lg-8">
            <div class="input-group input-group-lg">
              <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
              <input type="text" class="form-control form-control-lg border-start-0 ps-0" id="searchQuery" 
                     placeholder="Nhập mã booking, tên khách hàng hoặc số điện thoại..." required>
            </div>
          </div>
          <div class="col-sm-6 col-lg-2">
            <button type="submit" class="btn btn-sf-primary btn-lg w-100 h-100 rounded-3">
              <i class="bi bi-funnel-fill me-1"></i>Tìm kiếm
            </button>
          </div>
          <div class="col-sm-6 col-lg-2">
            <button type="button" class="btn btn-outline-success btn-lg w-100 h-100 rounded-3" onclick="openQrScanner()">
              <i class="bi bi-qr-code-scan me-1"></i>Quét QR
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

    <%
      @SuppressWarnings("unchecked")
      java.util.List<java.util.Map<String, Object>> pendingCheckinBookings = 
          (java.util.List<java.util.Map<String, Object>>) request.getAttribute("pendingCheckinBookings");
    %>
    <% if (booking == null && pendingCheckinBookings != null && !pendingCheckinBookings.isEmpty()) { 
         java.time.LocalDateTime nowTime = java.time.LocalDateTime.now();
         pendingCheckinBookings.sort((a, b) -> {
           String endA = a.get("endTime") != null ? a.get("endTime").toString() : "";
           String endB = b.get("endTime") != null ? b.get("endTime").toString() : "";
           boolean expA = false, expB = false;
           try {
             if (!endA.isEmpty()) expA = nowTime.isAfter(java.time.LocalDateTime.parse(endA.replace(" ", "T").substring(0, 19)));
             if (!endB.isEmpty()) expB = nowTime.isAfter(java.time.LocalDateTime.parse(endB.replace(" ", "T").substring(0, 19)));
           } catch (Exception ignored) {}

           if (expA != expB) {
             return expA ? 1 : -1;
           }
           String startA = a.get("startTime") != null ? a.get("startTime").toString() : "";
           String startB = b.get("startTime") != null ? b.get("startTime").toString() : "";
           return startA.compareTo(startB);
         });
    %>
      <div class="mt-4" id="default-pending-list">
        <h5 class="fw-bold mb-3"><i class="bi bi-clock-history text-warning me-2"></i>Danh sách Khách chờ Check-in hôm nay</h5>
        <div class="d-flex flex-column gap-3">
          <% for (java.util.Map<String, Object> b : pendingCheckinBookings) { 
               String bStart = b.get("startTime") != null ? b.get("startTime").toString() : "";
               String bEnd = b.get("endTime") != null ? b.get("endTime").toString() : "";
               if (bStart.contains(" ")) bStart = bStart.split(" ")[1];
               if (bStart.length() > 5) bStart = bStart.substring(0, 5);
               if (bEnd.contains(" ")) bEnd = bEnd.split(" ")[1];
               if (bEnd.length() > 5) bEnd = bEnd.substring(0, 5);

               java.math.BigDecimal tot = (java.math.BigDecimal) b.get("totalAmount");
               java.math.BigDecimal dep = (java.math.BigDecimal) b.get("depositAmount");
               long totVal = tot != null ? tot.longValue() : 0;
               long depVal = dep != null ? dep.longValue() : 0;

               boolean isExpired = false;
               boolean isLateNoShow = false;
               String rawStart = b.get("startTime") != null ? b.get("startTime").toString() : "";
               String rawEnd = b.get("endTime") != null ? b.get("endTime").toString() : "";
               if (!rawEnd.isEmpty()) {
                 try {
                   String isoEnd = rawEnd.replace(" ", "T");
                   if (isoEnd.contains(".")) {
                     isoEnd = isoEnd.substring(0, isoEnd.indexOf("."));
                   }
                   java.time.LocalDateTime endDt = java.time.LocalDateTime.parse(isoEnd);
                   isExpired = java.time.LocalDateTime.now().isAfter(endDt);
                 } catch (Exception ignored) {}
               }
               if (!rawStart.isEmpty()) {
                 try {
                   String isoStart = rawStart.replace(" ", "T");
                   if (isoStart.contains(".")) {
                     isoStart = isoStart.substring(0, isoStart.indexOf("."));
                   }
                   java.time.LocalDateTime startDt = java.time.LocalDateTime.parse(isoStart);
                   isLateNoShow = java.time.LocalDateTime.now().isAfter(startDt.plusMinutes(30)) && !isExpired;
                 } catch (Exception ignored) {}
               }
          %>
            <div class="booking-result-card p-4">
              <div class="row align-items-center g-3">
                <div class="col-md-8">
                  <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
                    <span class="code-badge"><%= b.get("bookingCode") %></span>
                    <span class="time-badge"><i class="bi bi-clock me-1"></i><%= bStart %> - <%= bEnd %></span>
                    <% if (isLateNoShow) { %>
                      <span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Muộn 30p</span>
                    <% } else if (isExpired) { %>
                      <span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Quá giờ</span>
                    <% } else { %>
                      <span class="badge bg-warning-subtle text-warning fw-bold text-dark"><i class="bi bi-hourglass-split me-1"></i>Chờ check-in</span>
                    <% } %>
                  </div>
                  <h5 class="fw-bold mb-1 text-dark"><%= b.get("fieldName") %></h5>
                  <div class="text-muted small">
                    <span class="me-3"><i class="bi bi-person me-1"></i><strong><%= b.get("customerName") %></strong></span>
                    <span><i class="bi bi-telephone me-1"></i><%= (b.get("customerPhone") != null && !b.get("customerPhone").toString().trim().isEmpty() && !"null".equalsIgnoreCase(b.get("customerPhone").toString().trim())) ? b.get("customerPhone") : "Không có SĐT" %></span>
                  </div>
                  <div class="text-muted small mt-1">
                    <span class="me-3"><i class="bi bi-cash me-1"></i>Tổng tiền: <strong class="text-success"><%= String.format("%,d ₫", totVal) %></strong></span>
                    <span><i class="bi bi-wallet2 me-1"></i>Cọc: <strong><%= String.format("%,d ₫", depVal) %></strong></span>
                  </div>
                </div>
                <div class="col-md-4 text-md-end">
                  <% if (isExpired) { %>
                    <button type="button" class="btn btn-secondary btn-lg px-4 rounded-3" disabled title="Khách quá giờ nhận sân" style="min-width: 150px;">
                      <i class="bi bi-exclamation-circle me-1"></i>Quá giờ nhận
                    </button>
                  <% } else { %>
                    <button type="button" class="btn btn-sf-primary btn-lg px-4 rounded-3" style="min-width: 150px;" onclick="openCheckinModalByData(<%= b.get("bookingId") %>, '<%= b.get("bookingCode") %>', '<%= b.get("customerName") %>', '<%= b.get("customerPhone") %>', '<%= b.get("fieldName") %>', '<%= bStart %>', '<%= bEnd %>', <%= totVal %>, <%= depVal %>)">
                      <i class="bi bi-person-check me-1"></i>Check-in
                    </button>
                  <% } %>
                </div>
              </div>
            </div>
          <% } %>
        </div>
      </div>
    <% } %>

  </div>
</main>

<!-- QR Scanner Modal -->
<div class="modal fade" id="qrScannerModal" tabindex="-1" aria-labelledby="qrScannerModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content rounded-4 border-0 shadow">
      <div class="modal-header border-bottom-0 pb-0">
        <h5 class="modal-title fw-bold" id="qrScannerModalLabel"><i class="bi bi-qr-code-scan text-success me-2"></i>Quét QR Check-in/out</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <div class="row g-4 align-items-stretch">
          <div class="col-lg-7">
            <div class="qr-video-wrap">
              <video id="qr-video" playsinline muted></video>
              <div class="qr-frame"></div>
            </div>
            <div class="qr-status small text-muted mt-3" id="qr-status">Đang chờ camera...</div>
          </div>
          <div class="col-lg-5">
            <div class="h-100 d-flex flex-column justify-content-between gap-3">
              <div>
                <label for="qr-manual-code" class="form-label small fw-bold text-muted">Mã booking từ QR</label>
                <div class="input-group">
                  <span class="input-group-text bg-white"><i class="bi bi-ticket-perforated"></i></span>
                  <input type="text" class="form-control" id="qr-manual-code" placeholder="BK...">
                </div>
              </div>
              <div class="d-grid gap-2">
                <button type="button" class="btn btn-sf-primary" onclick="submitManualQrCode()">
                  <i class="bi bi-search me-1"></i>Kiểm tra mã
                </button>
                <button type="button" class="btn btn-outline-secondary" onclick="restartQrScanner()">
                  <i class="bi bi-camera-video me-1"></i>Bật lại camera
                </button>
              </div>
              <div class="alert alert-light border rounded-4 small mb-0">
                Hệ thống chỉ hiển thị booking và hành động phù hợp sau khi server kiểm tra mã QR hợp lệ.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- Check-in Confirmation Modal with Notes & Details (Image 2 Modern Style) -->
<div class="modal fade" id="checkinModal" tabindex="-1" aria-labelledby="checkinModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content border-0 shadow-lg" style="border-radius: 24px; overflow: hidden; background: #ffffff;">
      <div class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-circle p-2 text-success d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-color: #dcfce7;">
            <i class="bi bi-box-arrow-in-right fs-4" style="color: #16a34a;"></i>
          </div>
          <div>
            <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="checkinModalLabel">Xác nhận nhận sân (Check-in)</h5>
            <span class="text-muted small" style="font-size: 0.8rem;">Mã đặt sân: <strong class="text-success" id="modal-code">—</strong></span>
          </div>
        </div>
        <button type="button" class="btn-close bg-light rounded-circle p-2" data-bs-dismiss="modal" aria-label="Close" style="font-size: 0.8rem;"></button>
      </div>

      <div class="modal-body p-4">
        <div id="modal-alert" class="d-none"></div>
        <input type="hidden" id="modal-booking-id">
        <div class="row g-4">
          <div class="col-md-7">
            <div class="mb-3">
              <h6 class="fw-bold text-muted uppercase small mb-2 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-person-fill me-2" style="color: #16a34a;"></i>THÔNG TIN KHÁCH HÀNG</h6>
              <div class="p-3 bg-light rounded-4 border border-light-subtle" style="background-color: #f8fafc !important;">
                <div class="mb-2">
                  <span class="text-muted small d-block" style="font-size: 0.75rem;">Tên khách hàng</span>
                  <span class="fw-bold text-dark fs-6" id="modal-name">—</span>
                </div>
                <div>
                  <span class="text-muted small d-block" style="font-size: 0.75rem;">Số điện thoại</span>
                  <span class="fw-bold text-success fs-6" id="modal-phone">—</span>
                </div>
              </div>
            </div>

            <div class="mb-3">
              <h6 class="fw-bold text-muted uppercase small mb-2 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-heptagon-fill me-2" style="color: #16a34a;"></i>SÂN & KHUNG GIỜ</h6>
              <div class="p-3 bg-light rounded-4 border border-light-subtle" style="background-color: #f8fafc !important;">
                <div class="row g-2">
                  <div class="col-6">
                    <span class="text-muted small d-block" style="font-size: 0.75rem;">Sân bóng</span>
                    <span class="fw-bold text-dark" id="modal-field">—</span>
                  </div>
                  <div class="col-6">
                    <span class="text-muted small d-block" style="font-size: 0.75rem;">Khung giờ</span>
                    <span class="fw-bold text-dark" id="modal-time">—</span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <label for="checkin-note" class="form-label fw-bold text-muted small mb-1"><i class="bi bi-journal-text me-1 text-success"></i>Ghi chú nhận sân (Tùy chọn)</label>
              <textarea class="form-control rounded-3" id="checkin-note" rows="2" placeholder="Ví dụ: Khách thuê thêm 2 áo tập, mượn 1 quả bóng..."></textarea>
            </div>
          </div>

          <div class="col-md-5">
            <h6 class="fw-bold text-muted uppercase small mb-2 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-receipt-cutoff me-2" style="color: #16a34a;"></i>THANH TOÁN HÔM NAY</h6>
            <div class="p-4 rounded-4 shadow-sm border border-success-subtle d-flex flex-column justify-content-between" style="background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%); border-color: #bbf7d0 !important; border: 1px solid; min-height: 230px;">
              <div>
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <span class="text-muted small">Giá gốc sân:</span>
                  <span class="fw-bold text-dark" id="modal-total">—</span>
                </div>
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <span class="text-muted small">Đã đặt cọc:</span>
                  <span class="fw-bold text-danger" id="modal-deposit">—</span>
                </div>
                <hr class="my-3 border-secondary-subtle">
              </div>
              <div class="text-center py-2">
                <span class="text-muted small d-block mb-1" style="font-size: 0.72rem; letter-spacing: 0.05em; font-weight: 700;">CẦN THANH TOÁN CÒN LẠI</span>
                <span class="fw-bold text-success display-6" style="font-weight: 800; font-size: 1.8rem;" id="modal-remaining">—</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 pt-0 d-flex justify-content-end gap-2" id="modal-footer">
        <button type="button" class="btn btn-light px-4 py-2 rounded-3" data-bs-dismiss="modal">Đóng</button>
        <div id="modal-actions" class="d-flex gap-2">
          <button type="button" class="btn btn-success px-4 py-2 rounded-3" id="modal-submit-btn" onclick="submitCheckin()"><i class="bi bi-check-circle me-1"></i>Xác nhận Check-in</button>
        </div>
      </div>
    </div>
  </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
  let checkinModalInstance = null;
  let qrScannerModalInstance = null;
  let qrStream = null;
  let qrDetector = null;
  let qrScanTimer = null;
  let qrServerDecodeBusy = false;
  let qrScanLocked = false;

  document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('checkinModal');
    if (modalEl) {
      checkinModalInstance = new bootstrap.Modal(modalEl);
    }

    const qrModalEl = document.getElementById('qrScannerModal');
    if (qrModalEl) {
      qrScannerModalInstance = new bootstrap.Modal(qrModalEl);
      qrModalEl.addEventListener('hidden.bs.modal', stopQrScanner);
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
        status: "<%= booking.get("status") != null ? booking.get("status") : "" %>",
        totalAmount: <%= booking.get("totalAmount") %>,
        depositAmount: <%= booking.get("depositAmount") != null ? booking.get("depositAmount") : 0 %>,
        paymentStatus: "<%= booking.get("paymentStatus") != null ? booking.get("paymentStatus") : "" %>",
        hasInvoice: <%= Boolean.TRUE.equals(booking.get("hasInvoice")) ? "true" : "false" %>,
        checkoutDue: <%= Boolean.TRUE.equals(booking.get("checkoutDue")) ? "true" : "false" %>,
        bookingToday: <%= Boolean.TRUE.equals(booking.get("bookingToday")) ? "true" : "false" %>,
        notExpired: <%= Boolean.TRUE.equals(booking.get("notExpired")) ? "true" : "false" %>
      };
      displayBookings([preSelectedBooking]);
    <% } else { %>
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get('pending') === 'true') {
        loadPendingBookings();
      }
    <% } %>
  });

  async function openQrScanner() {
    document.getElementById('qr-manual-code').value = '';
    setQrStatus('Đang mở camera...', 'muted');
    qrScanLocked = false;

    if (qrScannerModalInstance) {
      qrScannerModalInstance.show();
    }

    await startQrScanner();
  }

  async function restartQrScanner() {
    stopQrScanner();
    qrScanLocked = false;
    await startQrScanner();
  }

  async function startQrScanner() {
    const video = document.getElementById('qr-video');
    if (!video) return;

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setQrStatus('Trình duyệt không hỗ trợ camera. Vui lòng nhập mã booking.', 'danger');
      return;
    }

    try {
      qrStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false
      });
      video.srcObject = qrStream;
      await video.play();

      if ('BarcodeDetector' in window) {
        qrDetector = new BarcodeDetector({ formats: ['qr_code'] });
        setQrStatus('Camera đang quét QR...', 'success');
        scanWithBarcodeDetector();
      } else {
        qrDetector = null;
        setQrStatus('Camera đang quét QR...', 'success');
        scanWithServerDecoder();
      }
    } catch (err) {
      setQrStatus('Không thể mở camera: ' + err.message, 'danger');
    }
  }

  function stopQrScanner() {
    if (qrScanTimer) {
      clearTimeout(qrScanTimer);
      qrScanTimer = null;
    }
    if (qrStream) {
      qrStream.getTracks().forEach(track => track.stop());
      qrStream = null;
    }
    const video = document.getElementById('qr-video');
    if (video) {
      video.pause();
      video.srcObject = null;
    }
    qrServerDecodeBusy = false;
  }

  async function scanWithBarcodeDetector() {
    const video = document.getElementById('qr-video');
    if (!qrStream || !video || qrScanLocked) return;

    try {
      if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
        const codes = await qrDetector.detect(video);
        if (codes && codes.length > 0 && codes[0].rawValue) {
          const handled = await handleQrBookingCode(codes[0].rawValue);
          if (handled) {
            return;
          }
        }
      }
    } catch (err) {
      setQrStatus('Không đọc được QR trên khung hình hiện tại.', 'warning');
    }

    qrScanTimer = setTimeout(scanWithBarcodeDetector, 250);
  }

  async function scanWithServerDecoder() {
    if (!qrStream || qrScanLocked) return;
    const handled = await decodeCurrentFrameOnServer();
    if (!handled && qrStream && !qrScanLocked) {
      qrScanTimer = setTimeout(scanWithServerDecoder, 700);
    }
  }

  async function decodeCurrentFrameOnServer() {
    if (qrServerDecodeBusy || qrScanLocked) return false;

    const video = document.getElementById('qr-video');
    if (!video || video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) return false;

    qrServerDecodeBusy = true;
    try {
      const canvas = document.createElement('canvas');
      const ratio = video.videoWidth > 0 ? Math.min(1, 640 / video.videoWidth) : 1;
      canvas.width = Math.max(1, Math.floor(video.videoWidth * ratio));
      canvas.height = Math.max(1, Math.floor(video.videoHeight * ratio));
      const ctx2d = canvas.getContext('2d', { willReadFrequently: true });
      ctx2d.drawImage(video, 0, 0, canvas.width, canvas.height);

      const params = new URLSearchParams();
      params.append('imageData', canvas.toDataURL('image/jpeg', 0.72));

      const res = await fetch('<%= ctx %>/api/staff/checkin/qr-decode', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });

      if (res.ok) {
        const data = await res.json();
        if (data.found && data.bookingCode) {
          return await handleQrBookingCode(data.bookingCode);
        }
      }
    } catch (err) {
      setQrStatus('Camera đang quét QR...', 'success');
    } finally {
      qrServerDecodeBusy = false;
    }
    return false;
  }

  async function submitManualQrCode() {
    const code = document.getElementById('qr-manual-code').value.trim();
    if (!code) {
      setQrStatus('Vui lòng nhập mã booking.', 'warning');
      return;
    }
    await handleQrBookingCode(code);
  }

  async function handleQrBookingCode(rawCode) {
    const bookingCode = normalizeQrCode(rawCode);
    if (!bookingCode) {
      setQrStatus('QR không chứa mã booking hợp lệ.', 'danger');
      return false;
    }

    qrScanLocked = true;
    setQrStatus('Đã đọc mã ' + bookingCode + '. Đang kiểm tra...', 'success');
    document.getElementById('qr-manual-code').value = bookingCode;

    try {
      const booking = await previewQrBooking(bookingCode);
      stopQrScanner();
      if (qrScannerModalInstance) {
        qrScannerModalInstance.hide();
      }
      document.getElementById('searchQuery').value = booking.bookingCode || bookingCode;
      displayBookings([booking]);
      showToast('Đã tìm thấy booking từ QR. Vui lòng chọn hành động phù hợp.', 'success');
      return true;
    } catch (err) {
      qrScanLocked = false;
      setQrStatus(err.message, 'danger');
      showToast(err.message, 'danger');
      return false;
    }
  }

  async function previewQrBooking(bookingCode) {
    const res = await fetch(`<%= ctx %>/api/staff/checkin/qr-preview?bookingCode=${encodeURIComponent(bookingCode)}`, {
      credentials: 'include'
    });
    const data = await res.json();
    if (!res.ok || data.error) {
      throw new Error(data.error || 'Không thể kiểm tra mã QR.');
    }
    if (!data.booking) {
      throw new Error('Không tìm thấy thông tin booking từ QR.');
    }
    return data.booking;
  }

  function normalizeQrCode(rawValue) {
    if (!rawValue) return '';
    let value = String(rawValue).trim();
    if (value.startsWith('#')) {
      value = value.substring(1).trim();
    }
    return value;
  }

  function setQrStatus(message, type) {
    const statusEl = document.getElementById('qr-status');
    if (!statusEl) return;
    const color = type === 'danger' ? 'text-danger'
      : type === 'warning' ? 'text-warning'
      : type === 'success' ? 'text-success'
      : 'text-muted';
    statusEl.className = 'qr-status small mt-3 ' + color;
    statusEl.textContent = message;
  }

  async function loadPendingBookings() {
    const resultsContainer = document.getElementById('results-container');
    const loadingState = document.getElementById('loading-state');
    const resultsTitle = document.getElementById('results-title');

    resultsContainer.innerHTML = '';
    loadingState.classList.remove('d-none');
    
    if (resultsTitle) {
      resultsTitle.textContent = "Booking cần xử lý trong ca";
      resultsTitle.classList.remove('d-none');
    }

    try {
      const res = await fetch(`<%= ctx %>/api/staff/checkin/search?pendingOnly=true`, {
        credentials: 'include'
      });
            if (!res.ok) {
        let errMsg = 'HTTP ' + res.status;
        try {
          const errData = await res.json();
          if (errData.error) errMsg = errData.error;
        } catch(e) {}
        throw new Error(errMsg);
      }

      
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
  }

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
            if (!res.ok) {
        let errMsg = 'HTTP ' + res.status;
        try {
          const errData = await res.json();
          if (errData.error) errMsg = errData.error;
        } catch(e) {}
        throw new Error(errMsg);
      }

      
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

  function parseLocalDate(dtStr) {
    if (!dtStr) return null;
    try {
      let str = String(dtStr).trim();
      if (str.includes(' - ')) str = str.split(' - ')[0].trim();
      if (str.includes(' – ')) str = str.split(' – ')[0].trim();
      if (str.includes('.')) str = str.split('.')[0];
      str = str.replace('T', ' ');
      if (!str.includes('-') && !str.includes('/')) {
        const today = new Date();
        const parts = str.split(':');
        if (parts.length >= 2) {
          return new Date(today.getFullYear(), today.getMonth(), today.getDate(), parseInt(parts[0], 10), parseInt(parts[1], 10), parseInt(parts[2] || 0, 10));
        }
        return null;
      }
      const dateParts = str.split(' ');
      if (dateParts.length >= 2) {
        const ym = dateParts[0].split(dateParts[0].includes('-') ? '-' : '/');
        const hms = dateParts[1].split(':');
        return new Date(parseInt(ym[0], 10), parseInt(ym[1], 10) - 1, parseInt(ym[2], 10), parseInt(hms[0], 10), parseInt(hms[1], 10), parseInt(hms[2] || 0, 10));
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  function isBookingExpired(endTimeStr) {
    if (!endTimeStr) return false;
    const endDt = parseLocalDate(endTimeStr);
    if (!endDt) return false;
    return new Date() > endDt;
  }

  function isBookingLateNoShow(startTimeStr, endTimeStr) {
    if (!startTimeStr) return false;
    const startDt = parseLocalDate(startTimeStr);
    if (!startDt) return false;
    const now = new Date();
    const lateThreshold = new Date(startDt.getTime() + 30 * 60 * 1000);
    const endDt = parseLocalDate(endTimeStr);
    return now > lateThreshold && (!endDt || now <= endDt);
  }

  let currentSearchResults = [];

  function displayBookings(bookings) {
    const resultsContainer = document.getElementById('results-container');
    const resultsTitle = document.getElementById('results-title');
    
    resultsTitle.classList.remove('d-none');
    
    if (!bookings || bookings.length === 0) {
      currentSearchResults = [];
      resultsContainer.innerHTML = `
        <div class="card border-0 rounded-4 p-5 text-center shadow-sm">
          <i class="bi bi-calendar-x display-4 text-muted"></i>
          <h5 class="mt-3 fw-bold">Không tìm thấy lịch đặt nào</h5>
          <p class="text-muted small mb-0">Không có booking check-in/out nào khớp với thông tin tìm kiếm.</p>
        </div>`;
      return;
    }

    // Sort bookings: actionable check-in/checkout first, then invoices, then inactive states.
    const sortedBookings = [...bookings].sort((x, y) => {
      const priority = bookingActionPriority(x) - bookingActionPriority(y);
      if (priority !== 0) return priority;
      return String(y.startTime || '').localeCompare(String(x.startTime || ''));
    });

    currentSearchResults = sortedBookings;

    resultsContainer.innerHTML = sortedBookings.map((b, index) => {
      const isExpired = isBookingExpired(b.endTime);
      const statusBadgeHtml = statusBadge(b, isExpired);
      const actionBtnHtml = resultActionHtml(b, index, isExpired);

      return `
        <div class="booking-result-card p-4">
          <div class="row align-items-center g-3">
            <div class="col-md-8">
              <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
                <span class="code-badge">${escapeHtml(b.bookingCode || '')}</span>
                <span class="badge bg-light text-dark border"><i class="bi bi-calendar3 me-1"></i>${formatDate(b.startTime)}</span>
                <span class="time-badge"><i class="bi bi-clock me-1"></i>${formatTime(b.startTime)} - ${formatTime(b.endTime)}</span>
                ${statusBadgeHtml}
              </div>
              <h5 class="fw-bold mb-1 text-dark">${escapeHtml(b.fieldName || '—')}</h5>
              <div class="text-muted small">
                <span class="me-3"><i class="bi bi-person me-1"></i><strong>${escapeHtml(b.customerName || '—')}</strong></span>
                <span><i class="bi bi-telephone me-1"></i>${escapeHtml(b.customerPhone || 'Không có SĐT')}</span>
              </div>
              <div class="text-muted small mt-1">
                <span class="me-3"><i class="bi bi-cash me-1"></i>Tổng tiền: <strong class="text-success">${formatMoney(b.totalAmount)}</strong></span>
                <span><i class="bi bi-wallet2 me-1"></i>Cọc: <strong>${formatMoney(b.depositAmount)}</strong></span>
              </div>
            </div>
            <div class="col-md-4 text-md-end">
              ${actionBtnHtml}
            </div>
          </div>
        </div>`;
    }).join('');
  }

  function bookingActionPriority(booking) {
    const status = booking.status || '';
    const expired = isBookingExpired(booking.endTime);
    if (canCheckin(booking, expired)) return 0;
    if (status === 'CHECKED_IN') return 1;
    if ((status === 'PENDING_CHECKOUT_PAYMENT' || status === 'COMPLETED') && booking.hasInvoice) return 2;
    return 3;
  }

  function canCheckin(booking, expired) {
    return booking.status === 'CONFIRMED'
      && booking.bookingToday !== false
      && !expired;
  }
  function isBookingLateNoShow(startTimeStr, endTimeStr) {
    if (!startTimeStr) return false;
    const startDt = parseLocalDate(startTimeStr);
    if (!startDt) return false;
    const now = new Date();
    const lateThreshold = new Date(startDt.getTime() + 30 * 60 * 1000);
    const endDt = parseLocalDate(endTimeStr);
    return now > lateThreshold && (!endDt || now <= endDt);
  }

  function statusBadge(booking, expired) {
    const status = booking.status || '';
    if (status === 'CONFIRMED') {
      if (booking.bookingToday === false) {
        return '<span class="badge bg-secondary-subtle text-secondary fw-bold"><i class="bi bi-calendar3 me-1"></i>Khác ngày</span>';
      }
      if (isBookingLateNoShow(booking.startTime, booking.endTime)) {
        return '<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Muộn 30p</span>';
      }
      if (booking.notExpired === false || expired) {
        return '<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Quá giờ</span>';
      }
      return '<span class="badge bg-warning-subtle text-warning fw-bold text-dark"><i class="bi bi-hourglass-split me-1"></i>Chờ check-in</span>';
    }
    if (status === 'CHECKED_IN') {
      return '<span class="badge bg-info-subtle text-info fw-bold"><i class="bi bi-play-circle me-1"></i>Đang chơi</span>';
    }
    if (status === 'PENDING_CHECKOUT_PAYMENT') {
      return '<span class="badge fw-bold" style="background:#fae8ff;color:#a21caf;"><i class="bi bi-credit-card me-1"></i>Chờ thanh toán</span>';
    }
    if (status === 'COMPLETED') {
      return '<span class="badge bg-success-subtle text-success fw-bold"><i class="bi bi-check-circle me-1"></i>Đã xong</span>';
    }
    if (status === 'HOLD') {
      return '<span class="badge bg-primary-subtle text-primary fw-bold"><i class="bi bi-hourglass me-1"></i>Chờ cọc</span>';
    }
    if (status === 'CANCELLED') {
      return '<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-x-circle me-1"></i>Đã hủy</span>';
    }
    if (status === 'EXPIRED') {
      return '<span class="badge bg-secondary-subtle text-secondary fw-bold"><i class="bi bi-clock-history me-1"></i>Hết hạn</span>';
    }
    return `<span class="badge bg-light text-dark border">${escapeHtml(status || 'Không rõ')}</span>`;
  }

  function resultActionHtml(booking, index, expired) {
    const minWidth = 'min-width: 150px;';
    if (canCheckin(booking, expired)) {
      return `
        <button class="btn btn-sf-primary btn-lg px-4 rounded-3" onclick="openCheckinModalByIndex(${index})" style="${minWidth}">
          <i class="bi bi-person-check me-1"></i>Check-in
        </button>`;
    }
    if (booking.status === 'CHECKED_IN') {
      return `
        <a class="btn btn-outline-success btn-lg px-4 rounded-3" href="<%= ctx %>/staff/checkout?id=${booking.bookingId}" style="${minWidth}">
          <i class="bi bi-receipt-cutoff me-1"></i>Checkout
        </a>`;
    }
    if ((booking.status === 'PENDING_CHECKOUT_PAYMENT' || booking.status === 'COMPLETED') && booking.hasInvoice) {
      return `
        <a class="btn btn-outline-secondary btn-lg px-4 rounded-3" href="<%= ctx %>/staff/invoice?id=${booking.bookingId}" style="${minWidth}">
          <i class="bi bi-file-earmark-text me-1"></i>Hóa đơn
        </a>`;
    }

    let label = 'Không thể xử lý';
    let icon = 'bi-slash-circle';
    if (booking.status === 'CONFIRMED' && booking.bookingToday === false) {
      label = 'Khác ngày';
      icon = 'bi-calendar3';
    } else if (booking.status === 'CONFIRMED') {
      label = 'Quá giờ nhận';
      icon = 'bi-exclamation-circle';
    } else if (booking.status === 'HOLD') {
      label = 'Chờ cọc';
      icon = 'bi-hourglass';
    } else if (booking.status === 'CANCELLED') {
      label = 'Đã hủy';
      icon = 'bi-x-circle';
    } else if (booking.status === 'EXPIRED') {
      label = 'Hết hạn';
      icon = 'bi-clock-history';
    } else if (booking.status === 'COMPLETED') {
      label = 'Hoàn thành';
      icon = 'bi-check-circle';
    }

    return `
      <button class="btn btn-secondary btn-lg px-4 rounded-3" disabled style="${minWidth}">
        <i class="bi ${icon} me-1"></i>${label}
      </button>`;
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function openCheckinModalByIndex(index) {
    const booking = currentSearchResults[index];
    if (booking && canCheckin(booking, isBookingExpired(booking.endTime))) {
      openCheckinModal(booking);
    }
  }

  function openCheckinModalByData(bId, bCode, custName, phone, fieldName, start, end, total, deposit) {
    const obj = {
      bookingId: bId,
      bookingCode: bCode,
      customerName: custName,
      customerPhone: phone,
      fieldName: fieldName,
      startTime: start,
      endTime: end,
      totalAmount: total,
      depositAmount: deposit,
      paymentStatus: 'PAID'
    };
    openCheckinModal(obj);
  }

  async function cancelNoshow(bookingId) {
    showConfirm('Xác nhận hủy đặt sân này do khách hàng không đến nhận sân sau 30 phút?', async () => {
      try {
        const params = new URLSearchParams();
        params.append('bookingId', bookingId);

        const res = await fetch(`<%= ctx %>/api/staff/checkin/noshow`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: params,
          credentials: 'include'
        });

        if (!res.ok) {
          const errText = await res.text();
          let errMsg = 'Không rõ lỗi';
          try {
            const errJson = JSON.parse(errText);
            errMsg = errJson.error || errMsg;
          } catch(e) {}
          throw new Error(errMsg);
        }
        
        const data = await res.json();
        if (data.success) {
          if (checkinModalInstance) checkinModalInstance.hide();
          showToastAfterReload('Đã hủy đặt sân thành công (Khách không đến)', 'success');
          window.location.href = '<%= ctx %>/staff/schedule';
        } else {
          showToast('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'), 'danger');
        }
      } catch (err) {
        showToast('Lỗi khi hủy đặt sân: ' + err.message, 'danger');
      }
    });
  }

  function openCheckinModal(booking) {
    const isExpired = isBookingExpired(booking.endTime) || booking.notExpired === false;
    const isLateNoShow = isBookingLateNoShow(booking.startTime, booking.endTime);

    document.getElementById('modal-booking-id').value = booking.bookingId;
    document.getElementById('modal-code').textContent = booking.bookingCode || '—';
    document.getElementById('modal-name').textContent = booking.customerName || '—';
    document.getElementById('modal-phone').textContent = (booking.customerPhone && booking.customerPhone !== 'null' && String(booking.customerPhone).trim() !== '') ? booking.customerPhone : 'Không có SĐT';
    document.getElementById('modal-field').textContent = booking.fieldName || '—';
    document.getElementById('modal-time').textContent = `${formatTime(booking.startTime)} - ${formatTime(booking.endTime)}`;
    
    const tot = booking.totalAmount || 0;
    const dep = booking.depositAmount || 0;
    const rem = tot - dep;

    document.getElementById('modal-total').textContent = formatMoney(tot);
    document.getElementById('modal-deposit').textContent = formatMoney(dep);
    const remEl = document.getElementById('modal-remaining');
    if (remEl) remEl.textContent = formatMoney(rem >= 0 ? rem : 0);
    document.getElementById('checkin-note').value = '';
    
    const alertBox = document.getElementById('modal-alert');
    const actionsBox = document.getElementById('modal-actions');

    if (isExpired) {
      if (alertBox) {
        alertBox.innerHTML = '<div class="d-flex align-items-center gap-2 p-3 mb-3 rounded-4" style="background-color:#fef2f2;border:1px solid #fecaca;color:#991b1b;font-size:0.85rem;font-weight:600;"><i class="bi bi-exclamation-circle-fill fs-5 text-danger flex-shrink-0"></i><div>Lịch đặt sân này đã quá giờ nhận. Không thể thực hiện Check-in.</div></div>';
        alertBox.classList.remove('d-none');
      }
      if (actionsBox) {
        actionsBox.innerHTML = '<button class="btn btn-secondary px-4 py-2 rounded-3" disabled><i class="bi bi-exclamation-circle me-1"></i>Quá giờ nhận</button>';
      }
    } else if (isLateNoShow) {
      if (alertBox) {
        alertBox.innerHTML = '<div class="d-flex align-items-center gap-2 p-3 mb-3 rounded-4" style="background-color:#fffbeb;border:1px solid #fde68a;color:#92400e;font-size:0.85rem;font-weight:600;"><i class="bi bi-exclamation-triangle-fill fs-5 text-warning flex-shrink-0"></i><div>Khách hàng đã quá hạn 30 phút chưa đến nhận sân. Bạn có thể Hủy sân (khách không đến) hoặc tiếp tục Check-in.</div></div>';
        alertBox.classList.remove('d-none');
      }
      if (actionsBox) {
        actionsBox.innerHTML = 
          '<button type="button" class="btn btn-danger px-4 py-2 rounded-3" onclick="cancelNoshow(' + booking.bookingId + ')"><i class="bi bi-x-circle me-1"></i>Hủy sân (Khách không đến)</button>' +
          '<button type="button" class="btn btn-success px-4 py-2 rounded-3" id="modal-submit-btn" onclick="submitCheckin()"><i class="bi bi-check-circle me-1"></i>Xác nhận Check-in</button>';
      }
    } else {
      if (alertBox) {
        alertBox.innerHTML = '';
        alertBox.classList.add('d-none');
      }
      if (actionsBox) {
        actionsBox.innerHTML = '<button type="button" class="btn btn-success px-4 py-2 rounded-3" id="modal-submit-btn" onclick="submitCheckin()"><i class="bi bi-check-circle me-1"></i>Xác nhận Check-in</button>';
      }
    }

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

            if (!res.ok) {
        let errMsg = 'HTTP ' + res.status;
        try {
          const errData = await res.json();
          if (errData.error) errMsg = errData.error;
        } catch(e) {}
        throw new Error(errMsg);
      }

      const data = await res.json();

      if (data.success) {
        if (checkinModalInstance) checkinModalInstance.hide();
        showToastAfterReload('Check-in thành công!', 'success');
        window.location.href = '<%= ctx %>/staff/schedule';
      } else {
        showToast('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'), 'danger');
      }
    } catch (err) {
      showToast('Không thể thực hiện check-in: ' + err.message, 'danger');
    }
  }
</script>
</body>
</html>

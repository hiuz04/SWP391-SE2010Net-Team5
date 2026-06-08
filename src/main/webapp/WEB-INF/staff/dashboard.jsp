<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Staff Dashboard | Sport Field Booking</title>
  <meta name="description" content="Bảng điều khiển nhân viên sân - theo dõi ca làm việc, doanh thu và thao tác nhanh.">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    :root { 
      --sf-primary: #16a34a; 
      --shift-green:#16a34a; 
      --shift-blue:#0ea5e9; 
      --shift-orange:#f97316; 
      --shift-purple:#8b5cf6; 
    }
    body {
      font-family: 'Inter', sans-serif;
      background-color: #f8fafc;
    }
    .shift-banner { 
      background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 60%, #16a34a 100%); 
      border-radius: 24px; 
      color: #fff; 
      padding: 28px 32px; 
      position: relative; 
      overflow: hidden; 
    }
    .shift-banner::before { 
      content: ''; 
      position: absolute; 
      top: -40px; 
      right: -40px; 
      width: 200px; 
      height: 200px; 
      background: rgba(22,163,74,.18); 
      border-radius: 50%; 
    }
    .shift-banner::after  { 
      content: ''; 
      position: absolute; 
      bottom: -60px; 
      right: 80px; 
      width: 160px; 
      height: 160px; 
      background: rgba(14,165,233,.12); 
      border-radius: 50%; 
    }
    .live-dot { 
      width: 10px; 
      height: 10px; 
      background: #4ade80; 
      border-radius: 50%; 
      display: inline-block; 
      animation: pulse-dot 1.4s ease-in-out infinite; 
    }
    @keyframes pulse-dot { 
      0%, 100% { box-shadow: 0 0 0 0 rgba(74,222,128,.6) } 
      50% { box-shadow: 0 0 0 6px rgba(74,222,128,0) } 
    }
    .ring-wrap { 
      position: relative; 
      width: 90px; 
      height: 90px; 
      flex-shrink: 0; 
    }
    .ring-wrap svg { 
      transform: rotate(-90deg); 
    }
    .ring-bg  { 
      fill: none; 
      stroke: rgba(255,255,255,.15); 
      stroke-width: 8; 
    }
    .ring-val { 
      fill: none; 
      stroke: #4ade80; 
      stroke-width: 8; 
      stroke-linecap: round; 
      transition: stroke-dashoffset .8s ease; 
    }
    .ring-text { 
      position: absolute; 
      inset: 0; 
      display: flex; 
      flex-direction: column; 
      align-items: center; 
      justify-content: center; 
      font-size: .75rem; 
      font-weight: 700; 
      color: #fff; 
    }
    .kpi-card { 
      border-radius: 20px; 
      border: 1px solid #e2e8f0; 
      background: #fff; 
      box-shadow: 0 8px 24px rgba(15,23,42,.06); 
      padding: 22px 24px; 
      transition: transform .2s, box-shadow .2s; 
    }
    .kpi-card:hover { 
      transform: translateY(-4px); 
      box-shadow: 0 16px 40px rgba(15,23,42,.1); 
    }
    .kpi-icon { 
      width: 48px; 
      height: 48px; 
      border-radius: 14px; 
      display: flex; 
      align-items: center; 
      justify-content: center; 
      font-size: 1.35rem; 
    }
    .kpi-icon.green  { background: #dcfce7; color: #16a34a; }
    .kpi-icon.blue   { background: #e0f2fe; color: #0284c7; }
    .kpi-icon.orange { background: #fff7ed; color: #ea580c; }
    .kpi-icon.purple { background: #f5f3ff; color: #7c3aed; }
    .shift-progress-bar  { height: 10px; border-radius: 99px; background: #e2e8f0; overflow: hidden; }
    .shift-progress-fill { height: 100%; border-radius: 99px; background: linear-gradient(90deg, #16a34a, #4ade80); transition: width 1s ease; }
    .shortcut-btn { 
      display: flex; 
      flex-direction: column; 
      align-items: center; 
      justify-content: center; 
      gap: 10px; 
      padding: 22px 10px; 
      border-radius: 18px; 
      border: 1.5px solid #e2e8f0; 
      background: #fff; 
      cursor: pointer; 
      text-decoration: none; 
      color: #0f172a; 
      font-size: .85rem; 
      font-weight: 600; 
      transition: all .18s ease; 
      box-shadow: 0 4px 12px rgba(15,23,42,.05); 
    }
    .shortcut-btn:hover { 
      border-color: var(--sf-primary); 
      background: #f0fdf4; 
      color: var(--sf-primary); 
      transform: translateY(-3px); 
      box-shadow: 0 10px 28px rgba(22,163,74,.15); 
    }
    .shortcut-btn .sc-icon { 
      width: 48px; 
      height: 48px; 
      border-radius: 14px; 
      display: flex; 
      align-items: center; 
      justify-content: center; 
      font-size: 1.4rem; 
    }
    .booking-row td { vertical-align: middle; }
    .time-badge { 
      font-size: .78rem; 
      font-weight: 700; 
      background: #f1f5f9; 
      color: #475569; 
      border-radius: 8px; 
      padding: 3px 9px; 
    }
    .now-playing { background: #f0fdf4 !important; }
    .cash-card { 
      background: linear-gradient(135deg, #16a34a, #15803d); 
      border-radius: 20px; 
      color: #fff; 
      padding: 24px 28px; 
      box-shadow: 0 12px 32px rgba(22,163,74,.3); 
    }
    .cash-card .amount { font-size: 2rem; font-weight: 800; letter-spacing: -.03em; }
    .activity-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 5px; }
    #live-clock { font-variant-numeric: tabular-nums; }
    .no-shift-msg { text-align: center; padding: 60px 20px; color: #64748b; }
    .soft-card {
      border-radius: 20px;
      background: #fff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 8px 24px rgba(15,23,42,.04);
    }
    .badge-soft-success {
      background: #dcfce7;
      color: #15803d;
    }
    .badge-soft-info {
      background: #e0f2fe;
      color: #0369a1;
    }
    .badge-soft-warning {
      background: #fef3c7;
      color: #b45309;
    }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Dashboard"></div>

<main class="py-4">
  <div class="container">

    <!-- Page header -->
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-3">
      <div>
        <h1 class="fw-bold mb-1">Staff Dashboard</h1>
        <p class="text-muted mb-0">Xin chào, <strong id="staff-name">...</strong> 👋 — Tổng quan ca làm việc hôm nay.</p>
      </div>
      <div class="d-flex align-items-center gap-2">
        <span class="badge bg-success-subtle text-success fw-semibold px-3 py-2" style="border-radius:10px;">
          <i class="bi bi-calendar3 me-1"></i><span id="today-date"></span>
        </span>
        <span class="badge bg-dark text-white fw-bold px-3 py-2" style="border-radius:10px;font-size:.95rem;" id="live-clock">--:--:--</span>
      </div>
    </div>

    <!-- Loading state -->
    <div id="loading-state" class="text-center py-5">
      <div class="spinner-border text-success" role="status"></div>
      <p class="mt-2 text-muted">Đang tải dữ liệu...</p>
    </div>

    <!-- No shift today -->
    <div id="no-shift-state" class="no-shift-msg d-none">
      <i class="bi bi-calendar-x fs-1 text-muted"></i>
      <h5 class="mt-3">Bạn không có ca làm việc hôm nay</h5>
      <p class="text-muted small">Vui lòng liên hệ quản lý để được phân ca.</p>
      <div class="mt-4">
        <a href="<%= ctx %>/test_setup_db.jsp" class="btn btn-outline-success">
          <i class="bi bi-database-fill-add me-2"></i>Tạo nhanh dữ liệu test hôm nay
        </a>
      </div>
    </div>

    <!-- Dashboard content (hidden until data loads) -->
    <div id="dashboard-content" class="d-none">

      <!-- Shift Banner -->
      <div class="shift-banner mb-4">
        <div class="d-flex align-items-center gap-4 flex-wrap">
          <div class="ring-wrap">
            <svg width="90" height="90" viewBox="0 0 90 90">
              <circle class="ring-bg" cx="45" cy="45" r="38"/>
              <circle class="ring-val" id="shiftRing" cx="45" cy="45" r="38"
                      stroke-dasharray="238.76" stroke-dashoffset="238.76"/>
            </svg>
            <div class="ring-text"><span id="shift-pct" style="font-size:1.1rem;">0%</span><span style="opacity:.7;font-weight:500;">Ca</span></div>
          </div>
          <div class="flex-grow-1">
            <div class="d-flex align-items-center gap-2 mb-1">
              <span class="live-dot"></span>
              <span class="fw-semibold" style="color:#a3e635;">Ca đang diễn ra</span>
            </div>
            <h4 class="fw-bold mb-1">
              <span id="shift-name">—</span>&nbsp;
              <span id="shift-times" style="font-weight:400;opacity:.7;font-size:1rem;"></span>
            </h4>
            <p class="mb-2" style="opacity:.75;font-size:.9rem;">
              Cơ sở: <strong id="facility-name">—</strong>
            </p>
            <div class="d-flex align-items-center gap-3">
              <div class="shift-progress-bar flex-grow-1">
                <div class="shift-progress-fill" id="shiftBar" style="width:0%"></div>
              </div>
              <span id="shift-remaining" style="font-size:.82rem;opacity:.8;"></span>
            </div>
            <div class="mt-2 d-flex gap-3 flex-wrap" style="font-size:.82rem;opacity:.8;">
              <span><i class="bi bi-clock me-1"></i>Bắt đầu <span id="shift-start">—</span></span>
              <span><i class="bi bi-check2-circle me-1"></i>Kết thúc <span id="shift-end">—</span></span>
              <span><i class="bi bi-person-check me-1"></i>Đã check-in: <span id="checkin-count">0</span> booking</span>
            </div>
          </div>
        </div>
      </div>

      <!-- KPI Row -->
      <div class="row g-3 mb-4">
        <div class="col-6 col-md-3">
          <div class="kpi-card">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="kpi-icon green"><i class="bi bi-cash-stack"></i></div>
              <span class="badge text-bg-success bg-opacity-10 text-success" style="font-size:.72rem;">Ca này</span>
            </div>
            <div class="fs-4 fw-bold mb-0" style="font-weight:800;" id="kpi-cash">—</div>
            <div class="text-muted small mt-1">Tiền mặt thu được</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="kpi-card">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="kpi-icon blue"><i class="bi bi-calendar-check"></i></div>
              <span class="badge" style="background:#e0f2fe;color:#0284c7;font-size:.72rem;">Hôm nay</span>
            </div>
            <div class="fs-4 fw-bold mb-0" style="font-weight:800;" id="kpi-bookings">—</div>
            <div class="text-muted small mt-1">Booking hoàn thành</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="kpi-card">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="kpi-icon orange"><i class="bi bi-person-lines-fill"></i></div>
              <span class="badge" style="background:#fff7ed;color:#ea580c;font-size:.72rem;">Chờ xử lý</span>
            </div>
            <div class="fs-4 fw-bold mb-0" style="font-weight:800;" id="kpi-pending">—</div>
            <div class="text-muted small mt-1">Khách chờ check-in</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="kpi-card">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="kpi-icon purple"><i class="bi bi-star-fill"></i></div>
              <span class="badge" style="background:#f5f3ff;color:#7c3aed;font-size:.72rem;">TB hôm nay</span>
            </div>
            <div class="fs-4 fw-bold mb-0" style="font-weight:800;" id="kpi-rating">N/A</div>
            <div class="text-muted small mt-1">Đánh giá khách hàng</div>
          </div>
        </div>
      </div>

      <!-- Main content row -->
      <div class="row g-4">

        <!-- Left: today's bookings -->
        <div class="col-lg-7">
          <div class="soft-card p-4">
            <div class="d-flex align-items-center justify-content-between mb-3">
              <h5 class="mb-0 fw-bold"><i class="bi bi-calendar2-week text-success me-2"></i>Lịch sân trong ca</h5>
              <a href="<%= ctx %>/staff/schedule" class="btn btn-sm btn-outline-success">Xem tất cả</a>
            </div>
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0" style="font-size:.9rem;">
                <thead style="background:#f8fafc;">
                  <tr>
                    <th class="text-muted" style="font-weight:500;">Giờ</th>
                    <th class="text-muted" style="font-weight:500;">Sân</th>
                    <th class="text-muted" style="font-weight:500;">Khách</th>
                    <th class="text-muted" style="font-weight:500;">Trạng thái</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody id="booking-tbody">
                  <tr><td colspan="5" class="text-center text-muted py-4">Không có booking nào hôm nay</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Right column -->
        <div class="col-lg-5 d-flex flex-column gap-4">

          <!-- Cash card -->
          <div class="cash-card">
            <div class="d-flex align-items-start justify-content-between mb-3">
              <div>
                <div style="opacity:.8;font-size:.85rem;margin-bottom:4px;"><i class="bi bi-wallet2 me-1"></i>Tiền mặt thu được — <span id="cash-shift-name">Ca này</span></div>
                <div class="amount" id="cash-total">0 ₫</div>
                <div style="opacity:.75;font-size:.82rem;margin-top:4px;" id="cash-detail">0 giao dịch</div>
              </div>
              <div style="background:rgba(255,255,255,.15);border-radius:14px;padding:10px 14px;font-size:1.5rem;">
                <i class="bi bi-cash-coin"></i>
              </div>
            </div>
            <div class="shift-progress-bar" style="background:rgba(255,255,255,.2);">
              <div id="cash-bar" style="height:100%;border-radius:99px;background:#4ade80;width:0%;transition:width 1s ease;"></div>
            </div>
            <div class="d-flex justify-content-between mt-2" style="font-size:.78rem;opacity:.75;">
              <span>0 ₫</span><span>Mục tiêu: <span id="cash-target">—</span></span>
            </div>
          </div>

          <!-- Quick shortcuts -->
          <div class="soft-card p-4">
            <h5 class="fw-bold mb-3"><i class="bi bi-lightning-charge-fill text-warning me-2"></i>Thao tác nhanh</h5>
            <div class="row g-3">
              <div class="col-6">
                <a href="<%= ctx %>/staff/checkin" class="shortcut-btn w-100" id="sc-checkin">
                  <div class="sc-icon" style="background:#dcfce7;color:#16a34a;"><i class="bi bi-person-check-fill"></i></div>Check-in
                </a>
              </div>
              <div class="col-6">
                <a href="<%= ctx %>/staff/checkout" class="shortcut-btn w-100" id="sc-checkout">
                  <div class="sc-icon" style="background:#e0f2fe;color:#0284c7;"><i class="bi bi-receipt-cutoff"></i></div>Checkout
                </a>
              </div>
              <div class="col-6">
                <a href="<%= ctx %>/staff/schedule" class="shortcut-btn w-100" id="sc-schedule">
                  <div class="sc-icon" style="background:#fff7ed;color:#ea580c;"><i class="bi bi-calendar2-week-fill"></i></div>Lịch ngày
                </a>
              </div>
              <div class="col-6">
                <a href="<%= ctx %>/staff/invoice" class="shortcut-btn w-100" id="sc-invoice">
                  <div class="sc-icon" style="background:#f5f3ff;color:#7c3aed;"><i class="bi bi-file-earmark-text-fill"></i></div>Hóa đơn
                </a>
              </div>
            </div>
          </div>

          <!-- Recent activity -->
          <div class="soft-card p-4">
            <h5 class="fw-bold mb-3"><i class="bi bi-activity text-success me-2"></i>Hoạt động gần đây</h5>
            <div class="d-flex flex-column gap-3" id="activity-list">
              <p class="text-muted small mb-0">Chưa có hoạt động nào hôm nay.</p>
            </div>
          </div>

        </div>
      </div><!-- /row -->
    </div><!-- /dashboard-content -->

  </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
// ── Live clock & date ──────────────────────────────────────────────────────
function updateClock() {
  const now = new Date();
  const timeEl = document.getElementById('live-clock');
  const dateEl = document.getElementById('today-date');
  if (timeEl) timeEl.textContent = now.toLocaleTimeString('vi-VN', { hour:'2-digit', minute:'2-digit', second:'2-digit' });
  if (dateEl) dateEl.textContent = now.toLocaleDateString('vi-VN', { weekday:'long', day:'2-digit', month:'2-digit', year:'numeric' });
}
updateClock();
setInterval(updateClock, 1000);

// ── Helpers ────────────────────────────────────────────────────────────────
function fmt(amount) {
  if (amount == null) return '—';
  return Number(amount).toLocaleString('vi-VN') + ' ₫';
}

function statusBadge(status, nowPlaying) {
  if (nowPlaying) return '<span class="badge badge-soft-info"><i class="bi bi-play-circle me-1"></i>Đang chơi</span>';
  switch (status) {
    case 'COMPLETED':  return '<span class="badge badge-soft-success"><i class="bi bi-check-circle me-1"></i>Đã xong</span>';
    case 'CHECKED_IN': return '<span class="badge badge-soft-info"><i class="bi bi-play-circle me-1"></i>Đang chơi</span>';
    case 'CONFIRMED':  return '<span class="badge badge-soft-warning"><i class="bi bi-hourglass-split me-1"></i>Chờ check-in</span>';
    default:           return '<span class="badge" style="background:#f1f5f9;color:#64748b;">Sắp tới</span>';
  }
}

function actionBtn(status, bookingId) {
  if (status === 'CONFIRMED') return `<a href="<%= ctx %>/staff/checkin?id=${bookingId}" class="btn btn-sm btn-success">Check-in</a>`;
  if (status === 'CHECKED_IN') return `<a href="<%= ctx %>/staff/checkout?id=${bookingId}" class="btn btn-sm btn-outline-success">Checkout</a>`;
  return '';
}

function activityColor(type) {
  return type === 'CHECKIN' ? '#16a34a' : type === 'INVOICE' ? '#0ea5e9' : '#f97316';
}

function activityIcon(type) {
  return type === 'CHECKIN' ? 'Check-in' : type === 'INVOICE' ? 'Hóa đơn' : 'Checkout';
}

function timeOnly(dtStr) {
  if (!dtStr) return '';
  let t = dtStr;
  if (t.includes('T')) t = t.split('T')[1];
  else if (t.includes(' ')) t = t.split(' ')[1];
  return t.substring(0, 5);
}

// ── Load dashboard data ────────────────────────────────────────────────────
async function loadDashboard() {
  try {
    const res = await fetch('<%= ctx %>/api/staff/dashboard', { credentials: 'include' });
    if (res.status === 401) { window.location.href = '<%= ctx %>/login'; return; }
    if (!res.ok) throw new Error('HTTP ' + res.status);

    const data = await res.json();
    if (data.error) throw new Error(data.error);

    const loadingState = document.getElementById('loading-state');
    if (loadingState) loadingState.classList.add('d-none');

    // Staff name
    const staffNameEl = document.getElementById('staff-name');
    if (staffNameEl) staffNameEl.textContent = data.staffName || '—';

    if (!data.hasShift) {
      const noShiftState = document.getElementById('no-shift-state');
      if (noShiftState) noShiftState.classList.remove('d-none');
      return;
    }

    const dbContent = document.getElementById('dashboard-content');
    if (dbContent) dbContent.classList.remove('d-none');

    // ── Shift banner ─────────────────────────────────────────────────────
    const s = data.shift;
    document.getElementById('shift-name').textContent   = s.shiftName  || '—';
    document.getElementById('facility-name').textContent = s.facilityName || '—';
    document.getElementById('shift-times').textContent  = `${timeOnly(s.startTime)} – ${timeOnly(s.endTime)}`;
    document.getElementById('shift-start').textContent  = timeOnly(s.startTime);
    document.getElementById('shift-end').textContent    = timeOnly(s.endTime);
    document.getElementById('shift-remaining').textContent = s.remaining || '';
    document.getElementById('cash-shift-name').textContent = s.shiftName || 'Ca này';

    const pct = s.progressPct || 0;
    document.getElementById('shift-pct').textContent = Math.round(pct) + '%';
    document.getElementById('shiftBar').style.width  = pct + '%';
    const circumference = 238.76;
    document.getElementById('shiftRing').style.strokeDashoffset = circumference * (1 - pct / 100);

    // ── KPI ──────────────────────────────────────────────────────────────
    const cash = data.cashKpi || {};
    document.getElementById('kpi-cash').textContent = fmt(cash.totalCash);

    const bkpi = data.bookingKpi || {};
    document.getElementById('kpi-bookings').textContent = `${bkpi.completed || 0} / ${bkpi.totalBookings || 0}`;
    document.getElementById('kpi-pending').textContent  = data.pendingCheckin ?? 0;

    const rating = data.avgRating;
    document.getElementById('kpi-rating').innerHTML = rating != null
      ? `${rating} <span class="text-warning" style="font-size:1rem;">★</span>` : 'N/A';

    // Checkin count (completed bookings = checked-in)
    document.getElementById('checkin-count').textContent = bkpi.completed || 0;

    // ── Cash card ─────────────────────────────────────────────────────────
    document.getElementById('cash-total').textContent  = fmt(cash.totalCash);
    document.getElementById('cash-target').textContent = fmt(cash.targetAmount);
    document.getElementById('cash-bar').style.width    = (cash.targetPct || 0) + '%';
    const txCount = cash.txCount || 0;
    const avgTx   = cash.avgTransaction;
    document.getElementById('cash-detail').textContent =
      `${txCount} giao dịch${avgTx && txCount > 0 ? ' · Trung bình ' + fmt(avgTx) : ''}`;

    // ── Bookings table ────────────────────────────────────────────────────
    const tbody = document.getElementById('booking-tbody');
    const bookings = data.bookings || [];
    if (bookings.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">Không có booking nào hôm nay</td></tr>';
    } else {
      tbody.innerHTML = bookings.map(b => {
        const nowPlaying = b.nowPlaying && b.status === 'CHECKED_IN';
        const rowClass   = nowPlaying ? 'booking-row now-playing' : 'booking-row';
        const timeBgStyle = nowPlaying ? 'background:#dcfce7;color:#15803d;' : '';
        return `<tr class="${rowClass}">
          <td><span class="time-badge" style="${timeBgStyle}">${timeOnly(b.startTime)}–${timeOnly(b.endTime)}</span></td>
          <td><strong>${b.fieldName || '—'}</strong></td>
          <td>${b.customerName || '—'}</td>
          <td>${statusBadge(b.status, nowPlaying)}</td>
          <td>${actionBtn(b.status, b.bookingId)}</td>
        </tr>`;
      }).join('');
    }

    // ── Recent activity ───────────────────────────────────────────────────
    const actEl   = document.getElementById('activity-list');
    const activity = data.recentActivity || [];
    if (activity.length === 0) {
      actEl.innerHTML = '<p class="text-muted small mb-0">Chưa có hoạt động nào hôm nay.</p>';
    } else {
      actEl.innerHTML = activity.map(a => `
        <div class="d-flex gap-3">
          <div class="activity-dot" style="background:${activityColor(a.type)};"></div>
          <div style="font-size:.87rem;">
            <div class="fw-bold">${activityIcon(a.type)} #${a.refCode || ''} · ${a.fieldName || ''}</div>
            <div class="text-muted" style="font-size:.78rem;">${timeOnly(a.eventTime)} · ${a.customerName || ''}${a.amount > 0 ? ' · ' + fmt(a.amount) : ''}</div>
          </div>
        </div>`).join('');
    }

  } catch (err) {
    const loadingState = document.getElementById('loading-state');
    if (loadingState) {
      loadingState.innerHTML = `<div class="alert alert-danger">Không thể tải dữ liệu: ${err.message}</div>`;
    }
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', loadDashboard);
} else {
  loadDashboard();
}
</script>
</body>
</html>

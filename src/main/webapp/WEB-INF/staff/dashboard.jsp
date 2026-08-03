<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
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
    .live-dot.upcoming { 
      background: #3b82f6; 
      animation: pulse-blue 1.4s ease-in-out infinite; 
    }
    .live-dot.completed { 
      background: #94a3b8; 
      animation: none; 
    }
    @keyframes pulse-dot { 
      0%, 100% { box-shadow: 0 0 0 0 rgba(74,222,128,.6) } 
      50% { box-shadow: 0 0 0 6px rgba(74,222,128,0) } 
    }
    @keyframes pulse-blue { 
      0%, 100% { box-shadow: 0 0 0 0 rgba(59,130,246,.6) } 
      50% { box-shadow: 0 0 0 6px rgba(59,130,246,0) } 
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

    <!-- Alert message for shift restriction -->
    <div id="shift-restriction-alert" class="alert alert-warning alert-dismissible fade show d-none rounded-4 border-0 shadow-sm p-4 mb-4" role="alert" style="background-color: #fffbeb; border-left: 5px solid #f59e0b !important;">
      <div class="d-flex align-items-center gap-3">
        <div class="p-2 rounded-3 bg-warning text-white" style="font-size: 1.2rem; background-color: #f59e0b !important; display: flex; align-items: center; justify-content: center; width: 40px; height: 40px;">
          <i class="bi bi-exclamation-triangle-fill"></i>
        </div>
        <div>
          <h6 class="fw-bold mb-1" style="color: #92400e;">Tính năng bị giới hạn</h6>
          <p class="mb-0 small" style="color: #b45309;">Bạn chỉ được phép thực hiện check-in/checkout trong khung giờ ca làm việc được phân công của ngày hôm nay. Vui lòng quay lại khi đến giờ trực!</p>
        </div>
      </div>
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close" style="top: 1.5rem;"></button>
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
              <span class="live-dot" id="shift-status-dot"></span>
              <span class="fw-semibold" style="color:#a3e635;" id="shift-status-lbl">Ca đang diễn ra</span>
            </div>
            <h4 class="fw-bold mb-1">
              <span id="shift-name">—</span>&nbsp;
              <span id="shift-times" style="font-weight:400;opacity:.7;font-size:1rem;"></span>
            </h4>
            <p class="mb-2" style="opacity:.75;font-size:.9rem;">
              Cụm sân: <strong id="complex-name">—</strong>
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
          <div class="kpi-card h-100" onclick="showCashModal('cash')" style="cursor:pointer;" title="Nhấn để xem chi tiết tiền mặt">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="kpi-icon green"><i class="bi bi-cash-stack"></i></div>
              <span class="badge text-bg-success bg-opacity-10 text-success" style="font-size:.72rem;">Ca này</span>
            </div>
            <div class="fs-4 fw-bold mb-0" style="font-weight:800;" id="kpi-cash">—</div>
            <div class="text-muted small mt-1">Tiền mặt thu được</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="kpi-card h-100" onclick="showCompletedBookingsModal()" style="cursor:pointer;" title="Nhấn để xem danh sách booking hoàn thành">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="kpi-icon blue"><i class="bi bi-calendar-check"></i></div>
              <span class="badge" style="background:#e0f2fe;color:#0284c7;font-size:.72rem;">Hôm nay</span>
            </div>
            <div class="fs-4 fw-bold mb-0" style="font-weight:800;" id="kpi-bookings">—</div>
            <div class="text-muted small mt-1">Booking hoàn thành</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="kpi-card h-100" onclick="showPendingCheckinModal()" style="cursor:pointer;" title="Nhấn để xem danh sách khách chờ check-in">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="kpi-icon orange"><i class="bi bi-person-lines-fill"></i></div>
              <span class="badge" style="background:#fff7ed;color:#ea580c;font-size:.72rem;">Chờ xử lý</span>
            </div>
            <div class="fs-4 fw-bold mb-0" style="font-weight:800;" id="kpi-pending">—</div>
            <div class="text-muted small mt-1">Khách chờ check-in</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <a href="<%= ctx %>/staff/schedule" class="text-decoration-none text-dark d-block h-100">
            <div class="kpi-card h-100">
              <div class="d-flex align-items-center justify-content-between mb-2">
                <div class="kpi-icon purple"><i class="bi bi-calendar2-week-fill"></i></div>
                <span class="badge" style="background:#f5f3ff;color:#7c3aed;font-size:.72rem;">Hôm nay</span>
              </div>
              <div class="fs-4 fw-bold mb-0" style="font-weight:800;color: var(--sf-primary);">Xem Lịch</div>
              <div class="text-muted small mt-1">Biểu đồ lịch sân</div>
            </div>
          </a>
        </div>
      </div>

      <!-- Main content row -->
      <div class="row g-4">

        <!-- Left: today's bookings -->
        <div class="col-lg-7" id="bookings-table-section">
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
                    <th class="text-muted" style="font-weight:500;">Mã đặt sân</th>
                    <th class="text-muted" style="font-weight:500;">Sân</th>
                    <th class="text-muted" style="font-weight:500;">Khách</th>
                    <th class="text-muted" style="font-weight:500;">Trạng thái</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody id="booking-tbody">
                  <tr><td colspan="6" class="text-center text-muted py-4">Không có booking nào hôm nay</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Right column -->
        <div class="col-lg-5 d-flex flex-column gap-4">

          <!-- Cash card -->
          <div class="cash-card" id="cash-card-section" onclick="showCashModal('all')" style="cursor:pointer;" title="Nhấn để xem chi tiết tổng tiền thu được">
            <div class="d-flex align-items-start justify-content-between mb-3">
              <div>
                <div style="opacity:.8;font-size:.85rem;margin-bottom:4px;"><i class="bi bi-wallet2 me-1"></i>Tổng tiền thu được — <span id="cash-shift-name">Ca này</span></div>
                <div class="amount" id="cash-total">0 ₫</div>
                <div style="opacity:.75;font-size:.82rem;margin-top:4px;" id="cash-detail">0 giao dịch</div>
              </div>
              <div style="background:rgba(255,255,255,.15);border-radius:14px;padding:10px 14px;font-size:1.5rem;">
                <i class="bi bi-cash-coin"></i>
              </div>
            </div>

          </div>

          <!-- Quick shortcuts -->
          <div class="soft-card p-4">
            <h5 class="fw-bold mb-3"><i class="bi bi-lightning-charge-fill text-warning me-2"></i>Thao tác nhanh</h5>
            <div class="row g-3">
              <div class="col-6">
                <a href="<%= ctx %>/staff/checkin?pending=1" class="shortcut-btn w-100" id="sc-checkin">
                  <div class="sc-icon" style="background:#dcfce7;color:#16a34a;"><i class="bi bi-box-arrow-in-right"></i></div>Check-in
                </a>
              </div>
              <div class="col-6">
                <a href="<%= ctx %>/staff/checkout" class="shortcut-btn w-100" id="sc-checkout">
                  <div class="sc-icon" style="background:#e0f2fe;color:#0284c7;"><i class="bi bi-box-arrow-right"></i></div>Checkout
                </a>
              </div>
              <div class="col-6">
                <a href="<%= ctx %>/staff/schedule" class="shortcut-btn w-100" id="sc-schedule">
                  <div class="sc-icon" style="background:#fff7ed;color:#ea580c;"><i class="bi bi-calendar2-week-fill"></i></div>Lịch ngày
                </a>
              </div>
              <div class="col-6">
                <a href="<%= ctx %>/staff/schedule" class="shortcut-btn w-100" id="sc-invoice">
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

<!-- Cash Detail Modal -->
<div class="modal fade" id="cashDetailModal" tabindex="-1" aria-labelledby="cashDetailModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content border-0 shadow-lg" style="border-radius: 24px; overflow: hidden; background: #ffffff;">
      <div class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-circle p-2 text-success d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-color: #dcfce7;">
            <i class="bi bi-cash-stack fs-4" style="color: #16a34a;"></i>
          </div>
          <div>
            <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="cashDetailModalLabel">Chi tiết tiền mặt thu được trong ca</h5>
            <span class="text-muted small">Ca trực: <strong class="text-success" id="cash-modal-shift-name">—</strong></span>
          </div>
        </div>
        <button type="button" class="btn-close bg-light rounded-circle p-2" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>

      <div class="modal-body p-4">
        <div class="row g-3 mb-4">
          <div class="col-md-6">
            <div class="p-3 rounded-4 bg-light text-center border">
              <span class="text-muted small d-block mb-1" id="cash-modal-label-total">Tổng tiền mặt thu</span>
              <span class="fw-bold text-success fs-5" id="cash-modal-total">0 ₫</span>
            </div>
          </div>
          <div class="col-md-6">
            <div class="p-3 rounded-4 bg-light text-center border">
              <span class="text-muted small d-block mb-1" id="cash-modal-label-count">Số lượng hóa đơn</span>
              <span class="fw-bold text-dark fs-5" id="cash-modal-count">0 giao dịch</span>
            </div>
          </div>
        </div>

        <h6 class="fw-bold text-muted uppercase small mb-3" style="font-size: 0.75rem; letter-spacing: 0.05em;">
          <i class="bi bi-receipt me-2 text-success"></i>DANH SÁCH HÓA ĐƠN ĐÃ THANH TOÁN
        </h6>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0" style="font-size:0.88rem;">
            <thead class="bg-light" id="cash-modal-thead">
              <tr>
                <th>Mã HĐ / Booking</th>
                <th>Sân bóng</th>
                <th>Khách hàng</th>
                <th>Thời gian</th>
                <th class="text-end">Số tiền</th>
                <th class="text-center">Hóa đơn</th>
              </tr>
            </thead>
            <tbody id="cash-modal-tbody">
              <tr><td colspan="6" class="text-center text-muted py-4">Chưa có giao dịch tiền mặt nào trong ca này</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 pt-0">
        <button type="button" class="btn btn-light px-4" data-bs-dismiss="modal">Đóng</button>
      </div>
    </div>
  </div>
</div>

<!-- Completed Bookings Modal -->
<div class="modal fade" id="completedBookingsModal" tabindex="-1" aria-labelledby="completedBookingsModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content border-0 shadow-lg" style="border-radius: 24px; overflow: hidden; background: #ffffff;">
      <div class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-circle p-2 text-primary d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-color: #e0f2fe;">
            <i class="bi bi-check-circle-fill fs-4" style="color: #0284c7;"></i>
          </div>
          <div>
            <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="completedBookingsModalLabel">Booking đã hoàn thành</h5>
            <span class="text-muted small">Tổng số: <strong class="text-primary" id="completed-modal-count">0</strong> booking</span>
          </div>
        </div>
        <button type="button" class="btn-close bg-light rounded-circle p-2" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>

      <div class="modal-body p-4">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0" style="font-size:0.88rem;">
            <thead class="bg-light">
              <tr>
                <th>Khung giờ</th>
                <th>Mã đặt sân</th>
                <th>Sân bóng</th>
                <th>Khách hàng</th>
                <th class="text-end">Tổng tiền</th>
                <th class="text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody id="completed-modal-tbody">
              <tr><td colspan="6" class="text-center text-muted py-4">Chưa có booking nào hoàn thành hôm nay</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 pt-0">
        <button type="button" class="btn btn-light px-4" data-bs-dismiss="modal">Đóng</button>
      </div>
    </div>
  </div>
</div>

<!-- Pending Check-in Modal -->
<div class="modal fade" id="pendingCheckinModal" tabindex="-1" aria-labelledby="pendingCheckinModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content border-0 shadow-lg" style="border-radius: 24px; overflow: hidden; background: #ffffff;">
      <div class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-circle p-2 text-warning d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-color: #fff7ed;">
            <i class="bi bi-person-lines-fill fs-4" style="color: #ea580c;"></i>
          </div>
          <div>
            <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="pendingCheckinModalLabel">Danh sách Khách chờ Check-in</h5>
            <span class="text-muted small">Đang chờ: <strong class="text-warning" id="pending-modal-count">0</strong> lượt check-in</span>
          </div>
        </div>
        <button type="button" class="btn-close bg-light rounded-circle p-2" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>

      <div class="modal-body p-4">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0" style="font-size:0.88rem;">
            <thead class="bg-light">
              <tr>
                <th>Khung giờ</th>
                <th>Mã đặt sân</th>
                <th>Sân bóng</th>
                <th>Khách hàng</th>
                <th>Trạng thái</th>
                <th class="text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody id="pending-modal-tbody">
              <tr><td colspan="6" class="text-center text-muted py-4">Không có khách nào đang chờ check-in</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 pt-0">
        <button type="button" class="btn btn-light px-4" data-bs-dismiss="modal">Đóng</button>
      </div>
    </div>
  </div>
</div>

<!-- Booking Details Modal -->
<div class="modal fade" id="bookingDetailModal" tabindex="-1" aria-labelledby="bookingDetailModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content border-0 shadow-lg" style="border-radius: 24px; overflow: hidden; background: #ffffff;">
      <!-- Header -->
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

      <!-- Body -->
      <div class="modal-body p-4">
        <div class="row g-4">
          <!-- Left Column: Customer & Match Details -->
          <div class="col-md-7">
            <!-- Customer Block -->
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

            <!-- Match Details Block -->
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

          <!-- Right Column: Cost Details -->
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
              <div class="text-center py-2" id="det-payment-box">
                <span class="text-muted small d-block mb-1" style="font-size: 0.72rem; letter-spacing: 0.05em; font-weight: 700;" id="det-payment-label">CẦN THANH TOÁN CÒN LẠI</span>
                <span class="fw-bold text-success display-6" style="font-weight: 800; font-size: 1.8rem;" id="det-total-amount">—</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="modal-footer border-0 px-4 pb-4 pt-0 d-flex justify-content-end gap-2" id="det-modal-footer">
        <button type="button" class="btn btn-light px-4 py-2 rounded-3" data-bs-dismiss="modal" style="font-weight: 600;">Đóng</button>
      </div>
    </div>
  </div>
</div>

<!-- Check-in Confirmation Modal with Notes & Details -->
<div class="modal fade" id="checkinConfirmModal" tabindex="-1" aria-labelledby="checkinConfirmModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content border-0 shadow-lg" style="border-radius: 24px; overflow: hidden; background: #ffffff;">
      <div class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-circle p-2 text-success d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; background-color: #dcfce7;">
            <i class="bi bi-box-arrow-in-right fs-4" style="color: #16a34a;"></i>
          </div>
          <div>
            <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="checkinConfirmModalLabel">Xác nhận nhận sân (Check-in)</h5>
            <span class="text-muted small" style="font-size: 0.8rem;">Mã đặt sân: <strong class="text-success" id="chk-modal-code">—</strong></span>
          </div>
        </div>
        <button type="button" class="btn-close bg-light rounded-circle p-2" data-bs-dismiss="modal" aria-label="Close" style="font-size: 0.8rem;"></button>
      </div>

      <div class="modal-body p-4">
        <div id="chk-modal-alert" class="d-none"></div>
        <input type="hidden" id="chk-modal-booking-id">
        <div class="row g-4">
          <div class="col-md-7">
            <div class="mb-3">
              <h6 class="fw-bold text-muted uppercase small mb-2 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-person-fill me-2" style="color: #16a34a;"></i>THÔNG TIN KHÁCH HÀNG</h6>
              <div class="p-3 bg-light rounded-4 border border-light-subtle" style="background-color: #f8fafc !important;">
                <div class="mb-2">
                  <span class="text-muted small d-block" style="font-size: 0.75rem;">Tên khách hàng</span>
                  <span class="fw-bold text-dark fs-6" id="chk-modal-name">—</span>
                </div>
                <div>
                  <span class="text-muted small d-block" style="font-size: 0.75rem;">Số điện thoại</span>
                  <span class="fw-bold text-success fs-6" id="chk-modal-phone">—</span>
                </div>
              </div>
            </div>

            <div class="mb-3">
              <h6 class="fw-bold text-muted uppercase small mb-2 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-heptagon-fill me-2" style="color: #16a34a;"></i>SÂN & KHUNG GIỜ</h6>
              <div class="p-3 bg-light rounded-4 border border-light-subtle" style="background-color: #f8fafc !important;">
                <div class="row g-2">
                  <div class="col-6">
                    <span class="text-muted small d-block" style="font-size: 0.75rem;">Sân bóng</span>
                    <span class="fw-bold text-dark" id="chk-modal-field">—</span>
                  </div>
                  <div class="col-6">
                    <span class="text-muted small d-block" style="font-size: 0.75rem;">Khung giờ</span>
                    <span class="fw-bold text-dark" id="chk-modal-time">—</span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <label for="chk-modal-note" class="form-label fw-bold text-muted small mb-1"><i class="bi bi-journal-text me-1 text-success"></i>Ghi chú nhận sân (Tùy chọn)</label>
              <textarea class="form-control rounded-3" id="chk-modal-note" rows="2" placeholder="Ví dụ: Khách mượn 2 áo bít, mượn bóng số 5..."></textarea>
            </div>
          </div>

          <div class="col-md-5">
            <h6 class="fw-bold text-muted uppercase small mb-2 tracking-wider" style="font-size: 0.75rem; letter-spacing: 0.05em;"><i class="bi bi-receipt-cutoff me-2" style="color: #16a34a;"></i>THANH TOÁN HÔM NAY</h6>
            <div class="p-4 rounded-4 shadow-sm border border-success-subtle d-flex flex-column justify-content-between" style="background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%); border-color: #bbf7d0 !important; border: 1px solid; min-height: 230px;">
              <div>
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <span class="text-muted small">Giá gốc sân:</span>
                  <span class="fw-bold text-dark" id="chk-modal-price">—</span>
                </div>
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <span class="text-muted small">Đã đặt cọc:</span>
                  <span class="fw-bold text-danger" id="chk-modal-deposit">—</span>
                </div>
                <hr class="my-3 border-secondary-subtle">
              </div>
              <div class="text-center py-2">
                <span class="text-muted small d-block mb-1" style="font-size: 0.72rem; letter-spacing: 0.05em; font-weight: 700;">CẦN THANH TOÁN CÒN LẠI</span>
                <span class="fw-bold text-success display-6" style="font-weight: 800; font-size: 1.8rem;" id="chk-modal-remaining">—</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer border-0 px-4 pb-4 pt-0 d-flex justify-content-end gap-2" id="chk-modal-footer">
        <button type="button" class="btn btn-light px-4 py-2 rounded-3" data-bs-dismiss="modal">Đóng</button>
        <div id="chk-modal-actions" class="d-flex gap-2">
          <button type="button" class="btn btn-success px-4 py-2 rounded-3" id="chk-modal-submit-btn" onclick="submitCheckinForm(event)"><i class="bi bi-check-circle me-1"></i>Xác nhận Check-in</button>
        </div>
      </div>
    </div>
  </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js?v=<%= System.currentTimeMillis() %>"></script>
<script>
// ── Global Dashboard state ──────────────────────────────────────────────────
let todayBookings = [];
let globalCashData = {};
let activeParentModal = null; // 'cash' | 'completed' | 'pending' | null
let bookingDetailModalInstance = null;
let cashModalInstance = null;
let completedModalInstance = null;
let pendingModalInstance = null;

function handleRowClick(event, bookingId) {
  if (event.target.closest('button') || event.target.closest('a')) {
    return;
  }
  showBookingDetails(bookingId);
}

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
const fmt = window.fmtMoney;

function parseLocalDate(dtStr) {
  if (!dtStr) return null;
  try {
    let str = String(dtStr).trim();
    if (str.includes('.')) str = str.split('.')[0];
    str = str.replace('T', ' ');
    if (!str.includes('-') && !str.includes('/')) {
      const today = new Date();
      const parts = str.split(':');
      return new Date(today.getFullYear(), today.getMonth(), today.getDate(), parseInt(parts[0], 10), parseInt(parts[1], 10), parseInt(parts[2] || 0, 10));
    }
    const dateParts = str.split(' ');
    const ym = dateParts[0].split(dateParts[0].includes('-') ? '-' : '/');
    const hms = (dateParts[1] || '00:00:00').split(':');
    return new Date(parseInt(ym[0], 10), parseInt(ym[1], 10) - 1, parseInt(ym[2], 10), parseInt(hms[0], 10), parseInt(hms[1], 10), parseInt(hms[2] || 0, 10));
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

function statusBadge(status, nowPlaying, isExpired, startTimeStr, endTimeStr) {
  if (nowPlaying) return '<span class="badge badge-soft-info"><i class="bi bi-play-circle me-1"></i>Đang chơi</span>';
  switch (status) {
    case 'COMPLETED':  return '<span class="badge badge-soft-success"><i class="bi bi-check-circle me-1"></i>Đã xong</span>';
    case 'CHECKED_IN': return '<span class="badge badge-soft-info"><i class="bi bi-play-circle me-1"></i>Đang chơi</span>';
    case 'PENDING_CHECKOUT_PAYMENT': return '<span class="badge" style="background:#fae8ff;color:#a21caf;"><i class="bi bi-credit-card me-1"></i>Cho khach thanh toan</span>';
    case 'CONFIRMED':
      if (isBookingLateNoShow(startTimeStr, endTimeStr)) {
        return '<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Muộn 30p</span>';
      }
      if (isExpired) {
        return '<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Quá giờ</span>';
      }
      return '<span class="badge badge-soft-warning"><i class="bi bi-hourglass-split me-1"></i>Chờ check-in</span>';
    default:           return '<span class="badge" style="background:#f1f5f9;color:#64748b;">Sắp tới</span>';
  }
}

let currentShiftStatus = 'ONGOING';
let globalShiftStart = null;
let globalShiftEnd = null;
let globalHasShift = false;

// ── Xử lý hiển thị Nút Hành Động (Action Buttons) ──────────────────────────
// Hàm này quyết định nút nào sẽ hiển thị (Check-in, Checkout, Khóa, Quá giờ...)
function actionBtn(status, bookingId, isExpired, hasInvoice, checkoutDue, startTimeStr, endTimeStr, isOutShift) {
  // 1. Nếu ca trực đã qua (COMPLETED) -> Khóa toàn bộ thao tác
  if (currentShiftStatus === 'UPCOMING') {
    if (status === 'CONFIRMED' || status === 'CHECKED_IN') {
      return `<button class="btn btn-sm btn-secondary px-3" disabled title="Chưa đến giờ làm việc"><i class="bi bi-lock-fill me-1"></i>Chờ ca trực</button>`;
    }
  }
  if (currentShiftStatus === 'COMPLETED') {
    if (status === 'CONFIRMED' || status === 'CHECKED_IN') {
      return `<button class="btn btn-sm btn-secondary px-3" disabled title="Ca trực đã kết thúc"><i class="bi bi-lock-fill me-1"></i>Hết ca trực</button>`;
    }
  }

  // 2. Kiểm tra xem giờ đá của sân có nằm ngoài giờ ca trực hiện tại không
  const isOutsideShift = typeof isTimeInShiftStr === 'function' && !isTimeInShiftStr(startTimeStr, globalShiftStart, globalShiftEnd);
  
  // 3. Nếu là sân nằm ngoài ca trực (khác ca) -> Hiển thị nút Khóa (không cho thao tác)
  if (isOutShift || isOutsideShift) {
    if (status === 'CONFIRMED' || (status === 'CHECKED_IN' && checkoutDue)) {
      return `<button class="btn btn-sm btn-secondary px-3" disabled title="Khung giờ này không nằm trong ca trực của bạn"><i class="bi bi-lock-fill me-1"></i>Khóa</button>`;
    }
  }

  // 4. Nếu là sân trong ca trực và chưa Check-in (CONFIRMED)
  if (status === 'CONFIRMED') {
    // 4.1. Khách quá hạn nhận sân (Đã quá giờ)
    if (isExpired) {
      return `<button class="btn btn-sm btn-secondary px-3" disabled><i class="bi bi-exclamation-circle me-1"></i>Quá giờ nhận</button>`;
    }
    // 4.2. Hợp lệ -> Cho phép Check-in
    return `<button type="button" onclick="openCheckinModal(${bookingId})" class="btn btn-sm btn-success">Check-in</button>`;
  }
  
  // 5. Nếu khách đang chơi (CHECKED_IN)
  if (status === 'CHECKED_IN') {
    return checkoutDue
      ? `<a href="<%= ctx %>/staff/checkout?id=${bookingId}" class="btn btn-sm btn-outline-success">Checkout</a>` // Có thể Checkout
      : `<button class="btn btn-sm btn-secondary px-3" disabled>Đang sử dụng</button>`; // Chưa đến lúc
  }
  
  // 6. Nếu đã Checkout và có Hóa đơn
  if (status === 'PENDING_CHECKOUT_PAYMENT' && hasInvoice) return `<a href="<%= ctx %>/staff/invoice?id=${bookingId}" class="btn btn-sm btn-outline-secondary px-3"><i class="bi bi-file-earmark-text me-1"></i>Hóa đơn</a>`;
  if (status === 'COMPLETED' && hasInvoice) return `<a href="<%= ctx %>/staff/invoice?id=${bookingId}" class="btn btn-sm btn-outline-secondary px-3"><i class="bi bi-file-earmark-text me-1"></i>Hóa đơn</a>`;
  
  return '';
}

function activityColor(type) {
  return type === 'CHECKIN' ? '#16a34a' : type === 'INVOICE' ? '#0ea5e9' : '#f97316';
}

function activityIcon(type) {
  return type === 'CHECKIN' ? 'Check-in' : type === 'INVOICE' ? 'Hóa đơn' : 'Checkout';
}

// (Removed duplicate fmt, timeOnly, isBookingExpired, isBookingLateNoShow functions, now in app.js)
function fmtPhone(phone) {
  if (!phone || phone === 'null' || phone === 'undefined' || String(phone).trim() === '') {
    return 'Không có SĐT';
  }
  return phone;
}

// ── Xử lý Sắp xếp thứ tự ưu tiên hiển thị (Booking Sort Priority) ──────────────
// Quy tắc ưu tiên:
// 0. Đang chơi
// 1. Chờ Check-in (chưa quá hạn)
// 2. Có thể Checkout
// 3. Chờ thanh toán
// 4. Đã hoàn thành
// 5. Quá hạn Check-in (Đẩy xuống cuối)
// 6. Các trạng thái khác
function bookingSortPriority(b) {
  const isExpired = isBookingExpired(b.endTime);
  if (b.nowPlaying && b.status === 'CHECKED_IN') return 0;
  if (b.status === 'CONFIRMED' && !isExpired) return 1;
  if (b.status === 'CHECKED_IN') return 2;
  if (b.status === 'PENDING_CHECKOUT_PAYMENT') return 3;
  if (b.status === 'COMPLETED') return 4;
  if (b.status === 'CONFIRMED' && isExpired) return 5;
  return 6;
}

// ── Load dashboard data ────────────────────────────────────────────────────
async function loadDashboard() {
  try {
    const res = await fetch('<%= ctx %>/api/staff/dashboard', { credentials: 'include' });
    if (res.status === 401) { window.location.href = '<%= ctx %>/login'; return; }
          if (!res.ok) {
        let errMsg = 'HTTP ' + res.status;
        try {
          const errData = await res.json();
          if (errData.error) errMsg = errData.error;
        } catch(e) {}
        throw new Error(errMsg);
      }


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
    document.getElementById('complex-name').textContent = s.complexName || '—';
    document.getElementById('shift-times').textContent  = `${timeOnly(s.startTime)} – ${timeOnly(s.endTime)}`;
    document.getElementById('shift-start').textContent  = timeOnly(s.startTime);
    document.getElementById('shift-end').textContent    = timeOnly(s.endTime);
    document.getElementById('shift-remaining').textContent = s.remaining || '';
    document.getElementById('cash-shift-name').textContent = s.shiftName || 'Ca này';

    const statusDot = document.getElementById('shift-status-dot');
    const statusLbl = document.getElementById('shift-status-lbl');
    currentShiftStatus = s.status || 'ONGOING';
    
    if (statusDot && statusLbl) {
      statusDot.className = 'live-dot'; // reset
      if (currentShiftStatus === 'UPCOMING') {
        statusDot.classList.add('upcoming');
        statusLbl.textContent = 'Ca trực chưa diễn ra';
        statusLbl.style.color = '#3b82f6';
      } else if (currentShiftStatus === 'COMPLETED') {
        statusDot.classList.add('completed');
        statusLbl.textContent = 'Ca trực đã kết thúc';
        statusLbl.style.color = '#94a3b8';
      } else {
        statusLbl.textContent = 'Ca đang diễn ra';
        statusLbl.style.color = '#a3e635';
      }
    }

    if (currentShiftStatus === 'UPCOMING' || currentShiftStatus === 'COMPLETED') {
      const blockClick = (e) => {
        e.preventDefault();
        if (currentShiftStatus === 'UPCOMING') {
          showToast('Ca trực của bạn chưa bắt đầu (Ca làm việc: ' + timeOnly(s.startTime) + ' - ' + timeOnly(s.endTime) + '). Bạn chỉ được xem dữ liệu, không thể thực hiện thao tác này.', 'warning');
        } else {
          showToast('Ca trực của bạn đã kết thúc. Bạn không thể thực hiện thao tác này.', 'danger');
        }
      };
      
      const checkinBtn = document.getElementById('sc-checkin');
      const checkoutBtn = document.getElementById('sc-checkout');
      if (checkinBtn) {
        checkinBtn.addEventListener('click', blockClick);
        checkinBtn.style.opacity = '0.6';
        checkinBtn.style.cursor = 'not-allowed';
      }
      if (checkoutBtn) {
        checkoutBtn.addEventListener('click', blockClick);
        checkoutBtn.style.opacity = '0.6';
        checkoutBtn.style.cursor = 'not-allowed';
      }
    }

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
    document.getElementById('kpi-pending').textContent  = (data.pendingCheckin !== undefined && data.pendingCheckin !== null) ? data.pendingCheckin : 0;

    const rating = data.avgRating;
    const ratingEl = document.getElementById('kpi-rating');
    if (ratingEl) {
      ratingEl.innerHTML = rating != null
        ? `${rating} <span class="text-warning" style="font-size:1rem;">★</span>` : 'N/A';
    }

    // Checkin count (completed bookings = checked-in)
    document.getElementById('checkin-count').textContent = bkpi.completed || 0;

    // ── Total revenue card (Card 2) ──────────────────────────────────────────
    globalCashData = cash;
    const totalRev = data.totalRevenueKpi || cash;
    globalTotalRevenueData = totalRev;
    document.getElementById('cash-total').textContent  = fmt(totalRev.totalRevenue || totalRev.totalCash || 0);
    const txCount = totalRev.txCount || 0;
    const avgTx   = totalRev.avgTransaction || 0;
    document.getElementById('cash-detail').textContent =
      `${txCount} giao dịch${avgTx && txCount > 0 ? ' · Trung bình ' + fmt(avgTx) : ''}`;

    globalHasShift = data.hasShift !== false && data.shift && data.shift.shiftId;
    globalShiftStart = data.shift ? data.shift.startTime : null;
    globalShiftEnd = data.shift ? data.shift.endTime : null;

    const hasShift = globalHasShift;
    const currentShiftStart = globalShiftStart;
    const currentShiftEnd = globalShiftEnd;

    function isTimeInShift(timeStr) {
      if (!hasShift) return true;
      return isTimeInShiftStr(timeStr, globalShiftStart, globalShiftEnd);
    }

    function renderBookingRow(b, isOutShift = false) {
      const nowPlaying = b.nowPlaying && b.status === 'CHECKED_IN';
      const isExpired = isBookingExpired(b.endTime);
      const rowClass   = nowPlaying ? 'booking-row now-playing' : 'booking-row';
      const timeBgStyle = nowPlaying ? 'background:#dcfce7;color:#15803d;' : '';
      return `<tr class="${rowClass}" onclick="handleRowClick(event, ${b.bookingId})" style="cursor:pointer;">
        <td style="white-space: nowrap;"><span class="time-badge" style="${timeBgStyle}">${timeOnly(b.startTime)}–${timeOnly(b.endTime)}</span></td>
        <td style="white-space: nowrap;"><strong>${b.bookingCode || '—'}</strong></td>
        <td style="white-space: nowrap;"><strong>${b.fieldName || '—'}</strong></td>
        <td style="white-space: nowrap;">${b.customerName || '—'}</td>
        <td style="white-space: nowrap;">${statusBadge(b.status, nowPlaying, isExpired, b.startTime, b.endTime)}</td>
        <td style="white-space: nowrap;">${actionBtn(b.status, b.bookingId, isExpired, b.hasInvoice, b.checkoutDue, b.startTime, b.endTime, isOutShift)}</td>
      </tr>`;
    }

    // ── Bookings table ────────────────────────────────────────────────────
    const tbody = document.getElementById('booking-tbody');
    const bookings = data.bookings || [];
    todayBookings = bookings;
    
    // Initialize booking detail modal if not done yet
    const modalDetailEl = document.getElementById('bookingDetailModal');
    if (modalDetailEl && !bookingDetailModalInstance) {
      bookingDetailModalInstance = new bootstrap.Modal(modalDetailEl);
    }

    if (bookings.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Không có booking nào hôm nay</td></tr>';
    } else {
      const inShiftBk = [];
      const outShiftBk = [];
      bookings.forEach(b => {
        if (isTimeInShift(b.startTime)) {
          inShiftBk.push(b);
        } else {
          outShiftBk.push(b);
        }
      });

      inShiftBk.sort((x, y) => String(x.startTime || '').localeCompare(String(y.startTime || '')));
      outShiftBk.sort((x, y) => String(x.startTime || '').localeCompare(String(y.startTime || '')));

      let html = '';
      if (hasShift && outShiftBk.length > 0) {
        // Section 1: Trong ca làm việc
        html += `<tr><td colspan="6" class="py-2"><span class="badge bg-success-subtle text-success fw-bold" style="font-size:0.8rem;border-radius:8px;padding:5px 12px;"><i class="bi bi-clock-fill me-1"></i>Trong ca làm việc</span></td></tr>`;
        if (inShiftBk.length === 0) {
          html += `<tr><td colspan="6" class="text-muted text-center py-3 small">Không có booking nào trong ca làm việc</td></tr>`;
        } else {
          html += inShiftBk.map(b => renderBookingRow(b, false)).join('');
        }

        // Section 2: Ngoài ca làm việc
        html += `<tr><td colspan="6" class="py-2"><span class="badge bg-secondary-subtle text-secondary fw-bold" style="font-size:0.8rem;border-radius:8px;padding:5px 12px;"><i class="bi bi-clock me-1"></i>Ngoài ca làm việc</span></td></tr>`;
        html += outShiftBk.map(b => renderBookingRow(b, true)).join('');
      } else {
        if (hasShift) {
          html += `<tr><td colspan="6" class="py-2"><span class="badge bg-success-subtle text-success fw-bold" style="font-size:0.8rem;border-radius:8px;padding:5px 12px;"><i class="bi bi-clock-fill me-1"></i>Trong ca làm việc</span></td></tr>`;
        }
        html += inShiftBk.map(b => renderBookingRow(b, false)).join('');
      }

      tbody.innerHTML = html;
    }

    // ── Recent activity ───────────────────────────────────────────────────
    const actEl   = document.getElementById('activity-list');
    const activity = data.recentActivity || [];
    if (actEl) {
      if (activity.length === 0) {
        actEl.innerHTML = '<p class="text-muted small mb-0">Chưa có hoạt động nào hôm nay.</p>';
      } else {
        actEl.innerHTML = activity.map(a => 
          '<div class="d-flex gap-3">' +
            '<div class="activity-dot" style="background:' + activityColor(a.type) + ';"></div>' +
            '<div style="font-size:.87rem;">' +
              '<div class="fw-bold">' + activityIcon(a.type) + ' #' + (a.refCode || '') + ' · ' + (a.fieldName || '') + '</div>' +
              '<div class="text-muted" style="font-size:.78rem;">' + timeOnly(a.eventTime) + ' · ' + (a.customerName || '') + (a.amount > 0 ? ' · ' + fmt(a.amount) : '') + '</div>' +
            '</div>' +
          '</div>').join('');
      }
    }
  } catch (err) {
    const loadingState = document.getElementById('loading-state');
    if (loadingState) {
      loadingState.innerHTML = `<div class="alert alert-danger">Không thể tải dữ liệu: ${err.message}</div>`;
    }
  }
}

function showBookingDetails(bookingId) {
  const b = todayBookings.find(x => x.bookingId === bookingId);
  if (!b) return;

  const isOutsideShift = typeof isTimeInShiftStr === 'function' && !isTimeInShiftStr(b.startTime, globalShiftStart, globalShiftEnd);

  if (b.status === 'CONFIRMED') {
    openCheckinModal(bookingId);
    return;
  }

  const nowPlaying = b.nowPlaying && b.status === 'CHECKED_IN';
  const isExpired = isBookingExpired(b.endTime);

  document.getElementById('det-code').textContent = b.bookingCode || '—';
  document.getElementById('det-name').textContent = b.customerName || '—';
  document.getElementById('det-phone').textContent = fmtPhone(b.customerPhone);
  document.getElementById('det-field').textContent = b.fieldName || '—';
  document.getElementById('det-time').textContent = timeOnly(b.startTime) + " - " + timeOnly(b.endTime);
  document.getElementById('det-status-badge').innerHTML = statusBadge(b.status, nowPlaying, isExpired, b.startTime, b.endTime);

  document.getElementById('det-orig-price').textContent = fmt(b.totalAmount);
  document.getElementById('det-deposit').textContent = fmt(b.depositAmount);

  const isPaidOrCompleted = b.status === 'COMPLETED' || (b.status === 'PENDING_CHECKOUT_PAYMENT' && b.hasInvoice) || b.hasInvoice;
  const payContainer = document.getElementById('det-payment-box');

  if (isPaidOrCompleted) {
    const methodText = b.paymentMethodName || 'Tiền mặt';
    if (payContainer) {
      payContainer.innerHTML = `
        <span class="badge bg-success-subtle text-success fs-6 fw-bold px-3 py-1 rounded-pill mb-2 border border-success-subtle">
          <i class="bi bi-check-circle-fill me-1"></i>ĐÃ THANH TOÁN ĐỦ
        </span>
        <div class="fw-bold text-success display-6 my-1" style="font-weight: 800; font-size: 1.8rem;">${fmt(b.totalAmount || 0)}</div>
        <div class="text-muted small mt-2">
          <i class="bi bi-credit-card-2-front me-1 text-success"></i>Phương thức: <strong class="text-dark">${methodText}</strong>
        </div>
      `;
    }
  } else {
    const remaining = (b.totalAmount || 0) - (b.depositAmount || 0);
    if (payContainer) {
      payContainer.innerHTML = `
        <span class="text-muted small d-block mb-1" style="font-size: 0.72rem; letter-spacing: 0.05em; font-weight: 700;">CẦN THANH TOÁN CÒN LẠI</span>
        <span class="fw-bold text-success display-6" style="font-weight: 800; font-size: 1.8rem;">${fmt(remaining >= 0 ? remaining : 0)}</span>
      `;
    }
  }

  const footer = document.getElementById('det-modal-footer');
  let btnHtml = '<button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button>';

  if (isOutsideShift && (b.status === 'CONFIRMED' || b.status === 'CHECKED_IN')) {
    btnHtml += '<button class="btn btn-secondary px-3" disabled title="Khung giờ này không nằm trong ca trực của bạn"><i class="bi bi-lock-fill me-1"></i>Khóa</button>';
  } else if (currentShiftStatus === 'UPCOMING') {
    if (b.status === 'CHECKED_IN') {
      btnHtml += '<button class="btn btn-secondary px-3" disabled title="Chưa đến giờ làm việc"><i class="bi bi-lock-fill me-1"></i>Chờ ca trực</button>';
    }
  } else if (currentShiftStatus === 'COMPLETED') {
    if (b.status === 'CHECKED_IN') {
      btnHtml += '<button class="btn btn-secondary px-3" disabled title="Ca trực đã kết thúc"><i class="bi bi-lock-fill me-1"></i>Hết ca trực</button>';
    }
  } else {
    if (b.status === 'CHECKED_IN') {
      btnHtml += '<a href="<%= ctx %>/staff/checkout?id=' + b.bookingId + '" class="btn btn-success px-4">Checkout</a>';
    } else if ((b.status === 'PENDING_CHECKOUT_PAYMENT' || b.status === 'COMPLETED') && b.hasInvoice) {
      btnHtml += '<a href="<%= ctx %>/staff/invoice?id=' + b.bookingId + '" class="btn btn-outline-secondary px-4"><i class="bi bi-file-earmark-text me-1"></i>Hóa đơn</a>';
    }
  }

  footer.innerHTML = btnHtml;

  if (bookingDetailModalInstance) {
    bookingDetailModalInstance.show();
  }
}

// ── Modals for KPI Cards ─────────────────────────────────────────────────────
let globalTotalRevenueData = {};

function showCashModal(mode) {
  mode = mode || 'cash';
  activeParentModal = mode === 'cash' ? 'cash' : 'allRevenue';
  const modalEl = document.getElementById('cashDetailModal');
  if (!modalEl) return;

  if (!cashModalInstance) cashModalInstance = new bootstrap.Modal(modalEl);

  const titleEl = document.getElementById('cashDetailModalLabel');
  const totalLblEl = document.getElementById('cash-modal-label-total');
  const totalValEl = document.getElementById('cash-modal-total');
  const countValEl = document.getElementById('cash-modal-count');
  const tableHeadEl = document.getElementById('cash-modal-thead');
  const tbody = document.getElementById('cash-modal-tbody');

  const shiftName = document.getElementById('cash-shift-name').textContent || 'Ca này';
  document.getElementById('cash-modal-shift-name').textContent = shiftName;

  if (mode === 'cash') {
    if (titleEl) titleEl.innerHTML = '<i class="bi bi-cash-stack me-2 text-success"></i>Chi tiết tiền mặt thu được trong ca';
    if (totalLblEl) totalLblEl.textContent = 'Tổng tiền mặt thu';
    if (totalValEl) totalValEl.textContent = fmt(globalCashData.totalCash || 0);
    if (countValEl) countValEl.textContent = (globalCashData.txCount || 0) + ' giao dịch';

    if (tableHeadEl) {
      tableHeadEl.innerHTML = `
        <tr>
          <th>Mã HĐ / Booking</th>
          <th>Sân bóng</th>
          <th>Khách hàng</th>
          <th>Thời gian</th>
          <th class="text-end">Số tiền</th>
          <th class="text-center">Hóa đơn</th>
        </tr>
      `;
    }

    const txs = globalCashData.transactions || [];
    if (txs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Chưa có giao dịch tiền mặt nào trong ca này</td></tr>';
    } else {
      tbody.innerHTML = txs.map(t => `
        <tr class="booking-row" onclick="handleRowClick(event, ${t.bookingId})" style="cursor:pointer;" title="Nhấn để xem chi tiết đặt sân">
          <td><strong>${t.invoiceCode || t.bookingCode || '—'}</strong></td>
          <td><strong>${t.fieldName || '—'}</strong></td>
          <td>
            <div class="fw-bold">${t.customerName || '—'}</div>
            <div class="text-muted small">${fmtPhone(t.customerPhone)}</div>
          </td>
          <td><span class="badge bg-light text-dark">${timeOnly(t.issuedAt)}</span></td>
          <td class="text-end fw-bold text-success">${fmt(t.totalAmount)}</td>
          <td class="text-center">
            <a href="<%= ctx %>/staff/invoice?id=${t.bookingId}" class="btn btn-sm btn-outline-secondary">
              <i class="bi bi-file-earmark-text me-1"></i>Hóa đơn
            </a>
          </td>
        </tr>
      `).join('');
    }
  } else {
    // Mode 'all': Total revenue modal
    if (titleEl) titleEl.innerHTML = '<i class="bi bi-wallet2 me-2 text-success"></i>Chi tiết tổng tiền thu được trong ca';
    if (totalLblEl) totalLblEl.textContent = 'Tổng tiền thu được';
    if (totalValEl) totalValEl.textContent = fmt(globalTotalRevenueData.totalRevenue || globalTotalRevenueData.totalCash || 0);
    if (countValEl) countValEl.textContent = (globalTotalRevenueData.txCount || 0) + ' giao dịch';

    if (tableHeadEl) {
      tableHeadEl.innerHTML = `
        <tr>
          <th>Mã HĐ / Booking</th>
          <th>Sân bóng</th>
          <th>Khách hàng</th>
          <th>Phương thức</th>
          <th class="text-end">Số tiền</th>
          <th class="text-center">Hóa đơn</th>
        </tr>
      `;
    }

    const txs = globalTotalRevenueData.transactions || [];
    if (txs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Chưa có giao dịch nào trong ca này</td></tr>';
    } else {
      tbody.innerHTML = txs.map(t => {
        const pm = t.paymentMethodName || 'Tiền mặt';
        const pmBadgeClass = pm === 'Chuyển khoản' ? 'bg-primary-subtle text-primary border border-primary-subtle' : 'bg-success-subtle text-success border border-success-subtle';
        return `
          <tr class="booking-row" onclick="handleRowClick(event, ${t.bookingId})" style="cursor:pointer;" title="Nhấn để xem chi tiết đặt sân">
            <td><strong>${t.invoiceCode || t.bookingCode || '—'}</strong></td>
            <td><strong>${t.fieldName || '—'}</strong></td>
            <td>
              <div class="fw-bold">${t.customerName || '—'}</div>
              <div class="text-muted small">${fmtPhone(t.customerPhone)}</div>
            </td>
            <td><span class="badge ${pmBadgeClass}">${pm}</span></td>
            <td class="text-end fw-bold text-success">${fmt(t.totalAmount)}</td>
            <td class="text-center">
              <a href="<%= ctx %>/staff/invoice?id=${t.bookingId}" class="btn btn-sm btn-outline-secondary">
                <i class="bi bi-file-earmark-text me-1"></i>Hóa đơn
              </a>
            </td>
          </tr>
        `;
      }).join('');
    }
  }
  cashModalInstance.show();
}

function showCompletedBookingsModal() {
  activeParentModal = 'completed';
  const modalEl = document.getElementById('completedBookingsModal');
  if (modalEl) {
    if (!completedModalInstance) completedModalInstance = new bootstrap.Modal(modalEl);
    
    const completedList = todayBookings.filter(b => b.status === 'COMPLETED');
    document.getElementById('completed-modal-count').textContent = completedList.length;

    const tbody = document.getElementById('completed-modal-tbody');
    if (completedList.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Chưa có booking nào hoàn thành hôm nay</td></tr>';
    } else {
      tbody.innerHTML = completedList.map(b => `
        <tr class="booking-row" onclick="handleRowClick(event, ${b.bookingId})" style="cursor:pointer;" title="Nhấn để xem chi tiết đặt sân">
          <td><span class="time-badge">${timeOnly(b.startTime)}–${timeOnly(b.endTime)}</span></td>
          <td><strong>${b.bookingCode || '—'}</strong></td>
          <td><strong>${b.fieldName || '—'}</strong></td>
          <td>
            <div class="fw-bold">${b.customerName || '—'}</div>
            <div class="text-muted small">${fmtPhone(b.customerPhone)}</div>
          </td>
          <td class="text-end fw-bold">${fmt(b.totalAmount)}</td>
          <td class="text-center">
            ${b.hasInvoice ? `<a href="<%= ctx %>/staff/invoice?id=${b.bookingId}" class="btn btn-sm btn-outline-secondary"><i class="bi bi-file-earmark-text me-1"></i>Hóa đơn</a>` : '<span class="text-muted small">—</span>'}
          </td>
        </tr>
      `).join('');
    }
    completedModalInstance.show();
  }
}

function showPendingCheckinModal() {
  activeParentModal = 'pending';
  const modalEl = document.getElementById('pendingCheckinModal');
  if (modalEl) {
    if (!pendingModalInstance) pendingModalInstance = new bootstrap.Modal(modalEl);
    
    const pendingList = todayBookings.filter(b => b.status === 'CONFIRMED').sort((a, b) => {
      const expA = isBookingExpired(a.endTime);
      const expB = isBookingExpired(b.endTime);
      if (expA !== expB) return expA ? 1 : -1;
      return String(a.startTime || '').localeCompare(String(b.startTime || ''));
    });
    document.getElementById('pending-modal-count').textContent = pendingList.length;

    const tbody = document.getElementById('pending-modal-tbody');
    if (pendingList.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Không có khách nào đang chờ check-in</td></tr>';
    } else {
      tbody.innerHTML = pendingList.map(b => {
        const isExpired = isBookingExpired(b.endTime);
        return `
        <tr class="booking-row" onclick="handleRowClick(event, ${b.bookingId})" style="cursor:pointer;" title="Nhấn để xem chi tiết đặt sân">
          <td><span class="time-badge">${timeOnly(b.startTime)}–${timeOnly(b.endTime)}</span></td>
          <td><strong>${b.bookingCode || '—'}</strong></td>
          <td><strong>${b.fieldName || '—'}</strong></td>
          <td>
            <div class="fw-bold">${b.customerName || '—'}</div>
            <div class="text-muted small">${fmtPhone(b.customerPhone)}</div>
          </td>
          <td>${statusBadge(b.status, false, isExpired, b.startTime, b.endTime)}</td>
          <td class="text-center">
            ${actionBtn(b.status, b.bookingId, isExpired, b.hasInvoice, b.checkoutDue, b.startTime, b.endTime)}
          </td>
        </tr>
      `;
      }).join('');
    }
    pendingModalInstance.show();
  }
}

async function cancelNoshow(bookingId) {
  showConfirm('Xác nhận hủy đặt sân này do khách hàng không đến nhận sân sau 30 phút?', async () => {
    try {
      const params = new URLSearchParams();
      params.append('bookingId', bookingId);

      const res = await fetch('<%= ctx %>/api/staff/checkin/noshow', {
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
        if (bookingDetailModalInstance) {
          bookingDetailModalInstance.hide();
        }
        showToastAfterReload('Đã hủy đặt sân thành công (Khách không đến)', 'success');
        window.location.reload();
      } else {
        showToast('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'), 'danger');
      }
    } catch (err) {
      showToast('Lỗi khi hủy đặt sân: ' + err.message, 'danger');
    }
  });
}

async function cancelNoshow(bookingId) {
  showConfirm('Xác nhận hủy đặt sân này do khách hàng không đến nhận sân sau 30 phút?', async () => {
    try {
      const params = new URLSearchParams();
      params.append('bookingId', bookingId);

      const res = await fetch('<%= ctx %>/api/staff/checkin/noshow', {
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
        const modalEl = document.getElementById('checkinConfirmModal');
        if (modalEl && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
          const modalInstance = bootstrap.Modal.getInstance(modalEl);
          if (modalInstance) modalInstance.hide();
        }
        if (typeof bookingDetailModalInstance !== 'undefined' && bookingDetailModalInstance) bookingDetailModalInstance.hide();
        showToastAfterReload('Đã hủy đặt sân thành công (Khách không đến)', 'success');
        window.location.reload();
      } else {
        showToast('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'), 'danger');
      }
    } catch (err) {
      showToast('Lỗi khi hủy đặt sân: ' + err.message, 'danger');
    }
  });
}

function openCheckinModal(bookingId) {
  const b = todayBookings.find(x => x.bookingId === bookingId);
  if (!b) return;

  const isExpired = isBookingExpired(b.endTime) || b.notExpired === false;
  const isLateNoShow = isBookingLateNoShow(b.startTime, b.endTime);

  document.getElementById('chk-modal-code').textContent = b.bookingCode || '—';
  document.getElementById('chk-modal-name').textContent = b.customerName || '—';
  document.getElementById('chk-modal-phone').textContent = fmtPhone(b.customerPhone);
  document.getElementById('chk-modal-field').textContent = b.fieldName || '—';
  document.getElementById('chk-modal-time').textContent = timeOnly(b.startTime) + ' - ' + timeOnly(b.endTime);

  document.getElementById('chk-modal-price').textContent = fmt(b.totalAmount);
  document.getElementById('chk-modal-deposit').textContent = fmt(b.depositAmount);
  const rem = (b.totalAmount || 0) - (b.depositAmount || 0);
  document.getElementById('chk-modal-remaining').textContent = fmt(rem >= 0 ? rem : 0);

  document.getElementById('chk-modal-booking-id').value = b.bookingId;
  document.getElementById('chk-modal-note').value = '';

  const alertBox = document.getElementById('chk-modal-alert');
  const actionsBox = document.getElementById('chk-modal-actions');

  const isOutsideShift = typeof isTimeInShiftStr === 'function' && !isTimeInShiftStr(b.startTime, globalShiftStart, globalShiftEnd);

  if (isOutsideShift) {
    if (alertBox) {
      alertBox.innerHTML = '<div class="d-flex align-items-center gap-2 p-3 mb-3 rounded-4" style="background-color:#fffbeb;border:1px solid #fde68a;color:#92400e;font-size:0.85rem;font-weight:600;"><i class="bi bi-exclamation-triangle-fill fs-5 text-warning flex-shrink-0"></i><div>Khung giờ của sân này không nằm trong ca trực của bạn. Không thể thao tác.</div></div>';
      alertBox.classList.remove('d-none');
    }
    if (actionsBox) {
      actionsBox.innerHTML = '<button type="button" class="btn btn-secondary px-4 py-2 rounded-3" disabled><i class="bi bi-lock-fill me-1"></i>Khóa</button>';
    }
  } else if (isExpired) {
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
        '<button type="button" class="btn btn-danger px-4 py-2 rounded-3" onclick="cancelNoshow(' + b.bookingId + ')"><i class="bi bi-x-circle me-1"></i>Hủy sân (Khách không đến)</button>' +
        '<button type="button" class="btn btn-success px-4 py-2 rounded-3" id="chk-modal-submit-btn" onclick="submitCheckinForm(event)"><i class="bi bi-check-circle me-1"></i>Xác nhận Check-in</button>';
    }
  } else {
    if (alertBox) {
      alertBox.innerHTML = '';
      alertBox.classList.add('d-none');
    }
    if (actionsBox) {
      actionsBox.innerHTML = '<button type="button" class="btn btn-success px-4 py-2 rounded-3" id="chk-modal-submit-btn" onclick="submitCheckinForm(event)"><i class="bi bi-check-circle me-1"></i>Xác nhận Check-in</button>';
    }
  }

  const modalEl = document.getElementById('checkinConfirmModal');
  if (modalEl && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
    modalInstance.show();
  }
}

async function submitCheckinForm(e) {
  if (e) e.preventDefault();
  const bookingId = document.getElementById('chk-modal-booking-id').value;
  const note = document.getElementById('chk-modal-note').value.trim();
  const btn = document.getElementById('chk-modal-submit-btn');

  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang check-in...';
  }

  try {
    const params = new URLSearchParams();
    params.append('bookingId', bookingId);
    if (note) params.append('note', note);

    const res = await fetch('<%= ctx %>/api/staff/checkin', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params,
      credentials: 'include'
    });

    const data = await res.json().catch(() => ({}));
    if (res.ok && (data.success || !data.error)) {
      const modalEl = document.getElementById('checkinConfirmModal');
      if (modalEl && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) modalInstance.hide();
      }
      if (typeof showToastAfterReload === 'function') {
        showToastAfterReload('Đã check-in thành công!', 'success');
      }
      window.location.reload();
    } else {
      if (typeof showToast === 'function') {
        showToast('Không thể check-in: ' + (data.error || 'Lỗi không xác định'), 'danger');
      } else {
        alert('Không thể check-in: ' + (data.error || 'Lỗi không xác định'));
      }
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Xác nhận Check-in';
      }
    }
  } catch (err) {
    if (typeof showToast === 'function') {
      showToast('Lỗi khi thực hiện check-in: ' + err.message, 'danger');
    } else {
      alert('Lỗi khi thực hiện check-in: ' + err.message);
    }
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Xác nhận Check-in';
    }
  }
}

function checkShiftError() {
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('error') === 'not_in_shift') {
    const alertEl = document.getElementById('shift-restriction-alert');
    if (alertEl) {
      alertEl.classList.remove('d-none');
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }
}

let previousOpenParentId = null;

function initStackedModalManager() {
  document.addEventListener('show.bs.modal', function (e) {
    const targetModal = e.target;
    if (targetModal.id === 'checkinConfirmModal' || targetModal.id === 'bookingDetailModal') {
      const parentModals = Array.from(document.querySelectorAll('.modal.show')).filter(m => m !== targetModal);
      if (parentModals.length > 0) {
        const parentModal = parentModals[0];
        previousOpenParentId = parentModal.id;
        const bsParent = bootstrap.Modal.getInstance(parentModal);
        if (bsParent) {
          bsParent.hide();
        }
      }
    }
  });

  document.addEventListener('hidden.bs.modal', function (e) {
    const targetModal = e.target;
    if ((targetModal.id === 'checkinConfirmModal' || targetModal.id === 'bookingDetailModal') && previousOpenParentId) {
      const parentIdToRestore = previousOpenParentId;
      previousOpenParentId = null;
      setTimeout(() => {
        const parentEl = document.getElementById(parentIdToRestore);
        if (parentEl) {
          let parentInstance = bootstrap.Modal.getInstance(parentEl);
          if (!parentInstance) {
            parentInstance = new bootstrap.Modal(parentEl);
          }
          parentInstance.show();
        }
      }, 150);
    }
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    loadDashboard();
    checkShiftError();
    initStackedModalManager();
  });
} else {
  loadDashboard();
  checkShiftError();
  initStackedModalManager();
}
</script>
</body>
</html>

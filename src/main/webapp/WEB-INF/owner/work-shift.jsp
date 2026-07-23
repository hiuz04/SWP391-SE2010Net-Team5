<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.FootballComplex" %>
<%@ page import="com.swp.model.WorkShift" %>
<%@ page import="com.swp.dao.WorkShiftDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";

    List<FootballComplex> complexes = (List<FootballComplex>) request.getAttribute("complexes");
    List<User> staffList = (List<User>) request.getAttribute("staffList");
    List<WorkShift> shifts = (List<WorkShift>) request.getAttribute("shifts");
    Map<Long, Integer> staffShiftCounts = (Map<Long, Integer>) request.getAttribute("staffShiftCounts");
    WorkShiftDAO workShiftDAO = new WorkShiftDAO();

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    
    List<Map<String, Object>> staffListForJson = new java.util.ArrayList<>();
    if (staffList != null) {
        for (User staff : staffList) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("userId", staff.getUserId());
            map.put("fullName", staff.getFullName());
            map.put("phone", staff.getPhone());
            map.put("email", staff.getEmail());
            staffListForJson.add(map);
        }
    }
    com.google.gson.Gson gsonForJsp = new com.google.gson.Gson();
    String staffListJson = gsonForJsp.toJson(staffListForJson);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quản lý ca trực & Nhân sự | Sport Field Booking</title>
  <script>
    window.onerror = function(message, source, lineno, colno, error) {
      const errDiv = document.createElement('div');
      errDiv.style.position = 'fixed';
      errDiv.style.top = '10px';
      errDiv.style.left = '50%';
      errDiv.style.transform = 'translateX(-50%)';
      errDiv.style.zIndex = '99999';
      errDiv.style.background = '#f8d7da';
      errDiv.style.color = '#721c24';
      errDiv.style.border = '1px solid #f5c6cb';
      errDiv.style.padding = '15px';
      errDiv.style.borderRadius = '5px';
      errDiv.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';
      errDiv.style.maxWidth = '90%';
      errDiv.style.wordBreak = 'break-all';
      errDiv.innerHTML = '<strong>Lỗi JavaScript phát sinh:</strong><br>' + message + '<br><small>Tệp: ' + source + ' (Dòng ' + lineno + ')</small>';
      document.body.appendChild(errDiv);
      return false;
    };
  </script>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/owner/styles.css" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; font-family: 'Inter', sans-serif; }
    .soft-card {
      border-radius: 20px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 8px 30px rgba(15,23,42,.03);
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .soft-card:hover {
      box-shadow: 0 12px 35px rgba(15,23,42,.06);
    }
    .nav-pills .nav-link {
      color: #64748b;
      font-weight: 500;
      padding: 10px 20px;
      border-radius: 12px;
      transition: all 0.2s ease;
    }
    .nav-pills .nav-link.active {
      background-color: #10b981;
      color: #fff;
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);
    }
    .nav-pills .nav-link:hover:not(.active) {
      background-color: #f1f5f9;
      color: #0f172a;
    }
    .btn-sf-primary {
      background-color: #10b981;
      color: #ffffff;
      border: none;
      padding: 10px 20px;
      border-radius: 12px;
      font-weight: 600;
      transition: all 0.2s ease;
    }
    .btn-sf-primary:hover {
      background-color: #059669;
      color: #ffffff;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.15);
    }
    .btn-sf-outline-danger {
      color: #ef4444;
      border: 1.5px solid #fca5a5;
      background: transparent;
      padding: 8px 16px;
      border-radius: 10px;
      font-weight: 500;
      transition: all 0.15s ease;
    }
    .btn-sf-outline-danger:hover {
      color: white;
      background-color: #ef4444;
      border-color: #ef4444;
    }
    .btn-sf-outline-success {
      color: #10b981;
      border: 1.5px solid #a7f3d0;
      background: transparent;
      padding: 8px 16px;
      border-radius: 10px;
      font-weight: 500;
      transition: all 0.15s ease;
    }
    .btn-sf-outline-success:hover {
      color: white;
      background-color: #10b981;
      border-color: #10b981;
    }
    .avatar-circle {
      width: 45px;
      height: 45px;
      border-radius: 50%;
      background-color: #e2e8f0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      color: #475569;
      border: 2px solid #ffffff;
      box-shadow: 0 2px 5px rgba(0,0,0,0.05);
    }
    .avatar-stack {
      display: flex;
      align-items: center;
    }
    .avatar-stack .avatar-circle {
      margin-right: -10px;
      transition: transform 0.2s;
    }
    .avatar-stack .avatar-circle:hover {
      transform: scale(1.1);
      z-index: 10;
    }
    .table-responsive {
      border-radius: 12px;
      overflow: hidden;
    }
    .custom-table th {
      background-color: #f8fafc;
      color: #475569;
      font-weight: 600;
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-bottom: 2px solid #e2e8f0;
      padding: 15px;
    }
    .custom-table td {
      padding: 15px;
      vertical-align: middle;
      border-bottom: 1px solid #f1f5f9;
    }
    .stat-badge {
      font-size: 0.8rem;
      padding: 6px 12px;
      border-radius: 20px;
      font-weight: 550;
    }
    .bg-soft-emerald {
      background-color: #d1fae5;
      color: #065f46;
    }
    .bg-soft-amber {
      background-color: #fef3c7;
      color: #92400e;
    }
    .form-control, .form-select {
      border-radius: 10px;
      padding: 10px 14px;
      border: 1px solid #cbd5e1;
    }
    .form-control:focus, .form-select:focus {
      border-color: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
    }
    .modal-content {
      border-radius: 20px;
      border: none;
      box-shadow: 0 15px 40px rgba(0,0,0,0.1);
    }
    .modal-header {
      background-color: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
      border-top-left-radius: 20px;
      border-top-right-radius: 20px;
      padding: 20px;
    }
    .modal-footer {
      background-color: #f8fafc;
      border-top: 1px solid #e2e8f0;
      border-bottom-left-radius: 20px;
      border-bottom-right-radius: 20px;
      padding: 15px 20px;
    }
    .staff-card {
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      padding: 16px;
      margin-bottom: 16px;
      transition: all 0.2s;
    }
    .staff-card:hover {
      border-color: #10b981;
      box-shadow: 0 5px 15px rgba(16, 185, 129, 0.05);
    }
    .badge-shift-count {
      background-color: #f1f5f9;
      color: #475569;
      font-size: 0.75rem;
      font-weight: 600;
      padding: 4px 8px;
      border-radius: 6px;
    }
    @keyframes pulse-glow {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.75; transform: scale(0.97); }
    }
    .badge-ongoing {
      background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
      color: #ffffff;
      animation: pulse-glow 1.8s infinite ease-in-out;
      font-weight: 600;
      border: 1px solid rgba(239, 68, 68, 0.25);
      box-shadow: 0 2px 6px rgba(239, 68, 68, 0.35);
    }
  </style>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Ca trực"></div>

<main class="owner-content">

      <div class="container">

        <!-- Page Header -->
        <div class="page-header">
          <div class="page-header-left">
            <h1><i class="bi bi-clock-history me-2"></i>Quản lý Ca trực & Phân công</h1>
            <p>Thiết lập lịch trực, phân ca làm việc cho nhân viên và quản lý nhân sự tại các cơ sở sân bóng.</p>
          </div>
          <button class="btn btn-success px-4 py-2" onclick="openAddShiftModal()">
            <i class="bi bi-plus-lg me-1"></i>
            Thêm ca làm việc
          </button>
        </div>

        <!-- Quick Metrics -->
        <div class="row g-4 mb-4">
          <div class="col-md-4">
            <div class="card soft-card p-4">
              <div class="d-flex align-items-center justify-content-between">
                <div>
                  <span class="text-muted small fw-semibold">Tổng số nhân sự</span>
                  <h3 class="fw-bold mt-1 mb-0 text-slate-800"><%= staffList.size() %></h3>
                </div>
                <div class="p-3 bg-emerald-50 text-emerald-600 rounded-3" style="background:#e6fbf4; color:#10b981;">
                  <i class="bi bi-people-fill fs-4"></i>
                </div>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card soft-card p-4">
              <div class="d-flex align-items-center justify-content-between">
                <div>
                  <span class="text-muted small fw-semibold">Tổng số ca trực</span>
                  <h3 class="fw-bold mt-1 mb-0"><%= shifts.size() %></h3>
                </div>
                <div class="p-3 bg-blue-50 text-blue-600 rounded-3" style="background:#eff6ff; color:#3b82f6;">
                  <i class="bi bi-calendar-range fs-4"></i>
                </div>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card soft-card p-4">
              <div class="d-flex align-items-center justify-content-between">
                <div>
                  <span class="text-muted small fw-semibold">Cơ sở hoạt động</span>
                  <h3 class="fw-bold mt-1 mb-0"><%= complexes.size() %></h3>
                </div>
                <div class="p-3 bg-amber-50 text-amber-600 rounded-3" style="background:#fffbeb; color:#f59e0b;">
                  <i class="bi bi-geo-alt-fill fs-4"></i>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Navigation & Filters -->
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
          <ul class="nav nav-pills" id="shiftTabs" role="tablist">
            <li class="nav-item" role="presentation">
              <button class="nav-link active" id="shifts-tab" data-bs-toggle="tab" data-bs-target="#shifts-panel" type="button" role="tab" aria-controls="shifts-panel" aria-selected="true">
                <i class="bi bi-calendar-event me-2"></i>Lịch Trực
              </button>
            </li>
            <li class="nav-item" role="presentation">
              <button class="nav-link" id="staff-tab" data-bs-toggle="tab" data-bs-target="#staff-panel" type="button" role="tab" aria-controls="staff-panel" aria-selected="false">
                <i class="bi bi-person-lines-fill me-2"></i>Danh Sách Nhân Sự
              </button>
            </li>
          </ul>

          <!-- FootballComplex filter -->
          <div class="d-flex gap-2">
            <label for="complex-filter" class="visually-hidden">Lọc theo cơ sở</label>
            <select class="form-select border-0 shadow-sm" id="complex-filter" style="min-width: 220px;" onchange="filterShifts()">
              <option value="ALL">Tất cả cơ sở</option>
              <% for (FootballComplex fc : complexes) { %>
                <option value="<%= fc.getComplexId() %>"><%= fc.getComplexName() %></option>
              <% } %>
            </select>
          </div>
        </div>

        <!-- Tab Panels -->
        <div class="tab-content" id="shiftTabContent">

          <!-- Panel 1: Lịch Trực (Shift Table) -->
          <div class="tab-pane fade show active" id="shifts-panel" role="tabpanel" aria-labelledby="shifts-tab">
            <div class="card soft-card p-4">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold mb-0"><i class="bi bi-table text-emerald-600 me-2" style="color:#10b981;"></i>Danh sách ca trực đã thiết lập</h5>
                <button class="btn btn-danger btn-sm fw-bold px-3 py-2 rounded-3" id="btnDeleteSelected" style="display: none;" onclick="deleteSelectedShifts()">
                  <i class="bi bi-trash3-fill me-1"></i>Xóa ca đã chọn (<span id="selectedCount">0</span>)
                </button>
              </div>

              <!-- Advanced Filters Bar -->
              <div class="row g-3 mb-4 p-3 rounded-3" style="background: #f8fafc; border: 1px solid #e2e8f0; margin-left: 0; margin-right: 0;">
                <div class="col-md-3">
                  <label for="filter-shift-name" class="form-label small fw-bold text-muted mb-1">Tìm theo tên ca</label>
                  <div class="input-group input-group-sm">
                    <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
                    <input type="text" class="form-control border-start-0" id="filter-shift-name" placeholder="Ví dụ: Ca sáng..." onkeyup="filterShifts()">
                  </div>
                </div>
                <div class="col-md-3">
                  <label for="filter-shift-date" class="form-label small fw-bold text-muted mb-1">Lọc theo ngày</label>
                  <div class="input-group input-group-sm">
                    <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-calendar3"></i></span>
                    <input type="date" class="form-control border-start-0" id="filter-shift-date" onchange="filterShifts()">
                  </div>
                </div>
                <div class="col-md-3">
                  <label for="filter-shift-time" class="form-label small fw-bold text-muted mb-1">Lọc theo giờ trực</label>
                  <div class="input-group input-group-sm">
                    <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-clock"></i></span>
                    <input type="text" class="form-control border-start-0" id="filter-shift-time" placeholder="Ví dụ: 08:00..." onkeyup="filterShifts()">
                  </div>
                </div>
                <div class="col-md-3 d-flex align-items-end">
                  <button class="btn btn-outline-secondary btn-sm w-100 fw-semibold" type="button" onclick="resetFilters()">
                    <i class="bi bi-arrow-counterclockwise me-1"></i>Đặt lại bộ lọc
                  </button>
                </div>
              </div>

              <div class="table-responsive">
                <table class="table custom-table align-middle" id="shiftsTable">
                  <thead>
                    <tr>
                      <th style="width: 40px;"><input type="checkbox" id="selectAllShifts" onclick="toggleSelectAllShifts(this)"></th>
                      <th>Tên ca trực</th>
                      <th>Cơ sở</th>
                      <th>Ngày trực</th>
                      <th>Khung giờ</th>
                      <th>Nhân viên trực</th>
                      <th class="text-end">Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    <% if (shifts == null || shifts.isEmpty()) { %>
                      <tr class="no-shifts-row">
                        <td colspan="7" class="text-center text-muted py-5">
                          <i class="bi bi-calendar-x fs-1 d-block mb-3 opacity-50"></i>
                          Chưa có ca làm việc nào được thiết lập. Hãy nhấn "Thêm ca làm việc" để bắt đầu.
                        </td>
                      </tr>
                    <% } else {
                      java.time.LocalDateTime now = java.time.LocalDateTime.now();
                      for (WorkShift ws : shifts) {
                        FootballComplex curFc = null;
                        for (FootballComplex fc : complexes) {
                          if (fc.getComplexId().equals(ws.getComplexId())) {
                            curFc = fc;
                            break;
                          }
                        }
                        String fcName = curFc != null ? curFc.getComplexName() : "Không rõ";

                        // Fetch assigned staff list
                        List<User> assigned = workShiftDAO.getStaffAssignedToShift(ws.getShiftId());
                        Long assignedStaffId = assigned.isEmpty() ? null : assigned.get(0).getUserId();

                        boolean isPast = false;
                        boolean isOngoing = false;
                        if (ws.getShiftDate() != null && ws.getStartTime() != null && ws.getEndTime() != null) {
                          java.time.LocalDateTime shiftStart = java.time.LocalDateTime.of(ws.getShiftDate(), ws.getStartTime());
                          java.time.LocalDateTime shiftEnd;
                          if (ws.getEndTime().isBefore(ws.getStartTime())) {
                            shiftEnd = java.time.LocalDateTime.of(ws.getShiftDate().plusDays(1), ws.getEndTime());
                          } else {
                            shiftEnd = java.time.LocalDateTime.of(ws.getShiftDate(), ws.getEndTime());
                          }
                          isPast = shiftEnd.isBefore(now);
                          isOngoing = !isPast && !now.isBefore(shiftStart);
                        }
                    %>
                      <tr class="shift-row" data-complex-id="<%= ws.getComplexId() %>">
                        <td>
                          <% if (isPast) { %>
                            <input type="checkbox" disabled class="form-check-input">
                          <% } else { %>
                            <input type="checkbox" class="form-check-input shift-checkbox" value="<%= ws.getShiftId() %>" onclick="updateDeleteSelectedButtonVisibility()">
                          <% } %>
                        </td>
                        <td>
                          <strong class="text-slate-800"><%= ws.getShiftName() %></strong>
                        </td>
                        <td>
                          <span class="badge bg-light text-dark p-2 border"><%= fcName %></span>
                        </td>
                        <td>
                          <span class="text-secondary"><%= ws.getShiftDate().format(dateFormatter) %></span>
                        </td>
                        <td>
                          <span class="stat-badge bg-soft-emerald"><i class="bi bi-clock me-1"></i><%= ws.getStartTime().format(timeFormatter) %> - <%= ws.getEndTime().format(timeFormatter) %></span>
                          <% if (isOngoing) { %>
                            <span class="badge badge-ongoing ms-1" style="font-size: 0.75rem;"><i class="bi bi-broadcast me-1"></i>Đang diễn ra</span>
                          <% } %>
                        </td>
                        <td>
                          <div class="d-flex align-items-center gap-2">
                            <% if (assigned.isEmpty()) { %>
                              <span class="text-danger small fw-semibold"><i class="bi bi-exclamation-triangle-fill me-1"></i>Chưa phân công</span>
                            <% } else { %>
                              <div class="avatar-stack">
                                <% for (User staff : assigned) {
                                  String initials = staff.getFullName().substring(0, Math.min(staff.getFullName().length(), 2)).toUpperCase();
                                %>
                                  <div class="avatar-circle" title="<%= staff.getFullName() %> (<%= staff.getPhone() %>)">
                                    <%= initials %>
                                  </div>
                                <% } %>
                              </div>
                            <% } %>
                          </div>
                        </td>
                        <td class="text-end">
                          <% if (isPast) { %>
                            <span class="badge bg-secondary-subtle text-secondary px-2.5 py-1.5 border border-secondary-subtle">
                              <i class="bi bi-lock-fill me-1"></i>Chỉ xem
                            </span>
                          <% } else { %>
                            <div class="d-inline-flex gap-2">
                              <button class="btn btn-sm btn-outline-success border-0" title="Phân công nhân viên"
                                      onclick="openAssignModal(<%= ws.getShiftId() %>, '<%= ws.getShiftName() %>')">
                                <i class="bi bi-person-plus-fill fs-5"></i>
                              </button>
                              <button class="btn btn-sm btn-outline-primary border-0" title="Sửa ca"
                                      onclick="openEditShiftModal(<%= ws.getShiftId() %>, <%= ws.getComplexId() %>, '<%= ws.getShiftName() %>', '<%= ws.getShiftDate() %>', '<%= ws.getStartTime() %>', '<%= ws.getEndTime() %>', <%= assignedStaffId != null ? assignedStaffId : "''" %>)">
                                <i class="bi bi-pencil-square fs-5"></i>
                              </button>
                              <button class="btn btn-sm btn-outline-danger border-0" title="Xóa ca"
                                      onclick="deleteShift(<%= ws.getShiftId() %>)">
                                <i class="bi bi-trash3-fill fs-5"></i>
                              </button>
                            </div>
                          <% } %>
                        </td>
                      </tr>
                    <% }
                    } %>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- Panel 2: Danh Sách Nhân Sự -->
          <div class="tab-pane fade" id="staff-panel" role="tabpanel" aria-labelledby="staff-tab">
            <div class="card soft-card p-4">
              <h5 class="fw-bold mb-4"><i class="bi bi-people-fill text-emerald-600 me-2" style="color:#10b981;"></i>Thông tin nhân sự sân bóng</h5>

              <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
                <% if (staffList == null || staffList.isEmpty()) { %>
                  <div class="col-12 text-center text-muted py-5">
                    Chưa có nhân viên nào có tài khoản ACTIVE.
                  </div>
                <% } else {
                  for (User staff : staffList) {
                    int shiftCount = staffShiftCounts.getOrDefault(staff.getUserId(), 0);
                %>
                  <div class="col">
                    <div class="staff-card d-flex flex-column justify-content-between h-100">
                      <div>
                        <div class="d-flex align-items-center gap-3 mb-3">
                          <div class="avatar-circle fs-5 bg-emerald-100 text-emerald-800" style="width:55px; height:55px; background-color:#d1fae5; color:#065f46;">
                            <%= staff.getFullName().substring(0, Math.min(staff.getFullName().length(), 2)).toUpperCase() %>
                          </div>
                          <div>
                            <h6 class="fw-bold mb-0 text-slate-800"><%= staff.getFullName() %></h6>
                            <span class="text-success small fw-semibold"><%= staff.getRoleName() %></span>
                          </div>
                        </div>
                        <div class="text-secondary small mb-2"><i class="bi bi-telephone me-2"></i><%= staff.getPhone() %></div>
                        <div class="text-secondary small mb-3"><i class="bi bi-envelope me-2"></i><%= staff.getEmail() %></div>
                      </div>
                      <div class="border-top pt-3 d-flex align-items-center justify-content-between">
                        <span class="small text-muted">Số ca trực được giao</span>
                        <span class="badge-shift-count"><%= shiftCount %> ca</span>
                      </div>
                    </div>
                  </div>
                <% }
                } %>
              </div>
            </div>
          </div>

        </div>
      </div>
    </main>

<!-- Modal: Thêm / Sửa ca làm việc -->
<div class="modal fade" id="shiftModal" tabindex="-1" aria-labelledby="shiftModalTitle" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title fw-bold" id="shiftModalTitle">Thêm ca làm việc mới</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <form id="shiftForm" onsubmit="submitShiftForm(event)">
        <input type="hidden" id="modal-shift-id">
        <div class="modal-body">

          <div class="mb-3">
            <label for="modal-complex" class="form-label small fw-bold text-muted">Cơ sở / Địa điểm</label>
            <select class="form-select" id="modal-complex" required>
              <option value="" disabled selected>-- Chọn cơ sở --</option>
              <% for (FootballComplex fc : complexes) { %>
                <option value="<%= fc.getComplexId() %>"><%= fc.getComplexName() %></option>
              <% } %>
            </select>
          </div>

          <div class="mb-3" id="creation-mode-container">
            <label for="modal-create-mode" class="form-label small fw-bold text-muted">Chế độ tạo</label>
            <select class="form-select" id="modal-create-mode" onchange="toggleCreationMode()">
              <option value="single" selected>Tạo một ca duy nhất</option>
              <option value="batch">Tạo hàng loạt (Theo tuần/tháng)</option>
            </select>
          </div>

          <div class="mb-3" style="display: none;">
            <label for="modal-shift-name" class="form-label small fw-bold text-muted">Tên ca làm việc</label>
            <select class="form-select" id="modal-shift-name">
              <option value="Auto" selected>Auto</option>
            </select>
          </div>

          <div class="mb-3" id="single-date-container">
            <label for="modal-shift-date" class="form-label small fw-bold text-muted">Ngày trực</label>
            <input type="date" class="form-control" id="modal-shift-date" required>
          </div>

          <div id="batch-container" style="display: none;">
            <div class="row">
              <div class="col-6 mb-3">
                <label for="modal-start-date" class="form-label small fw-bold text-muted">Từ ngày</label>
                <input type="date" class="form-control" id="modal-start-date">
              </div>
              <div class="col-6 mb-3">
                <label for="modal-end-date" class="form-label small fw-bold text-muted">Đến ngày</label>
                <input type="date" class="form-control" id="modal-end-date">
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label small fw-bold text-muted d-block">Lặp vào các ngày</label>
              <div class="d-flex flex-wrap gap-2">
                <div class="form-check form-check-inline">
                  <input class="form-check-input repeat-day-cb" type="checkbox" id="cb-mon" value="1">
                  <label class="form-check-label small" for="cb-mon">T2</label>
                </div>
                <div class="form-check form-check-inline">
                  <input class="form-check-input repeat-day-cb" type="checkbox" id="cb-tue" value="2">
                  <label class="form-check-label small" for="cb-tue">T3</label>
                </div>
                <div class="form-check form-check-inline">
                  <input class="form-check-input repeat-day-cb" type="checkbox" id="cb-wed" value="3">
                  <label class="form-check-label small" for="cb-wed">T4</label>
                </div>
                <div class="form-check form-check-inline">
                  <input class="form-check-input repeat-day-cb" type="checkbox" id="cb-thu" value="4">
                  <label class="form-check-label small" for="cb-thu">T5</label>
                </div>
                <div class="form-check form-check-inline">
                  <input class="form-check-input repeat-day-cb" type="checkbox" id="cb-fri" value="5">
                  <label class="form-check-label small" for="cb-fri">T6</label>
                </div>
                <div class="form-check form-check-inline">
                  <input class="form-check-input repeat-day-cb" type="checkbox" id="cb-sat" value="6">
                  <label class="form-check-label small" for="cb-sat">T7</label>
                </div>
                <div class="form-check form-check-inline">
                  <input class="form-check-input repeat-day-cb" type="checkbox" id="cb-sun" value="7">
                  <label class="form-check-label small" for="cb-sun">CN</label>
                </div>
              </div>
            </div>
          </div>

          <div class="row">
            <div class="col-6 mb-3">
              <label for="modal-start-time" class="form-label small fw-bold text-muted">Giờ bắt đầu</label>
              <input type="time" class="form-control" id="modal-start-time" required>
            </div>
            <div class="col-6 mb-3">
              <label for="modal-end-time" class="form-label small fw-bold text-muted">Giờ kết thúc</label>
              <input type="time" class="form-control" id="modal-end-time" required>
            </div>
          </div>

          <div class="mb-3">
            <label for="modal-staff-id" class="form-label small fw-bold text-muted">Nhân viên trực</label>
            <select class="form-select" id="modal-staff-id">
              <option value="">-- Chưa phân công --</option>
              <% if (staffList != null) {
                   for (User staff : staffList) { %>
                     <option value="<%= staff.getUserId() %>"><%= staff.getFullName() %> (<%= staff.getPhone() %>)</option>
              <%   }
                 } %>
            </select>
          </div>

        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-light px-4 py-2.5 rounded-3 fw-semibold" data-bs-dismiss="modal">Đóng</button>
          <button type="submit" class="btn btn-sf-primary px-4 py-2.5" id="submitBtn">Lưu ca trực</button>
        </div>
      </form>
    </div>
  </div>
</div>

<!-- Modal: Phân công nhân viên vào ca -->
<div class="modal fade" id="assignModal" tabindex="-1" aria-labelledby="assignModalTitle" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <div>
          <h5 class="modal-title fw-bold" id="assignModalTitle">Phân công nhân viên</h5>
          <span class="text-muted small" id="assignModalSub">Ca làm việc: <strong class="text-success" id="assignShiftName"></strong></span>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body py-4">
        <input type="hidden" id="assign-shift-id">

        <h6 class="fw-bold mb-3"><i class="bi bi-people-fill text-success me-2"></i>Chọn nhân viên để phân công</h6>
        <div class="row row-cols-1 row-cols-md-2 g-3" id="assignStaffContainer">
          <!-- Dynamically populated via javascript fetch -->
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-sf-primary px-4 py-2.5" data-bs-dismiss="modal">Hoàn tất</button>
      </div>
    </div>
  </div>
</div>

<div id="footer" data-root="../../"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
  let shiftModal = null;
  let assignModal = null;

  function initModals() {
    const modalEl = document.getElementById('shiftModal');
    if (modalEl) shiftModal = new bootstrap.Modal(modalEl);

    const assignModalEl = document.getElementById('assignModal');
    if (assignModalEl) assignModal = new bootstrap.Modal(assignModalEl);
    
    // Autofill today's date on add modal
    const today = new Date().toISOString().split('T')[0];
    const shiftDateEl = document.getElementById('modal-shift-date');
    if (shiftDateEl) shiftDateEl.value = today;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initModals);
  } else {
    initModals();
  }

  // Unified shift filter: checks complex, name search, date filter, and time search
  function filterShifts() {
    const selectedComplex = document.getElementById('complex-filter').value;
    const searchName = document.getElementById('filter-shift-name').value.trim().toLowerCase();
    const filterDate = document.getElementById('filter-shift-date').value;
    const searchTime = document.getElementById('filter-shift-time').value.trim();

    const rows = document.querySelectorAll('.shift-row');
    let visibleCount = 0;

    rows.forEach(row => {
      // 1. FootballComplex match
      const fcId = row.getAttribute('data-complex-id');
      const matchComplex = (selectedComplex === 'ALL' || fcId === selectedComplex);

      // 2. Name match (2nd column: td:nth-child(2))
      const nameText = row.querySelector('td:nth-child(2) strong').innerText.toLowerCase();
      const matchName = nameText.includes(searchName);

      // 3. Date match (4th column: td:nth-child(4))
      let matchDate = true;
      if (filterDate) {
        const parts = filterDate.split('-'); // [YYYY, MM, DD]
        const formattedFilterDate = parts[2] + '/' + parts[1] + '/' + parts[0]; // DD/MM/YYYY
        const rowDateText = row.querySelector('td:nth-child(4) span').innerText.trim();
        matchDate = (rowDateText === formattedFilterDate);
      }

      // 4. Time match (5th column: td:nth-child(5))
      const timeText = row.querySelector('td:nth-child(5) span').innerText.trim().toLowerCase();
      const matchTime = timeText.includes(searchTime.toLowerCase());

      if (matchComplex && matchName && matchDate && matchTime) {
        row.style.display = '';
        visibleCount++;
      } else {
        row.style.display = 'none';
      }
    });

    // Handle empty table states
    const tbody = document.querySelector('#shiftsTable tbody');
    const existingTempRow = document.querySelector('.no-shifts-row-temp');
    if (existingTempRow) existingTempRow.remove();

    const noShiftsRow = document.querySelector('.no-shifts-row');
    if (visibleCount === 0) {
      if (noShiftsRow) {
        noShiftsRow.style.display = '';
      } else {
        const emptyTr = document.createElement('tr');
        emptyTr.className = 'no-shifts-row-temp';
        emptyTr.innerHTML = `<td colspan="7" class="text-center text-muted py-5">Không tìm thấy ca làm việc nào khớp với bộ lọc hiện tại.</td>`;
        tbody.appendChild(emptyTr);
      }
    } else {
      if (noShiftsRow) noShiftsRow.style.display = 'none';
    }
  }

  // Reset all filters in advanced filter bar
  function resetFilters() {
    document.getElementById('filter-shift-name').value = '';
    document.getElementById('filter-shift-date').value = '';
    document.getElementById('filter-shift-time').value = '';
    document.getElementById('complex-filter').value = 'ALL';
    filterShifts();
  }

  // Toggle between single day and batch creation input fields
  function toggleCreationMode() {
    const mode = document.getElementById('modal-create-mode').value;
    const singleDateContainer = document.getElementById('single-date-container');
    const batchContainer = document.getElementById('batch-container');
    const modalShiftDate = document.getElementById('modal-shift-date');
    const modalStartDate = document.getElementById('modal-start-date');
    const modalEndDate = document.getElementById('modal-end-date');

    if (mode === 'batch') {
      singleDateContainer.style.display = 'none';
      batchContainer.style.display = 'block';
      modalShiftDate.required = false;
      modalStartDate.required = true;
      modalEndDate.required = true;
    } else {
      singleDateContainer.style.display = 'block';
      batchContainer.style.display = 'none';
      modalShiftDate.required = true;
      modalStartDate.required = false;
      modalEndDate.required = false;
    }
  }

  // Open modal in Add mode
  function openAddShiftModal() {
    document.getElementById('shiftModalTitle').innerText = 'Thêm ca làm việc mới';
    document.getElementById('modal-shift-id').value = '';
    document.getElementById('modal-complex').value = '';
    
    // Reset options
    const selectEl = document.getElementById('modal-shift-name');
    selectEl.innerHTML = `
      <option value="Auto" selected>Auto</option>
    `;
    selectEl.value = 'Auto';
    
    document.getElementById('modal-start-time').value = '';
    document.getElementById('modal-end-time').value = '';
    document.getElementById('modal-staff-id').value = '';
    
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('modal-shift-date').value = today;

    // Reset batch fields
    document.getElementById('modal-create-mode').value = 'single';
    document.getElementById('creation-mode-container').style.display = 'block';
    document.getElementById('modal-start-date').value = today;
    document.getElementById('modal-end-date').value = today;
    document.querySelectorAll('.repeat-day-cb').forEach(cb => cb.checked = false);
    
    toggleCreationMode();

    bootstrap.Modal.getOrCreateInstance(document.getElementById('shiftModal')).show();
  }

  // Open modal in Edit mode
  function openEditShiftModal(id, complexId, name, date, start, end, staffId) {
    document.getElementById('shiftModalTitle').innerText = 'Cập nhật ca làm việc';
    document.getElementById('modal-shift-id').value = id;
    document.getElementById('modal-complex').value = complexId;
    
    const selectEl = document.getElementById('modal-shift-name');
    selectEl.innerHTML = `
      <option value="Auto" selected>Auto</option>
    `;
    selectEl.value = 'Auto';
    
    document.getElementById('modal-shift-date').value = date;
    document.getElementById('modal-start-time').value = start.substring(0,5);
    document.getElementById('modal-end-time').value = end.substring(0,5);
    document.getElementById('modal-staff-id').value = staffId || '';

    // Force single mode for edit
    document.getElementById('modal-create-mode').value = 'single';
    document.getElementById('creation-mode-container').style.display = 'none';
    
    toggleCreationMode();

    bootstrap.Modal.getOrCreateInstance(document.getElementById('shiftModal')).show();
  }

  // Submit add/edit shift form
  async function submitShiftForm(event) {
    event.preventDefault();

    const id = document.getElementById('modal-shift-id').value;
    const complexId = document.getElementById('modal-complex').value;
    const shiftName = document.getElementById('modal-shift-name').value;
    const startTime = document.getElementById('modal-start-time').value;
    const endTime = document.getElementById('modal-end-time').value;
    const staffId = document.getElementById('modal-staff-id').value;
    const mode = document.getElementById('modal-create-mode').value;

    function formatTime24h(timeStr) {
      if (!timeStr) return '';
      timeStr = timeStr.trim().toUpperCase();
      let pm = timeStr.includes('CH') || timeStr.includes('PM');
      let am = timeStr.includes('SA') || timeStr.includes('AM');
      let clean = timeStr.replace(/[^0-9:]/g, '').trim();
      if (!clean) return '';
      let parts = clean.split(':');
      let hour = parseInt(parts[0], 10);
      let min = parts.length > 1 ? parseInt(parts[1], 10) : 0;
      if (pm) {
        if (hour < 12) hour += 12;
      } else if (am) {
        if (hour === 12) hour = 0;
      }
      return (hour < 10 ? '0' + hour : hour) + ':' + (min < 10 ? '0' + min : min);
    }

    const action = id ? 'edit' : 'create';

    const params = new URLSearchParams();
    params.append('action', action);
    if (id) params.append('shiftId', id);
    params.append('complexId', complexId);
    params.append('shiftName', shiftName);
    params.append('startTime', formatTime24h(startTime));
    params.append('endTime', formatTime24h(endTime));
    params.append('staffId', staffId);
    params.append('mode', mode);

    if (mode === 'batch') {
      const startDate = document.getElementById('modal-start-date').value;
      const endDate = document.getElementById('modal-end-date').value;
      
      const checkedDays = [];
      document.querySelectorAll('.repeat-day-cb:checked').forEach(cb => {
        checkedDays.push(cb.value);
      });
      
      if (checkedDays.length === 0) {
        showToast('Vui lòng chọn ít nhất một ngày trong tuần để tạo ca trực hàng loạt.', 'warning');
        return;
      }
      
      params.append('startDate', startDate);
      params.append('endDate', endDate);
      params.append('repeatDays', checkedDays.join(','));
    } else {
      const shiftDate = document.getElementById('modal-shift-date').value;
      params.append('shiftDate', shiftDate);
    }

    try {
      const res = await fetch('<%= ctx %>/owner/work-shift', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
      });

      let data = null;
      try {
        data = await res.json();
      } catch (e) {}

      if (data && data.success) {
        if (data.message) {
          showToastAfterReload(data.message, 'success');
        } else {
          showToastAfterReload('Lưu ca làm việc thành công!', 'success');
        }
        bootstrap.Modal.getOrCreateInstance(document.getElementById('shiftModal')).hide();
        window.location.reload();
      } else {
        showToast('Lỗi: ' + (data && data.error ? data.error : 'HTTP ' + res.status), 'danger');
      }
    } catch (err) {
      showToast('Đã xảy ra lỗi kết nối: ' + err.message, 'danger');
    }
  }

  // Delete shift
  async function deleteShift(shiftId) {
    showConfirm('Bạn có chắc chắn muốn xóa ca làm việc này? Mọi thông tin phân công liên quan cũng sẽ bị xóa.', async () => {
      const params = new URLSearchParams();
      params.append('action', 'delete');
      params.append('shiftId', shiftId);

      try {
        const res = await fetch('<%= ctx %>/owner/work-shift', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: params
        });

        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();

        if (data.success) {
          showToastAfterReload('Xóa ca làm việc thành công!', 'success');
          window.location.reload();
        } else {
          showToast('Lỗi: ' + (data.error || 'Không thể xóa ca trực'), 'danger');
        }
      } catch (err) {
        showToast('Đã xảy ra lỗi kết nối: ' + err.message, 'danger');
      }
    });
  }

  // Toggle select all shift checkboxes (only not disabled ones)
  function toggleSelectAllShifts(masterCb) {
    const cbs = document.querySelectorAll('.shift-checkbox');
    cbs.forEach(cb => {
      if (!cb.disabled) {
        cb.checked = masterCb.checked;
      }
    });
    updateDeleteSelectedButtonVisibility();
  }

  // Update batch delete button visibility and count
  function updateDeleteSelectedButtonVisibility() {
    const checked = document.querySelectorAll('.shift-checkbox:checked');
    const btn = document.getElementById('btnDeleteSelected');
    const countSpan = document.getElementById('selectedCount');
    
    if (checked.length > 0) {
      countSpan.innerText = checked.length;
      btn.style.display = 'inline-block';
    } else {
      btn.style.display = 'none';
      const masterCb = document.getElementById('selectAllShifts');
      if (masterCb) masterCb.checked = false;
    }
  }

  // Submit batch delete request
  async function deleteSelectedShifts() {
    const checked = document.querySelectorAll('.shift-checkbox:checked');
    if (checked.length === 0) return;

    showConfirm('Bạn có chắc chắn muốn xóa ' + checked.length + ' ca làm việc đã chọn? Mọi thông tin phân công liên quan cũng sẽ bị xóa.', async () => {
      const ids = [];
      checked.forEach(cb => ids.push(cb.value));

      const params = new URLSearchParams();
      params.append('action', 'deleteBatch');
      params.append('shiftIds', ids.join(','));

      try {
        const res = await fetch('<%= ctx %>/owner/work-shift', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: params
        });

        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();

        if (data.success) {
          if (data.message) {
            showToastAfterReload(data.message, 'success');
          } else {
            showToastAfterReload('Xóa các ca làm việc thành công!', 'success');
          }
          window.location.reload();
        } else {
          showToast('Lỗi: ' + (data.error || 'Không thể xóa ca trực'), 'danger');
        }
      } catch (err) {
        showToast('Đã xảy ra lỗi kết nối: ' + err.message, 'danger');
      }
    });
  }

  // Open Assign Modal and fetch assignments
  async function openAssignModal(shiftId, shiftName) {
    document.getElementById('assign-shift-id').value = shiftId;
    document.getElementById('assignShiftName').innerText = shiftName;
    
    const container = document.getElementById('assignStaffContainer');
    container.innerHTML = `<div class="w-100 text-center py-4"><div class="spinner-border text-success" role="status"></div></div>`;
    
    bootstrap.Modal.getOrCreateInstance(document.getElementById('assignModal')).show();

    try {
      // 1. Get currently assigned staff
      const res = await fetch('<%= ctx %>/owner/work-shift?action=getAssignments&shiftId=' + shiftId);
      if (!res.ok) {
        const errText = await res.text();
        throw new Error('HTTP ' + res.status + ': ' + errText);
      }
      const assignedStaff = await res.json();
      
      const assignedIds = new Set(assignedStaff.map(s => s.userId));

      // 2. Render all active staff and show toggle buttons
      container.innerHTML = '';
      
      const allStaff = <%= staffListJson %>;

      if (allStaff.length === 0) {
        container.innerHTML = '<div class="w-100 text-center py-3 text-muted">Không tìm thấy tài khoản nhân viên nào hoạt động.</div>';
        return;
      }

      allStaff.forEach(staff => {
        const isAssigned = assignedIds.has(staff.userId);
        const col = document.createElement('div');
        col.className = 'col';
        
        const staffName = staff.fullName || 'Nhân viên';
        const displayInitials = staffName.trim().substring(0, 2).toUpperCase();
        const displayPhone = staff.phone || 'Chưa cập nhật SĐT';
        
        let btnHtml = '';
        if (isAssigned) {
          btnHtml = '<button class="btn btn-sm btn-sf-outline-danger" onclick="toggleAssign(' + shiftId + ', ' + staff.userId + ', \'unassign\', this)">Hủy ca</button>';
        } else {
          btnHtml = '<button class="btn btn-sm btn-sf-outline-success" onclick="toggleAssign(' + shiftId + ', ' + staff.userId + ', \'assign\', this)">Giao ca</button>';
        }

        col.innerHTML = `
          <div class="card p-3 d-flex flex-row align-items-center justify-content-between shadow-sm border border-light-subtle rounded-3">
            <div class="d-flex align-items-center gap-3">
              <div class="avatar-circle fs-6 bg-emerald-50 text-emerald-800" style="width:40px; height:40px; background-color:#e6fbf4; color:#10b981;">
                \${displayInitials}
              </div>
              <div>
                <h6 class="fw-bold mb-0 text-slate-800">\${staffName}</h6>
                <span class="text-secondary small">\${displayPhone}</span>
              </div>
            </div>
            <div>
              \${btnHtml}
            </div>
          </div>
        `;
        container.appendChild(col);
      });

    } catch (err) {
      container.innerHTML = '<div class="w-100 text-center text-danger py-3">Không thể tải dữ liệu: ' + err.message + '</div>';
    }
  }

  // Toggle staff assignment via AJAX
  async function toggleAssign(shiftId, staffId, action, buttonEl) {
    buttonEl.disabled = true;
    buttonEl.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>`;

    const params = new URLSearchParams();
    params.append('action', action);
    params.append('shiftId', shiftId);
    params.append('staffId', staffId);

    try {
      const res = await fetch('<%= ctx %>/owner/work-shift', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
      });

      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();

      if (data.success) {
        // Dynamic re-render inside the modal
        if (action === 'assign') {
          buttonEl.outerHTML = '<button class="btn btn-sm btn-sf-outline-danger" onclick="toggleAssign(' + shiftId + ', ' + staffId + ', \'unassign\', this)">Hủy ca</button>';
        } else {
          buttonEl.outerHTML = '<button class="btn btn-sm btn-sf-outline-success" onclick="toggleAssign(' + shiftId + ', ' + staffId + ', \'assign\', this)">Giao ca</button>';
        }
      } else {
        showToast('Lỗi: ' + (data.error || 'Thao tác phân công thất bại'), 'danger');
        buttonEl.disabled = false;
        buttonEl.innerText = action === 'assign' ? 'Giao ca' : 'Hủy ca';
      }
    } catch (err) {
      showToast('Lỗi kết nối: ' + err.message, 'danger');
      buttonEl.disabled = false;
      buttonEl.innerText = action === 'assign' ? 'Giao ca' : 'Hủy ca';
    }
  }

  document.getElementById('assignModal').addEventListener('hidden.bs.modal', function () {
    window.location.reload();
  });
</script>
</body>
</html>

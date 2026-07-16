<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.time.LocalTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    String ctx = request.getContextPath();
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";

    boolean hasShift = (Boolean) request.getAttribute("hasShift") != null && (Boolean) request.getAttribute("hasShift");
    String selectedDate = (String) request.getAttribute("selectedDate");
    String facilityName = (String) request.getAttribute("facilityName");
    Long facilityId = (Long) request.getAttribute("facilityId");
    
    List<Map<String, Object>> fields = (List<Map<String, Object>>) request.getAttribute("fields");
    List<Map<String, Object>> bookings = (List<Map<String, Object>>) request.getAttribute("bookings");

    boolean isUpcomingShift = false;
    boolean isEndedShift = false;
    if (hasShift) {
        String sStart = (String) request.getAttribute("shiftStartTime");
        String sEnd = (String) request.getAttribute("shiftEndTime");
        if (sStart != null && sEnd != null) {
            try {
                String cleanStart = sStart.contains(" ") ? sStart.split(" ")[1] : sStart;
                if (cleanStart.contains(".")) cleanStart = cleanStart.split("\\.")[0];
                if (cleanStart.length() > 5) cleanStart = cleanStart.substring(0, 5);
                
                java.time.LocalTime start = java.time.LocalTime.parse(cleanStart);
                
                String cleanEnd = sEnd.contains(" ") ? sEnd.split(" ")[1] : sEnd;
                if (cleanEnd.contains(".")) cleanEnd = cleanEnd.split("\\.")[0];
                if (cleanEnd.length() > 5) cleanEnd = cleanEnd.substring(0, 5);
                
                java.time.LocalTime end = java.time.LocalTime.parse(cleanEnd);
                
                java.time.LocalTime now = java.time.LocalTime.now();
                java.time.LocalDate today = java.time.LocalDate.now();
                if (selectedDate.equals(today.toString())) {
                    if (now.isBefore(start)) {
                        isUpcomingShift = true;
                    } else if (now.isAfter(end)) {
                        isEndedShift = true;
                    }
                }
            } catch (Exception ignored) {}
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Lịch trong ngày | Sport Field Booking</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
  <style>
    body { background: #f8fafc; font-family: 'Inter', sans-serif; }
    .schedule-header { background: #ffffff; border-radius: 20px; box-shadow: 0 4px 20px rgba(15,23,42,.03); padding: 24px; border: 1px solid #e2e8f0; }
    .grid-container { overflow-x: auto; background: #ffffff; border-radius: 20px; border: 1px solid #e2e8f0; box-shadow: 0 4px 20px rgba(15,23,42,.03); padding: 24px; margin-top: 24px; }
    .timeline-table { border-collapse: collapse; width: 100%; min-width: 1200px; font-size: 0.82rem; }
    .timeline-table th, .timeline-table td { border: 1px solid #cbd5e1; height: 50px; text-align: center; vertical-align: middle; }
    .field-col { width: 160px; min-width: 160px; font-weight: 700; color: #0f172a; text-align: left; padding-left: 16px; background: #f8fafc; position: sticky; left: 0; z-index: 10; border-right: 2px solid #94a3b8; }
    .time-hdr { font-weight: 700; background: #f1f5f9; color: #475569; width: 60px; min-width: 60px; }
    .slot-cell { position: relative; cursor: pointer; transition: background-color 0.2s; }
    .slot-cell:hover { filter: brightness(0.95); }
    .status-available { background-color: #f8fafc; color: #94a3b8; }
    .status-booked-confirmed { background-color: #fef3c7; color: #d97706; font-weight: 600; border: 1px solid #fde68a; }
    .status-booked-checkedin { background-color: #e0f2fe; color: #0284c7; font-weight: 600; border: 1px solid #bae6fd; }
    .status-booked-completed { background-color: #dcfce7; color: #16a34a; font-weight: 600; border: 1px solid #bbf7d0; }
    .status-field-maintenance { background-color: #f1f5f9; color: #64748b; font-weight: 500; cursor: not-allowed; }
    .status-field-disabled { background-color: #e2e8f0; color: #94a3b8; font-weight: 500; cursor: not-allowed; }
    .legend-item { display: inline-flex; align-items: center; gap: 8px; font-size: 0.85rem; font-weight: 500; }
    .legend-box { width: 18px; height: 18px; border-radius: 4px; border: 1px solid #cbd5e1; }
    .field-badge { cursor: pointer; transition: all 0.15s ease-in-out; }
    .field-badge:hover { transform: scale(1.05); }
    .pulse-playing { width: 8px; height: 8px; background: #0ea5e9; border-radius: 50%; display: inline-block; animation: pulse-playing-ani 1.2s infinite; }
    @keyframes pulse-playing-ani { 0%,100% { box-shadow: 0 0 0 0 rgba(14,165,233,.6); } 50% { box-shadow: 0 0 0 4px rgba(14,165,233,0); } }
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
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Lịch trong ngày"></div>

<main class="py-5">
  <div class="container">

    <% if (!hasShift) { %>
      <div class="card p-5 text-center shadow-sm border-0 rounded-4 mt-5">
        <i class="bi bi-calendar-x display-3 text-muted"></i>
        <h3 class="mt-4 fw-bold">Không có ca làm việc hôm nay</h3>
        <p class="text-muted">Bạn không được phân ca làm việc nào cho ngày hôm nay. Hãy liên hệ với quản lý.</p>
        <div class="mt-4 d-flex justify-content-center gap-3">
          <a href="<%= ctx %>/staff/dashboard" class="btn btn-sf-primary">Quay lại Dashboard</a>
          <a href="<%= ctx %>/test_setup_db.jsp" class="btn btn-outline-success">
            <i class="bi bi-database-fill-add me-2"></i>Tạo nhanh dữ liệu test hôm nay
          </a>
        </div>
      </div>
    <% } else { %>
      
      <!-- Schedule header / Filters -->
      <div class="schedule-header">
        <div class="row align-items-center g-3">
          <div class="col-md-6">
            <h1 class="fw-bold mb-1">Lịch sân hàng ngày</h1>
            <p class="text-muted mb-0">Cơ sở: <strong class="text-success"><%= facilityName %></strong></p>
          </div>
          <div class="col-md-6 d-flex justify-content-md-end align-items-center gap-3">
            <div>
              <label for="date-selector" class="form-label small fw-bold text-muted mb-1">Chọn ngày xem lịch</label>
              <input type="date" id="date-selector" class="form-control" value="<%= selectedDate %>">
            </div>
          </div>
        </div>

        <hr class="my-4">

        <!-- Legends -->
        <div class="d-flex flex-wrap gap-4 align-items-center">
          <div class="legend-item"><div class="legend-box" style="background:#f8fafc;"></div> Sân trống</div>
          <div class="legend-item"><div class="legend-box" style="background:#fef3c7;border-color:#fde68a;"></div> Chờ check-in</div>
          <div class="legend-item"><div class="legend-box" style="background:#e0f2fe;border-color:#bae6fd;"></div> Đang chơi</div>
          <div class="legend-item"><div class="legend-box" style="background:#dcfce7;border-color:#bbf7d0;"></div> Hoàn thành</div>
          <div class="legend-item"><div class="legend-box" style="background:#f1f5f9;"></div> Bảo trì</div>
          <div class="legend-item"><div class="legend-box" style="background:#e2e8f0;"></div> Tạm ngưng</div>
        </div>
      </div>

      <!-- TIMELINE GRID -->
      <div class="grid-container">
        <h4 class="fw-bold mb-3"><i class="bi bi-grid-3x3-gap text-success me-2"></i>Bản đồ thời gian</h4>
        <div class="table-responsive">
          <table class="timeline-table">
            <thead>
              <tr>
                <th class="field-col">Sân bóng</th>
                <%
                  for (int hour = 5; hour <= 22; hour++) {
                    String timeLabel = String.format("%02d:00", hour);
                %>
                  <th class="time-hdr"><%= timeLabel %></th>
                <% } %>
              </tr>
            </thead>
            <tbody>
              <% if (fields == null || fields.isEmpty()) { %>
                <tr>
                  <td class="field-col">Không có sân nào</td>
                  <td colspan="18" class="text-center text-muted py-4">Không tìm thấy sân bóng nào trong cơ sở này.</td>
                </tr>
              <% } else {
                for (Map<String, Object> field : fields) {
                  long fieldId = (Long) field.get("fieldId");
                  String fieldName = (String) field.get("fieldName");
                  String fieldStatus = (String) field.get("status");
                  
                  String fieldBadgeClass = "bg-success";
                  String fieldBadgeText = "Hoạt động";
                  if ("MAINTENANCE".equals(fieldStatus)) {
                    fieldBadgeClass = "bg-warning text-dark";
                    fieldBadgeText = "Bảo trì";
                  } else if ("DISABLED".equals(fieldStatus)) {
                    fieldBadgeClass = "bg-danger";
                    fieldBadgeText = "Đóng cửa";
                  }
              %>
                <tr>
                  <td class="field-col">
                    <div class="d-flex flex-column">
                      <span><%= fieldName %></span>
                      <span class="badge <%= fieldBadgeClass %> field-badge align-self-start mt-1" 
                            style="font-size:0.65rem;"
                            onclick="<%= isUpcomingShift ? "showToast('Ca trực chưa bắt đầu. Bạn không thể thay đổi trạng thái sân.', 'warning')" : (isEndedShift ? "showToast('Ca trực đã kết thúc. Bạn không thể thay đổi trạng thái sân.', 'danger')" : "openFieldStatusModal('" + fieldId + "', '" + fieldName + "', '" + fieldStatus + "')") %>">
                        <%= fieldBadgeText %> <i class="bi bi-pencil-square ms-1"></i>
                      </span>
                    </div>
                  </td>

                  <%
                    // Loop hourly slots
                    for (int hour = 5; hour <= 22; hour++) {
                      LocalTime slotTimeStart = LocalTime.of(hour, 0);
                      LocalTime slotTimeEnd = LocalTime.of(hour, 59);

                      Map<String, Object> foundBooking = null;
                      if (bookings != null) {
                        for (Map<String, Object> b : bookings) {
                          long bFieldId = (Long) b.get("fieldId");
                          if (bFieldId != fieldId) continue;

                          String bStartStr = (String) b.get("startTime"); 
                          String bEndStr = (String) b.get("endTime");
                           
                          String sTimeVal = bStartStr != null ? bStartStr : "00:00:00";
                          if (sTimeVal.contains(" ")) sTimeVal = sTimeVal.split(" ")[1];
                          if (sTimeVal.contains(".")) sTimeVal = sTimeVal.split("\\.")[0];
                          if (sTimeVal.length() > 5) sTimeVal = sTimeVal.substring(0, 5);

                          String eTimeVal = bEndStr != null ? bEndStr : "00:00:00";
                          if (eTimeVal.contains(" ")) eTimeVal = eTimeVal.split(" ")[1];
                          if (eTimeVal.contains(".")) eTimeVal = eTimeVal.split("\\.")[0];
                          if (eTimeVal.length() > 5) eTimeVal = eTimeVal.substring(0, 5);
                           
                          LocalTime bStart = LocalTime.parse(sTimeVal);
                          LocalTime bEnd = LocalTime.parse(eTimeVal);

                          // Check overlap
                          if (!bStart.isAfter(slotTimeEnd) && bEnd.isAfter(slotTimeStart)) {
                            foundBooking = b;
                            break;
                          }
                        }
                      }

                      String cellClass = "status-available";
                      String cellText = "";
                      String cellOnclick = "";
                      
                      if ("MAINTENANCE".equals(fieldStatus)) {
                        cellClass = "status-field-maintenance";
                        cellText = "Bảo trì";
                      } else if ("DISABLED".equals(fieldStatus)) {
                        cellClass = "status-field-disabled";
                        cellText = "Khóa";
                      } else if (foundBooking != null) {
                        String bStatus = (String) foundBooking.get("status");
                        long bId = (Long) foundBooking.get("bookingId");
                        String custName = (String) foundBooking.get("customerName");
                        String bCode = (String) foundBooking.get("bookingCode");
                        boolean hasInvoice = Boolean.TRUE.equals(foundBooking.get("hasInvoice"));
                        boolean checkoutDue = Boolean.TRUE.equals(foundBooking.get("checkoutDue"));
                        
                        if ("CONFIRMED".equals(bStatus)) {
                          cellClass = "status-booked-confirmed";
                          cellText = bCode + " - " + custName;
                          cellOnclick = isUpcomingShift 
                             ? "showToast('Ca trực chưa bắt đầu. Bạn chỉ có thể xem lịch, không thể thao tác.', 'warning')" 
                             : (isEndedShift ? "showToast('Ca trực đã kết thúc. Bạn không thể thao tác.', 'danger')" : "location.href='" + ctx + "/staff/checkin?id=" + bId + "'");
                        } else if ("CHECKED_IN".equals(bStatus)) {
                          cellClass = "status-booked-checkedin";
                          cellText = bCode + " - " + custName;
                          cellOnclick = isUpcomingShift 
                             ? "showToast('Ca trực chưa bắt đầu. Bạn chỉ có thể xem lịch, không thể thao tác.', 'warning')" 
                             : (isEndedShift ? "showToast('Ca trực đã kết thúc. Bạn không thể thao tác.', 'danger')" : "location.href='" + ctx + "/staff/checkout?id=" + bId + "'");
                          if (!checkoutDue) {
                            cellOnclick = "";
                          }
                        } else if ("PENDING_CHECKOUT_PAYMENT".equals(bStatus)) {
                          cellClass = "status-booked-pending-payment";
                          cellText = bCode + " - Cho thanh toan";
                          if (hasInvoice) {
                            cellOnclick = "location.href='" + ctx + "/staff/invoice?id=" + bId + "'";
                          }
                        } else if ("COMPLETED".equals(bStatus)) {
                          cellClass = "status-booked-completed";
                          cellText = bCode + " - Xong";
                          if (hasInvoice) {
                            cellOnclick = "location.href='" + ctx + "/staff/invoice?id=" + bId + "'";
                          }
                        }
                      }
                  %>
                    <td class="slot-cell <%= cellClass %>" 
                        onclick="<%= cellOnclick %>"
                        title="<%= cellText.isEmpty() ? "Khung giờ trống" : cellText %>">
                      <%= cellText %>
                    </td>
                  <% } %>
                </tr>
              <% }
              } %>
            </tbody>
          </table>
        </div>
      </div>

      <!-- MATCH DETAILED LIST -->
      <div class="card soft-card p-4 mt-4">
        <h4 class="fw-bold mb-3"><i class="bi bi-list-stars text-success me-2"></i>Chi tiết trận đấu trong ngày</h4>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0" style="font-size: 0.9rem;">
            <thead class="table-light">
              <tr>
                <th>Giờ</th>
                <th>Mã đặt sân</th>
                <th>Sân bóng</th>
                <th>Khách hàng</th>
                <th>Số điện thoại</th>
                <th>Tổng thanh toán</th>
                <th>Trạng thái</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              <% if (bookings == null || bookings.isEmpty()) { %>
                <tr>
                  <td colspan="8" class="text-center text-muted py-4">Không có trận đấu nào trong ngày này.</td>
                </tr>
              <% } else {
                for (Map<String, Object> b : bookings) {
                  long bId = (Long) b.get("bookingId");
                  String bCode = (String) b.get("bookingCode");
                  String bStart = (String) b.get("startTime"); 
                  String bEnd = (String) b.get("endTime");
                  String bStatus = (String) b.get("status");
                  String fieldName = (String) b.get("fieldName");
                  String customerName = (String) b.get("customerName");
                  String customerPhone = (String) b.get("customerPhone");
                  java.math.BigDecimal total = (java.math.BigDecimal) b.get("totalAmount");
                  boolean hasInvoice = Boolean.TRUE.equals(b.get("hasInvoice"));
                  boolean checkoutDue = Boolean.TRUE.equals(b.get("checkoutDue"));
                  boolean lateNoShowEligible = Boolean.TRUE.equals(b.get("lateNoShowEligible"));

                  String sTimeVal = bStart != null ? bStart : "00:00:00";
                  if (sTimeVal.contains(" ")) sTimeVal = sTimeVal.split(" ")[1];
                  if (sTimeVal.contains(".")) sTimeVal = sTimeVal.split("\\.")[0];
                  if (sTimeVal.length() > 5) sTimeVal = sTimeVal.substring(0, 5);

                  String eTimeVal = bEnd != null ? bEnd : "00:00:00";
                  if (eTimeVal.contains(" ")) eTimeVal = eTimeVal.split(" ")[1];
                  if (eTimeVal.contains(".")) eTimeVal = eTimeVal.split("\\.")[0];
                  if (eTimeVal.length() > 5) eTimeVal = eTimeVal.substring(0, 5);

                  String formattedTime = sTimeVal + " - " + eTimeVal;
                  
                  String statusBadge = "";
                  String actionButton = "";
                  
                  boolean isExpired = false;
                  if ("CONFIRMED".equals(bStatus) && bEnd != null) {
                    try {
                      String isoEnd = bEnd.replace(" ", "T");
                      if (isoEnd.contains(".")) {
                        isoEnd = isoEnd.substring(0, isoEnd.indexOf("."));
                      }
                      java.time.LocalDateTime endDt = java.time.LocalDateTime.parse(isoEnd);
                      isExpired = endDt.isBefore(java.time.LocalDateTime.now());
                    } catch (Exception ignored) {}
                  }

                  if ("CONFIRMED".equals(bStatus)) {
                    if (lateNoShowEligible) {
                      statusBadge = "<span class='badge bg-danger-subtle text-danger fw-bold'><i class='bi bi-person-x me-1'></i>No-show</span>";
                      actionButton = isUpcomingShift
                        ? "<button class='btn btn-sm btn-secondary px-3' disabled><i class='bi bi-lock-fill me-1'></i>Chờ ca trực</button>"
                        : (isEndedShift
                          ? "<button class='btn btn-sm btn-secondary px-3' disabled><i class='bi bi-lock-fill me-1'></i>Hết ca trực</button>"
                          : "<button type='button' class='btn btn-sm btn-outline-danger px-3' onclick='cancelNoShow(" + bId + ")'><i class='bi bi-person-x me-1'></i>Hủy do khách muộn</button>");
                    } else if (isExpired) {
                      statusBadge = "<span class='badge bg-danger-subtle text-danger fw-bold'><i class='bi bi-exclamation-triangle me-1'></i>Quá giờ</span>";
                      actionButton = "<button class='btn btn-sm btn-secondary px-3' disabled><i class='bi bi-exclamation-circle me-1'></i>Quá giờ nhận</button>";
                    } else {
                      statusBadge = "<span class='badge badge-soft-warning'><i class='bi bi-hourglass-split me-1'></i>Chờ check-in</span>";
                      actionButton = isUpcomingShift 
                        ? "<button class='btn btn-sm btn-secondary px-3' disabled title='Chưa đến giờ làm việc'><i class='bi bi-lock-fill me-1'></i>Chờ ca trực</button>"
                        : (isEndedShift 
                          ? "<button class='btn btn-sm btn-secondary px-3' disabled title='Ca trực đã kết thúc'><i class='bi bi-lock-fill me-1'></i>Hết ca trực</button>"
                          : "<a href='" + ctx + "/staff/checkin?id=" + bId + "' class='btn btn-sm btn-sf-primary px-3'>Check-in</a>");
                    }
                  } else if ("CHECKED_IN".equals(bStatus)) {
                    statusBadge = "<span class='badge badge-soft-info'><i class='pulse-playing me-1'></i>Đang đá</span>";
                    actionButton = isUpcomingShift 
                      ? "<button class='btn btn-sm btn-secondary px-3' disabled title='Chưa đến giờ làm việc'><i class='bi bi-lock-fill me-1'></i>Chờ ca trực</button>"
                      : (isEndedShift 
                        ? "<button class='btn btn-sm btn-secondary px-3' disabled title='Ca trực đã kết thúc'><i class='bi bi-lock-fill me-1'></i>Hết ca trực</button>"
                        : "<a href='" + ctx + "/staff/checkout?id=" + bId + "' class='btn btn-sm btn-outline-success px-3'>Checkout</a>");
                    if (!checkoutDue && !isUpcomingShift && !isEndedShift) {
                      actionButton = "<button class='btn btn-sm btn-secondary px-3' disabled>Đang sử dụng</button>";
                    }
                  } else if ("PENDING_CHECKOUT_PAYMENT".equals(bStatus)) {
                    statusBadge = "<span class='badge fw-bold' style='background:#fae8ff;color:#a21caf;'><i class='bi bi-credit-card me-1'></i>Chờ khách thanh toán</span>";
                    if (hasInvoice) {
                      actionButton = "<a href='" + ctx + "/staff/invoice?id=" + bId + "' class='btn btn-sm btn-outline-secondary px-3'><i class='bi bi-file-earmark-text me-1'></i>Hóa đơn</a>";
                    }
                  } else if ("COMPLETED".equals(bStatus)) {
                    statusBadge = "<span class='badge badge-soft-success'><i class='bi bi-check-circle me-1'></i>Đã xong</span>";
                    if (hasInvoice) {
                      actionButton = "<a href='" + ctx + "/staff/invoice?id=" + bId + "' class='btn btn-sm btn-outline-secondary px-3'><i class='bi bi-file-earmark-text me-1'></i>Hóa đơn</a>";
                    }
                  }
              %>
                <tr>
                  <td style="white-space: nowrap;"><strong class="text-dark"><%= formattedTime %></strong></td>
                  <td style="white-space: nowrap;"><strong class="text-success"><%= bCode %></strong></td>
                  <td style="white-space: nowrap;"><strong><%= fieldName %></strong></td>
                  <td style="white-space: nowrap;"><%= customerName %></td>
                  <td style="white-space: nowrap;"><%= customerPhone %></td>
                  <td class="text-success fw-bold" style="white-space: nowrap;"><%= String.format("%,d ₫", total.longValue()) %></td>
                  <td style="white-space: nowrap;"><%= statusBadge %></td>
                  <td style="white-space: nowrap;"><%= actionButton %></td>
                </tr>
              <% }
              } %>
            </tbody>
          </table>
        </div>
      </div>
    <% } %>

  </div>
</main>

<!-- Modal Cập Nhật Trạng Thái Sân -->
<div class="modal fade" id="fieldStatusModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content rounded-4 border-0 shadow">
      <div class="modal-header border-bottom-0 pb-0">
        <h5 class="modal-title fw-bold" id="statusModalTitle">Cập nhật trạng thái sân</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body py-4">
        <form id="field-status-form">
          <input type="hidden" id="modal-field-id">
          <div class="mb-3">
            <label class="form-label small fw-bold text-muted">Sân bóng</label>
            <input type="text" class="form-control" id="modal-field-name" readonly style="background:#f1f5f9;">
          </div>
          <div class="mb-3">
            <label class="form-label small fw-bold text-muted">Trạng thái vận hành</label>
            <select class="form-select" id="modal-field-status">
              <option value="AVAILABLE">Hoạt động bình thường</option>
              <option value="MAINTENANCE">Đang bảo trì / Sửa chữa</option>
              <option value="DISABLED">Tạm ngưng hoạt động</option>
            </select>
          </div>
        </form>
      </div>
      <div class="modal-footer border-top-0 pt-0">
        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button>
        <button type="button" class="btn btn-sf-primary" onclick="submitFieldStatusChange()">Lưu thay đổi</button>
      </div>
    </div>
  </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
  document.getElementById('date-selector')?.addEventListener('change', function() {
    window.location.href = '<%= ctx %>/staff/schedule?date=' + this.value;
  });

  let statusModal = null;
  document.addEventListener('DOMContentLoaded', function() {
    const modalEl = document.getElementById('fieldStatusModal');
    if (modalEl) statusModal = new bootstrap.Modal(modalEl);
    
    <%
      String errorParam = request.getParameter("error");
      if (errorParam != null) {
    %>
      <% if ("facility_mismatch".equals(errorParam)) { %>
        showToast("Lượt đặt sân này thuộc cơ sở khác. Bạn không thể thực hiện thao tác này.", "danger");
      <% } else { %>
        showToast("<%= errorParam %>", "danger");
      <% } %>
    <%
      }
    %>
  });

  function openFieldStatusModal(fieldId, fieldName, currentStatus) {
    document.getElementById('modal-field-id').value = fieldId;
    document.getElementById('modal-field-name').value = fieldName;
    document.getElementById('modal-field-status').value = currentStatus;
    if (statusModal) statusModal.show();
  }

  async function submitFieldStatusChange() {
    const fieldId = document.getElementById('modal-field-id').value;
    const status = document.getElementById('modal-field-status').value;

    try {
      const params = new URLSearchParams();
      params.append('fieldId', fieldId);
      params.append('status', status);

      const res = await fetch('<%= ctx %>/api/staff/field/update-status', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });

      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();
      
      if (data.success) {
        if (statusModal) statusModal.hide();
        showToastAfterReload('Cập nhật trạng thái sân thành công!', 'success');
        window.location.reload();
      } else {
        showToast('Lỗi: ' + (data.error || 'Không rõ nguyên nhân'), 'danger');
      }
    } catch (err) {
      showToast('Không thể cập nhật trạng thái sân: ' + err.message, 'danger');
    }
  }

  async function cancelNoShow(bookingId) {
    if (!confirm('Hủy lượt đặt sân này do khách đến muộn quá 30 phút?')) {
      return;
    }
    try {
      const params = new URLSearchParams();
      params.append('bookingId', bookingId);
      const res = await fetch('<%= ctx %>/api/staff/no-show-cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params,
        credentials: 'include'
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Không thể hủy lượt đặt sân no-show.');
      }
      showToastAfterReload('Hủy lượt đặt sân do khách đến muộn thành công!', 'success');
      window.location.reload();
    } catch (err) {
      showToast(err.message || 'Không thể hủy lượt đặt sân no-show.', 'danger');
    }
  }
</script>
</body>
</html>

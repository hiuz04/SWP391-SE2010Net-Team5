<%@ page contentType="text/html;charset=UTF-8" language="java" %>
  <%@ page import="com.swp.model.User" %>
    <%@ page import="java.util.List" %>
      <%@ page import="java.util.Map" %>
        <%@ page import="java.time.LocalTime" %>
          <%@ page import="java.time.format.DateTimeFormatter" %>
            <% String ctx=request.getContextPath(); User sessionUser=(User) session.getAttribute("user"); String
              navRole=sessionUser==null ? "guest" : (String) session.getAttribute("navRole"); String
              displayName=sessionUser !=null ? sessionUser.getFullName() : "" ; boolean hasShift=(Boolean)
              request.getAttribute("hasShift") !=null && (Boolean) request.getAttribute("hasShift"); String
              selectedDate=(String) request.getAttribute("selectedDate"); String complexName=(String)
              request.getAttribute("complexName"); Long complexId=(Long) request.getAttribute("complexId");
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
                    if ("24:00".equals(cleanEnd)) cleanEnd = "23:59";

                    java.time.LocalTime end = java.time.LocalTime.parse(cleanEnd);

                    java.time.LocalTime now = java.time.LocalTime.now();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    if (selectedDate != null && selectedDate.equals(today.toString())) {
                    if (start.isBefore(end) || start.equals(end)) {
                    if (now.isBefore(start)) {
                    isUpcomingShift = true;
                    } else if (now.isAfter(end)) {
                    isEndedShift = true;
                    }
                    } else {
                    if (now.isAfter(end) && now.isBefore(start)) {
                    isUpcomingShift = true;
                    }
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
                      <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
                        rel="stylesheet">
                      <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"
                        rel="stylesheet">
                      <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap"
                        rel="stylesheet">
                      <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
                      <style>
                        body {
                          background: #f8fafc;
                          font-family: 'Inter', sans-serif;
                        }

                        .schedule-header {
                          background: #ffffff;
                          border-radius: 20px;
                          box-shadow: 0 4px 20px rgba(15, 23, 42, .03);
                          padding: 24px;
                          border: 1px solid #e2e8f0;
                        }

                        .grid-container {
                          overflow-x: auto;
                          background: #ffffff;
                          border-radius: 20px;
                          border: 1px solid #e2e8f0;
                          box-shadow: 0 4px 20px rgba(15, 23, 42, .03);
                          padding: 24px;
                          margin-top: 24px;
                        }

                        .timeline-table {
                          border-collapse: collapse;
                          width: 100%;
                          min-width: 1600px;
                          font-size: 0.78rem;
                        }

                        .timeline-table th,
                        .timeline-table td {
                          border: 1px solid #cbd5e1;
                          height: 42px;
                          text-align: center;
                          vertical-align: middle;
                        }

                        .field-col {
                          width: 140px;
                          min-width: 140px;
                          font-weight: 700;
                          color: #0f172a;
                          text-align: left;
                          padding-left: 12px;
                          background: #f8fafc;
                          position: sticky;
                          left: 0;
                          z-index: 10;
                          border-right: 2px solid #94a3b8;
                        }

                        .time-hdr {
                          font-weight: 700;
                          background: #f1f5f9;
                          color: #475569;
                          width: 52px;
                          min-width: 52px;
                        }

                        .slot-cell {
                          position: relative;
                          cursor: pointer;
                          transition: background-color 0.2s;
                          min-width: 70px;
                          padding: 2px 4px;
                          word-break: break-word;
                          white-space: normal;
                          font-size: 0.75rem;
                          line-height: 1.2;
                          vertical-align: top;
                        }

                        .slot-cell:hover {
                          filter: brightness(0.95);
                        }

                        .status-available {
                          background-color: #f8fafc;
                          color: #94a3b8;
                        }

                        .status-booked-confirmed {
                          background-color: #fef3c7;
                          color: #d97706;
                          font-weight: 600;
                          border: 1px solid #fde68a;
                        }

                        .status-booked-late {
                          background-color: #fee2e2;
                          color: #dc2626;
                          font-weight: 600;
                          border: 1px solid #fca5a5;
                        }

                        .status-booked-checkedin {
                          background-color: #e0f2fe;
                          color: #0284c7;
                          font-weight: 600;
                          border: 1px solid #bae6fd;
                        }

                        .status-booked-completed {
                          background-color: #dcfce7;
                          color: #16a34a;
                          font-weight: 600;
                          border: 1px solid #bbf7d0;
                        }

                        .status-booked-pending-payment {
                          background-color: #fae8ff;
                          color: #a21caf;
                          font-weight: 600;
                          border: 1px solid #f0abfc;
                        }

                        .status-field-maintenance {
                          background-color: #f1f5f9;
                          color: #64748b;
                          font-weight: 500;
                          cursor: not-allowed;
                        }

                        .status-field-disabled {
                          background-color: #e2e8f0;
                          color: #94a3b8;
                          font-weight: 500;
                          cursor: not-allowed;
                        }

                        .legend-item {
                          display: inline-flex;
                          align-items: center;
                          gap: 8px;
                          font-size: 0.85rem;
                          font-weight: 500;
                        }

                        .legend-box {
                          width: 18px;
                          height: 18px;
                          border-radius: 4px;
                          border: 1px solid #cbd5e1;
                        }

                        .field-badge {
                          cursor: pointer;
                          transition: all 0.15s ease-in-out;
                        }

                        .field-badge:hover {
                          transform: scale(1.05);
                        }

                        .pulse-playing {
                          width: 8px;
                          height: 8px;
                          background: #0ea5e9;
                          border-radius: 50%;
                          display: inline-block;
                          animation: pulse-playing-ani 1.2s infinite;
                        }

                        @keyframes pulse-playing-ani {

                          0%,
                          100% {
                            box-shadow: 0 0 0 0 rgba(14, 165, 233, .6);
                          }

                          50% {
                            box-shadow: 0 0 0 4px rgba(14, 165, 233, 0);
                          }
                        }

                        .soft-card {
                          border-radius: 20px;
                          background: #fff;
                          border: 1px solid #e2e8f0;
                          box-shadow: 0 8px 24px rgba(15, 23, 42, .04);
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
                      <div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>"
                        data-active="Lịch trong ngày"></div>

                      <main class="py-5">
                        <div class="container">

                          <% if (!hasShift) { %>
                            <div class="card p-5 text-center shadow-sm border-0 rounded-4 mt-5">
                              <i class="bi bi-calendar-x display-3 text-muted"></i>
                              <h3 class="mt-4 fw-bold">Không có ca làm việc hôm nay</h3>
                              <p class="text-muted">Bạn không được phân ca làm việc nào cho ngày hôm nay. Hãy liên hệ
                                với quản lý.</p>
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
                                    <p class="text-muted mb-0">Cơ sở: <strong class="text-success">
                                        <%= complexName %>
                                      </strong></p>
                                  </div>
                                  <div class="col-md-6 d-flex justify-content-md-end align-items-center gap-3">
                                    <div>
                                      <label for="date-selector" class="form-label small fw-bold text-muted mb-1">Chọn
                                        ngày xem lịch</label>
                                      <input type="date" id="date-selector" class="form-control"
                                        value="<%= selectedDate %>">
                                    </div>
                                  </div>
                                </div>

                                <hr class="my-4">

                                <!-- Legends -->
                                <div class="d-flex flex-wrap gap-4 align-items-center">
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#f8fafc;"></div> Sân trống
                                  </div>
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#fef3c7;border-color:#fde68a;"></div> Chờ
                                    check-in
                                  </div>
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#fee2e2;border-color:#fca5a5;"></div> Quá
                                    giờ / Muộn (&gt;30p)
                                  </div>
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#e0f2fe;border-color:#bae6fd;"></div> Đang
                                    chơi
                                  </div>
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#fae8ff;border-color:#f0abfc;"></div> Chờ
                                    thanh toán
                                  </div>
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#dcfce7;border-color:#bbf7d0;"></div> Hoàn
                                    thành
                                  </div>
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#f1f5f9;"></div> Bảo trì
                                  </div>
                                  <div class="legend-item">
                                    <div class="legend-box" style="background:#e2e8f0;"></div> Tạm ngưng
                                  </div>
                                </div>
                              </div>

                              <!-- TIMELINE GRID -->
                              <div class="grid-container">
                                <h4 class="fw-bold mb-3"><i class="bi bi-grid-3x3-gap text-success me-2"></i>Bản đồ thời
                                  gian</h4>
                                <div class="table-responsive">
                                  <table class="timeline-table">
                                    <thead>
                                      <tr>
                                        <th class="field-col">Sân bóng</th>
                                        <% for (int m=5 * 60; m <=22 * 60 + 30; m +=30) { int h=m / 60; int min=m % 60;
                                          String timeLabel=String.format("%02d:%02d", h, min); %>
                                          <th class="time-hdr" data-time="<%= timeLabel %>">
                                            <%= timeLabel %>
                                          </th>
                                          <% } %>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      <% if (fields==null || fields.isEmpty()) { %>
                                        <tr>
                                          <td class="field-col">Không có sân nào</td>
                                          <td colspan="36" class="text-center text-muted py-4">Không tìm thấy sân bóng
                                            nào trong cơ sở này.</td>
                                        </tr>
                                        <% } else { for (Map<String, Object> field : fields) {
                                          long fieldId = ((Number) field.get("fieldId")).longValue();
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
                                                <span>
                                                  <%= fieldName %>
                                                </span>
                                                <span class="badge <%= fieldBadgeClass %> field-badge align-self-start mt-1"
                                                   style="font-size:0.65rem;" onclick="<%= isUpcomingShift ? "showToast('Ca trực chưa bắt đầu. Bạn không thể thay đổi trạng thái sân.', 'warning')" : (isEndedShift ? "showToast('Ca trực đã kết thúc. Bạn không thể thay đổi trạng thái sân.', 'danger')" : "openFieldStatusModal('" + fieldId + "', '" + fieldName + "', '" + fieldStatus + "')") %>">
                                                   <%= fieldBadgeText %> <i class="bi bi-pencil-square ms-1"></i>
                                                 </span>
                                              </div>
                                            </td>

                                            <% for (int m=5 * 60; m <= 22 * 60; m += 30) {
                                                java.time.LocalTime slotTimeStart = java.time.LocalTime.of(m / 60, m % 60);
                                                java.time.LocalTime slotTimeEnd = slotTimeStart.plusMinutes(30);

                                                Map<String, Object> foundBooking = null;
                                                if (bookings != null) {
                                                for (Map<String, Object> b : bookings) {
                                                long bFieldId = ((Number) b.get("fieldId")).longValue();
                                                if (bFieldId != fieldId) continue;

                                                String bStartStr = (String) b.get("startTime");
                                                String bEndStr = (String) b.get("endTime");

                                                String sTimeVal = bStartStr != null ? bStartStr : "00:00:00";
                                                if (sTimeVal.contains(" ")) sTimeVal = sTimeVal.split(" ")[1];
                                                if (sTimeVal.contains(".")) sTimeVal = sTimeVal.split("\\.")[0];
                                                if (sTimeVal.length() > 5) sTimeVal = sTimeVal.substring(0, 5);
                                                if ("24:00".equals(sTimeVal)) sTimeVal = "23:59";

                                                String eTimeVal = bEndStr != null ? bEndStr : "00:00:00";
                                                if (eTimeVal.contains(" ")) eTimeVal = eTimeVal.split(" ")[1];
                                                if (eTimeVal.contains(".")) eTimeVal = eTimeVal.split("\\.")[0];
                                                if (eTimeVal.length() > 5) eTimeVal = eTimeVal.substring(0, 5);
                                                if ("24:00".equals(eTimeVal)) eTimeVal = "23:59";

                                                LocalTime bStart = LocalTime.MIN;
                                                LocalTime bEnd = LocalTime.MAX;
                                                try { bStart = LocalTime.parse(sTimeVal); } catch (Exception ign) {}
                                                try { bEnd = LocalTime.parse(eTimeVal); } catch (Exception ign) {}

                                                // Check overlap with 30-min slot
                                                if (!bStart.isAfter(slotTimeEnd) && bEnd.isAfter(slotTimeStart)) {
                                                foundBooking = b;
                                                break;
                                                }
                                                }
                                                }

                                                String cellClass = "status-available";
                                                String cellText = "";
                                                String cellTooltip = "Khung giờ trống (" + String.format("%02d:%02d", m / 60, m % 60) + ")";
                                                String cellOnclick = "";

                                                if ("MAINTENANCE".equals(fieldStatus)) {
                                                cellClass = "status-field-maintenance";
                                                cellText = "Bảo trì";
                                                cellTooltip = "Sân đang bảo trì";
                                                } else if ("DISABLED".equals(fieldStatus)) {
                                                cellClass = "status-field-disabled";
                                                cellText = "Khóa";
                                                cellTooltip = "Sân đang tạm ngưng";
                                                } else if (foundBooking != null) {
                                                String bStatus = (String) foundBooking.get("status");
                                                long bId = ((Number) foundBooking.get("bookingId")).longValue();
                                                String custName = (String) foundBooking.get("customerName");
                                                String bCode = (String) foundBooking.get("bookingCode");
                                                String slotDisplay = (custName != null && !custName.trim().isEmpty()) ?
                                                custName : bCode;

                                                if ("CONFIRMED".equals(bStatus)) {
                                                boolean isCellExp = false;
                                                boolean isCellLate = false;
                                                String bStartStr = (String) foundBooking.get("startTime");
                                                String bEndStr = (String) foundBooking.get("endTime");
                                                java.time.LocalDateTime nowR = java.time.LocalDateTime.now();
                                                java.time.LocalDate today = java.time.LocalDate.now();
                                                if (selectedDate != null && selectedDate.equals(today.toString())) {
                                                if (bEndStr != null && !bEndStr.isEmpty()) {
                                                try {
                                                String ie = bEndStr.contains(" ") ? bEndStr.replace(" ", "T") :
                                                (selectedDate + "T" + bEndStr);
                                                if (ie.contains(".")) ie = ie.substring(0, ie.indexOf("."));
                                                java.time.LocalDateTime eDt = java.time.LocalDateTime.parse(ie);
                                                isCellExp = nowR.isAfter(eDt);
                                                } catch (Exception ign) {}
                                                }
                                                if (bStartStr != null && !bStartStr.isEmpty()) {
                                                try {
                                                String is2 = bStartStr.contains(" ") ? bStartStr.replace(" ", "T") :
                                                (selectedDate + "T" + bStartStr);
                                                if (is2.contains(".")) is2 = is2.substring(0, is2.indexOf("."));
                                                java.time.LocalDateTime sDt = java.time.LocalDateTime.parse(is2);
                                                isCellLate = nowR.isAfter(sDt.plusMinutes(30)) && !isCellExp;
                                                } catch (Exception ign) {}
                                                }
                                                }

                                                if (isCellLate) {
                                                cellClass = "status-booked-late";
                                                cellText = slotDisplay;
                                                cellTooltip = bCode + " - " + slotDisplay + " (Muộn >30p)";
                                                } else if (isCellExp) {
                                                cellClass = "status-booked-late";
                                                cellText = slotDisplay;
                                                cellTooltip = bCode + " - " + slotDisplay + " (Quá giờ)";
                                                } else {
                                                cellClass = "status-booked-confirmed";
                                                cellText = slotDisplay;
                                                cellTooltip = bCode + " - " + slotDisplay + " (Chờ check-in)";
                                                }
                                                cellOnclick = "showBookingDetails(" + bId + ");";
                                                } else if ("CHECKED_IN".equals(bStatus)) {
                                                cellClass = "status-booked-checkedin";
                                                cellText = slotDisplay;
                                                cellTooltip = bCode + " - " + slotDisplay + " (Đang chơi)";
                                                cellOnclick = "showBookingDetails(" + bId + ");";
                                                } else if ("PENDING_CHECKOUT_PAYMENT".equals(bStatus)) {
                                                cellClass = "status-booked-pending-payment";
                                                cellText = slotDisplay;
                                                cellTooltip = bCode + " - " + slotDisplay + " (Chờ thanh toán)";
                                                cellOnclick = "showBookingDetails(" + bId + ");";
                                                } else if ("COMPLETED".equals(bStatus)) {
                                                cellClass = "status-booked-completed";
                                                cellText = slotDisplay;
                                                cellTooltip = bCode + " - " + slotDisplay + " (Hoàn thành)";
                                                cellOnclick = "showBookingDetails(" + bId + ");";
                                                }
                                                }
                                                %>
                                                <td class="slot-cell <%= cellClass %>" onclick="<%= cellOnclick %>"
                                                  title="<%= cellTooltip %>">
                                                  <%= cellText %>
                                                </td>
                                                <% } %>
                                          </tr>
                                          <% } } %>
                                    </tbody>
                                  </table>
                                </div>
                              </div>

                              <!-- MATCH DETAILED LIST -->
                              <div class="card soft-card p-4 mt-4">
                                <h4 class="fw-bold mb-3"><i class="bi bi-list-stars text-success me-2"></i>Chi tiết trận
                                  đấu trong ngày</h4>

                                <%
                                  String sShiftStart2 = (String) request.getAttribute("shiftStartTime");
                                  String sShiftEnd2 = (String) request.getAttribute("shiftEndTime");
                                  java.time.LocalTime shiftStartLT2 = null;
                                  java.time.LocalTime shiftEndLT2 = null;
                                  try {
                                    if (sShiftStart2 != null) {
                                      String cs2 = sShiftStart2.contains(" ") ? sShiftStart2.split(" ")[1] : sShiftStart2;
                                      if (cs2.contains(".")) cs2 = cs2.split("\\.")[0];
                                      if (cs2.length() > 5) cs2 = cs2.substring(0, 5);
                                      if ("24:00".equals(cs2)) cs2 = "23:59";
                                      shiftStartLT2 = java.time.LocalTime.parse(cs2);
                                    }
                                    if (sShiftEnd2 != null) {
                                      String ce2 = sShiftEnd2.contains(" ") ? sShiftEnd2.split(" ")[1] : sShiftEnd2;
                                      if (ce2.contains(".")) ce2 = ce2.split("\\.")[0];
                                      if (ce2.length() > 5) ce2 = ce2.substring(0, 5);
                                      if ("24:00".equals(ce2)) ce2 = "23:59";
                                      shiftEndLT2 = java.time.LocalTime.parse(ce2);
                                    }
                                  } catch (Exception ignored) {}
                                  java.util.List<Map<String,Object>> inShiftBk = new java.util.ArrayList<>();
                                  java.util.List<Map<String,Object>> outShiftBk = new java.util.ArrayList<>();
                                  if (bookings != null) {
                                    for (Map<String,Object> bk2 : bookings) {
                                      boolean inShift2 = true;
                                      if (hasShift && shiftStartLT2 != null && shiftEndLT2 != null) {
                                        String bkS2 = (String) bk2.get("startTime");
                                        if (bkS2 != null) {
                                          try {
                                            String tv2 = bkS2.contains(" ") ? bkS2.split(" ")[1] : bkS2;
                                            if (tv2.contains(".")) tv2 = tv2.split("\\.")[0];
                                            if (tv2.length() > 5) tv2 = tv2.substring(0, 5);
                                            java.time.LocalTime bkT2 = java.time.LocalTime.parse(tv2);
                                            if (shiftStartLT2.isBefore(shiftEndLT2) || shiftStartLT2.equals(shiftEndLT2)) {
                                              inShift2 = !bkT2.isBefore(shiftStartLT2) && !bkT2.isAfter(shiftEndLT2);
                                            } else {
                                              inShift2 = !bkT2.isBefore(shiftStartLT2) || !bkT2.isAfter(shiftEndLT2);
                                            }
                                          } catch (Exception ignored2) {}
                                        }
                                      }
                                      if (inShift2) inShiftBk.add(bk2); else outShiftBk.add(bk2);
                                    }
                                  }
                                %>

                                            <!-- ── Section 1: Booking TRONG CA ── -->
                                            <div class="mb-3">
                                              <div class="d-flex align-items-center gap-2 mb-3">
                                                <span class="badge bg-success-subtle text-success fw-bold"
                                                  style="font-size:0.8rem;border-radius:8px;padding:5px 12px;"><i
                                                    class="bi bi-clock-fill me-1"></i>Trong ca làm việc</span>
                                              </div>
                                              <div class="table-responsive">
                                                <table class="table table-hover align-middle mb-0"
                                                  style="font-size:0.9rem;table-layout:fixed;min-width:900px;">
                                                  <colgroup>
                                                    <col style="width:110px">
                                                    <col style="width:130px">
                                                    <col style="width:100px">
                                                    <col style="width:160px">
                                                    <col style="width:130px">
                                                    <col style="width:120px">
                                                    <col style="width:150px">
                                                    <col style="width:120px">
                                                  </colgroup>
                                                  <thead class="table-light">
                                                    <tr>
                                                      <th>Giờ</th>
                                                      <th>Mã đặt sân</th>
                                                      <th>Sân bóng</th>
                                                      <th>Khách hàng</th>
                                                      <th>Số điện thoại</th>
                                                      <th class="text-center">Tổng thanh toán</th>
                                                      <th>Trạng thái</th>
                                                      <th>Hành động</th>
                                                    </tr>
                                                  </thead>
                                                   <tbody>
                                                     <% if (inShiftBk.isEmpty()) { %>
                                                       <tr>
                                                         <td colspan="8" class="text-center text-muted py-4" style="font-size:0.9rem;">
                                                           <i class="bi bi-calendar-x me-2"></i>Không có trận đấu nào trong ca này.
                                                         </td>
                                                       </tr>
                                                     <% } else { for (Map<String,Object> b : inShiftBk) {
                                                         long bId = ((Number) b.get("bookingId")).longValue();
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
                                                         String sT = bStart != null ? bStart : "00:00";
                                                         if (sT.contains(" ")) sT = sT.split(" ")[1];
                                                         if (sT.contains(".")) sT = sT.split("\\.")[0];
                                                         if (sT.length() > 5) sT = sT.substring(0, 5);
                                                         String eT = bEnd != null ? bEnd : "00:00";
                                                         if (eT.contains(" ")) eT = eT.split(" ")[1];
                                                         if (eT.contains(".")) eT = eT.split("\\.")[0];
                                                         if (eT.length() > 5) eT = eT.substring(0, 5);
                                                         boolean isExpRow = false;
                                                         boolean isLateRow = false;
                                                         if ("CONFIRMED".equals(bStatus)) {
                                                           java.time.LocalDateTime nowR = java.time.LocalDateTime.now();
                                                           if (bEnd != null && !bEnd.isEmpty()) {
                                                             try {
                                                               String ie = bEnd.contains(" ") ? bEnd.replace(" ", "T") : (selectedDate + "T" + bEnd);
                                                               if (ie.contains(".")) ie = ie.substring(0, ie.indexOf("."));
                                                               java.time.LocalDateTime eDt = java.time.LocalDateTime.parse(ie);
                                                               isExpRow = nowR.isAfter(eDt);
                                                             } catch (Exception ign) {}
                                                           }
                                                           if (bStart != null && !bStart.isEmpty()) {
                                                             try {
                                                               String is2 = bStart.contains(" ") ? bStart.replace(" ", "T") : (selectedDate + "T" + bStart);
                                                               if (is2.contains(".")) is2 = is2.substring(0, is2.indexOf("."));
                                                               java.time.LocalDateTime sDt = java.time.LocalDateTime.parse(is2);
                                                               isLateRow = nowR.isAfter(sDt.plusMinutes(30)) && !isExpRow;
                                                             } catch (Exception ign) {}
                                                           }
                                                         }
                                                         String sb = "", ab = "";
                                                         if ("CONFIRMED".equals(bStatus)) {
                                                           if (isLateRow) {
                                                             sb = "<span class='badge bg-danger-subtle text-danger fw-bold'><i class='bi bi-exclamation-triangle me-1'></i>Muộn &gt;30p</span>";
                                                             ab = isUpcomingShift ? "<button class='btn btn-sm btn-secondary' disabled>Chờ ca trực</button>" : (isEndedShift ? "<button class='btn btn-sm btn-secondary' disabled>Hết ca trực</button>" : "<button type='button' class='btn btn-sm btn-sf-primary' onclick='openCheckinModal(" + bId + ")'>Check-in</button>");
                                                           } else if (isExpRow) {
                                                             sb = "<span class='badge bg-danger-subtle text-danger fw-bold'><i class='bi bi-exclamation-triangle me-1'></i>Quá giờ</span>";
                                                             ab = "<button class='btn btn-sm btn-secondary' disabled>Quá giờ nhận</button>";
                                                           } else {
                                                             sb = "<span class='badge badge-soft-warning'><i class='bi bi-hourglass-split me-1'></i>Chờ check-in</span>";
                                                             ab = isUpcomingShift ? "<button class='btn btn-sm btn-secondary' disabled>Chờ ca trực</button>" : (isEndedShift ? "<button class='btn btn-sm btn-secondary' disabled>Hết ca trực</button>" : "<button type='button' class='btn btn-sm btn-sf-primary' onclick='openCheckinModal(" + bId + ")'>Check-in</button>");
                                                           }
                                                         } else if ("CHECKED_IN".equals(bStatus)) {
                                                           sb = "<span class='badge badge-soft-info'><i class='pulse-playing me-1'></i>Đang đá</span>";
                                                           if (isUpcomingShift || isEndedShift) {
                                                             ab = "<button class='btn btn-sm btn-secondary' disabled>Đang sử dụng</button>";
                                                           } else if (checkoutDue) {
                                                             ab = "<a href='" + ctx + "/staff/checkout?id=" + bId + "' class='btn btn-sm btn-outline-success'>Checkout</a>";
                                                           } else {
                                                             ab = "<button class='btn btn-sm btn-secondary' disabled>Đang sử dụng</button>";
                                                           }
                                                         } else if ("PENDING_CHECKOUT_PAYMENT".equals(bStatus)) {
                                                           sb = "<span class='badge fw-bold' style='background:#fae8ff;color:#a21caf;'><i class='bi bi-credit-card me-1'></i>Chờ thanh toán</span>";
                                                           if (hasInvoice) ab = "<a href='" + ctx + "/staff/invoice?id=" + bId + "' class='btn btn-sm btn-outline-secondary'><i class='bi bi-file-earmark-text me-1'></i>Hóa đơn</a>";
                                                         } else if ("COMPLETED".equals(bStatus)) {
                                                           sb = "<span class='badge badge-soft-success'><i class='bi bi-check-circle me-1'></i>Đã xong</span>";
                                                           if (hasInvoice) ab = "<a href='" + ctx + "/staff/invoice?id=" + bId + "' class='btn btn-sm btn-outline-secondary'><i class='bi bi-file-earmark-text me-1'></i>Hóa đơn</a>";
                                                         }
                                                     %>
                                                       <tr onclick="handleRowClick(event, <%= bId %>)" style="cursor:pointer;">
                                                         <td style="white-space:nowrap;"><strong class="text-dark"><%= sT %> - <%= eT %></strong></td>
                                                         <td style="white-space:nowrap;"><strong class="text-success"><%= bCode %></strong></td>
                                                         <td style="white-space:nowrap;"><strong><%= fieldName %></strong></td>
                                                         <td style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;"><%= customerName %></td>
                                                         <td style="white-space:nowrap;"><%= (customerPhone != null && !customerPhone.trim().isEmpty()) ? customerPhone : "Không có SĐT" %></td>
                                                         <td class="text-center text-success fw-bold" style="white-space:nowrap;"><%= total != null ? String.format("%,d ₫", total.longValue()) : "—" %></td>
                                                         <td style="white-space:nowrap;"><%= sb %></td>
                                                         <td style="white-space:nowrap;"><%= ab %></td>
                                                       </tr>
                                                     <% } } %>
                                                   </tbody>
                                                 </table>
                                               </div>
                                             </div>

                                            <!-- ── Section 2: Booking NGOÀI CA ── -->
                                            <% if (!outShiftBk.isEmpty()) { %>
                                              <div class="mt-4 pt-3 border-top">
                                                <div class="d-flex align-items-center gap-2 mb-3">
                                                  <span class="badge bg-secondary-subtle text-secondary fw-bold"
                                                    style="font-size:0.8rem;border-radius:8px;padding:5px 12px;"><i
                                                      class="bi bi-clock me-1"></i>Ngoài ca làm việc</span>
                                                </div>
                                                <div class="table-responsive">
                                                  <table class="table table-hover align-middle mb-0"
                                                    style="font-size:0.9rem;table-layout:fixed;min-width:900px;opacity:0.88;">
                                                    <colgroup>
                                                      <col style="width:110px">
                                                      <col style="width:130px">
                                                      <col style="width:100px">
                                                      <col style="width:160px">
                                                      <col style="width:130px">
                                                      <col style="width:120px">
                                                      <col style="width:150px">
                                                      <col style="width:120px">
                                                    </colgroup>
                                                    <thead class="table-light">
                                                      <tr>
                                                        <th>Giờ</th>
                                                        <th>Mã đặt sân</th>
                                                        <th>Sân bóng</th>
                                                        <th>Khách hàng</th>
                                                        <th>Số điện thoại</th>
                                                        <th class="text-center">Tổng thanh toán</th>
                                                        <th>Trạng thái</th>
                                                        <th>Hành động</th>
                                                      </tr>
                                                    </thead>
                                                    <tbody>
                                                      <% for (Map<String,Object> b : outShiftBk) {
                                                        long bId = ((Number) b.get("bookingId")).longValue();
                                                        String bCode = (String) b.get("bookingCode");
                                                        String bStart = (String) b.get("startTime");
                                                        String bEnd = (String) b.get("endTime");
                                                        String bStatus = (String) b.get("status");
                                                        String fieldName = (String) b.get("fieldName");
                                                        String customerName = (String) b.get("customerName");
                                                        String customerPhone = (String) b.get("customerPhone");
                                                        java.math.BigDecimal total = (java.math.BigDecimal)
                                                        b.get("totalAmount");
                                                        boolean hasInvoice = Boolean.TRUE.equals(b.get("hasInvoice"));
                                                        boolean checkoutDue = Boolean.TRUE.equals(b.get("checkoutDue"));
                                                        String sT = bStart != null ? bStart : "00:00";
                                                        if (sT.contains(" ")) sT = sT.split(" ")[1];
                                                        if (sT.contains(".")) sT = sT.split("\\.")[0];
                                                        if (sT.length() > 5) sT = sT.substring(0, 5);
                                                        String eT = bEnd != null ? bEnd : "00:00";
                                                        if (eT.contains(" ")) eT = eT.split(" ")[1];
                                                        if (eT.contains(".")) eT = eT.split("\\.")[0];
                                                        if (eT.length() > 5) eT = eT.substring(0, 5);
                                                        boolean isExpRow2 = false;
                                                        boolean isLateRow2 = false;
                                                        if ("CONFIRMED".equals(bStatus)) {
                                                        java.time.LocalDateTime nowR2 = java.time.LocalDateTime.now();
                                                        if (bEnd != null && !bEnd.isEmpty()) {
                                                        try {
                                                        String ie2 = bEnd.contains(" ") ? bEnd.replace(" ", "T") :
                                                        (selectedDate + "T" + bEnd);
                                                        if (ie2.contains(".")) ie2 = ie2.substring(0, ie2.indexOf("."));
                                                        java.time.LocalDateTime eDt2 =
                                                        java.time.LocalDateTime.parse(ie2);
                                                        isExpRow2 = nowR2.isAfter(eDt2);
                                                        } catch(Exception ign){}
                                                        }
                                                        if (bStart != null && !bStart.isEmpty()) {
                                                        try {
                                                        String is3 = bStart.contains(" ") ? bStart.replace(" ", "T") :
                                                        (selectedDate + "T" + bStart);
                                                        if (is3.contains(".")) is3 = is3.substring(0, is3.indexOf("."));
                                                        java.time.LocalDateTime sDt2 = java.time.LocalDateTime.parse(is3);
                                                        isLateRow2 = nowR2.isAfter(sDt2.plusMinutes(30)) && !isExpRow2;
                                                        } catch(Exception ign){}
                                                        }
                                                        }
                                                        boolean isWithinExpandedWindow = false;
                                                        if (shiftStartLT2 != null && shiftEndLT2 != null) {
                                                          try {
                                                            java.time.LocalTime bkT3 = java.time.LocalTime.parse(sT);
                                                            java.time.LocalTime expandedStart = shiftStartLT2.minusHours(1);
                                                            java.time.LocalTime expandedEnd = shiftEndLT2.plusHours(1);

                                                            if (expandedStart.isBefore(expandedEnd) || expandedStart.equals(expandedEnd)) {
                                                              isWithinExpandedWindow = !bkT3.isBefore(expandedStart) && !bkT3.isAfter(expandedEnd);
                                                            } else {
                                                              isWithinExpandedWindow = !bkT3.isBefore(expandedStart) || !bkT3.isAfter(expandedEnd);
                                                            }
                                                          } catch (Exception ignored) {}
                                                        }

                                                        String sb2 = "", ab2 = "";
                                                        if ("CONFIRMED".equals(bStatus)) {
                                                          if (isLateRow2) sb2 = "<span class='badge bg-danger-subtle text-danger fw-bold'><i class='bi bi-exclamation-triangle me-1'></i>Muộn &gt;30p</span>";
                                                          else if (isExpRow2) sb2 = "<span class='badge bg-danger-subtle text-danger fw-bold'><i class='bi bi-exclamation-triangle me-1'></i>Quá giờ</span>";
                                                          else sb2 = "<span class='badge badge-soft-warning'><i class='bi bi-hourglass-split me-1'></i>Chờ check-in</span>";

                                                          if (isWithinExpandedWindow) {
                                                            ab2 = "<button type='button' class='btn btn-sm btn-sf-primary' onclick='openCheckinModal(" + bId + ")'>Check-in</button>";
                                                          } else {
                                                            ab2 = "<button class='btn btn-sm btn-secondary' disabled><i class='bi bi-lock-fill'></i> Khóa</button>";
                                                          }
                                                        } else if ("CHECKED_IN".equals(bStatus)) {
                                                          sb2 = "<span class='badge badge-soft-info'><i class='pulse-playing me-1'></i>Đang đá</span>";
                                                          if (checkoutDue) {
                                                            if (isWithinExpandedWindow) {
                                                              ab2 = "<a href='" + ctx + "/staff/checkout?id=" + bId + "' class='btn btn-sm btn-outline-success'>Checkout</a>";
                                                            } else {
                                                              ab2 = "<button class='btn btn-sm btn-secondary' disabled><i class='bi bi-lock-fill'></i> Khóa</button>";
                                                            }
                                                          }
                                                        } else if ("PENDING_CHECKOUT_PAYMENT".equals(bStatus)) {
                                                          sb2 = "<span class='badge fw-bold' style='background:#fae8ff;color:#a21caf;'><i class='bi bi-credit-card me-1'></i>Chờ thanh toán</span>";
                                                          if (hasInvoice) ab2 = "<a href='" + ctx + "/staff/invoice?id=" + bId + "' class='btn btn-sm btn-outline-secondary'><i class='bi bi-file-earmark-text me-1'></i>Hóa đơn</a>";
                                                        } else if ("COMPLETED".equals(bStatus)) {
                                                          sb2 = "<span class='badge badge-soft-success'><i class='bi bi-check-circle me-1'></i>Đã xong</span>";
                                                          if (hasInvoice) ab2 = "<a href='" + ctx + "/staff/invoice?id=" + bId + "' class='btn btn-sm btn-outline-secondary'><i class='bi bi-file-earmark-text me-1'></i>Hóa đơn</a>";
                                                        }
                                                        %>
                                                        <tr onclick="handleRowClick(event, <%= bId %>)"
                                                          style="cursor:pointer;">
                                                          <td style="white-space:nowrap;"><strong class="text-dark">
                                                              <%= sT %> - <%= eT %>
                                                            </strong></td>
                                                          <td style="white-space:nowrap;"><strong
                                                              class="text-secondary">
                                                              <%= bCode %>
                                                            </strong></td>
                                                          <td style="white-space:nowrap;"><strong>
                                                              <%= fieldName %>
                                                            </strong></td>
                                                          <td
                                                            style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
                                                            <%= customerName %>
                                                          </td>
                                                          <td style="white-space:nowrap;">
                                                            <%= (customerPhone !=null &&
                                                              !customerPhone.trim().isEmpty()) ? customerPhone
                                                              : "Không có SĐT" %>
                                                          </td>
                                                          <td class="text-center fw-bold" style="white-space:nowrap;">
                                                            <%= total!=null?String.format("%,d ₫",total.longValue()):"—"
                                                              %>
                                                          </td>
                                                          <td style="white-space:nowrap;">
                                                            <%= sb2 %>
                                                          </td>
                                                          <td style="white-space:nowrap;">
                                                            <%= ab2 %>
                                                          </td>
                                                        </tr>
                                                        <% } %>
                                                    </tbody>
                                                  </table>
                                                </div>
                                              </div>
                                              <% } %>
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
                              <button type="button" class="btn-close" data-bs-dismiss="modal"
                                aria-label="Close"></button>
                            </div>
                            <div class="modal-body py-4">
                              <form id="field-status-form">
                                <input type="hidden" id="modal-field-id">
                                <div class="mb-3">
                                  <label class="form-label small fw-bold text-muted">Sân bóng</label>
                                  <input type="text" class="form-control" id="modal-field-name" readonly
                                    style="background:#f1f5f9;">
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
                              <button type="button" class="btn btn-sf-primary" onclick="submitFieldStatusChange()">Lưu
                                thay đổi</button>
                            </div>
                          </div>
                        </div>
                      </div>

                      <!-- Booking Details Modal -->
                      <div class="modal fade" id="bookingDetailModal" tabindex="-1"
                        aria-labelledby="bookingDetailModalLabel" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-centered modal-lg">
                          <div class="modal-content border-0 shadow-lg"
                            style="border-radius: 24px; overflow: hidden; background: #ffffff;">
                            <!-- Header -->
                            <div
                              class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
                              <div class="d-flex align-items-center gap-3">
                                <div
                                  class="rounded-circle bg-success-subtle p-2 text-success d-flex align-items-center justify-content-center"
                                  style="width: 48px; height: 48px; background-color: #dcfce7;">
                                  <i class="bi bi-calendar-check-fill fs-4" style="color: #16a34a;"></i>
                                </div>
                                <div>
                                  <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="bookingDetailModalLabel">Chi
                                    tiết lịch đặt sân</h5>
                                  <span class="text-muted small" style="font-size: 0.8rem;">Mã đặt sân: <strong
                                      class="text-success" id="det-code">—</strong></span>
                                </div>
                              </div>
                              <button type="button" class="btn-close bg-light rounded-circle p-2"
                                data-bs-dismiss="modal" aria-label="Close" style="font-size: 0.8rem;"></button>
                            </div>

                            <!-- Body -->
                            <div class="modal-body p-4">
                              <div class="row g-4">
                                <!-- Left Column: Customer & Match Details -->
                                <div class="col-md-7">
                                  <!-- Customer Block -->
                                  <div class="mb-4">
                                    <h6 class="fw-bold text-muted uppercase small mb-3 tracking-wider"
                                      style="font-size: 0.75rem; letter-spacing: 0.05em;"><i
                                        class="bi bi-person-fill me-2" style="color: #16a34a;"></i>THÔNG TIN KHÁCH HÀNG
                                    </h6>
                                    <div class="p-3 bg-light rounded-4 border border-light-subtle"
                                      style="background-color: #f8fafc !important;">
                                      <div class="mb-2">
                                        <span class="text-muted small d-block" style="font-size: 0.75rem;">Tên khách
                                          hàng</span>
                                        <span class="fw-bold text-dark fs-6" id="det-name">—</span>
                                      </div>
                                      <div>
                                        <span class="text-muted small d-block" style="font-size: 0.75rem;">Số điện
                                          thoại</span>
                                        <span class="fw-bold text-success fs-6" id="det-phone">—</span>
                                      </div>
                                    </div>
                                  </div>

                                  <!-- Match Details Block -->
                                  <div>
                                    <h6 class="fw-bold text-muted uppercase small mb-3 tracking-wider"
                                      style="font-size: 0.75rem; letter-spacing: 0.05em;"><i
                                        class="bi bi-heptagon-fill me-2" style="color: #16a34a;"></i>THÔNG TIN TRẬN ĐẤU
                                    </h6>
                                    <div class="p-3 bg-light rounded-4 border border-light-subtle"
                                      style="background-color: #f8fafc !important;">
                                      <div class="row g-3">
                                        <div class="col-6">
                                          <span class="text-muted small d-block" style="font-size: 0.75rem;">Sân
                                            bóng</span>
                                          <span class="fw-bold text-dark" id="det-field">—</span>
                                        </div>
                                        <div class="col-6">
                                          <span class="text-muted small d-block" style="font-size: 0.75rem;">Trạng
                                            thái</span>
                                          <div id="det-status-badge">—</div>
                                        </div>
                                        <div class="col-12">
                                          <span class="text-muted small d-block" style="font-size: 0.75rem;">Khung giờ
                                            sử dụng</span>
                                          <span class="fw-bold text-dark fs-6"><i
                                              class="bi bi-clock me-2 text-muted"></i><span
                                              id="det-time">—</span></span>
                                        </div>
                                      </div>
                                    </div>
                                  </div>
                                </div>

                                <!-- Right Column: Cost Details -->
                                <div class="col-md-5">
                                  <h6 class="fw-bold text-muted uppercase small mb-3 tracking-wider"
                                    style="font-size: 0.75rem; letter-spacing: 0.05em;"><i
                                      class="bi bi-receipt-cutoff me-2" style="color: #16a34a;"></i>CHI TIẾT THANH TOÁN
                                  </h6>
                                  <div
                                    class="p-4 rounded-4 shadow-sm border border-success-subtle d-flex flex-column justify-content-between"
                                    style="background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%); border-color: #bbf7d0 !important; border: 1px solid; min-height: 230px;">
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
                                      <span class="text-muted small d-block mb-1"
                                        style="font-size: 0.72rem; letter-spacing: 0.05em; font-weight: 700;">CẦN THANH
                                        TOÁN CÒN LẠI</span>
                                      <span class="fw-bold text-success display-6"
                                        style="font-weight: 800; font-size: 1.8rem;" id="det-total-amount">—</span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>

                            <!-- Footer -->
                            <div class="modal-footer border-0 px-4 pb-4 pt-0 d-flex justify-content-end gap-2"
                              id="det-modal-footer">
                              <button type="button" class="btn btn-light px-4 py-2 rounded-3" data-bs-dismiss="modal"
                                style="font-weight: 600;">Đóng</button>
                            </div>
                          </div>
                        </div>
                      </div>

                      <!-- Check-in Confirmation Modal -->
                      <div class="modal fade" id="checkinConfirmModal" tabindex="-1"
                        aria-labelledby="checkinConfirmModalLabel" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-centered modal-lg">
                          <div class="modal-content border-0 shadow-lg"
                            style="border-radius:24px;overflow:hidden;background:#ffffff;">
                            <div
                              class="modal-header border-0 px-4 pt-4 pb-0 d-flex align-items-center justify-content-between">
                              <div class="d-flex align-items-center gap-3">
                                <div class="rounded-circle p-2 d-flex align-items-center justify-content-center"
                                  style="width:48px;height:48px;background-color:#dcfce7;"><i
                                    class="bi bi-box-arrow-in-right fs-4" style="color:#16a34a;"></i></div>
                                <div>
                                  <h5 class="modal-title fw-bold text-dark fs-5 mb-0" id="checkinConfirmModalLabel">Xác
                                    nhận nhận sân (Check-in)</h5>
                                  <span class="text-muted small">Mã đặt sân: <strong class="text-success"
                                      id="chk-modal-code">—</strong></span>
                                </div>
                              </div>
                              <button type="button" class="btn-close bg-light rounded-circle p-2"
                                data-bs-dismiss="modal" aria-label="Close" style="font-size:0.8rem;"></button>
                            </div>
                            <div class="modal-body p-4">
                              <div id="chk-modal-alert" class="d-none"></div>
                              <input type="hidden" id="chk-modal-booking-id">
                              <div class="row g-4">
                                <div class="col-md-7">
                                  <h6 class="fw-bold text-muted small mb-2"
                                    style="font-size:0.75rem;letter-spacing:0.05em;"><i class="bi bi-person-fill me-2"
                                      style="color:#16a34a;"></i>THÔNG TIN KHÁCH HÀNG</h6>
                                  <div class="p-3 rounded-4 border" style="background:#f8fafc;">
                                    <div class="mb-2"><span class="text-muted small d-block"
                                        style="font-size:0.75rem;">Tên khách hàng</span><span
                                        class="fw-bold text-dark fs-6" id="chk-modal-name">—</span></div>
                                    <div><span class="text-muted small d-block" style="font-size:0.75rem;">Số điện
                                        thoại</span><span class="fw-bold text-success fs-6"
                                        id="chk-modal-phone">—</span></div>
                                  </div>
                                  <h6 class="fw-bold text-muted small mb-2 mt-3"
                                    style="font-size:0.75rem;letter-spacing:0.05em;"><i class="bi bi-heptagon-fill me-2"
                                      style="color:#16a34a;"></i>SÂN & KHUNG GIỜ</h6>
                                  <div class="p-3 rounded-4 border" style="background:#f8fafc;">
                                    <div class="row g-2">
                                      <div class="col-6"><span class="text-muted small d-block"
                                          style="font-size:0.75rem;">Sân bóng</span><span class="fw-bold text-dark"
                                          id="chk-modal-field">—</span></div>
                                      <div class="col-6"><span class="text-muted small d-block"
                                          style="font-size:0.75rem;">Khung giờ</span><span class="fw-bold text-dark"
                                          id="chk-modal-time">—</span></div>
                                    </div>
                                  </div>
                                  <div class="mt-3">
                                    <label for="chk-modal-note" class="form-label fw-bold text-muted small mb-1"><i
                                        class="bi bi-journal-text me-1 text-success"></i>Ghi chú nhận sân (Tùy
                                      chọn)</label>
                                    <textarea class="form-control rounded-3" id="chk-modal-note" rows="2"
                                      placeholder="Ví dụ: Khách mượn bóng số 5..."></textarea>
                                  </div>
                                </div>
                                <div class="col-md-5">
                                  <h6 class="fw-bold text-muted small mb-2"
                                    style="font-size:0.75rem;letter-spacing:0.05em;"><i
                                      class="bi bi-receipt-cutoff me-2" style="color:#16a34a;"></i>THANH TOÁN HÔM NAY
                                  </h6>
                                  <div
                                    class="p-4 rounded-4 border border-success-subtle d-flex flex-column justify-content-between"
                                    style="background:linear-gradient(135deg,#f0fdf4 0%,#ffffff 100%);min-height:210px;">
                                    <div>
                                      <div class="d-flex justify-content-between mb-3"><span
                                          class="text-muted small">Giá gốc sân:</span><span class="fw-bold text-dark"
                                          id="chk-modal-price">—</span></div>
                                      <div class="d-flex justify-content-between mb-3"><span class="text-muted small">Đã
                                          đặt cọc:</span><span class="fw-bold text-danger"
                                          id="chk-modal-deposit">—</span></div>
                                      <hr class="my-2">
                                    </div>
                                    <div class="text-center py-2">
                                      <span class="text-muted small d-block mb-1"
                                        style="font-size:0.72rem;font-weight:700;">CẦN THANH TOÁN CÒN LẠI</span>
                                      <span class="fw-bold text-success" style="font-size:1.8rem;"
                                        id="chk-modal-remaining">—</span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                            <div class="modal-footer border-0 px-4 pb-4 pt-0 d-flex justify-content-end gap-2"
                              id="chk-modal-footer">
                              <button type="button" class="btn btn-light px-4 py-2 rounded-3"
                                data-bs-dismiss="modal">Đóng</button>
                              <div id="chk-modal-actions" class="d-flex gap-2">
                                <button type="button" class="btn btn-success px-4 py-2 rounded-3"
                                  id="chk-modal-submit-btn" onclick="submitCheckinForm(event)">
                                  <i class="bi bi-check-circle me-1"></i>Xác nhận Check-in
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      <script
                        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
                      <script src="<%= ctx %>/assets/js/app.js"></script>
                      <script>
                        // ── Global Schedule / Timeline Booking Data ───────────────────────────────
                        const bookingsData = [
    <% 
      if (bookings != null && !bookings.isEmpty()) {
                          for (int i = 0; i < bookings.size(); i++) {
                            Map < String, Object > b = bookings.get(i);
          long bId = ((Number) b.get("bookingId")).longValue();
          String bCode = b.get("bookingCode") != null ? b.get("bookingCode").toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") : "";
          String bStart = b.get("startTime") != null ? b.get("startTime").toString() : ""; 
          String bEnd = b.get("endTime") != null ? b.get("endTime").toString() : "";
          String bStatus = b.get("status") != null ? b.get("status").toString() : "";
          String fieldName = b.get("fieldName") != null ? b.get("fieldName").toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") : "";
          String customerName = b.get("customerName") != null ? b.get("customerName").toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") : "";
          String customerPhone = b.get("customerPhone") != null ? b.get("customerPhone").toString().replace("\\", "\\\\").replace("\"", "\\\"") : "";
                            java.math.BigDecimal total = (java.math.BigDecimal) b.get("totalAmount");
                            java.math.BigDecimal deposit = (java.math.BigDecimal) b.get("depositAmount");
          boolean hasInvoice = Boolean.TRUE.equals(b.get("hasInvoice"));
          boolean checkoutDue = Boolean.TRUE.equals(b.get("checkoutDue"));
          String payMethod = b.get("paymentMethodName") != null ? b.get("paymentMethodName").toString().replace("\\", "\\\\").replace("\"", "\\\"") : "Tiền mặt";
    %>
                              {
                                bookingId: <%= bId %>,
                              bookingCode: "<%= bCode %>",
                                startTime: "<%= bStart %>",
                                  endTime: "<%= bEnd %>",
                                    status: "<%= bStatus %>",
                                      fieldName: "<%= fieldName %>",
                                        customerName: "<%= customerName %>",
                                          customerPhone: "<%= customerPhone %>",
                                            totalAmount: <%= total != null ? total : 0 %>,
                                              depositAmount: <%= deposit != null ? deposit : 0 %>,
                                                hasInvoice: <%= hasInvoice %>,
                                                  checkoutDue: <%= checkoutDue %>,
                                                    paymentMethodName: "<%= payMethod %>"
                          }<%= (i < bookings.size() - 1) ? "," : "" %>
    <% 
        }
      }
    %>
  ];

                        const isUpcomingShift = <%= isUpcomingShift %>;
                        const isEndedShift = <%= isEndedShift %>;

                        let bookingDetailModalInstance = null;
                        let checkinConfirmModalInstance = null;
                        let fieldStatusModalInstance = null;
                        let statusModal = null;

                        document.addEventListener('DOMContentLoaded', () => {
                          const modalDetailEl = document.getElementById('bookingDetailModal');
                          if (modalDetailEl) bookingDetailModalInstance = new bootstrap.Modal(modalDetailEl);

                          const modalChkEl = document.getElementById('checkinConfirmModal');
                          if (modalChkEl) checkinConfirmModalInstance = new bootstrap.Modal(modalChkEl);

                          const modalFieldEl = document.getElementById('fieldStatusModal');
                          if (modalFieldEl) fieldStatusModalInstance = new bootstrap.Modal(modalFieldEl);
                        });

                        function handleRowClick(event, bookingId) {
                          if (event.target.closest('button') || event.target.closest('a')) {
                            return;
                          }
                          showBookingDetails(bookingId);
                        }

                        function fmt(amount) {
                          if (amount == null) return '—';
                          return Number(amount).toLocaleString('vi-VN') + ' ₫';
                        }

                        function timeOnly(dateTimeStr) {
                          if (!dateTimeStr) return '';
                          let t = dateTimeStr;
                          if (t.includes(' ')) t = t.split(' ')[1];
                          if (t.includes('.')) t = t.split('.')[0];
                          return t.substring(0, 5);
                        }

                        function isBookingExpired(endTimeStr) {
                          if (!endTimeStr) return false;
                          try {
                            let str = String(endTimeStr).trim();
                            if (!str.includes('-') && !str.includes('/')) {
                              const selectedDateStr = "<%= selectedDate %>";
                              str = selectedDateStr + ' ' + str;
                            }
                            const isoStr = str.replace(' ', 'T').substring(0, 19);
                            const endDt = new Date(isoStr);
                            const now = new Date();
                            return endDt < now;
                          } catch (e) {
                            return false;
                          }
                        }

                        function isBookingLateNoShow(startTimeStr, endTimeStr) {
                          if (!startTimeStr || !endTimeStr) return false;
                          try {
                            const selectedDateStr = "<%= selectedDate %>";
                            let sStr = String(startTimeStr).trim();
                            if (!sStr.includes('-') && !sStr.includes('/')) {
                              sStr = selectedDateStr + ' ' + sStr;
                            }
                            let eStr = String(endTimeStr).trim();
                            if (!eStr.includes('-') && !eStr.includes('/')) {
                              eStr = selectedDateStr + ' ' + eStr;
                            }
                            const isoStart = sStr.replace(' ', 'T').substring(0, 19);
                            const startDt = new Date(isoStart);
                            const thresholdDt = new Date(startDt.getTime() + 30 * 60 * 1000);

                            const isoEnd = eStr.replace(' ', 'T').substring(0, 19);
                            const endDt = new Date(isoEnd);

                            const now = new Date();
                            return now > thresholdDt && now <= endDt;
                          } catch (e) {
                            return false;
                          }
                        }

                        function statusBadge(status, nowPlaying, isExpired, startTimeStr, endTimeStr) {
                          if (status === 'CHECKED_IN') {
                            return `<span class="badge badge-soft-info"><i class="pulse-playing me-1"></i>Đang chơi</span>`;
                          }
                          if (status === 'PENDING_CHECKOUT_PAYMENT') {
                            return `<span class="badge fw-bold" style="background:#fae8ff;color:#a21caf;"><i class="bi bi-credit-card me-1"></i>Chờ thanh toán</span>`;
                          }
                          if (status === 'COMPLETED') {
                            return `<span class="badge badge-soft-success"><i class="bi bi-check-circle me-1"></i>Đã xong</span>`;
                          }
                          if (status === 'CONFIRMED' && isBookingLateNoShow(startTimeStr, endTimeStr)) {
                            return `<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Muộn 30p</span>`;
                          }
                          if (isExpired) {
                            return `<span class="badge bg-danger-subtle text-danger fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>Quá giờ</span>`;
                          }
                          return `<span class="badge badge-soft-warning"><i class="bi bi-hourglass-split me-1"></i>Chờ check-in</span>`;
                        }

                        function showBookingDetails(bookingId) {
                          const b = bookingsData.find(x => Number(x.bookingId) === Number(bookingId));
                          if (!b) return;

                          if (b.status === 'CONFIRMED') {
                            openCheckinModal(bookingId);
                            return;
                          }

                          const isExpired = isBookingExpired(b.endTime);

                          document.getElementById('det-code').textContent = b.bookingCode || '—';
                          document.getElementById('det-name').textContent = b.customerName || '—';
                          document.getElementById('det-phone').textContent = (b.customerPhone && b.customerPhone !== 'null' && String(b.customerPhone).trim() !== '') ? b.customerPhone : 'Không có SĐT';
                          document.getElementById('det-field').textContent = b.fieldName || '—';
                          document.getElementById('det-time').textContent = timeOnly(b.startTime) + " - " + timeOnly(b.endTime);
                          document.getElementById('det-status-badge').innerHTML = statusBadge(b.status, false, isExpired, b.startTime, b.endTime);

                          document.getElementById('det-orig-price').textContent = fmt(b.totalAmount);
                          document.getElementById('det-deposit').textContent = fmt(b.depositAmount);

                          const isPaidOrCompleted = b.status === 'COMPLETED' || (b.status === 'PENDING_CHECKOUT_PAYMENT' && b.hasInvoice) || b.hasInvoice;
                          const payContainer = document.getElementById('det-payment-box');

                          if (isPaidOrCompleted) {
                            const methodText = b.paymentMethodName || 'Tiền mặt';
                            if (payContainer) {
                              payContainer.innerHTML =
                                '<span class="badge bg-success-subtle text-success fs-6 fw-bold px-3 py-1 rounded-pill mb-2 border border-success-subtle">' +
                                '<i class="bi bi-check-circle-fill me-1"></i>ĐÃ THANH TOÁN ĐỦ' +
                                '</span>' +
                                '<div class="fw-bold text-success display-6 my-1" style="font-weight: 800; font-size: 1.8rem;">' + fmt(b.totalAmount || 0) + '</div>' +
                                '<div class="text-muted small mt-2">' +
                                '<i class="bi bi-credit-card-2-front me-1 text-success"></i>Phương thức: <strong class="text-dark">' + methodText + '</strong>' +
                                '</div>';
                            }
                          } else {
                            const remaining = (b.totalAmount || 0) - (b.depositAmount || 0);
                            if (payContainer) {
                              payContainer.innerHTML =
                                '<span class="text-muted small d-block mb-1" style="font-size: 0.72rem; letter-spacing: 0.05em; font-weight: 700;">CẦN THANH TOÁN CÒN LẠI</span>' +
                                '<span class="fw-bold text-success display-6" style="font-weight: 800; font-size: 1.8rem;">' + fmt(remaining >= 0 ? remaining : 0) + '</span>';
                            }
                          }

                          const footer = document.getElementById('det-modal-footer');
                          let btnHtml = '<button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button>';

                          if (isUpcomingShift) {
                            if (b.status === 'CONFIRMED' || b.status === 'CHECKED_IN') {
                              btnHtml += '<button class="btn btn-secondary px-3" disabled title="Chưa đến giờ làm việc"><i class="bi bi-lock-fill me-1"></i>Chờ ca trực</button>';
                            }
                          } else if (isEndedShift) {
                            if (b.status === 'CONFIRMED' || b.status === 'CHECKED_IN') {
                              btnHtml += '<button class="btn btn-secondary px-3" disabled title="Ca trực đã kết thúc"><i class="bi bi-lock-fill me-1"></i>Hết ca trực</button>';
                            }
                          } else {
                            if (b.status === 'CONFIRMED') {
                              if (isBookingLateNoShow(b.startTime, b.endTime)) {
                                btnHtml += '<button type="button" class="btn btn-danger px-4" onclick="cancelNoshow(' + b.bookingId + ')"><i class="bi bi-x-circle me-1"></i>Hủy sân</button>';
                                btnHtml += '<button type="button" class="btn btn-success px-4" onclick="openCheckinModal(' + b.bookingId + ')"><i class="bi bi-check-circle me-1"></i>Check-in</button>';
                              } else if (isExpired) {
                                btnHtml += '<button class="btn btn-secondary px-3" disabled><i class="bi bi-exclamation-circle me-1"></i>Quá giờ nhận</button>';
                              } else {
                                btnHtml += '<button type="button" class="btn btn-success px-4" onclick="openCheckinModal(' + b.bookingId + ')"><i class="bi bi-check-circle me-1"></i>Check-in</button>';
                              }
                            } else if (b.status === 'CHECKED_IN') {
                              btnHtml += b.checkoutDue
                                ? ('<a href="<%= ctx %>/staff/checkout?id=' + b.bookingId + '" class="btn btn-success px-4">Checkout</a>')
                                : '<button class="btn btn-secondary px-3" disabled>Đang sử dụng</button>';
                            } else if ((b.status === 'PENDING_CHECKOUT_PAYMENT' || b.status === 'COMPLETED') && b.hasInvoice) {
                              btnHtml += '<a href="<%= ctx %>/staff/invoice?id=' + b.bookingId + '" class="btn btn-outline-secondary px-4"><i class="bi bi-file-earmark-text me-1"></i>Hóa đơn</a>';
                            }
                          }

                          footer.innerHTML = btnHtml;

                          if (!bookingDetailModalInstance) {
                            const el = document.getElementById('bookingDetailModal');
                            if (el && typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                              bookingDetailModalInstance = new bootstrap.Modal(el);
                            }
                          }
                          if (bookingDetailModalInstance) {
                            bookingDetailModalInstance.show();
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
                                } catch (e) { }
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

                        document.getElementById('date-selector') && document.getElementById('date-selector').addEventListener('change', function () {
                          window.location.href = '<%= ctx %>/staff/schedule?date=' + this.value;
                        });

                        function scrollToShiftTime() {
                          const shiftStartTimeStr = "<%= request.getAttribute("shiftStartTime") != null ? request.getAttribute("shiftStartTime") : "" %>";
                          const selectedDateStr = "<%= selectedDate %>";

                          let targetTimeStr = shiftStartTimeStr || "08:00";

                          // If viewing today's date, scroll to current time if within 05:00 - 22:30 range
                          const now = new Date();
                          const todayStr = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0');
                          if (selectedDateStr === todayStr) {
                            const curH = now.getHours();
                            const curM = now.getMinutes();
                            if (curH >= 5 && curH <= 22) {
                              targetTimeStr = String(curH).padStart(2, '0') + ':' + String(curM).padStart(2, '0');
                            }
                          }

                          let cleanTime = targetTimeStr.includes(' ') ? targetTimeStr.split(' ')[1] : targetTimeStr;
                          if (cleanTime.includes('.')) cleanTime = cleanTime.split('.')[0];
                          const parts = cleanTime.split(':');
                          let targetSlot = "05:00";
                          if (parts.length >= 2) {
                            let h = parseInt(parts[0], 10);
                            let m = parseInt(parts[1], 10);
                            if (isNaN(h)) h = 5;
                            if (isNaN(m)) m = 0;
                            m = m < 30 ? 0 : 30;
                            targetSlot = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
                          }

                          const targetTh = document.querySelector('th[data-time="' + targetSlot + '"]');
                          if (targetTh) {
                            const container = targetTh.closest('.table-responsive');
                            if (container) {
                              const fieldCol = container.querySelector('.field-col');
                              const stickyWidth = fieldCol ? fieldCol.offsetWidth : 140;
                              setTimeout(function () {
                                const scrollPos = Math.max(0, targetTh.offsetLeft - stickyWidth);
                                container.scrollTo({ left: scrollPos, behavior: 'smooth' });
                              }, 200);
                            }
                          }
                        }

                        window.addEventListener('DOMContentLoaded', function () {
                          const modalEl = document.getElementById('fieldStatusModal');
                          if (modalEl) statusModal = new bootstrap.Modal(modalEl);

                          const modalDetailEl = document.getElementById('bookingDetailModal');
                          if (modalDetailEl) bookingDetailModalInstance = new bootstrap.Modal(modalDetailEl);

                          const checkinEl = document.getElementById('checkinConfirmModal');
                          if (checkinEl) checkinConfirmModalInstance = new bootstrap.Modal(checkinEl);

                          // Auto scroll timeline grid to shift time / current time
                          scrollToShiftTime();
    
    <%
                            String errorParam = request.getParameter("error");
                          if (errorParam != null) {
    %>
      <% if ("facility_mismatch".equals(errorParam)) { %>
        showToast("Lượt đặt sân này thuộc cụm sân khác. Bạn không thể thực hiện thao tác này.", "danger");
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

                        function openCheckinModal(bookingId) {
                          const b = bookingsData.find(function (x) { return Number(x.bookingId) === Number(bookingId); });
                          if (!b) return;
                          if (bookingDetailModalInstance) {
                            try { bookingDetailModalInstance.hide(); } catch (e) { }
                          }
                          const isExpired = isBookingExpired(b.endTime);
                          const isLateNoShow = isBookingLateNoShow(b.startTime, b.endTime);

                          document.getElementById('chk-modal-code').textContent = b.bookingCode || '—';
                          document.getElementById('chk-modal-name').textContent = b.customerName || '—';
                          document.getElementById('chk-modal-phone').textContent = (b.customerPhone && b.customerPhone !== 'null' && String(b.customerPhone).trim() !== '') ? b.customerPhone : 'Không có SĐT';
                          document.getElementById('chk-modal-field').textContent = b.fieldName || '—';
                          document.getElementById('chk-modal-time').textContent = timeOnly(b.startTime) + ' – ' + timeOnly(b.endTime);
                          document.getElementById('chk-modal-price').textContent = fmt(b.totalAmount);
                          document.getElementById('chk-modal-deposit').textContent = fmt(b.depositAmount);
                          const rem = (Number(b.totalAmount) || 0) - (Number(b.depositAmount) || 0);
                          document.getElementById('chk-modal-remaining').textContent = fmt(rem >= 0 ? rem : 0);
                          document.getElementById('chk-modal-booking-id').value = b.bookingId;
                          document.getElementById('chk-modal-note').value = '';

                          const alertBox = document.getElementById('chk-modal-alert');
                          const actionsBox = document.getElementById('chk-modal-actions');

                          if (isUpcomingShift) {
                            if (alertBox) {
                              alertBox.innerHTML = '<div class="d-flex align-items-center gap-2 p-3 mb-3 rounded-4" style="background-color:#fffbeb;border:1px solid #fde68a;color:#92400e;font-size:0.85rem;font-weight:600;"><i class="bi bi-exclamation-triangle-fill fs-5 text-warning flex-shrink-0"></i><div>Ca trực chưa bắt đầu. Bạn không thể thực hiện check-in.</div></div>';
                              alertBox.classList.remove('d-none');
                            }
                            if (actionsBox) {
                              actionsBox.innerHTML = '<button class="btn btn-secondary px-4 py-2 rounded-3" disabled><i class="bi bi-lock-fill me-1"></i>Chờ ca trực</button>';
                            }
                          } else if (isEndedShift) {
                            if (alertBox) {
                              alertBox.innerHTML = '<div class="d-flex align-items-center gap-2 p-3 mb-3 rounded-4" style="background-color:#fef2f2;border:1px solid #fecaca;color:#991b1b;font-size:0.85rem;font-weight:600;"><i class="bi bi-exclamation-circle-fill fs-5 text-danger flex-shrink-0"></i><div>Ca trực đã kết thúc. Bạn không thể thực hiện check-in.</div></div>';
                              alertBox.classList.remove('d-none');
                            }
                            if (actionsBox) {
                              actionsBox.innerHTML = '<button class="btn btn-secondary px-4 py-2 rounded-3" disabled><i class="bi bi-lock-fill me-1"></i>Hết ca trực</button>';
                            }
                          } else if (isExpired) {
                            if (alertBox) {
                              alertBox.innerHTML = '<div class="d-flex align-items-center gap-2 p-3 mb-3 rounded-4" style="background-color:#fef2f2;border:1px solid #fecaca;color:#991b1b;font-size:0.85rem;font-weight:600;"><i class="bi bi-exclamation-circle-fill fs-5 text-danger flex-shrink-0"></i><div>Lịch đặt sân này đã quá giờ nhận.</div></div>';
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
                                '<button type="button" class="btn btn-danger px-4 py-2 rounded-3 me-2" onclick="cancelNoshow(' + b.bookingId + ')"><i class="bi bi-x-circle me-1"></i>Hủy sân (Khách không đến)</button>' +
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
                          if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang check-in...'; }
                          try {
                            const params = new URLSearchParams();
                            params.append('bookingId', bookingId);
                            if (note) params.append('note', note);
                            const res = await fetch('<%= ctx %>/api/staff/checkin', {
                              method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                              body: params, credentials: 'include'
                            });
                            let data = {}; try { data = await res.json(); } catch (e2) { }
                            if (res.ok && (data.success || !data.error)) {
                              if (checkinConfirmModalInstance) checkinConfirmModalInstance.hide();
                              if (typeof showToastAfterReload === 'function') showToastAfterReload('Đã check-in thành công!', 'success');
                              window.location.reload();
                            } else {
                              if (typeof showToast === 'function') showToast('Không thể check-in: ' + (data.error || 'Lỗi không xác định'), 'danger');
                              if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Xác nhận Check-in'; }
                            }
                          } catch (err) {
                            if (typeof showToast === 'function') showToast('Lỗi khi thực hiện check-in: ' + err.message, 'danger');
                            if (btn) { btn.disabled = false; btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Xác nhận Check-in'; }
                          }
                        }

                      </script>
                    </body>

                    </html>
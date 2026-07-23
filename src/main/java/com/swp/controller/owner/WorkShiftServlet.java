package com.swp.controller.owner;

import com.google.gson.Gson;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.WorkShiftDAO;
import com.swp.model.FootballComplex;
import com.swp.model.User;
import com.swp.model.WorkShift;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/owner/work-shift")
public class WorkShiftServlet extends HttpServlet {

    private final WorkShiftDAO workShiftDAO = new WorkShiftDAO();
    private final FootballComplexDAO footballComplexDAO = new FootballComplexDAO();
    private final com.swp.dao.NotificationDAO notificationDAO = new com.swp.dao.NotificationDAO();
    private final Gson gson = new Gson();

    private void notifyStaffAssigned(long staffId, String shiftName, LocalDate shiftDate, long shiftId) {
        com.swp.model.Notification notif = new com.swp.model.Notification();
        notif.setUserId(staffId);
        notif.setTitle("Phân công ca trực");
        notif.setMessage("Bạn đã được phân công ca trực: " + shiftName + " vào ngày " + shiftDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        notif.setNotificationType("SYSTEM");
        notif.setReferenceId(shiftId);
        notif.setIsRead(false);
        notif.setCreatedAt(java.time.LocalDateTime.now());
        notificationDAO.insertNotification(notif);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        // Auth Check
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"OWNER".equals(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        // Get action for sub-queries (e.g. AJAX load assignments)
        String action = req.getParameter("action");
        if ("getAssignments".equals(action)) {
            resp.setContentType("application/json;charset=UTF-8");
            try {
                long shiftId = Long.parseLong(req.getParameter("shiftId"));
                List<User> assignedStaff = workShiftDAO.getStaffAssignedToShift(shiftId);
                java.util.List<Map<String, Object>> staffMaps = new java.util.ArrayList<>();
                for (User u : assignedStaff) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("fullName", u.getFullName());
                    map.put("email", u.getEmail());
                    map.put("phone", u.getPhone());
                    map.put("avatarUrl", u.getAvatarUrl());
                    map.put("status", u.getStatus());
                    staffMaps.add(map);
                }
                writeJson(resp, staffMaps);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeError(resp, e.getMessage());
            }
            return;
        }

        // Standard Page Load
        List<FootballComplex> complexes = footballComplexDAO.getAllActiveComplex();
        List<User> staffList = workShiftDAO.getAllActiveStaff();
        List<WorkShift> shifts = workShiftDAO.getAllShifts();

        if (shifts != null) {
            LocalDateTime nowTime = LocalDateTime.now();
            shifts.sort((s1, s2) -> {
                LocalDateTime start1 = LocalDateTime.of(s1.getShiftDate(), s1.getStartTime());
                LocalDateTime end1;
                if (s1.getEndTime().isBefore(s1.getStartTime())) {
                    end1 = LocalDateTime.of(s1.getShiftDate().plusDays(1), s1.getEndTime());
                } else {
                    end1 = LocalDateTime.of(s1.getShiftDate(), s1.getEndTime());
                }
                int status1; // 0: ONGOING, 1: FUTURE, 2: PAST
                if (nowTime.isBefore(start1)) {
                    status1 = 1;
                } else if (nowTime.isAfter(end1)) {
                    status1 = 2;
                } else {
                    status1 = 0;
                }

                LocalDateTime start2 = LocalDateTime.of(s2.getShiftDate(), s2.getStartTime());
                LocalDateTime end2;
                if (s2.getEndTime().isBefore(s2.getStartTime())) {
                    end2 = LocalDateTime.of(s2.getShiftDate().plusDays(1), s2.getEndTime());
                } else {
                    end2 = LocalDateTime.of(s2.getShiftDate(), s2.getEndTime());
                }
                int status2;
                if (nowTime.isBefore(start2)) {
                    status2 = 1;
                } else if (nowTime.isAfter(end2)) {
                    status2 = 2;
                } else {
                    status2 = 0;
                }

                if (status1 != status2) {
                    return Integer.compare(status1, status2);
                }

                if (status1 == 0) { // ONGOING: closest to now start time first
                    long diff1 = Math.abs(java.time.Duration.between(nowTime, start1).toSeconds());
                    long diff2 = Math.abs(java.time.Duration.between(nowTime, start2).toSeconds());
                    return Long.compare(diff1, diff2);
                } else if (status1 == 1) { // FUTURE: closest to now first (chronological order)
                    return start1.compareTo(start2);
                } else { // PAST: closest to now first (reverse-chronological order)
                    return end2.compareTo(end1);
                }
            });
        }

        // Build a map of staff shift count for personnel dashboard
        Map<Long, Integer> staffShiftCounts = new HashMap<>();
        for (User staff : staffList) {
            staffShiftCounts.put(staff.getUserId(), 0);
        }
        for (WorkShift ws : shifts) {
            List<User> assigned = workShiftDAO.getStaffAssignedToShift(ws.getShiftId());
            for (User u : assigned) {
                staffShiftCounts.put(u.getUserId(), staffShiftCounts.getOrDefault(u.getUserId(), 0) + 1);
            }
        }

        req.setAttribute("complexes", complexes);
        req.setAttribute("staffList", staffList);
        req.setAttribute("shifts", shifts);
        req.setAttribute("staffShiftCounts", staffShiftCounts);

        req.getRequestDispatcher("/WEB-INF/owner/work-shift.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Chưa đăng nhập");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"OWNER".equals(user.getRoleName())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeError(resp, "Không có quyền truy cập");
            return;
        }

        String action = req.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Thiếu tham số action");
            return;
        }

        try {
            switch (action) {
                case "create" -> handleCreateShift(req, resp);
                case "edit" -> handleEditShift(req, resp);
                case "delete" -> handleDeleteShift(req, resp);
                case "deleteBatch" -> handleDeleteBatchShift(req, resp);
                case "assign" -> handleAssignStaff(req, resp);
                case "unassign" -> handleUnassignStaff(req, resp);
                default -> {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    writeError(resp, "Hành động không hợp lệ: " + action);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Lỗi xử lý: " + e.getMessage());
        }
    }

    private String determineShiftName(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return "Ca gãy";
        }
        // Ca sáng: 08:00 - 12:00
        if (start.equals(LocalTime.of(8, 0)) && end.equals(LocalTime.of(12, 0))) {
            return "Ca sáng";
        }
        // Ca chiều: 12:00 - 18:00
        if (start.equals(LocalTime.of(12, 0)) && end.equals(LocalTime.of(18, 0))) {
            return "Ca chiều";
        }
        // Ca tối: 18:00 - 22:00
        if (start.equals(LocalTime.of(18, 0)) && end.equals(LocalTime.of(22, 0))) {
            return "Ca tối";
        }
        return "Ca gãy";
    }

    private String getComplexShortName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String name = fullName.trim();
        if (name.toLowerCase().startsWith("sân bóng ")) {
            name = name.substring(9).trim();
        }
        String[] parts = name.split("\\s+");
        if (parts.length >= 2) {
            if (parts.length >= 3 && "Hồ".equalsIgnoreCase(parts[0]) && "Chí".equalsIgnoreCase(parts[1]) && "Minh".equalsIgnoreCase(parts[2])) {
                return "Hồ Chí Minh";
            }
            return parts[0] + " " + parts[1];
        }
        return name;
    }

    private void handleCreateShift(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long complexId = Long.parseLong(req.getParameter("complexId"));
        String startStr = req.getParameter("startTime");
        String endStr = req.getParameter("endTime");
        String staffIdStr = req.getParameter("staffId");

        LocalTime startTime = parseTime(startStr);
        LocalTime endTime = parseTime(endStr);
        String baseName = determineShiftName(startTime, endTime);
        
        com.swp.dao.FootballComplexDAO complexDAO = new com.swp.dao.FootballComplexDAO();
        com.swp.model.FootballComplex complex = complexDAO.getFootballComplexDataByID(complexId);
        String suffix = "";
        if (complex != null) {
            suffix = " " + getComplexShortName(complex.getComplexName());
        }
        String shiftName = baseName + suffix;

        String mode = req.getParameter("mode");
        if ("batch".equals(mode)) {
            String startDateStr = req.getParameter("startDate");
            String endDateStr = req.getParameter("endDate");
            String repeatDaysStr = req.getParameter("repeatDays");

            if (startDateStr == null || endDateStr == null || repeatDaysStr == null) {
                writeError(resp, "Thiếu tham số cho chế độ tạo hàng loạt.");
                return;
            }

            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            if (endDate.isBefore(startDate)) {
                writeError(resp, "Ngày kết thúc không thể trước ngày bắt đầu.");
                return;
            }

            java.util.Set<Integer> repeatDays = new java.util.HashSet<>();
            for (String day : repeatDaysStr.split(",")) {
                try {
                    repeatDays.add(Integer.parseInt(day.trim()));
                } catch (NumberFormatException ignored) {
                }
            }

            int successCount = 0;
            int conflictCount = 0;

            LocalDate curDate = startDate;
            while (!curDate.isAfter(endDate)) {
                int dayValue = curDate.getDayOfWeek().getValue();
                if (repeatDays.contains(dayValue)) {
                    boolean hasConflict = workShiftDAO.hasOverlappingShiftAtComplex(complexId, curDate, startTime, endTime, null);
                    long staffId = -1;
                    if (!hasConflict && staffIdStr != null && !staffIdStr.trim().isEmpty()) {
                        try {
                            staffId = Long.parseLong(staffIdStr.trim());
                            if (workShiftDAO.hasOverlappingShift(staffId, curDate, startTime, endTime, null)) {
                                hasConflict = true;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    if (!hasConflict) {
                        WorkShift shift = new WorkShift(null, complexId, shiftName, curDate, startTime, endTime, null);
                        long shiftId = workShiftDAO.insertShift(shift);
                        if (shiftId > 0) {
                            if (staffId > 0) {
                                workShiftDAO.assignStaffToShift(shiftId, staffId);
                                notifyStaffAssigned(staffId, shiftName, curDate, shiftId);
                            }
                            successCount++;
                        } else {
                            conflictCount++;
                        }
                    } else {
                        conflictCount++;
                    }
                }
                curDate = curDate.plusDays(1);
            }

            if (successCount > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Đã tạo thành công " + successCount + " ca trực. (Bỏ qua " + conflictCount + " ca trùng lịch hoặc lỗi)");
                writeJson(resp, result);
            } else {
                writeError(resp, "Không thể tạo ca trực nào. Tất cả các ngày lặp đều bị trùng lịch hoặc lỗi.");
            }
            return;
        }

        // Single mode
        String dateStr = req.getParameter("shiftDate");
        LocalDate shiftDate = LocalDate.parse(dateStr);

        if (workShiftDAO.hasOverlappingShiftAtComplex(complexId, shiftDate, startTime, endTime, null)) {
            writeError(resp, "Cơ sở này đã được phân ca trực trùng khung giờ này trong ngày.");
            return;
        }

        if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
            try {
                long staffId = Long.parseLong(staffIdStr.trim());
                if (workShiftDAO.hasOverlappingShift(staffId, shiftDate, startTime, endTime, null)) {
                    writeError(resp, "Nhân viên này đã được phân công một ca làm việc khác trùng khung giờ này trong ngày.");
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        WorkShift shift = new WorkShift(null, complexId, shiftName, shiftDate, startTime, endTime, null);
        long shiftId = workShiftDAO.insertShift(shift);

        if (shiftId > 0) {
            if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
                try {
                    long staffId = Long.parseLong(staffIdStr.trim());
                    workShiftDAO.assignStaffToShift(shiftId, staffId);
                    notifyStaffAssigned(staffId, shiftName, shiftDate, shiftId);
                } catch (NumberFormatException ignored) {
                }
            }
            writeSuccess(resp);
        } else {
            writeError(resp, "Không thể tạo ca làm việc.");
        }
    }

    private boolean isShiftPast(long shiftId) {
        WorkShift ws = workShiftDAO.getShiftById(shiftId);
        if (ws != null && ws.getShiftDate() != null && ws.getEndTime() != null) {
            LocalDateTime shiftEnd;
            if (ws.getStartTime() != null && ws.getEndTime().isBefore(ws.getStartTime())) {
                shiftEnd = LocalDateTime.of(ws.getShiftDate().plusDays(1), ws.getEndTime());
            } else {
                shiftEnd = LocalDateTime.of(ws.getShiftDate(), ws.getEndTime());
            }
            return java.time.LocalDateTime.now().isAfter(shiftEnd);
        }
        return false;
    }

    private void handleEditShift(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long shiftId = Long.parseLong(req.getParameter("shiftId"));
        if (isShiftPast(shiftId)) {
            writeError(resp, "Ca trực này đã kết thúc, không thể chỉnh sửa.");
            return;
        }

        long complexId = Long.parseLong(req.getParameter("complexId"));
        String dateStr = req.getParameter("shiftDate");
        String startStr = req.getParameter("startTime");
        String endStr = req.getParameter("endTime");
        String staffIdStr = req.getParameter("staffId");

        LocalDate shiftDate = LocalDate.parse(dateStr);
        LocalTime startTime = parseTime(startStr);
        LocalTime endTime = parseTime(endStr);
        String baseName = determineShiftName(startTime, endTime);
        com.swp.dao.FootballComplexDAO complexDAO = new com.swp.dao.FootballComplexDAO();
        com.swp.model.FootballComplex complex = complexDAO.getFootballComplexDataByID(complexId);
        String suffix = "";
        if (complex != null) {
            suffix = " " + getComplexShortName(complex.getComplexName());
        }
        String shiftName = baseName + suffix;

        if (workShiftDAO.hasOverlappingShiftAtComplex(complexId, shiftDate, startTime, endTime, shiftId)) {
            writeError(resp, "Cơ sở này đã được phân ca trực trùng khung giờ này trong ngày.");
            return;
        }

        if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
            try {
                long staffId = Long.parseLong(staffIdStr.trim());
                if (workShiftDAO.hasOverlappingShift(staffId, shiftDate, startTime, endTime, shiftId)) {
                    writeError(resp, "Nhân viên này đã được phân công một ca làm việc khác trùng khung giờ này trong ngày.");
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        WorkShift shift = new WorkShift(shiftId, complexId, shiftName, shiftDate, startTime, endTime, null);
        boolean success = workShiftDAO.updateShift(shift);

        if (success) {
            List<User> assigned = workShiftDAO.getStaffAssignedToShift(shiftId);
            if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
                try {
                    long staffId = Long.parseLong(staffIdStr.trim());
                    boolean alreadyAssigned = false;
                    for (User u : assigned) {
                        if (u.getUserId() == staffId) {
                            alreadyAssigned = true;
                        } else {
                            workShiftDAO.removeStaffFromShift(shiftId, u.getUserId());
                        }
                    }
                    if (!alreadyAssigned) {
                        workShiftDAO.assignStaffToShift(shiftId, staffId);
                        notifyStaffAssigned(staffId, shiftName, shiftDate, shiftId);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else {
                for (User u : assigned) {
                    workShiftDAO.removeStaffFromShift(shiftId, u.getUserId());
                }
            }
            writeSuccess(resp);
        } else {
            writeError(resp, "Không thể cập nhật ca làm việc.");
        }
    }

    private void handleDeleteShift(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long shiftId = Long.parseLong(req.getParameter("shiftId"));
        if (isShiftPast(shiftId)) {
            writeError(resp, "Ca trực này đã kết thúc, không thể chỉnh sửa.");
            return;
        }

        boolean success = workShiftDAO.deleteShift(shiftId);

        if (success) {
            writeSuccess(resp);
        } else {
            writeError(resp, "Không thể xóa ca làm việc.");
        }
    }

    private void handleDeleteBatchShift(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String shiftIdsStr = req.getParameter("shiftIds");
        if (shiftIdsStr == null || shiftIdsStr.trim().isEmpty()) {
            writeError(resp, "Không nhận được danh sách ca trực cần xóa.");
            return;
        }

        String[] ids = shiftIdsStr.split(",");
        int successCount = 0;
        int skippedCount = 0;

        for (String idStr : ids) {
            try {
                long shiftId = Long.parseLong(idStr.trim());
                if (!isShiftPast(shiftId)) {
                    boolean success = workShiftDAO.deleteShift(shiftId);
                    if (success) {
                        successCount++;
                    } else {
                        skippedCount++;
                    }
                } else {
                    skippedCount++;
                }
            } catch (NumberFormatException ignored) {
                skippedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Đã xóa thành công " + successCount + " ca trực. (Bỏ qua " + skippedCount + " ca quá khứ hoặc lỗi)");
        writeJson(resp, result);
    }

    private void handleAssignStaff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long shiftId = Long.parseLong(req.getParameter("shiftId"));
        long staffId = Long.parseLong(req.getParameter("staffId"));

        WorkShift ws = workShiftDAO.getShiftById(shiftId);
        if (ws == null) {
            writeError(resp, "Không tìm thấy thông tin ca trực.");
            return;
        }

        if (ws.getShiftDate() != null && ws.getEndTime() != null) {
            LocalDateTime shiftEnd;
            if (ws.getStartTime() != null && ws.getEndTime().isBefore(ws.getStartTime())) {
                shiftEnd = LocalDateTime.of(ws.getShiftDate().plusDays(1), ws.getEndTime());
            } else {
                shiftEnd = LocalDateTime.of(ws.getShiftDate(), ws.getEndTime());
            }
            if (java.time.LocalDateTime.now().isAfter(shiftEnd)) {
                writeError(resp, "Ca trực này đã kết thúc, không thể chỉnh sửa.");
                return;
            }
        }

        if (workShiftDAO.hasOverlappingShift(staffId, ws.getShiftDate(), ws.getStartTime(), ws.getEndTime(), shiftId)) {
            writeError(resp, "Nhân viên này đã được phân công một ca làm việc khác trùng khung giờ này trong ngày.");
            return;
        }

        boolean success = workShiftDAO.assignStaffToShift(shiftId, staffId);

        if (success) {
            notifyStaffAssigned(staffId, ws.getShiftName(), ws.getShiftDate(), shiftId);
            writeSuccess(resp);
        } else {
            writeError(resp, "Không thể phân công nhân viên.");
        }
    }

    private void handleUnassignStaff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long shiftId = Long.parseLong(req.getParameter("shiftId"));
        long staffId = Long.parseLong(req.getParameter("staffId"));
        if (isShiftPast(shiftId)) {
            writeError(resp, "Ca trực này đã kết thúc, không thể chỉnh sửa.");
            return;
        }

        boolean success = workShiftDAO.removeStaffFromShift(shiftId, staffId);

        if (success) {
            writeSuccess(resp);
        } else {
            writeError(resp, "Không thể hủy phân công nhân viên.");
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        timeStr = timeStr.trim().toUpperCase();

        boolean pm = timeStr.contains("CH") || timeStr.contains("PM");
        boolean am = timeStr.contains("SA") || timeStr.contains("AM");

        if (timeStr.contains(" ")) {
            String[] parts = timeStr.split(" ");
            for (String part : parts) {
                if (part.contains(":")) {
                    timeStr = part;
                    break;
                }
            }
        }
        if (timeStr.contains(".")) {
            timeStr = timeStr.split("\\.")[0];
        }
        String clean = timeStr.replaceAll("[^0-9:]", "").trim();
        if (clean.isEmpty()) return null;

        String[] parts = clean.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        if (pm) {
            if (hour < 12) hour += 12;
        } else if (am) {
            if (hour == 12) hour = 0;
        }

        return LocalTime.of(hour, min, sec);
    }

    private void writeJson(HttpServletResponse resp, Object obj) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print(gson.toJson(obj));
        out.flush();
    }

    private void writeSuccess(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":true}");
        out.flush();
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":false,\"error\":\"" + escapeJson(message) + "\"}");
        out.flush();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

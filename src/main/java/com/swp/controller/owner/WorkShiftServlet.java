package com.swp.controller.owner;

import com.google.gson.Gson;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.WorkShiftDAO;
import com.swp.dao.NotificationDAO;
import com.swp.model.FootballComplex;
import com.swp.model.Notification;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WorkShiftServlet - Servlet điều hướng & quản lý toàn bộ các tính năng Quản lý ca trực
 * dành cho Chủ cơ sở (Owner) tại đường dẫn /owner/work-shift.
 */
@WebServlet("/owner/work-shift")
public class WorkShiftServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    private final WorkShiftDAO workShiftDAO = new WorkShiftDAO();
    private final FootballComplexDAO footballComplexDAO = new FootballComplexDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final Gson gson = new Gson();

    /**
     * Gửi thông báo hệ thống đến nhân viên khi được phân công ca trực mới.
     */
    private void notifyStaffAssigned(long staffId, String shiftName, LocalDate shiftDate, long shiftId) {
        Notification notif = new Notification();
        notif.setUserId(staffId);
        notif.setTitle("Phân công ca trực");
        notif.setMessage("Bạn đã được phân công ca trực: " + shiftName + " vào ngày " + shiftDate.format(DATE_FORMATTER));
        notif.setNotificationType("SYSTEM");
        notif.setReferenceId(shiftId);
        notif.setIsRead(false);
        notif.setCreatedAt(LocalDateTime.now());
        notificationDAO.insertNotification(notif);
    }

    /**
     * Xác định trạng thái ca làm việc phục vụ việc sắp xếp danh sách ca:
     * @return 0: Đang diễn ra (ONGOING), 1: Sắp diễn ra (FUTURE), 2: Đã kết thúc (PAST)
     */
    private int getShiftStatus(WorkShift s, LocalDateTime nowTime) {
        LocalDateTime start = LocalDateTime.of(s.getShiftDate(), s.getStartTime());
        LocalDateTime end = s.getEndTime().isBefore(s.getStartTime())
                ? LocalDateTime.of(s.getShiftDate().plusDays(1), s.getEndTime())
                : LocalDateTime.of(s.getShiftDate(), s.getEndTime());

        if (nowTime.isBefore(start)) {
            return 1; // FUTURE
        } else if (nowTime.isAfter(end)) {
            return 2; // PAST
        } else {
            return 0; // ONGOING
        }
    }

    /**
     * Kiểm tra một ca làm việc đã kết thúc hay chưa.
     */
    private boolean isShiftPast(long shiftId) {
        WorkShift ws = workShiftDAO.getShiftById(shiftId);
        if (ws != null && ws.getShiftDate() != null && ws.getEndTime() != null) {
            LocalDateTime shiftEnd = ws.getEndTime().isBefore(ws.getStartTime())
                    ? LocalDateTime.of(ws.getShiftDate().plusDays(1), ws.getEndTime())
                    : LocalDateTime.of(ws.getShiftDate(), ws.getEndTime());
            return LocalDateTime.now().isAfter(shiftEnd);
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        // Kiểm tra quyền Đăng nhập & Vai trò OWNER
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"OWNER".equalsIgnoreCase(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        // Xử lý AJAX request lấy danh sách nhân viên phân công của một ca
        String action = req.getParameter("action");
        if ("getAssignments".equals(action)) {
            resp.setContentType("application/json;charset=UTF-8");
            try {
                long shiftId = Long.parseLong(req.getParameter("shiftId"));
                List<User> assignedStaff = workShiftDAO.getStaffAssignedToShift(shiftId);
                List<Map<String, Object>> staffMaps = new ArrayList<>();
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

        // Tải dữ liệu cho trang Quản lý ca trực
        List<FootballComplex> complexes = footballComplexDAO.getAllActiveComplex();
        List<User> staffList = workShiftDAO.getAllActiveStaff();
        List<WorkShift> shifts = workShiftDAO.getAllShifts();

        // Sắp xếp danh sách ca làm việc: Ca đang diễn ra -> Ca sắp diễn ra -> Ca đã kết thúc
        if (shifts != null) {
            LocalDateTime nowTime = LocalDateTime.now();
            shifts.sort((s1, s2) -> {
                int status1 = getShiftStatus(s1, nowTime);
                int status2 = getShiftStatus(s2, nowTime);

                if (status1 != status2) {
                    return Integer.compare(status1, status2);
                }

                LocalDateTime start1 = LocalDateTime.of(s1.getShiftDate(), s1.getStartTime());
                LocalDateTime start2 = LocalDateTime.of(s2.getShiftDate(), s2.getStartTime());

                if (status1 == 0) { // ONGOING: Ca bắt đầu gần nhất lên trước
                    long diff1 = Math.abs(java.time.Duration.between(nowTime, start1).toSeconds());
                    long diff2 = Math.abs(java.time.Duration.between(nowTime, start2).toSeconds());
                    return Long.compare(diff1, diff2);
                } else if (status1 == 1) { // FUTURE: Theo thứ tự thời gian tăng dần
                    return start1.compareTo(start2);
                } else { // PAST: Theo thứ tự thời gian giảm dần (mới nhất lên trước)
                    LocalDateTime end1 = s1.getEndTime().isBefore(s1.getStartTime()) ? LocalDateTime.of(s1.getShiftDate().plusDays(1), s1.getEndTime()) : LocalDateTime.of(s1.getShiftDate(), s1.getEndTime());
                    LocalDateTime end2 = s2.getEndTime().isBefore(s2.getStartTime()) ? LocalDateTime.of(s2.getShiftDate().plusDays(1), s2.getEndTime()) : LocalDateTime.of(s2.getShiftDate(), s2.getEndTime());
                    return end2.compareTo(end1);
                }
            });
        }

        // Đếm số lượng ca trực của từng nhân viên để hiển thị trên Dashboard nhân sự
        Map<Long, Integer> staffShiftCounts = new HashMap<>();
        for (User staff : staffList) {
            staffShiftCounts.put(staff.getUserId(), 0);
        }
        if (shifts != null) {
            for (WorkShift ws : shifts) {
                List<User> assigned = workShiftDAO.getStaffAssignedToShift(ws.getShiftId());
                for (User u : assigned) {
                    staffShiftCounts.put(u.getUserId(), staffShiftCounts.getOrDefault(u.getUserId(), 0) + 1);
                }
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
        if (!"OWNER".equalsIgnoreCase(user.getRoleName())) {
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

    /**
     * Tự động xác định tên chuẩn cho ca trực dựa trên giờ bắt đầu và giờ kết thúc.
     */
    private String determineShiftName(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return "Ca gãy";
        }
        // Ca đêm: Giờ kết thúc nhỏ hơn giờ bắt đầu (xuyên đêm qua 00:00) HOẶC bắt đầu từ 22:00 trở đi HOẶC bắt đầu trước 06:00 sáng
        if (end.isBefore(start) || start.isAfter(LocalTime.of(21, 59)) || start.isBefore(LocalTime.of(6, 0))) {
            return "Ca đêm";
        }
        if (start.equals(LocalTime.of(8, 0)) && end.equals(LocalTime.of(12, 0))) {
            return "Ca sáng";
        }
        if (start.equals(LocalTime.of(12, 0)) && end.equals(LocalTime.of(18, 0))) {
            return "Ca chiều";
        }
        if (start.equals(LocalTime.of(18, 0)) && end.equals(LocalTime.of(22, 0))) {
            return "Ca tối";
        }
        return "Ca gãy";
    }

    /**
     * Trích xuất tên ngắn gọn của cơ sở sân bóng.
     */
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

    /**
     * Xử lý Tạo mới ca làm việc (Hỗ trợ Tạo đơn lẻ & Tạo hàng loạt theo dải ngày).
     */
    private void handleCreateShift(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long complexId = Long.parseLong(req.getParameter("complexId"));
        String startStr = req.getParameter("startTime");
        String endStr = req.getParameter("endTime");

        LocalTime startTime = parseTime(startStr);
        LocalTime endTime = parseTime(endStr);
        String baseName = determineShiftName(startTime, endTime);
        
        FootballComplex complex = footballComplexDAO.getFootballComplexDataByID(complexId);
        String suffix = (complex != null) ? " " + getComplexShortName(complex.getComplexName()) : "";
        String shiftName = baseName + suffix;

        String staffIdsParam = req.getParameter("staffIds");
        List<Long> selectedStaffIds = new ArrayList<>();
        if (staffIdsParam != null && !staffIdsParam.trim().isEmpty()) {
            for (String s : staffIdsParam.split(",")) {
                try {
                    if (!s.trim().isEmpty()) selectedStaffIds.add(Long.parseLong(s.trim()));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            String staffIdStr = req.getParameter("staffId");
            if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
                try { selectedStaffIds.add(Long.parseLong(staffIdStr.trim())); } catch (NumberFormatException ignored) {}
            }
        }

        String mode = req.getParameter("mode");
        
        // Xử lý tạo hàng loạt (Batch creation)
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

            Set<Integer> repeatDays = new HashSet<>();
            for (String day : repeatDaysStr.split(",")) {
                try {
                    repeatDays.add(Integer.parseInt(day.trim()));
                } catch (NumberFormatException ignored) {}
            }

            int successCount = 0;
            int conflictCount = 0;

            LocalDate curDate = startDate;
            while (!curDate.isAfter(endDate)) {
                int dayValue = curDate.getDayOfWeek().getValue();
                if (repeatDays.contains(dayValue)) {
                    boolean hasConflict = workShiftDAO.hasOverlappingShiftAtComplex(complexId, curDate, startTime, endTime, null);
                    if (!hasConflict) {
                        for (Long sId : selectedStaffIds) {
                            if (workShiftDAO.hasOverlappingShift(sId, curDate, startTime, endTime, null)) {
                                hasConflict = true;
                                break;
                            }
                        }
                    }

                    if (!hasConflict) {
                        WorkShift shift = new WorkShift(null, complexId, shiftName, curDate, startTime, endTime, null);
                        long shiftId = workShiftDAO.insertShift(shift);
                        if (shiftId > 0) {
                            for (Long sId : selectedStaffIds) {
                                workShiftDAO.assignStaffToShift(shiftId, sId);
                                notifyStaffAssigned(sId, shiftName, curDate, shiftId);
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

        // Xử lý tạo ca đơn lẻ (Single creation)
        String dateStr = req.getParameter("shiftDate");
        LocalDate shiftDate = LocalDate.parse(dateStr);

        // Kiểm tra cơ sở này đã có ca trực trùng khung giờ trong ngày hay chưa
        if (workShiftDAO.hasOverlappingShiftAtComplex(complexId, shiftDate, startTime, endTime, null)) {
            writeError(resp, "Cụm sân này đã được phân ca trực trùng khung giờ này trong ngày.");
            return;
        }

        for (Long sId : selectedStaffIds) {
            if (workShiftDAO.hasOverlappingShift(sId, shiftDate, startTime, endTime, null)) {
                User staffUser = workShiftDAO.getStaffById(sId);
                String name = staffUser != null ? staffUser.getFullName() : ("ID " + sId);
                writeError(resp, "Nhân viên " + name + " đã được phân công ca làm việc khác trùng khung giờ này trong ngày.");
                return;
            }
        }

        WorkShift shift = new WorkShift(null, complexId, shiftName, shiftDate, startTime, endTime, null);
        long shiftId = workShiftDAO.insertShift(shift);

        if (shiftId > 0) {
            for (Long sId : selectedStaffIds) {
                workShiftDAO.assignStaffToShift(shiftId, sId);
                notifyStaffAssigned(sId, shiftName, shiftDate, shiftId);
            }
            writeSuccess(resp);
        } else {
            writeError(resp, "Không thể tạo ca làm việc.");
        }
    }

    /**
     * Xử lý Chỉnh sửa ca làm việc (Hỗ trợ phân công nhiều nhân viên cùng lúc).
     */
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

        LocalDate shiftDate = LocalDate.parse(dateStr);
        LocalTime startTime = parseTime(startStr);
        LocalTime endTime = parseTime(endStr);
        String baseName = determineShiftName(startTime, endTime);
        
        FootballComplex complex = footballComplexDAO.getFootballComplexDataByID(complexId);
        String suffix = (complex != null) ? " " + getComplexShortName(complex.getComplexName()) : "";
        String shiftName = baseName + suffix;

        String staffIdsParam = req.getParameter("staffIds");
        List<Long> selectedStaffIds = new ArrayList<>();
        if (staffIdsParam != null && !staffIdsParam.trim().isEmpty()) {
            for (String s : staffIdsParam.split(",")) {
                try {
                    if (!s.trim().isEmpty()) selectedStaffIds.add(Long.parseLong(s.trim()));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            String staffIdStr = req.getParameter("staffId");
            if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
                try { selectedStaffIds.add(Long.parseLong(staffIdStr.trim())); } catch (NumberFormatException ignored) {}
            }
        }

        // Kiểm tra cơ sở này đã có ca làm việc khác trùng khung giờ trong ngày hay chưa (Bỏ qua chính ca trực đang chỉnh sửa)
        if (workShiftDAO.hasOverlappingShiftAtComplex(complexId, shiftDate, startTime, endTime, shiftId)) {
            writeError(resp, "Cụm sân này đã được phân ca trực trùng khung giờ này trong ngày.");
            return;
        }

        // Kiểm tra trùng lịch làm việc cho các nhân viên được chọn (Bỏ qua ca trực hiện tại)
        for (Long sId : selectedStaffIds) {
            if (workShiftDAO.hasOverlappingShift(sId, shiftDate, startTime, endTime, shiftId)) {
                User staffUser = workShiftDAO.getStaffById(sId);
                String name = staffUser != null ? staffUser.getFullName() : ("ID " + sId);
                writeError(resp, "Nhân viên " + name + " đã được phân công một ca làm việc khác trùng khung giờ này trong ngày.");
                return;
            }
        }

        WorkShift shift = new WorkShift(shiftId, complexId, shiftName, shiftDate, startTime, endTime, null);
        boolean success = workShiftDAO.updateShift(shift);

        if (success) {
            List<User> currentlyAssigned = workShiftDAO.getStaffAssignedToShift(shiftId);
            Set<Long> currentIds = new HashSet<>();
            for (User u : currentlyAssigned) {
                currentIds.add(u.getUserId());
            }

            Set<Long> targetIds = new HashSet<>(selectedStaffIds);

            // Bỏ phân công các nhân viên không còn được tích chọn
            for (Long curId : currentIds) {
                if (!targetIds.contains(curId)) {
                    workShiftDAO.removeStaffFromShift(shiftId, curId);
                }
            }

            // Thêm phân công cho các nhân viên mới được tích chọn
            for (Long targetId : targetIds) {
                if (!currentIds.contains(targetId)) {
                    workShiftDAO.assignStaffToShift(shiftId, targetId);
                    notifyStaffAssigned(targetId, shiftName, shiftDate, shiftId);
                }
            }

            // Dọn dẹp ca trực rác trùng lặp khung giờ cùng cơ sở
            workShiftDAO.cleanDuplicateShifts(complexId, shiftDate, startTime, endTime, shiftId);

            writeSuccess(resp);
        } else {
            writeError(resp, "Không thể cập nhật ca làm việc.");
        }
    }

    /**
     * Xử lý Xóa 1 ca làm việc.
     */
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

    /**
     * Xử lý Xóa nhiều ca làm việc hàng loạt.
     */
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

    /**
     * Xử lý Phân công nhanh 1 nhân viên vào ca.
     */
    private void handleAssignStaff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long shiftId = Long.parseLong(req.getParameter("shiftId"));
        long staffId = Long.parseLong(req.getParameter("staffId"));

        WorkShift ws = workShiftDAO.getShiftById(shiftId);
        if (ws == null) {
            writeError(resp, "Không tìm thấy thông tin ca trực.");
            return;
        }

        if (isShiftPast(shiftId)) {
            writeError(resp, "Ca trực này đã kết thúc, không thể chỉnh sửa.");
            return;
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

    /**
     * Xử lý Hủy phân công 1 nhân viên khỏi ca.
     */
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

    /**
     * Parse chuỗi thời gian linh hoạt (Ví dụ: "08:00", "08:00:00", "5:00 CH", "17:00:00").
     */
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

    /**
     * Gửi phản hồi JSON về phía client.
     */
    private void writeJson(HttpServletResponse resp, Object obj) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print(gson.toJson(obj));
        out.flush();
    }

    /**
     * Gửi phản hồi thành công { "success": true }.
     */
    private void writeSuccess(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":true}");
        out.flush();
    }

    /**
     * Gửi phản hồi lỗi { "success": false, "error": "..." }.
     */
    private void writeError(HttpServletResponse resp, String message) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":false,\"error\":\"" + escapeJson(message) + "\"}");
        out.flush();
    }

    /**
     * Chuẩn hóa chuỗi JSON để tránh lỗi ký tự đặc biệt.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

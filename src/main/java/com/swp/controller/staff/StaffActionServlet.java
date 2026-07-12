package com.swp.controller.staff;

import com.swp.dao.StaffDashboardDAO;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet({"/api/staff/checkin", "/api/staff/checkin/search", "/api/staff/field/update-status"})
public class StaffActionServlet extends HttpServlet {

    private final StaffDashboardDAO staffDAO = new StaffDashboardDAO();
    private static final int ROLE_STAFF = 3;
    private static final int ROLE_OWNER = 2;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        User user = getSessionUser(req);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            write(resp, "{\"error\":\"Chưa đăng nhập\"}");
            return;
        }
        if (!isStaffOrOwner(user)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            write(resp, "{\"error\":\"Không có quyền truy cập\"}");
            return;
        }

        String path = getPath(req);
        long staffId = user.getUserId();

        // Enforce shift time check for STAFF role (ROLE_STAFF = 3)
        if (user.getRoleId() == ROLE_STAFF) {
            java.util.Map<String, Object> shift = staffDAO.getCurrentShift(staffId);
            if (shift.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, "{\"error\":\"Bạn không có ca làm việc hôm nay để thực hiện thao tác.\"}");
                return;
            }
            
            String startStr = (String) shift.get("startTime");
            String endStr = (String) shift.get("endTime");
            java.time.LocalTime start = parseTime(startStr);
            java.time.LocalTime end = parseTime(endStr);
            java.time.LocalTime now = java.time.LocalTime.now();
            
            if (now.isBefore(start)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, "{\"error\":\"Ca trực của bạn chưa bắt đầu (Ca làm việc: " + startStr.substring(0,5) + " - " + endStr.substring(0,5) + "). Bạn không thể thực hiện thao tác này.\"}");
                return;
            }
            if (now.isAfter(end)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, "{\"error\":\"Ca trực của bạn đã kết thúc. Bạn không thể thực hiện thao tác này.\"}");
                return;
            }
        }

        try {
            if (path.startsWith("/api/staff/checkin/search")) {
                String query = req.getParameter("query");
                if (query == null) {
                    query = "";
                }
                query = query.trim();
                if (query.startsWith("#")) {
                    query = query.substring(1).trim();
                }

                java.util.Map<String, Object> shift = staffDAO.getCurrentShift(staffId);
                if (shift.isEmpty()) {
                    write(resp, "{\"error\":\"Bạn không có ca làm việc hôm nay để thực hiện tìm kiếm\"}");
                    return;
                }
                long complexId = (Long) shift.get("complexId");

                java.util.List<java.util.Map<String, Object>> list = staffDAO.searchConfirmedBookings(complexId, query);
                write(resp, toJson(list));
                return;
            }

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            write(resp, "{\"error\":\"Không tìm thấy API\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(resp, "{\"error\":\"Lỗi hệ thống: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        User user = getSessionUser(req);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            write(resp, "{\"error\":\"Chưa đăng nhập\"}");
            return;
        }
        if (!isStaffOrOwner(user)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            write(resp, "{\"error\":\"Không có quyền truy cập\"}");
            return;
        }

        String path = getPath(req);
        long staffId = user.getUserId();

        if (user.getRoleId() == ROLE_STAFF && !ensureActiveShift(resp, staffId)) {
            return;
        }

        try {
            if (path.startsWith("/api/staff/checkin")) {
                handleCheckin(req, resp, staffId);
                return;
            }

            if (path.startsWith("/api/staff/field/update-status")) {
                handleFieldStatusUpdate(req, resp);
                return;
            }

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            write(resp, "{\"error\":\"Không tìm thấy API\"}");
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Định dạng số không hợp lệ\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(resp, "{\"error\":\"Lỗi hệ thống: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleCheckin(HttpServletRequest req, HttpServletResponse resp, long staffId) throws IOException {
        String bookingIdStr = req.getParameter("bookingId");
        String note = req.getParameter("note");

        if (bookingIdStr == null || bookingIdStr.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Mã đặt sân không được bỏ trống\"}");
            return;
        }

        long bookingId = Long.parseLong(bookingIdStr.trim());
        boolean success = staffDAO.checkinBooking(bookingId, staffId, note);

        if (success) {
            write(resp, "{\"success\":true}");
        } else {
            write(resp, "{\"error\":\"Check-in không thành công. Lịch đặt có thể đã check-in hoặc hủy.\"}");
        }
    }

    private void handleFieldStatusUpdate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fieldIdStr = req.getParameter("fieldId");
        String status = req.getParameter("status");

        if (fieldIdStr == null || fieldIdStr.trim().isEmpty() || status == null || status.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Thiếu thông tin cập nhật\"}");
            return;
        }

        long fieldId = Long.parseLong(fieldIdStr.trim());
        boolean success = staffDAO.updateFieldStatus(fieldId, status.trim().toUpperCase());

        if (success) {
            write(resp, "{\"success\":true}");
        } else {
            write(resp, "{\"error\":\"Không thể cập nhật trạng thái sân.\"}");
        }
    }

    private boolean ensureActiveShift(HttpServletResponse resp, long staffId) throws IOException {
        java.util.Map<String, Object> shift = staffDAO.getCurrentShift(staffId);
        if (shift.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Bạn không có ca làm việc hôm nay để thực hiện thao tác.\"}");
            return false;
        }

        String startStr = (String) shift.get("startTime");
        String endStr = (String) shift.get("endTime");
        java.time.LocalTime start = parseTime(startStr);
        java.time.LocalTime end = parseTime(endStr);
        java.time.LocalTime now = java.time.LocalTime.now();

        if (now.isBefore(start)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Ca trực của bạn chưa bắt đầu. Bạn không thể thực hiện thao tác này.\"}");
            return false;
        }
        if (now.isAfter(end)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Ca trực của bạn đã kết thúc. Bạn không thể thực hiện thao tác này.\"}");
            return false;
        }
        return true;
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return (session != null) ? (User) session.getAttribute("user") : null;
    }

    private boolean isStaffOrOwner(User user) {
        return user.getRoleId() == ROLE_STAFF || user.getRoleId() == ROLE_OWNER;
    }

    private String getPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        return uri.substring(contextPath.length());
    }

    private void write(HttpServletResponse resp, String json) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Boolean b) return b.toString();
        if (obj instanceof Number n) return n.toString();
        if (obj instanceof String s) return "\"" + escapeJson(s) + "\"";
        if (obj instanceof java.util.List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(list.get(i)));
            }
            return sb.append(']').toString();
        }
        if (obj instanceof java.util.Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) map).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escapeJson(e.getKey().toString())).append('"')
                        .append(':').append(toJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static java.time.LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        timeStr = timeStr.trim().toUpperCase();

        boolean pm = timeStr.contains("CH") || timeStr.contains("PM");

        if (timeStr.contains(" ")) timeStr = timeStr.split(" ")[1];
        if (timeStr.contains(".")) timeStr = timeStr.split("\\.")[0];

        String clean = timeStr.replaceAll("[^0-9:]", "").trim();
        if (clean.isEmpty()) return null;

        String[] parts = clean.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        if (pm && hour < 12) {
            hour += 12;
        } else if (!pm && hour == 12) {
            hour = 0;
        }

        return java.time.LocalTime.of(hour, min, sec);
    }

}

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
import java.math.BigDecimal;

@WebServlet({"/api/staff/checkin", "/api/staff/checkin/search", "/api/staff/checkout", "/api/staff/field/update-status"})
public class StaffActionServlet extends HttpServlet {

    private final StaffDashboardDAO staffDAO = new StaffDashboardDAO();
    private static final int ROLE_STAFF = 3; // Staff role_id in DB is 3
    private static final int ROLE_OWNER = 2; // Owner role_id in DB is 2

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        // ── Auth Check ──────────────────────────────────────────────────────
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            write(resp, "{\"error\":\"Chưa đăng nhập\"}");
            return;
        }
        if (user.getRoleId() != ROLE_STAFF && user.getRoleId() != ROLE_OWNER) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            write(resp, "{\"error\":\"Không có quyền truy cập\"}");
            return;
        }

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());
        long staffId = user.getUserId();

        try {
            if (path.startsWith("/api/staff/checkin/search")) {
                String query = req.getParameter("query");
                if (query == null) query = "";
                query = query.trim();
                if (query.startsWith("#")) {
                    query = query.substring(1).trim();
                }

                java.util.Map<String, Object> shift = staffDAO.getCurrentShift(staffId);
                if (shift.isEmpty()) {
                    write(resp, "{\"error\":\"Bạn không có ca làm việc hôm nay để thực hiện tìm kiếm\"}");
                    return;
                }
                long facilityId = (Long) shift.get("facilityId");

                java.util.List<java.util.Map<String, Object>> list = staffDAO.searchConfirmedBookings(facilityId, query);
                write(resp, toJson(list));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                write(resp, "{\"error\":\"Không tìm thấy API\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(resp, "{\"error\":\"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        resp.setContentType("application/json;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        // ── Auth Check ──────────────────────────────────────────────────────
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            write(resp, "{\"error\":\"Chưa đăng nhập\"}");
            return;
        }
        if (user.getRoleId() != ROLE_STAFF && user.getRoleId() != ROLE_OWNER) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            write(resp, "{\"error\":\"Không có quyền truy cập\"}");
            return;
        }

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());
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
            if (path.startsWith("/api/staff/checkin")) {
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

            } else if (path.startsWith("/api/staff/checkout")) {
                String bookingIdStr = req.getParameter("bookingId");
                String subtotalStr = req.getParameter("subtotal");
                String discountStr = req.getParameter("discountAmount");
                String totalStr = req.getParameter("totalAmount");
                String paidStr = req.getParameter("paidAmount");
                String note = req.getParameter("note");

                if (bookingIdStr == null || bookingIdStr.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    write(resp, "{\"error\":\"Mã đặt sân không được bỏ trống\"}");
                    return;
                }

                long bookingId = Long.parseLong(bookingIdStr.trim());
                BigDecimal subtotal = toBigDecimal(subtotalStr);
                BigDecimal discountAmount = toBigDecimal(discountStr);
                BigDecimal totalAmount = toBigDecimal(totalStr);
                BigDecimal paidAmount = toBigDecimal(paidStr);

                boolean success = staffDAO.checkoutBooking(bookingId, staffId, subtotal, discountAmount, totalAmount, paidAmount, note);

                if (success) {
                    write(resp, "{\"success\":true}");
                } else {
                    write(resp, "{\"error\":\"Checkout không thành công. Vui lòng thử lại.\"}");
                }

            } else if (path.startsWith("/api/staff/field/update-status")) {
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
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Định dạng số không hợp lệ\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(resp, "{\"error\":\"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    private void write(HttpServletResponse resp, String json) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    private BigDecimal toBigDecimal(String val) {
        if (val == null || val.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            String cleaned = val.replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ── Lightweight JSON serialiser (no external library) ─────────────────────
    @SuppressWarnings("unchecked")
    private static String toJson(Object obj) {
        if (obj == null)                         return "null";
        if (obj instanceof Boolean b)            return b.toString();
        if (obj instanceof Number n)             return n.toString();
        if (obj instanceof String s)             return "\"" + escapeJson(s) + "\"";
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
        // Fallback for any other type
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
        
        boolean pm = false;
        if (timeStr.contains("CH") || timeStr.contains("PM")) {
            pm = true;
        }
        
        // Handle standard space delimiters
        if (timeStr.contains(" ")) timeStr = timeStr.split(" ")[1];
        if (timeStr.contains(".")) timeStr = timeStr.split("\\.")[0];
        
        String clean = timeStr.replaceAll("[^0-9:]", "").trim();
        if (clean.isEmpty()) return null;
        
        String[] parts = clean.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        
        if (pm) {
            if (hour < 12) hour += 12;
        } else {
            if (hour == 12) hour = 0;
        }
        
        return java.time.LocalTime.of(hour, min, sec);
    }
}

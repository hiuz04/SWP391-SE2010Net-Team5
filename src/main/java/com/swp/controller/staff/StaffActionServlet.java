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

@WebServlet({"/api/staff/checkin", "/api/staff/checkout", "/api/staff/field/update-status"})
public class StaffActionServlet extends HttpServlet {

    private final StaffDashboardDAO staffDAO = new StaffDashboardDAO();
    private static final int ROLE_STAFF = 3; // Staff role_id in DB is 3
    private static final int ROLE_OWNER = 2; // Owner role_id in DB is 2

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
}

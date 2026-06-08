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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@WebServlet({"/staff/dashboard", "/staff/schedule", "/staff/checkin", "/staff/checkout", "/staff/invoice"})
public class StaffPagesServlet extends HttpServlet {

    private final StaffDashboardDAO staffDAO = new StaffDashboardDAO();
    private static final int ROLE_STAFF = 3; // Staff role_id in DB is 3
    private static final int ROLE_OWNER = 2; // Owner role_id in DB is 2

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        // ── Auth Check ──────────────────────────────────────────────────────
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (user.getRoleId() != ROLE_STAFF && user.getRoleId() != ROLE_OWNER) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập.");
            return;
        }

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

        if (path.startsWith("/staff/dashboard")) {
            req.getRequestDispatcher("/WEB-INF/staff/dashboard.jsp").forward(req, resp);
            
        } else if (path.startsWith("/staff/schedule")) {
            String dateParam = req.getParameter("date");
            String dateStr = (dateParam != null && !dateParam.trim().isEmpty()) 
                    ? dateParam.trim() 
                    : LocalDate.now().toString();

            Map<String, Object> shift = staffDAO.getCurrentShift(user.getUserId());
            if (!shift.isEmpty()) {
                long facilityId = (Long) shift.get("facilityId");
                List<Map<String, Object>> fields = staffDAO.getFieldsForFacility(facilityId);
                List<Map<String, Object>> bookings = staffDAO.getBookingsForDate(facilityId, dateStr);

                req.setAttribute("facilityId", facilityId);
                req.setAttribute("facilityName", shift.get("facilityName"));
                req.setAttribute("fields", fields);
                req.setAttribute("bookings", bookings);
                req.setAttribute("hasShift", true);
            } else {
                req.setAttribute("hasShift", false);
            }
            req.setAttribute("selectedDate", dateStr);
            req.getRequestDispatcher("/WEB-INF/staff/schedule.jsp").forward(req, resp);

        } else if (path.startsWith("/staff/checkin")) {
            String bookingIdParam = req.getParameter("id");
            if (bookingIdParam != null && !bookingIdParam.isEmpty()) {
                try {
                    long bookingId = Long.parseLong(bookingIdParam);
                    Map<String, Object> booking = staffDAO.getBookingDetailForCheckout(bookingId);
                    req.setAttribute("booking", booking);
                } catch (NumberFormatException ignored) {}
            }
            req.getRequestDispatcher("/WEB-INF/staff/checkin.jsp").forward(req, resp);

        } else if (path.startsWith("/staff/checkout")) {
            String bookingIdParam = req.getParameter("id");
            if (bookingIdParam != null && !bookingIdParam.isEmpty()) {
                try {
                    long bookingId = Long.parseLong(bookingIdParam);
                    Map<String, Object> booking = staffDAO.getBookingDetailForCheckout(bookingId);
                    req.setAttribute("booking", booking);
                } catch (NumberFormatException ignored) {
                    req.setAttribute("error", "Mã đặt sân không hợp lệ.");
                }
            }
            req.getRequestDispatcher("/WEB-INF/staff/checkout.jsp").forward(req, resp);

        } else if (path.startsWith("/staff/invoice")) {
            String bookingIdParam = req.getParameter("id");
            if (bookingIdParam != null && !bookingIdParam.isEmpty()) {
                try {
                    long bookingId = Long.parseLong(bookingIdParam);
                    Map<String, Object> invoice = staffDAO.getInvoiceDetail(bookingId);
                    req.setAttribute("invoice", invoice);
                } catch (NumberFormatException ignored) {}
            }
            req.getRequestDispatcher("/WEB-INF/staff/invoice.jsp").forward(req, resp);
        }
    }
}

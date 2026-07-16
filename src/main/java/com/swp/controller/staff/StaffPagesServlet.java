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

@WebServlet({"/staff/dashboard", "/staff/schedule", "/staff/checkin"})
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
        User user = (User) session.getAttribute("user");

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

        // Redirect check-in requests if staff has no active shift
        if (path.startsWith("/staff/checkin")) {
            if (user.getRoleId() == ROLE_STAFF) {
                Map<String, Object> shift = staffDAO.getCurrentShift(user.getUserId());
                if (shift.isEmpty()) {
                    resp.sendRedirect(req.getContextPath() + "/staff/dashboard?error=not_in_shift");
                    return;
                }

                String startStr = (String) shift.get("startTime");
                String endStr = (String) shift.get("endTime");
                java.time.LocalTime start = parseTime(startStr);
                java.time.LocalTime end = parseTime(endStr);
                java.time.LocalTime now = java.time.LocalTime.now();

                if (now.isBefore(start) || now.isAfter(end)) {
                    resp.sendRedirect(req.getContextPath() + "/staff/dashboard?error=not_in_shift");
                    return;
                }
            }
        }

        if (path.startsWith("/staff/dashboard")) {
            req.getRequestDispatcher("/WEB-INF/staff/dashboard.jsp").forward(req, resp);
            
        } else if (path.startsWith("/staff/schedule")) {
            String dateParam = req.getParameter("date");
            String dateStr = (dateParam != null && !dateParam.trim().isEmpty()) 
                    ? dateParam.trim() 
                    : LocalDate.now().toString();

            Map<String, Object> shift = staffDAO.getCurrentShift(user.getUserId());
            if (!shift.isEmpty()) {
                long complexId = (Long) shift.get("complexId");
                List<Map<String, Object>> fields = staffDAO.getFieldsForComplex(complexId);
                List<Map<String, Object>> bookings = staffDAO.getBookingsForDate(complexId, dateStr);

                req.setAttribute("complexId", complexId);
                req.setAttribute("complexName", shift.get("complexName"));
                req.setAttribute("fields", fields);
                req.setAttribute("bookings", bookings);
                req.setAttribute("hasShift", true);
                req.setAttribute("shiftStartTime", shift.get("startTime"));
                req.setAttribute("shiftEndTime", shift.get("endTime"));
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
                    Map<String, Object> booking = staffDAO.getBookingDetailForCheckin(bookingId);
                    req.setAttribute("booking", booking);
                } catch (NumberFormatException ignored) {}
            }
            req.getRequestDispatcher("/WEB-INF/staff/checkin.jsp").forward(req, resp);
        }
    }

    private static java.time.LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        timeStr = timeStr.trim().toUpperCase();

        boolean pm = timeStr.contains("CH") || timeStr.contains("PM");

        if (timeStr.contains(" ")) {
            timeStr = timeStr.split(" ")[1];
        }
        if (timeStr.contains(".")) {
            timeStr = timeStr.split("\\.")[0];
        }

        String clean = timeStr.replaceAll("[^0-9:]", "").trim();
        if (clean.isEmpty()) {
            return null;
        }

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

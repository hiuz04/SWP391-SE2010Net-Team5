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
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

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

                if (start != null && end != null) {
                    boolean inShift;
                    if (end.isBefore(start)) {
                        inShift = !now.isBefore(start) || now.isBefore(end);
                    } else {
                        inShift = !now.isBefore(start) && !now.isAfter(end);
                    }
                    if (!inShift) {
                        resp.sendRedirect(req.getContextPath() + "/staff/dashboard?error=not_in_shift");
                        return;
                    }
                    req.setAttribute("shiftStartTime", startStr);
                    req.setAttribute("shiftEndTime", endStr);
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
            long complexId;
            String complexName;
            String shiftStart = "17:00:00";
            String shiftEnd = "23:59:59";

            if (!shift.isEmpty()) {
                complexId = (Long) shift.get("complexId");
                complexName = (String) shift.get("complexName");
                if (shift.get("startTime") != null) shiftStart = (String) shift.get("startTime");
                if (shift.get("endTime") != null) shiftEnd = (String) shift.get("endTime");
            } else {
                Map<String, Object> defComp = staffDAO.getDefaultComplex(user.getUserId());
                complexId = defComp.containsKey("complexId") ? (Long) defComp.get("complexId") : 1L;
                complexName = defComp.containsKey("complexName") ? (String) defComp.get("complexName") : "Sân bóng";
            }

            List<Map<String, Object>> fields = staffDAO.getFieldsForComplex(complexId);
            List<Map<String, Object>> bookings = staffDAO.getBookingsForDate(complexId, dateStr);

            if (bookings != null) {
                final String fShiftStart = shiftStart;
                final String fShiftEnd = shiftEnd;
                bookings.sort((b1, b2) -> {
                    if (b1 == null) return 1;
                    if (b2 == null) return -1;
                    String s1 = (String) b1.get("startTime");
                    String s2 = (String) b2.get("startTime");
                    boolean in1 = isTimeInShift(s1, fShiftStart, fShiftEnd);
                    boolean in2 = isTimeInShift(s2, fShiftStart, fShiftEnd);
                    if (in1 != in2) {
                        return in1 ? -1 : 1;
                    }
                    if (s1 == null) return 1;
                    if (s2 == null) return -1;
                    return s1.compareTo(s2);
                });
            }

            boolean hasShift = !shift.isEmpty();

            req.setAttribute("complexId", complexId);
            req.setAttribute("complexName", complexName);
            req.setAttribute("fields", fields);
            req.setAttribute("bookings", bookings);
            req.setAttribute("hasShift", hasShift);
            req.setAttribute("shiftStartTime", shiftStart);
            req.setAttribute("shiftEndTime", shiftEnd);
            req.setAttribute("selectedDate", dateStr);
            req.getRequestDispatcher("/WEB-INF/staff/schedule.jsp").forward(req, resp);

        } else if (path.startsWith("/staff/checkin")) {
            String bookingIdParam = req.getParameter("id");
            String pendingParam = req.getParameter("pending");

            if (bookingIdParam != null && !bookingIdParam.isEmpty()) {
                try {
                    long bookingId = Long.parseLong(bookingIdParam);
                    Map<String, Object> booking = staffDAO.getBookingDetailForCheckin(bookingId);
                    
                    // Verify facility match for security
                    if (user != null && user.getRoleId() == 3) { // Role Staff = 3
                        Map<String, Object> shift = staffDAO.getCurrentShift(user.getUserId());
                        if (!shift.isEmpty() && !booking.isEmpty()) {
                            long staffComplexId = (Long) shift.get("complexId");
                            Long bookingComplexId = (Long) booking.get("complexId");
                            if (bookingComplexId != null && bookingComplexId != staffComplexId) {
                                resp.sendRedirect(req.getContextPath() + "/staff/schedule?error=facility_mismatch");
                                return;
                            }
                        }
                    }
                    req.setAttribute("booking", booking);
                } catch (NumberFormatException ignored) {}
            } else if (pendingParam != null) {
                // If pending parameter is passed (from Quick Action), load pending check-in list for staff complex today
                try {
                    Map<String, Object> shift = staffDAO.getCurrentShift(user.getUserId());
                    if (!shift.isEmpty()) {
                        long complexId = (Long) shift.get("complexId");
                        List<Map<String, Object>> todayBookings = staffDAO.getTodayBookings(complexId);
                        List<Map<String, Object>> pendingCheckinBookings = new java.util.ArrayList<>();
                        for (Map<String, Object> b : todayBookings) {
                            if ("CONFIRMED".equals(b.get("status"))) {
                                pendingCheckinBookings.add(b);
                            }
                        }
                        req.setAttribute("pendingCheckinBookings", pendingCheckinBookings);
                    }
                } catch (Exception e) {
                    getServletContext().log("Cannot load pending checkin list", e);
                }
            }
            req.getRequestDispatcher("/WEB-INF/staff/checkin.jsp").forward(req, resp);
        }
    }

    private boolean isTimeInShift(String timeStr, String shiftStart, String shiftEnd) {
        java.time.LocalTime t = parseTime(timeStr);
        java.time.LocalTime s = parseTime(shiftStart);
        java.time.LocalTime e = parseTime(shiftEnd);
        if (t == null || s == null || e == null) return true;
        return !t.isBefore(s) && !t.isAfter(e);
    }

    private static java.time.LocalTime parseTime(String timeStr) {
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

        try {
            String[] parts = clean.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (pm) {
                if (hour < 12) hour += 12;
            } else if (am) {
                if (hour == 12) hour = 0;
            }

            if (hour >= 24) {
                return java.time.LocalTime.MAX;
            }

            return java.time.LocalTime.of(hour, min, sec);
        } catch (Exception e) {
            return null;
        }
    }

}

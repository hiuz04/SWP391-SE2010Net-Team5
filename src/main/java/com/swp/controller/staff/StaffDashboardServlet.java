package com.swp.controller.staff;

import com.swp.dao.StaffDashboardDAO;
import com.swp.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * REST endpoint: GET /api/staff/dashboard
 *
 * Returns a JSON object containing all data the Staff Dashboard page needs:
 *  - shift        : current shift info + progress %
 *  - kpi          : cash, booking counts, pending check-ins, avg rating
 *  - bookings     : list of today's bookings for the shift's complex
 *  - recentActivity: last 5 events (check-ins + invoices)
 *  - staffName    : display name of the logged-in staff
 *
 * Requires the user to be logged in as STAFF (role_id == 2).
 */
@WebServlet("/api/staff/dashboard")
public class StaffDashboardServlet extends HttpServlet {


    private final StaffDashboardDAO dao = new StaffDashboardDAO();

    // Role IDs – adjust if your DB uses different values
    private static final int ROLE_STAFF   = 3;
    private static final int ROLE_OWNER   = 2;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");  // dev-only; tighten in prod

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;


        long staffId = user.getUserId();

        try {
            // ── 1. Current shift ─────────────────────────────────────────────
            Map<String, Object> shift = dao.getCurrentShift(staffId);

            if (shift.isEmpty()) {
                // No shift today → return a lightweight response
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("hasShift",   false);
                result.put("staffName",  user.getFullName());
                write(resp, toJson(result));
                return;
            }

            // ── 2. Compute shift progress ────────────────────────────────────
            String startStr = (String) shift.get("startTime");   // HH:mm:ss or HH:mm
            String endStr   = (String) shift.get("endTime");
            LocalTime start = parseTime(startStr);
            LocalTime end   = parseTime(endStr);

            Object shiftDateObj = shift.get("shiftDate");
            LocalDate sDate = LocalDate.now();
            if (shiftDateObj != null) {
                try {
                    sDate = LocalDate.parse(shiftDateObj.toString().trim());
                } catch (Exception ignored) {}
            }

            LocalDateTime shiftStartDT = LocalDateTime.of(sDate, start);
            LocalDateTime shiftEndDT;
            if (end.isBefore(start)) {
                shiftEndDT = LocalDateTime.of(sDate.plusDays(1), end);
            } else {
                shiftEndDT = LocalDateTime.of(sDate, end);
            }
            LocalDateTime nowDT = LocalDateTime.now();

            double progressPct = 0.0;
            String remainStr   = "—";
            String shiftStatus = "ONGOING";

            if (nowDT.isBefore(shiftStartDT)) {
                progressPct = 0.0;
                remainStr   = formatDurationBetween(nowDT, shiftStartDT);
                shiftStatus = "UPCOMING";
            } else if (!nowDT.isBefore(shiftEndDT)) {
                progressPct = 100.0;
                remainStr   = "Đã kết thúc";
                shiftStatus = "COMPLETED";
            } else {
                long totalSec   = java.time.Duration.between(shiftStartDT, shiftEndDT).getSeconds();
                long elapsedSec = java.time.Duration.between(shiftStartDT, nowDT).getSeconds();
                progressPct     = totalSec <= 0 ? 0 : (elapsedSec * 100.0 / totalSec);
                remainStr       = formatDurationBetween(nowDT, shiftEndDT);
                shiftStatus     = "ONGOING";
            }
            shift.put("progressPct", Math.round(progressPct * 10) / 10.0);
            shift.put("remaining",   remainStr);
            shift.put("status",      shiftStatus);

            long complexId = (Long) shift.get("complexId");
            String dateStr  = LocalDate.now().toString();

            // ── 3. Cash & Total Revenue KPI ──────────────────────────────────
            Map<String, Object> cash = dao.getCashKpi(staffId, dateStr, startStr, endStr);
            Map<String, Object> totalRevenueKpi = dao.getTotalRevenueKpi(complexId, dateStr, startStr, endStr);

            // Compute average transaction for cash
            BigDecimal totalCash = toBigDecimal(cash.get("totalCash"));
            int txCount          = (Integer) cash.get("txCount");
            BigDecimal avgTx     = (txCount > 0)
                    ? totalCash.divide(BigDecimal.valueOf(txCount), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            cash.put("avgTransaction", avgTx);
            cash.put("transactions", dao.getCashTransactions(staffId, dateStr, startStr, endStr));
            totalRevenueKpi.put("transactions", dao.getAllTransactions(complexId, dateStr, startStr, endStr));

            // Estimate revenue target (cấu hình mục tiêu doanh thu tiền mặt ca trực - 1.400.000đ)
            BigDecimal target = BigDecimal.valueOf(1_400_000L);
            double cashPct = (target.compareTo(BigDecimal.ZERO) > 0)
                    ? totalCash.divide(target, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0;
            cash.put("targetAmount", target);
            cash.put("targetPct",    Math.min(Math.round(cashPct * 10) / 10.0, 100.0));

            // ── 4. Booking KPI ───────────────────────────────────────────────
            Map<String, Object> bookingKpi = dao.getBookingKpi(complexId);

            // ── 5. Pending check-ins ─────────────────────────────────────────
            int pending = dao.getPendingCheckinCount(complexId);

            // ── 6. Average rating ────────────────────────────────────────────
            Double avgRating = dao.getAverageRatingToday(complexId);

            // ── 7. Bookings list ─────────────────────────────────────────────
            List<Map<String, Object>> bookings = dao.getTodayBookings(complexId);

            // Attach "currentlyPlaying" flag based on server time
            LocalTime nowTime = nowDT.toLocalTime();
            for (Map<String, Object> b : bookings) {
                LocalTime bStart = parseTime((String) b.get("startTime"));
                LocalTime bEnd   = parseTime((String) b.get("endTime"));
                b.put("nowPlaying", !nowTime.isBefore(bStart) && nowTime.isBefore(bEnd));
            }

            // ── 8. Recent activity ───────────────────────────────────────────
            List<Map<String, Object>> activity = dao.getRecentActivity(complexId);

            // ── 9. Assemble response ─────────────────────────────────────────
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("hasShift",        true);
            payload.put("staffName",       user.getFullName());
            payload.put("shift",           shift);
            payload.put("cashKpi",         cash);
            payload.put("totalRevenueKpi", totalRevenueKpi);
            payload.put("bookingKpi",      bookingKpi);
            payload.put("pendingCheckin",  pending);
            payload.put("avgRating",       avgRating);
            payload.put("bookings",        bookings);
            payload.put("recentActivity",  activity);

            write(resp, toJson(payload));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(resp, error("Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void write(HttpServletResponse resp, String json) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    private String error(String msg) {
        return "{\"error\":\"" + escapeJson(msg) + "\"}";
    }

    // ── Lightweight JSON serialiser (no external library) ─────────────────────
    @SuppressWarnings("unchecked")
    private static String toJson(Object obj) {
        if (obj == null)                         return "null";
        if (obj instanceof Boolean b)            return b.toString();
        if (obj instanceof Number n)             return n.toString();
        if (obj instanceof String s)             return "\"" + escapeJson(s) + "\"";
        if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(list.get(i)));
            }
            return sb.append(']').toString();
        }
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) map).entrySet()) {
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

    private static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return LocalTime.MIDNIGHT;
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
        if (clean.isEmpty()) return LocalTime.MIDNIGHT;

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

    private static long toSeconds(LocalTime from, LocalTime to) {
        return to.toSecondOfDay() - from.toSecondOfDay();
    }

    private static String formatDurationBetween(LocalDateTime from, LocalDateTime to) {
        long secs = java.time.Duration.between(from, to).getSeconds();
        if (secs <= 0) return "0 phút";
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        if (h > 0 && m > 0) return h + "h " + m + "m";
        if (h > 0)           return h + "h";
        return m + " phút";
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n)     return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}

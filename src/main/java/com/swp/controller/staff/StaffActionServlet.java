package com.swp.controller.staff;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.swp.dao.StaffDashboardDAO;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@WebServlet({
        "/api/staff/checkin",
        "/api/staff/checkin/search",
        "/api/staff/checkin/qr-preview",
        "/api/staff/checkin/qr-decode",
        "/api/staff/field/update-status",
        "/api/staff/checkin/noshow"
})
public class StaffActionServlet extends HttpServlet {

    private final StaffDashboardDAO staffDAO = new StaffDashboardDAO();
    private static final int ROLE_STAFF = 3;
    private static final int ROLE_OWNER = 2;

    @Override
    /**
     * Xử lý các API GET của Staff như tìm kiếm booking đã xác nhận trong ca trực hiện tại.
     * Staff phải có ca hợp lệ trước khi lấy dữ liệu thao tác.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;


        String path = getPath(req);
        long staffId = user.getUserId();

        // Business Rule BR-12: Staff chỉ được thao tác khi có ca trực đang hoạt động trong ngày.
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
            if (path.startsWith("/api/staff/checkin/qr-preview")) {
                handleQrPreview(req, resp, user);
                return;
            }

            if (path.startsWith("/api/staff/checkin/search")) {
                String query = req.getParameter("query");
                String pendingOnly = req.getParameter("pendingOnly");

                java.util.Map<String, Object> shift = staffDAO.getCurrentShift(staffId);
                if (shift.isEmpty()) {
                    write(resp, "{\"error\":\"Bạn không có ca làm việc hôm nay để thực hiện tìm kiếm\"}");
                    return;
                }
                long complexId = (Long) shift.get("complexId");

                java.util.List<java.util.Map<String, Object>> list;
                if ("true".equalsIgnoreCase(pendingOnly)) {
                    list = staffDAO.getPendingCheckinBookings(complexId);
                } else {
                    if (query == null) {
                        query = "";
                    }
                    query = query.trim();
                    if (query.startsWith("#")) {
                        query = query.substring(1).trim();
                    }
                    list = staffDAO.searchConfirmedBookings(complexId, query);
                }
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
    /**
     * Xử lý API POST cho check-in và cập nhật trạng thái sân.
     * Các request của Staff được kiểm tra ca trực trước khi đi vào handler cụ thể.
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;


        String path = getPath(req);
        long staffId = user.getUserId();

        // Business Rule BR-12: Staff phải có ca đang hoạt động trước khi gọi API check-in/cập nhật sân.
        if (user.getRoleId() == ROLE_STAFF && !ensureActiveShift(resp, staffId)) {
            return;
        }

        try {
            if (path.equals("/api/staff/checkin/qr-decode")) {
                handleQrDecode(req, resp);
                return;
            }

            if (path.equals("/api/staff/checkin/noshow")) {
                handleCancelNoshow(req, resp, staffId);
                return;
            }

            if (path.equals("/api/staff/checkin")) {
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

    /**
     * Thực hiện check-in booking cho Staff đang trực.
     * Method kiểm tra bookingId, complex của ca trực và trả JSON theo kết quả cập nhật trong DAO.
     */
    private void handleQrPreview(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        String bookingCode = normalizeBookingCode(req.getParameter("bookingCode"));
        if (bookingCode.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Mã QR không chứa mã đặt sân hợp lệ.\"}");
            return;
        }
        if (bookingCode.length() > 80) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Mã đặt sân trong QR quá dài.\"}");
            return;
        }

        Long expectedComplexId = resolveStaffComplexId(user);
        if (user.getRoleId() == ROLE_STAFF && expectedComplexId == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Không xác định được cơ sở trong ca trực hiện tại.\"}");
            return;
        }

        Map<String, Object> booking = staffDAO.getBookingDetailForCheckinByCode(bookingCode);
        if (booking.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            write(resp, "{\"error\":\"Không tìm thấy booking tương ứng với QR.\"}");
            return;
        }

        String validationError = validateBookingAccess(booking, expectedComplexId);
        if (validationError != null) {
            resp.setStatus(isFacilityMismatch(validationError)
                    ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"" + escapeJson(validationError) + "\"}");
            return;
        }

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("booking", booking);
        write(resp, toJson(payload));
    }

    private void handleQrDecode(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String imageData = req.getParameter("imageData");
        if (imageData == null || imageData.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Thiếu dữ liệu ảnh từ camera.\"}");
            return;
        }

        try {
            int commaIndex = imageData.indexOf(',');
            String base64 = commaIndex >= 0 ? imageData.substring(commaIndex + 1) : imageData;
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, "{\"error\":\"Ảnh camera không hợp lệ.\"}");
                return;
            }

            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(image))
            );
            java.util.Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            Result result = new MultiFormatReader().decode(bitmap, hints);
            String bookingCode = normalizeBookingCode(result.getText());
            if (bookingCode.isEmpty()) {
                write(resp, "{\"found\":false}");
                return;
            }

            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("found", true);
            payload.put("bookingCode", bookingCode);
            write(resp, toJson(payload));
        } catch (ReaderException e) {
            write(resp, "{\"found\":false}");
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Dữ liệu ảnh QR không hợp lệ.\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(resp, "{\"error\":\"Không thể đọc QR từ camera: " + escapeJson(e.getMessage()) + "\"}");
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

        // Business Rule BR-12: Staff không được check-in booking thuộc cơ sở khác với ca trực hiện tại.
        // Security check: Verify that the staff's current shift facility matches the booking's facility
        User user = getSessionUser(req);
        java.util.Map<String, Object> booking = staffDAO.getBookingDetailForCheckin(bookingId);
        if (booking.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            write(resp, "{\"error\":\"Không tìm thấy lịch đặt sân.\"}");
            return;
        }

        Long expectedComplexId = user != null ? resolveStaffComplexId(user) : null;
        if (user != null && user.getRoleId() == ROLE_STAFF && expectedComplexId == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Không xác định được cơ sở trong ca trực hiện tại.\"}");
            return;
        }

        String validationError = validateBookingForCheckin(booking, expectedComplexId);
        if (validationError != null) {
            resp.setStatus(isFacilityMismatch(validationError)
                    ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"" + escapeJson(validationError) + "\"}");
            return;
        }

        // Business Rule BR-13: DAO chỉ check-in booking CONFIRMED và tạo bản ghi check-in khi cập nhật thành công.
        boolean success = staffDAO.checkinBooking(bookingId, staffId, note);

        if (success) {
            write(resp, "{\"success\":true}");
        } else {
            write(resp, "{\"error\":\"Check-in không thành công. Lịch đặt có thể đã check-in hoặc hủy.\"}");
        }
    }

    private void handleCancelNoshow(HttpServletRequest req, HttpServletResponse resp, long staffId) throws IOException {
        String bookingIdStr = req.getParameter("bookingId");
        if (bookingIdStr == null || bookingIdStr.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"Mã đặt sân không được bỏ trống\"}");
            return;
        }

        long bookingId = Long.parseLong(bookingIdStr.trim());
        try {
            com.swp.dao.StaffBillingDAO billingDAO = new com.swp.dao.StaffBillingDAO();
            boolean success = billingDAO.cancelLateNoShowBooking(bookingId, staffId, true);
            if (success) {
                write(resp, "{\"success\":true}");
            } else {
                write(resp, "{\"error\":\"Hủy no-show không thành công.\"}");
            }
        } catch (IllegalArgumentException | SecurityException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(resp, "{\"error\":\"Lỗi hệ thống khi hủy đặt sân: " + escapeJson(e.getMessage()) + "\"}");
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

    /**
     * Kiểm tra Staff có ca làm việc hiện tại và thời gian hệ thống đang nằm trong ca đó.
     * Trả false và ghi JSON lỗi nếu Staff chưa tới ca hoặc ca đã kết thúc.
     */
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

    private String validateBookingAccess(Map<String, Object> booking, Long expectedComplexId) {
        if (booking == null || booking.isEmpty()) {
            return "Không tìm thấy lịch đặt sân.";
        }

        if (expectedComplexId != null) {
            Long bookingComplexId = asLong(booking.get("complexId"));
            if (bookingComplexId == null || !bookingComplexId.equals(expectedComplexId)) {
                return "Lượt đặt sân này thuộc cơ sở khác. Bạn không thể thực hiện thao tác.";
            }
        }

        return null;
    }

    private Long resolveStaffComplexId(User user) {
        if (user == null || user.getRoleId() == null || user.getRoleId() != ROLE_STAFF) {
            return null;
        }

        java.util.Map<String, Object> shift = staffDAO.getCurrentShift(user.getUserId());
        if (shift.isEmpty()) {
            return null;
        }
        return asLong(shift.get("complexId"));
    }

    private String validateBookingForCheckin(Map<String, Object> booking, Long expectedComplexId) {
        String accessError = validateBookingAccess(booking, expectedComplexId);
        if (accessError != null) {
            return accessError;
        }

        String status = booking.get("status") == null ? "" : booking.get("status").toString();
        if (!"CONFIRMED".equalsIgnoreCase(status)) {
            return switch (status.toUpperCase()) {
                case "HOLD" -> "Booking vẫn đang giữ chỗ HOLD, chưa xác nhận thanh toán cọc.";
                case "CHECKED_IN" -> "Booking này đã được check-in trước đó.";
                case "COMPLETED" -> "Booking này đã hoàn tất.";
                case "CANCELLED" -> "Booking này đã bị hủy.";
                case "EXPIRED" -> "Booking này đã hết hạn.";
                case "PENDING_CHECKOUT_PAYMENT" -> "Booking này đang chờ thanh toán checkout.";
                default -> "Chỉ booking ở trạng thái CONFIRMED mới được check-in.";
            };
        }

        if (!Boolean.TRUE.equals(booking.get("bookingToday"))) {
            return "Chỉ có thể check-in booking trong ngày hôm nay.";
        }

        if (!Boolean.TRUE.equals(booking.get("notExpired"))) {
            return "Lịch đặt đã quá giờ nhận sân.";
        }

        return null;
    }

    private boolean isFacilityMismatch(String message) {
        return message != null && message.startsWith("Lượt đặt sân này thuộc cơ sở khác");
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeBookingCode(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String value = rawValue.trim();
        if (value.startsWith("#")) {
            value = value.substring(1).trim();
        }
        return value;
    }

    private String getPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        return uri.substring(contextPath.length());
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
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

        return java.time.LocalTime.of(hour, min, sec);
    }

}

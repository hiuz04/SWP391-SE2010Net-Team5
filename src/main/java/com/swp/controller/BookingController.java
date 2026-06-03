package com.swp.controller;

import com.swp.dao.BookingDAO;
import com.swp.model.Booking;
import com.swp.model.Field;
import com.swp.model.FieldMaintenanceSchedule;
import com.swp.model.User;
import com.swp.model.dto.BookingView;
import com.swp.model.dto.FieldScheduleSlot;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@WebServlet(name = "BookingController", urlPatterns = {"/booking"})
public class BookingController extends HttpServlet {

    private final BookingDAO bookingDAO = new BookingDAO();

    private static final LocalTime GRID_START_TIME = LocalTime.of(5, 0);
    private static final LocalTime GRID_LAST_SLOT_START = LocalTime.of(20, 30);
    private static final int SLOT_MINUTES = 30;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String action = trim(request.getParameter("action"));

        try {
            switch (action == null || action.isEmpty() ? "create" : action) {
                case "create" -> showSchedulePage(request, response);
                case "confirm" -> showConfirmationPage(request, response);
                default -> response.sendRedirect(request.getContextPath() + "/");
            }
        } catch (SQLException e) {
            handleError(response, e);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }



    private void showSchedulePage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        Long facilityId = getFacilityIdFromRequest(request);

        if (facilityId == null) {
            request.setAttribute("error", "Không tìm thấy cơ sở sân.");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
            return;
        }

        LocalDate selectedDate = parseBookingDate(request.getParameter("date"));

        List<Field> fields = bookingDAO.getFieldsByFacility(facilityId);
        List<Booking> bookings = bookingDAO.getBookingsByFacilityAndDate(facilityId, selectedDate);
        List<FieldMaintenanceSchedule> maintenances =
                bookingDAO.getMaintenanceByFacilityAndDate(facilityId, selectedDate);

        List<String> timeHeaders = buildTimeHeaders();
        Map<Long, List<FieldScheduleSlot>> scheduleMap = buildScheduleMap(
                fields,
                bookings,
                maintenances,
                selectedDate
        );

        request.setAttribute("facilityId", facilityId);
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("fields", fields);
        request.setAttribute("timeHeaders", timeHeaders);
        request.setAttribute("scheduleMap", scheduleMap);

        request.getRequestDispatcher("/WEB-INF/booking/create-booking.jsp").forward(request, response);
    }

    private void showConfirmationPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        Long fieldId = parseLong(request.getParameter("fieldId"), "fieldId không hợp lệ.");
        LocalDateTime startTime = parseLocalDateTime(request.getParameter("startTime"), "Giờ bắt đầu không hợp lệ.");
        LocalDateTime endTime = parseLocalDateTime(request.getParameter("endTime"), "Giờ kết thúc không hợp lệ.");
        validateTimeRange(startTime, endTime);

        BookingView bookingInfo = bookingDAO.getBookingPreviewInfoByFieldId(fieldId, currentUser.getUserId());
        if (bookingInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy sân.");
            return;
        }

        if (!bookingDAO.isFieldAvailable(fieldId, startTime, endTime)) {
            redirectWithError(request, response, "create", "Khung giờ đã được đặt hoặc sân đang bảo trì.");
            return;
        }

        BigDecimal originalPrice = bookingDAO.calculatePrice(fieldId, startTime, endTime);
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = originalPrice.subtract(discountAmount);
        BigDecimal depositAmount = totalAmount;
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(15);

        Booking bookingPreview = new Booking();
        bookingPreview.setFieldId(fieldId);
        bookingPreview.setFacilityId(bookingInfo.getFacilityId());
        bookingPreview.setCustomerId(currentUser.getUserId());
        bookingPreview.setStartTime(startTime);
        bookingPreview.setEndTime(endTime);
        bookingPreview.setOriginalPrice(originalPrice);
        bookingPreview.setDiscountAmount(discountAmount);
        bookingPreview.setTotalAmount(totalAmount);
        bookingPreview.setDepositAmount(depositAmount);
        bookingPreview.setStatus("HOLD");
        bookingPreview.setHoldExpiresAt(holdExpiresAt);

        bookingInfo.setStartTime(startTime);
        bookingInfo.setEndTime(endTime);
        bookingInfo.setOriginalPrice(originalPrice);
        bookingInfo.setDiscountAmount(discountAmount);
        bookingInfo.setTotalAmount(totalAmount);
        bookingInfo.setDepositAmount(depositAmount);
        bookingInfo.setStatus("HOLD");
        bookingInfo.setHoldExpiresAt(holdExpiresAt);

        request.setAttribute("bookingInfo", bookingInfo);
        request.setAttribute("bookingPreview", bookingPreview);
        request.setAttribute("startTimeValue", startTime.toString());
        request.setAttribute("endTimeValue", endTime.toString());

        request.getRequestDispatcher("/WEB-INF/booking/booking-confirm.jsp").forward(request, response);
    }

    private Long getFacilityIdFromRequest(HttpServletRequest request) throws SQLException {
        String facilityIdRaw = request.getParameter("facilityId");
        String fieldIdRaw = request.getParameter("fieldId");

        if (facilityIdRaw != null && !facilityIdRaw.trim().isEmpty()) {
            return Long.parseLong(facilityIdRaw.trim());
        }

        if (fieldIdRaw != null && !fieldIdRaw.trim().isEmpty()) {
            return bookingDAO.getFacilityIdByFieldId(Long.parseLong(fieldIdRaw.trim()));
        }

        return null;
    }

    private Map<Long, List<FieldScheduleSlot>> buildScheduleMap(
            List<Field> fields,
            List<Booking> bookings,
            List<FieldMaintenanceSchedule> maintenances,
            LocalDate selectedDate
    ) {
        Map<Long, List<FieldScheduleSlot>> scheduleMap = new LinkedHashMap<>();

        for (Field field : fields) {
            List<FieldScheduleSlot> slots = new ArrayList<>();

            LocalTime current = GRID_START_TIME;

            while (!current.isAfter(GRID_LAST_SLOT_START)) {
                LocalDateTime slotStart = selectedDate.atTime(current);
                LocalDateTime slotEnd = slotStart.plusMinutes(SLOT_MINUTES);

                String status = getSlotStatus(field, slotStart, slotEnd, bookings, maintenances);
                String title = getSlotTitle(status);

                FieldScheduleSlot slot = new FieldScheduleSlot(
                        field.getFieldId(),
                        field.getFieldName(),
                        slotStart,
                        slotEnd,
                        status,
                        title
                );

                slots.add(slot);
                current = current.plusMinutes(SLOT_MINUTES);
            }

            scheduleMap.put(field.getFieldId(), slots);
        }

        return scheduleMap;
    }

    private LocalDate parseBookingDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(rawDate.trim());
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private List<String> buildTimeHeaders() {
        List<String> headers = new ArrayList<>();

        LocalTime current = GRID_START_TIME;

        while (!current.isAfter(GRID_LAST_SLOT_START)) {
            headers.add(current.toString());
            current = current.plusMinutes(SLOT_MINUTES);
        }

        return headers;
    }

    private String getSlotStatus(
            Field field,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            List<Booking> bookings,
            List<FieldMaintenanceSchedule> maintenances
    ) {
        if (field.getStatus() == null || !"AVAILABLE".equalsIgnoreCase(field.getStatus())) {
            return "DISABLED";
        }

        for (FieldMaintenanceSchedule maintenance : maintenances) {
            if (maintenance.getFieldId().equals(field.getFieldId())
                    && isOverlap(slotStart, slotEnd, maintenance.getStartTime(), maintenance.getEndTime())) {
                return "MAINTENANCE";
            }
        }

        for (Booking booking : bookings) {
            if (booking.getFieldId().equals(field.getFieldId())
                    && isOverlap(slotStart, slotEnd, booking.getStartTime(), booking.getEndTime())) {
                return "BOOKED";
            }
        }

        return "AVAILABLE";
    }

    private boolean isOverlap(
            LocalDateTime start1,
            LocalDateTime end1,
            LocalDateTime start2,
            LocalDateTime end2
    ) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private String getSlotTitle(String status) {
        if (status == null) {
            return "Không khả dụng";
        }

        switch (status) {
            case "AVAILABLE":
                return "Còn trống";
            case "BOOKED":
                return "Đã có booking";
            case "MAINTENANCE":
                return "Sân đang bảo trì";
            case "DISABLED":
                return "Sân không khả dụng";
            default:
                return "Không khả dụng";
        }
    }

    private User requireLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        return (User) session.getAttribute("user");
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void handleError(HttpServletResponse response, Exception e)
            throws IOException {

        e.printStackTrace();
        response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Có lỗi xảy ra khi hiển thị lịch sân: " + e.getMessage()
        );
    }

    private Long parseLong(String rawValue, String message) {
        String value = trim(rawValue);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private LocalDateTime parseLocalDateTime(String rawValue, String message) {
        String value = trim(rawValue);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response,
                                   String targetAction, String message) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath())
                .append("/booking?action=").append(targetAction)
                .append("&error=")
                .append(URLEncoder.encode(message, StandardCharsets.UTF_8));

        String facilityId = trim(request.getParameter("facilityId"));
        if (facilityId == null || facilityId.isEmpty()) {
            Long fieldId = null;
            try {
                fieldId = parseLong(request.getParameter("fieldId"), "");
            } catch (IllegalArgumentException ignored) {
            }
            if (fieldId != null) {
                try {
                    Long facility = bookingDAO.getFacilityIdByFieldId(fieldId);
                    if (facility != null) {
                        facilityId = facility.toString();
                    }
                } catch (SQLException ignored) {
                }
            }
        }

        if (facilityId != null && !facilityId.isEmpty()) {
            url.append("&facilityId=").append(facilityId);
        }

        String startTime = trim(request.getParameter("startTime"));
        if (startTime != null && startTime.length() >= 10) {
            url.append("&date=").append(startTime, 0, 10);
        }

        response.sendRedirect(url.toString());
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc.");
        }
    }
}
package com.swp.controller.customer;

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
import java.math.RoundingMode;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@WebServlet(name = "BookingController", urlPatterns = {"/booking"})
/*
 * BookingController handles booking request validation, routing, and page data.
 * SQL and persistence details stay in BookingDAO.
 */
public class BookingController extends HttpServlet {

    private final BookingDAO bookingDAO = new BookingDAO();

    private static final LocalTime GRID_START_TIME = LocalTime.of(5, 0);
    private static final LocalTime GRID_LAST_SLOT_START = LocalTime.of(20, 30);
    private static final int SLOT_MINUTES = 30;
    private static final int HOLD_MINUTES = 15;
    private static final int MAX_RECURRING_BOOKINGS = 10;
    private static final int MAX_BOOKING_ADVANCE_MONTHS = 1;
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");
    private static final String STATUS_HOLD = "HOLD";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String REPEAT_NONE = "NONE";
    private static final String REPEAT_MONTHLY = "MONTHLY";

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
                case "history" -> showBookingHistory(request, response);
                case "detail" -> showBookingDetail(request, response);
                default -> response.sendRedirect(request.getContextPath() + "/");
            }
        } catch (SQLException e) {
            handleError(response, e);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String action = trim(request.getParameter("action"));

        try {
            if ("confirm".equals(action)) {
                createBookingHoldWithRepeat(request, response);
            } else if ("cancel".equals(action)) {
                cancelBooking(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/");
            }
        } catch (SQLException e) {
            handleError(response, e);
        } catch (IllegalArgumentException e) {
            redirectWithError(request, response, "create", e.getMessage());
        }
    }

    private void showSchedulePage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        User currentUser = requireLogin(request, response);
        // Neu chua dang nhap thi requireLogin da chuyen huong sang trang login.
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
        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = getMaxBookingDate();
        if (selectedDate.isBefore(today) || selectedDate.isAfter(maxBookingDate)) {
            selectedDate = today;
            request.setAttribute("error", "Chỉ cho phép đặt sân trong tháng này và tháng sau.");
        }

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
        request.setAttribute("maxBookingDate", maxBookingDate);
        request.setAttribute("fields", fields);
        request.setAttribute("timeHeaders", timeHeaders);
        request.setAttribute("scheduleMap", scheduleMap);
        if (request.getAttribute("error") == null) {
            request.setAttribute("error", request.getParameter("error"));
        }

        request.getRequestDispatcher("/WEB-INF/booking/create-booking.jsp").forward(request, response);
    }

    private void showConfirmationPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        //check login
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }
        //Parse dữ liệu từ URL
        Long fieldId = parseLong(request.getParameter("fieldId"), "fieldId không hợp lệ.");
        LocalDateTime startTime = parseLocalDateTime(request.getParameter("startTime"), "Giờ bắt đầu không hợp lệ.");
        LocalDateTime endTime = parseLocalDateTime(request.getParameter("endTime"), "Giờ kết thúc không hợp lệ.");
        //Validate giờ
        validateTimeRange(startTime, endTime);
        validateBookingAdvanceWindow(startTime);
        //Parse loại thuê
        RepeatRequest repeatRequest = parseRepeatRequest(
                request.getParameter("repeatType"),
                startTime.toLocalDate()
        );
        //Lấy thông tin preview
        BookingView bookingInfo = bookingDAO.getBookingPreviewInfoByFieldId(fieldId, currentUser.getUserId());
        // Khong co thong tin san thi khong the tao booking hold.
        if (bookingInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy sân.");
            return;
        }
        //Check sân còn trống
        if (!bookingDAO.isFieldAvailable(fieldId, startTime, endTime)) {
            redirectWithError(request, response, "create", "Khung giờ đã được đặt hoặc sân đang bảo trì.");
            return;
        }

        //Nếu thuê lặp thì check từng slot
        List<BookingSlot> bookingSlots = buildBookingSlots(startTime, endTime, repeatRequest);
        for (BookingSlot slot : bookingSlots) {
            if (!bookingDAO.isFieldAvailable(fieldId, slot.startTime(), slot.endTime())) {
                redirectWithError(request, response, "create", "Khung giờ đã được đặt hoặc sân đang bảo trì.");
                return;
            }
        }

        //Tính tiền
        boolean fullPaymentRequired = REPEAT_MONTHLY.equals(repeatRequest.repeatType());
        BookingAmounts amounts = calculateAggregateBookingAmounts(fieldId, bookingSlots, fullPaymentRequired);
        //Tạo preview HOLD 15 phút
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(HOLD_MINUTES);

        Booking bookingPreview = new Booking();
        bookingPreview.setFieldId(fieldId);
        bookingPreview.setFacilityId(bookingInfo.getFacilityId());
        bookingPreview.setCustomerId(currentUser.getUserId());
        bookingPreview.setStartTime(startTime);
        bookingPreview.setEndTime(endTime);
        bookingPreview.setOriginalPrice(amounts.originalPrice());
        bookingPreview.setDiscountAmount(amounts.discountAmount());
        bookingPreview.setTotalAmount(amounts.totalAmount());
        bookingPreview.setDepositAmount(amounts.depositAmount());
        bookingPreview.setStatus(STATUS_HOLD);
        bookingPreview.setHoldExpiresAt(holdExpiresAt);

        bookingInfo.setStartTime(startTime);
        bookingInfo.setEndTime(endTime);
        bookingInfo.setOriginalPrice(amounts.originalPrice());
        bookingInfo.setDiscountAmount(amounts.discountAmount());
        bookingInfo.setTotalAmount(amounts.totalAmount());
        bookingInfo.setDepositAmount(amounts.depositAmount());
        bookingInfo.setStatus(STATUS_HOLD);
        bookingInfo.setHoldExpiresAt(holdExpiresAt);

        request.setAttribute("bookingInfo", bookingInfo);
        request.setAttribute("bookingPreview", bookingPreview);
        request.setAttribute("startTimeValue", startTime.toString());
        request.setAttribute("endTimeValue", endTime.toString());
        request.setAttribute("repeatType", repeatRequest.repeatType());
        request.setAttribute("recurringCount", bookingSlots.size());

        request.getRequestDispatcher("/WEB-INF/booking/booking-confirm.jsp").forward(request, response);
    }


    private void createBookingHoldWithRepeat(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        //Check login
        User currentUser = requireLogin(request, response);
        // Neu chua dang nhap thi dung xu ly de tranh tao booking khong co customer.
        if (currentUser == null) {
            return;
        }
        //Parse dữ liệu, validate thời gian
        Long fieldId = parseLong(request.getParameter("fieldId"), "fieldId không hợp lệ.");
        LocalDateTime startTime = parseLocalDateTime(request.getParameter("startTime"), "Giờ bắt đầu không hợp lệ.");
        LocalDateTime endTime = parseLocalDateTime(request.getParameter("endTime"), "Giờ kết thúc không hợp lệ.");
        validateTimeRange(startTime, endTime);
        validateBookingAdvanceWindow(startTime);
        RepeatRequest repeatRequest = parseRepeatRequest(
                request.getParameter("repeatType"),
                startTime.toLocalDate()
        );
        //Lấy thông tin sân
        BookingView bookingInfo = bookingDAO.getBookingPreviewInfoByFieldId(fieldId, currentUser.getUserId());
        // Chan tao booking khi fieldId khong ton tai hoac khong lay duoc du lieu san.
        if (bookingInfo == null) {
            throw new IllegalArgumentException("Khong tim thay san.");
        }

        /*
         * Chuan bi danh sach booking HOLD.
         * Voi thue lap, moi slot se tao mot booking rieng; DAO se kiem tra lai
         * toan bo slot trong transaction truoc khi ghi DB.
         */

        //Build danh sách booking cần tạo
        List<BookingSlot> bookingSlots = buildBookingSlots(startTime, endTime, repeatRequest);
        List<Booking> bookings = new ArrayList<>();
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
        // Tinh tien va tao object Booking cho tung lan dat trong chuoi lap.
        for (BookingSlot slot : bookingSlots) {
            //Tính tiền từng booking
            BookingAmounts amounts = calculateBookingAmounts(
                    fieldId,
                    slot.startTime(),
                    slot.endTime(),
                    REPEAT_MONTHLY.equals(repeatRequest.repeatType())
            );
            bookings.add(buildBooking(
                    currentUser.getUserId(),
                    bookingInfo.getFacilityId(),
                    fieldId,
                    slot.startTime(),
                    slot.endTime(),
                    holdExpiresAt,
                    amounts
            ));
        }
        long bookingId;
        // Thue don le thi chi insert mot booking HOLD.
        if (REPEAT_NONE.equals(repeatRequest.repeatType())) {
            bookingId = bookingDAO.createBookingHold(
                    bookings.get(0),
                    currentUser.getUserId(),
                    "Customer created booking hold"
            );
        } else {
            // Thue lap thi insert nhom recurring va nhieu booking trong cung transaction.
            List<Long> bookingIds = bookingDAO.createRecurringBookingHolds(
                    bookings,
                    repeatRequest.repeatType(),
                    repeatRequest.repeatUntil(),
                    currentUser.getUserId(),
                    "Customer created recurring booking hold"
            );
            bookingId = bookingIds.get(0);
        }

        response.sendRedirect(request.getContextPath()
                + "/booking?action=detail&id=" + bookingId + "&success=created");
    }

    private void showBookingHistory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        List<BookingView> bookings = bookingDAO.getBookingHistoryByCustomerId(currentUser.getUserId());
        // Tinh san quyen huy cho tung dong de UI biet co cho bam huy hay khong.
        for (BookingView booking : bookings) {
            applyCancellationRule(booking);
        }
        request.setAttribute("bookings", bookings);
        request.getRequestDispatcher("/WEB-INF/booking/booking-history.jsp").forward(request, response);
    }

    private void showBookingDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        Long bookingId = parseLong(request.getParameter("id"), "bookingId không hợp lệ.");
        BookingView booking = bookingDAO.getBookingDetailByIdAndCustomerId(bookingId, currentUser.getUserId());
        // Booking phai thuoc dung customer hien tai.
        if (booking == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy booking.");
            return;
        }

        // Gan du lieu va thong bao truoc khi render trang chi tiet.
        request.setAttribute("booking", booking);
        request.setAttribute("success", request.getParameter("success"));
        applyCancellationRule(booking);
        request.getRequestDispatcher("/WEB-INF/booking/booking-details.jsp").forward(request, response);
    }

    private void cancelBooking(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        User currentUser = requireLogin(request, response);
        // Neu chua dang nhap thi khong cho gui yeu cau huy.
        if (currentUser == null) {
            return;
        }

        Long bookingId = parseLong(request.getParameter("id"), "bookingId không hợp lệ.");
        BookingView booking = bookingDAO.getBookingDetailByIdAndCustomerId(bookingId, currentUser.getUserId());
        // Chi customer so huu booking moi duoc huy booking do.
        if (booking == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy booking.");
            return;
        }

        applyCancellationRule(booking);
        // Neu vi pham rule huy thi quay lai trang chi tiet kem ly do.
        if (!booking.isCanCancel()) {
            response.sendRedirect(request.getContextPath()
                    + "/booking?action=detail&id=" + bookingId
                    + "&error=" + URLEncoder.encode(booking.getCancelReasonMessage(), StandardCharsets.UTF_8));
            return;
        }

        String reason = trim(request.getParameter("reason"));
        // Neu nguoi dung khong nhap ly do thi dung ly do mac dinh de luu log.
        if (reason == null || reason.isEmpty()) {
            reason = "Customer cancelled booking";
        }

        // Cap nhat trang thai booking va ghi log trong DAO.
        bookingDAO.cancelBooking(bookingId, currentUser.getUserId(), reason, currentUser.getUserId());
        
        try {
            com.swp.dao.NotificationDAO notificationDAO = new com.swp.dao.NotificationDAO();
            String msg = "Khách hàng " + currentUser.getFullName() + " đã hủy lịch đặt sân (Mã đặt: " + bookingId + "). Lý do: " + reason;
            notificationDAO.notifyRole("OWNER", "Khách hàng hủy đặt sân", msg, "BOOKING", bookingId);
            notificationDAO.notifyRole("STAFF", "Khách hàng hủy đặt sân", msg, "BOOKING", bookingId);
        } catch (Exception ignored) {}

        response.sendRedirect(request.getContextPath()
                + "/booking?action=detail&id=" + bookingId + "&success=cancelled");
    }

    private Booking buildBooking(
            Long customerId,
            Long facilityId,
            Long fieldId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime holdExpiresAt,
            BookingAmounts amounts
    ) {
        // Ham helper dung object Booking thong nhat cho ca thue don va thue lap.
        Booking booking = new Booking();
        booking.setBookingCode(generateBookingCode());
        booking.setCustomerId(customerId);
        booking.setFacilityId(facilityId);
        booking.setFieldId(fieldId);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setOriginalPrice(amounts.originalPrice());
        booking.setDiscountAmount(amounts.discountAmount());
        booking.setTotalAmount(amounts.totalAmount());
        booking.setDepositAmount(amounts.depositAmount());
        booking.setStatus(STATUS_HOLD);
        booking.setHoldExpiresAt(holdExpiresAt);
        booking.setQrCode(booking.getBookingCode());
        return booking;
    }

    private RepeatRequest parseRepeatRequest(String rawRepeatType, LocalDate bookingDate) {
        String repeatType = trim(rawRepeatType);
        // Khong chon loai lap thi mac dinh la thue mot lan.
        if (repeatType == null || repeatType.isEmpty()) {
            repeatType = REPEAT_NONE;
        }
        repeatType = repeatType.toUpperCase(Locale.ROOT);

        // Hien tai chi ho tro khong lap hoac lap theo thang.
        if (!REPEAT_NONE.equals(repeatType) && !REPEAT_MONTHLY.equals(repeatType)) {
            throw new IllegalArgumentException("Ch\u1ec9 h\u1ed7 tr\u1ee3 thu\u00ea \u0111\u01a1n l\u1ebb ho\u1eb7c thu\u00ea theo th\u00e1ng.");
        }

        // Thue don thi khong can ngay ket thuc lap.
        if (REPEAT_NONE.equals(repeatType)) {
            return new RepeatRequest(REPEAT_NONE, null);
        }

        LocalDate repeatUntil = getMaxBookingDate();

        return new RepeatRequest(repeatType, repeatUntil);
    }

    private List<BookingSlot> buildBookingSlots(
            LocalDateTime startTime,
            LocalDateTime endTime,
            RepeatRequest repeatRequest
    ) {
        List<BookingSlot> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDate maxBookingDate = getMaxBookingDate();
        LocalDateTime currentStart = startTime;
        LocalDateTime currentEnd = endTime;

        while (true) {
            if (currentStart.toLocalDate().isAfter(maxBookingDate)) {
                break;
            }
            if (repeatRequest.repeatUntil() != null
                    && currentStart.toLocalDate().isAfter(repeatRequest.repeatUntil())) {
                break;
            }

            if (currentStart.isAfter(now)) {
                slots.add(new BookingSlot(currentStart, currentEnd));
            } else if (REPEAT_NONE.equals(repeatRequest.repeatType())) {
                throw new IllegalArgumentException("Kh\u00f4ng th\u1ec3 \u0111\u1eb7t s\u00e2n tr\u01b0\u1edbc gi\u1edd hi\u1ec7n t\u1ea1i.");
            }

            // Thue don thi chi can mot slot.
            if (REPEAT_NONE.equals(repeatRequest.repeatType())) {
                break;
            }
            // Chan tao qua nhieu booking lap trong mot lan submit.
            if (slots.size() >= MAX_RECURRING_BOOKINGS) {
                throw new IllegalArgumentException("Ch\u1ec9 cho ph\u00e9p \u0111\u1eb7t s\u00e2n tr\u01b0\u1edbc trong v\u00f2ng 1 th\u00e1ng.");
            }

            currentStart = currentStart.plusWeeks(1);
            currentEnd = currentEnd.plusWeeks(1);
        }

        if (slots.isEmpty()) {
            throw new IllegalArgumentException("Kh\u00f4ng c\u00f3 khung gi\u1edd n\u00e0o trong t\u01b0\u01a1ng lai \u0111\u1ec3 t\u1ea1o booking.");
        }

        return slots;
    }

    /*
     * Quy tac huy booking.
     * Chi cho huy khi booking chua bat dau, chua o trang thai ket thuc,
     * va con nam ngoai moc chan huy truoc gio da.
     */
    private void applyCancellationRule(BookingView booking) {
        int cancelBeforeHours = bookingDAO.getCancelBeforeHours();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = booking.getStartTime();

        // Thieu gio bat dau thi khong du du lieu de xet quyen huy.
        if (startTime == null) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Booking thieu thoi gian bat dau.");
            return;
        }

        String status = booking.getStatus();
        // Cac trang thai ket thuc hoac dang su dung san khong duoc huy nua.
        if (STATUS_CANCELLED.equals(status) || STATUS_COMPLETED.equals(status) || STATUS_CHECKED_IN.equals(status)) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Booking khong the huy voi trang thai hien tai.");
            return;
        }

        // Booking da bat dau thi customer khong con duoc huy.
        if (!now.isBefore(startTime)) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Booking da bat dau nen khong the huy.");
            return;
        }

        // Neu da qua sat gio da so voi cau hinh thi chan huy.
        if (now.isAfter(startTime.minusHours(cancelBeforeHours))) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Chi co the huy truoc gio bat dau toi thieu "
                    + cancelBeforeHours + " gio.");
            return;
        }

        // Qua het cac dieu kien chan thi booking duoc phep huy.
        booking.setCanCancel(true);
        booking.setCancelReasonMessage("Co the huy booking.");
    }
    private Long getFacilityIdFromRequest(HttpServletRequest request) throws SQLException {
        String facilityIdRaw = request.getParameter("facilityId");
        String fieldIdRaw = request.getParameter("fieldId");

        // Uu tien facilityId neu request da truyen truc tiep.
        if (facilityIdRaw != null && !facilityIdRaw.trim().isEmpty()) {
            return Long.parseLong(facilityIdRaw.trim());
        }

        // Neu chi co fieldId thi suy ra facilityId tu DB.
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

    private LocalDate getMaxBookingDate() {
        LocalDate lastAllowedMonth = LocalDate.now().plusMonths(MAX_BOOKING_ADVANCE_MONTHS);
        return lastAllowedMonth.withDayOfMonth(lastAllowedMonth.lengthOfMonth());
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
        if (slotStart != null && slotStart.isBefore(LocalDateTime.now())) {
            return "DISABLED";
        }

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

        return switch (status) {
            case "AVAILABLE" -> "Còn trống";
            case "BOOKED" -> "Đã có booking";
            case "MAINTENANCE" -> "Sân đang bảo trì";
            case "DISABLED" -> "Sân không khả dụng";
            default -> "Không khả dụng";
        };
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

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc.");
        }
    }

    private void validateBookingAdvanceWindow(LocalDateTime startTime) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Kh\u00f4ng th\u1ec3 \u0111\u1eb7t s\u00e2n tr\u01b0\u1edbc gi\u1edd hi\u1ec7n t\u1ea1i.");
        }

        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = getMaxBookingDate();
        LocalDate bookingDate = startTime.toLocalDate();

        if (bookingDate.isBefore(today)) {
            throw new IllegalArgumentException("Không thể đặt sân cho ngày đã qua.");
        }
        if (bookingDate.isAfter(maxBookingDate)) {
            throw new IllegalArgumentException("Chỉ cho phép đặt sân trong tháng này và tháng sau.");
        }
    }

    private BookingAmounts calculateBookingAmounts(Long fieldId, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {
        return calculateBookingAmounts(fieldId, startTime, endTime, false);
    }

    private BookingAmounts calculateBookingAmounts(
            Long fieldId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean fullPaymentRequired
    ) throws SQLException {

        BigDecimal originalPrice = bookingDAO.calculatePrice(fieldId, startTime, endTime)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = originalPrice;
        BigDecimal depositAmount = fullPaymentRequired
                ? totalAmount
                : totalAmount.multiply(DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);

        return new BookingAmounts(originalPrice, discountAmount, totalAmount, depositAmount);
    }

    private BookingAmounts calculateAggregateBookingAmounts(
            Long fieldId,
            List<BookingSlot> bookingSlots,
            boolean fullPaymentRequired
    ) throws SQLException {
        BigDecimal originalPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (BookingSlot slot : bookingSlots) {
            BookingAmounts slotAmounts = calculateBookingAmounts(
                    fieldId,
                    slot.startTime(),
                    slot.endTime(),
                    fullPaymentRequired
            );
            originalPrice = originalPrice.add(slotAmounts.originalPrice());
            discountAmount = discountAmount.add(slotAmounts.discountAmount());
            totalAmount = totalAmount.add(slotAmounts.totalAmount());
        }

        BigDecimal depositAmount = fullPaymentRequired
                ? totalAmount
                : totalAmount.multiply(DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);

        return new BookingAmounts(originalPrice, discountAmount, totalAmount, depositAmount);
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
                fieldId = parseLong(request.getParameter("fieldId"), "fieldId không hợp lệ.");
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

    private String generateBookingCode() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "BK"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
                + suffix;
    }

    private void handleError(HttpServletResponse response, Exception e)
            throws IOException {

        e.printStackTrace();
        response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Có lỗi xảy ra khi xử lý booking: " + e.getMessage()
        );
    }

    private record BookingAmounts(
            BigDecimal originalPrice,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal depositAmount
    ) {
    }

    private record RepeatRequest(
            String repeatType,
            LocalDate repeatUntil
    ) {
    }

    private record BookingSlot(
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }
}

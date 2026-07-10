package com.swp.controller.customer;

import com.swp.dao.BookingDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.dao.VoucherDAO;
import com.swp.model.Booking;
import com.swp.model.Field;
import com.swp.model.FieldMaintenanceSchedule;
import com.swp.model.FieldType;
import com.swp.model.User;
import com.swp.model.dto.BookingView;
import com.swp.model.dto.FieldScheduleSlot;
import com.swp.model.dto.VoucherValidationResult;
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
    private final FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    private static final int SLOT_MINUTES = 30;
    private static final LocalTime GRID_START_TIME = LocalTime.of(5, 0);
    private static final LocalTime GRID_LAST_SLOT_START = LocalTime.of(20, 30);
    private static final LocalTime GRID_END_TIME = GRID_LAST_SLOT_START.plusMinutes(SLOT_MINUTES);
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
            if ("confirm".equals(action)) {
                redirectWithError(request, response, "create", e.getMessage());
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
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
        List<FieldType> fieldTypes = fieldTypeDAO.getAllFieldTypes();
        Map<Long, String> fieldTypeNameByFieldId = buildFieldTypeNameByFieldId(fields, fieldTypes);
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
        request.setAttribute("fieldTypes", fieldTypes);
        request.setAttribute("fieldTypeNameByFieldId", fieldTypeNameByFieldId);
        request.setAttribute("timeHeaders", timeHeaders);
        request.setAttribute("scheduleMap", scheduleMap);
        if (request.getAttribute("error") == null) {
            request.setAttribute("error", request.getParameter("error"));
        }

        request.getRequestDispatcher("/WEB-INF/booking/create-booking.jsp").forward(request, response);
    }

    private Map<Long, String> buildFieldTypeNameByFieldId(List<Field> fields, List<FieldType> fieldTypes) {
        Map<Integer, String> fieldTypeNameById = new LinkedHashMap<>();
        if (fieldTypes != null) {
            for (FieldType fieldType : fieldTypes) {
                if (fieldType == null || fieldType.getFieldTypeId() == null) {
                    continue;
                }

                fieldTypeNameById.put(fieldType.getFieldTypeId(), resolveFieldTypeName(fieldType));
            }
        }

        Map<Long, String> fieldTypeNameByFieldId = new LinkedHashMap<>();
        if (fields != null) {
            for (Field field : fields) {
                if (field == null || field.getFieldId() == null || field.getFieldTypeId() == null) {
                    continue;
                }

                fieldTypeNameByFieldId.put(
                        field.getFieldId(),
                        fieldTypeNameById.get(field.getFieldTypeId())
                );
            }
        }

        return fieldTypeNameByFieldId;
    }

    private String resolveFieldTypeName(FieldType fieldType) {
        String typeName = trim(fieldType.getTypeName());
        if ((typeName == null || typeName.isEmpty()) && fieldType.getNumberOfPlayers() != null) {
            return "Sân " + fieldType.getNumberOfPlayers();
        }

        return typeName;
    }

    private void showConfirmationPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        //check login
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }
        ConfirmationContext context = buildConfirmationContext(
                request,
                currentUser,
                trim(request.getParameter("voucherCode"))
        );
        forwardConfirmationPage(request, response, context);
    }


    private void createBookingHoldWithRepeat(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, ServletException {
        //Check login
        User currentUser = requireLogin(request, response);
        // Neu chua dang nhap thi dung xu ly de tranh tao booking khong co customer.
        if (currentUser == null) {
            return;
        }
        ConfirmationContext context = buildConfirmationContext(
                request,
                currentUser,
                trim(request.getParameter("voucherCode"))
        );
        if (context.voucherError() != null) {
            forwardConfirmationPage(request, response, context);
            return;
        }

        /*
         * Chuan bi danh sach booking HOLD.
         * Voi thue lap, moi slot se tao mot booking rieng; DAO se kiem tra lai
         * toan bo slot trong transaction truoc khi ghi DB.
         */

        long bookingId;
        // Thue don le thi chi insert mot booking HOLD.
        if (REPEAT_NONE.equals(context.repeatRequest().repeatType())) {
            Booking booking = buildBooking(
                    currentUser.getUserId(),
                    context.bookingInfo().getFacilityId(),
                    context.bookingPreview().getFieldId(),
                    context.bookingPreview().getStartTime(),
                    context.bookingPreview().getEndTime(),
                    context.bookingPreview().getHoldExpiresAt(),
                    context.amounts()
            );
            bookingId = bookingDAO.createBookingHold(
                    booking,
                    currentUser.getUserId(),
                    "Customer created booking hold"
            );
        } else {
            List<Booking> bookings = new ArrayList<>();
            boolean fullPaymentRequired = REPEAT_MONTHLY.equals(context.repeatRequest().repeatType());
            for (BookingSlot slot : context.bookingSlots()) {
                BookingAmounts amounts = calculateBookingAmounts(
                        context.bookingPreview().getFieldId(),
                        slot.startTime(),
                        slot.endTime(),
                        fullPaymentRequired
                );
                bookings.add(buildBooking(
                        currentUser.getUserId(),
                        context.bookingInfo().getFacilityId(),
                        context.bookingPreview().getFieldId(),
                        slot.startTime(),
                        slot.endTime(),
                        context.bookingPreview().getHoldExpiresAt(),
                        amounts
                ));
            }
            // Thue lap thi insert nhom recurring va nhieu booking trong cung transaction.
            List<Long> bookingIds = bookingDAO.createRecurringBookingHolds(
                    bookings,
                    context.repeatRequest().repeatType(),
                    context.repeatRequest().repeatUntil(),
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

    private ConfirmationContext buildConfirmationContext(
            HttpServletRequest request,
            User currentUser,
            String rawVoucherCode
    ) throws SQLException {
        Long fieldId = parseLong(request.getParameter("fieldId"), "fieldId không hợp lệ.");
        LocalDateTime startTime = parseLocalDateTime(request.getParameter("startTime"), "Giờ bắt đầu không hợp lệ.");
        LocalDateTime endTime = parseLocalDateTime(request.getParameter("endTime"), "Giờ kết thúc không hợp lệ.");
        validateBookingTimeOrThrow(startTime, endTime);

        RepeatRequest repeatRequest = parseRepeatRequest(
                request.getParameter("repeatType"),
                startTime.toLocalDate()
        );

        BookingView bookingInfo = bookingDAO.getBookingPreviewInfoByFieldId(fieldId, currentUser.getUserId());
        if (bookingInfo == null) {
            throw new IllegalArgumentException("Khong tim thay san.");
        }

        List<BookingSlot> bookingSlots = buildBookingSlots(startTime, endTime, repeatRequest);
        for (BookingSlot slot : bookingSlots) {
            if (!bookingDAO.isFieldAvailable(fieldId, slot.startTime(), slot.endTime())) {
                throw new IllegalArgumentException("Khung giờ đã được đặt hoặc sân đang bảo trì.");
            }
        }

        boolean fullPaymentRequired = REPEAT_MONTHLY.equals(repeatRequest.repeatType());
        BookingAmounts amounts = calculateAggregateBookingAmounts(fieldId, bookingSlots, fullPaymentRequired);

        String voucherCode = trim(rawVoucherCode);
        String voucherError = null;
        String voucherMessage = null;
        if (voucherCode != null && !voucherCode.isEmpty()) {
            if (!REPEAT_NONE.equals(repeatRequest.repeatType())) {
                voucherError = "Mã giảm giá hiện chưa hỗ trợ cho đặt lịch lặp lại.";
            } else {
                VoucherValidationResult validationResult =
                        voucherDAO.validateVoucher(voucherCode, amounts.originalPrice(), currentUser.getUserId());
                if (validationResult.isValid()) {
                    amounts = applyVoucher(amounts, validationResult, fullPaymentRequired);
                    voucherCode = validationResult.getVoucher().getCode();
                    voucherMessage = validationResult.getMessage();
                } else {
                    voucherError = validationResult.getMessage();
                }
            }
        }

        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
        Booking bookingPreview = new Booking();
        bookingPreview.setFieldId(fieldId);
        bookingPreview.setFacilityId(bookingInfo.getFacilityId());
        bookingPreview.setCustomerId(currentUser.getUserId());
        bookingPreview.setStartTime(startTime);
        bookingPreview.setEndTime(endTime);
        bookingPreview.setVoucherId(amounts.voucherId());
        bookingPreview.setOriginalPrice(amounts.originalPrice());
        bookingPreview.setDiscountAmount(amounts.discountAmount());
        bookingPreview.setTotalAmount(amounts.totalAmount());
        bookingPreview.setFinalAmount(amounts.finalAmount());
        bookingPreview.setDepositAmount(amounts.depositAmount());
        bookingPreview.setStatus(STATUS_HOLD);
        bookingPreview.setHoldExpiresAt(holdExpiresAt);

        bookingInfo.setStartTime(startTime);
        bookingInfo.setEndTime(endTime);
        bookingInfo.setVoucherId(amounts.voucherId());
        bookingInfo.setVoucherCode(amounts.voucherCode());
        bookingInfo.setOriginalPrice(amounts.originalPrice());
        bookingInfo.setDiscountAmount(amounts.discountAmount());
        bookingInfo.setTotalAmount(amounts.totalAmount());
        bookingInfo.setFinalAmount(amounts.finalAmount());
        bookingInfo.setDepositAmount(amounts.depositAmount());
        bookingInfo.setStatus(STATUS_HOLD);
        bookingInfo.setHoldExpiresAt(holdExpiresAt);

        return new ConfirmationContext(
                bookingInfo,
                bookingPreview,
                repeatRequest,
                bookingSlots,
                amounts,
                voucherCode,
                voucherMessage,
                voucherError
        );
    }

    private void forwardConfirmationPage(
            HttpServletRequest request,
            HttpServletResponse response,
            ConfirmationContext context
    ) throws ServletException, IOException {
        request.setAttribute("bookingInfo", context.bookingInfo());
        request.setAttribute("bookingPreview", context.bookingPreview());
        request.setAttribute("startTimeValue", context.bookingPreview().getStartTime().toString());
        request.setAttribute("endTimeValue", context.bookingPreview().getEndTime().toString());
        request.setAttribute("repeatType", context.repeatRequest().repeatType());
        request.setAttribute("recurringCount", context.bookingSlots().size());
        request.setAttribute("voucherCode", context.voucherCode());
        request.setAttribute("voucherMessage", context.voucherMessage());
        request.setAttribute("voucherError", context.voucherError());
        request.getRequestDispatcher("/WEB-INF/booking/booking-confirm.jsp").forward(request, response);
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
        booking.setVoucherId(amounts.voucherId());
        booking.setOriginalPrice(amounts.originalPrice());
        booking.setDiscountAmount(amounts.discountAmount());
        booking.setTotalAmount(amounts.totalAmount());
        booking.setFinalAmount(amounts.finalAmount());
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

            // Validate each generated occurrence before any booking is inserted.
            validateBookingTimeOrThrow(currentStart, currentEnd);
            slots.add(new BookingSlot(currentStart, currentEnd));

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

    private void validateBookingTimeOrThrow(LocalDateTime startTime, LocalDateTime endTime) {
        String errorMessage = validateBookingTime(startTime, endTime);
        if (errorMessage != null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String validateBookingTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return "Gi\u1edd b\u1eaft \u0111\u1ea7u ph\u1ea3i nh\u1ecf h\u01a1n gi\u1edd k\u1ebft th\u00fac.";
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            return "Kh\u00f4ng th\u1ec3 \u0111\u1eb7t s\u00e2n trong qu\u00e1 kh\u1ee9.";
        }

        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = getMaxBookingDate();
        LocalDate bookingDate = startTime.toLocalDate();
        if (bookingDate.isBefore(today)) {
            return "Kh\u00f4ng th\u1ec3 \u0111\u1eb7t s\u00e2n trong qu\u00e1 kh\u1ee9.";
        }
        if (bookingDate.isAfter(maxBookingDate)) {
            return "Ch\u1ec9 cho ph\u00e9p \u0111\u1eb7t s\u00e2n trong th\u00e1ng n\u00e0y v\u00e0 th\u00e1ng sau.";
        }

        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            return "Th\u1eddi gian \u0111\u1eb7t s\u00e2n ph\u1ea3i trong c\u00f9ng m\u1ed9t ng\u00e0y.";
        }

        if (!isOnThirtyMinuteBlock(startTime) || !isOnThirtyMinuteBlock(endTime)) {
            return "Th\u1eddi gian \u0111\u1eb7t s\u00e2n ph\u1ea3i theo block 30 ph\u00fat.";
        }

        LocalTime start = startTime.toLocalTime();
        LocalTime end = endTime.toLocalTime();
        if (start.isBefore(GRID_START_TIME) || end.isAfter(GRID_END_TIME)) {
            return "Th\u1eddi gian \u0111\u1eb7t s\u00e2n ph\u1ea3i n\u1eb1m trong gi\u1edd ho\u1ea1t \u0111\u1ed9ng t\u1eeb 05:00 \u0111\u1ebfn 21:00.";
        }

        return null;
    }

    private boolean isOnThirtyMinuteBlock(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }

        int minute = dateTime.getMinute();
        return dateTime.getSecond() == 0
                && dateTime.getNano() == 0
                && (minute == 0 || minute == 30);
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
        BigDecimal finalAmount = totalAmount;
        BigDecimal depositAmount = calculateDepositAmount(finalAmount, fullPaymentRequired);

        return new BookingAmounts(
                originalPrice,
                discountAmount,
                totalAmount,
                finalAmount,
                depositAmount,
                null,
                null
        );
    }

    private BookingAmounts calculateAggregateBookingAmounts(
            Long fieldId,
            List<BookingSlot> bookingSlots,
            boolean fullPaymentRequired
    ) throws SQLException {
        BigDecimal originalPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

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
            finalAmount = finalAmount.add(slotAmounts.finalAmount());
        }

        BigDecimal depositAmount = calculateDepositAmount(finalAmount, fullPaymentRequired);

        return new BookingAmounts(
                originalPrice,
                discountAmount,
                totalAmount,
                finalAmount,
                depositAmount,
                null,
                null
        );
    }

    private BookingAmounts applyVoucher(
            BookingAmounts baseAmounts,
            VoucherValidationResult validationResult,
            boolean fullPaymentRequired
    ) {
        BigDecimal discountAmount = money(validationResult.getDiscountAmount());
        BigDecimal finalAmount = money(validationResult.getFinalAmount());
        BigDecimal depositAmount = calculateDepositAmount(finalAmount, fullPaymentRequired);

        return new BookingAmounts(
                baseAmounts.originalPrice(),
                discountAmount,
                finalAmount,
                finalAmount,
                depositAmount,
                validationResult.getVoucher().getId(),
                validationResult.getVoucher().getCode()
        );
    }

    private BigDecimal calculateDepositAmount(BigDecimal finalAmount, boolean fullPaymentRequired) {
        BigDecimal safeFinalAmount = money(finalAmount);
        return fullPaymentRequired
                ? safeFinalAmount
                : safeFinalAmount.multiply(DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
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
            BigDecimal finalAmount,
            BigDecimal depositAmount,
            Integer voucherId,
            String voucherCode
    ) {
    }

    private record ConfirmationContext(
            BookingView bookingInfo,
            Booking bookingPreview,
            RepeatRequest repeatRequest,
            List<BookingSlot> bookingSlots,
            BookingAmounts amounts,
            String voucherCode,
            String voucherMessage,
            String voucherError
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

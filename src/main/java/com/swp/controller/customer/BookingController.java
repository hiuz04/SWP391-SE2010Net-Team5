package com.swp.controller.customer;

import com.swp.dao.BookingDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.dao.UserDAO;
import com.swp.dao.VoucherDAO;
import com.swp.model.Booking;
import com.swp.model.Field;
import com.swp.model.FieldMaintenanceSchedule;
import com.swp.model.FieldType;
import com.swp.model.User;
import com.swp.model.dto.BookingSlotPreview;
import com.swp.model.dto.BookingView;
import com.swp.model.dto.FieldScheduleSlot;
import com.swp.model.dto.RecurringBookingCreationResult;
import com.swp.model.dto.SkippedBookingSlot;
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

/**
 * Điều phối các luồng đặt sân của Customer: xem lịch sân, xác nhận thông tin,
 * tạo booking HOLD, đặt định kỳ, xem lịch sử, xem chi tiết và hủy booking.
 * Controller chỉ chuẩn bị dữ liệu/validate đầu vào, còn việc khóa dữ liệu và ghi DB nằm ở {@link BookingDAO}.
 */
@WebServlet(name = "BookingController", urlPatterns = {"/booking"})
/*
 * BookingController handles booking request validation, routing, and page data.
 * SQL and persistence details stay in BookingDAO.
 */
public class BookingController extends HttpServlet {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();
    private final UserDAO userDAO = new UserDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final com.swp.dao.SystemSettingDAO systemSettingDAO = new com.swp.dao.SystemSettingDAO();

    // Business Rule BR-02: Lưới đặt sân sử dụng các ô 30 phút và thời lượng lấy theo giờ bắt đầu/kết thúc.
    private static final int SLOT_MINUTES = 30;
    private static final LocalTime GRID_START_TIME = LocalTime.of(5, 0);
    private static final LocalTime GRID_LAST_SLOT_START = LocalTime.of(20, 30);
    private static final LocalTime GRID_END_TIME = GRID_LAST_SLOT_START.plusMinutes(SLOT_MINUTES);
    // Business Rule BR-04: Booking sau khi xác nhận được giữ tạm ở trạng thái HOLD trong 15 phút.
    private static final int HOLD_MINUTES = 15;
    private static final int MAX_RECURRING_BOOKINGS = 50;
    // Business Rule BR-06: Booking thường thanh toán cọc 30% trên số tiền cuối cùng.
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");
    private static final BigDecimal VIP_DISCOUNT_RATE = new BigDecimal("0.05");
    private static final String STATUS_HOLD = "HOLD";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String REPEAT_NONE = "NONE";
    private static final String REPEAT_MONTHLY = "MONTHLY";
    private static final String SLOT_STATUS_AVAILABLE = "Khả dụng";
    private static final String SLOT_STATUS_SKIPPED = "Bỏ qua";
    private static final String MONTHLY_NO_AVAILABLE_SLOT = "Không có buổi nào khả dụng trong tháng đã chọn.";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String action = trim(request.getParameter("action"));

        // Điều hướng request GET theo action để mỗi màn hình booking dùng đúng luồng dữ liệu.
        try {
            // Nếu không truyền action thì mặc định mở màn hình tạo booking.
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
            // Riêng lỗi khi xác nhận booking cần quay lại màn hình chọn sân kèm thông báo để Customer nhập lại.
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

        // Chỉ các action ghi dữ liệu hợp lệ mới được phép đi vào luồng POST của booking.
        try {
            // Action confirm tạo booking HOLD sau khi đã preview/xác nhận thông tin.
            if ("confirm".equals(action)) {
                createBookingHoldWithRepeat(request, response);
            } else if ("cancel".equals(action)) {
                // Action cancel dùng luồng kiểm tra quyền sở hữu và quy tắc hủy booking.
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

    /**
     * Hiển thị ma trận sân theo ngày để Customer chọn khung giờ còn trống.
     * Dữ liệu booking và bảo trì được lấy từ DB trước, sau đó ghép thành từng slot 30 phút cho JSP.
     */
    private void showSchedulePage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        User currentUser = requireLogin(request, response);
        // Neu chua dang nhap thi requireLogin da chuyen huong sang trang login.
        if (currentUser == null) {
            return;
        }

        Long complexId = getComplexIdFromRequest(request);

        // Không có complexId thì không thể dựng lịch sân theo cụm, nên quay về trang chủ với lỗi.
        if (complexId == null) {
            request.setAttribute("error", "Không tìm thấy cụm sân sân.");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
            return;
        }

        LocalDate selectedDate = parseBookingDate(request.getParameter("date"));
        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = getMaxBookingDate();
        // Ngày ngoài phạm vi cấu hình được ép về hôm nay để tránh render slot không cho phép đặt.
        if (selectedDate.isBefore(today) || selectedDate.isAfter(maxBookingDate)) {
            selectedDate = today;
            request.setAttribute("error", "Chỉ cho phép đặt sân trong giới hạn ngày đã cấu hình (Tối đa đến " + maxBookingDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
        }

        // Lịch hiển thị chỉ là ảnh chụp tại thời điểm render; DAO vẫn kiểm tra lại khi tạo booking.
        List<Field> fields = bookingDAO.getFieldsByComplex(complexId);
        List<FieldType> fieldTypes = fieldTypeDAO.getAllFieldTypes();
        Map<Long, String> fieldTypeNameByFieldId = buildFieldTypeNameByFieldId(fields, fieldTypes);
        List<Booking> bookings = bookingDAO.getBookingsByComplexAndDate(complexId, selectedDate);
        List<FieldMaintenanceSchedule> maintenances =
                bookingDAO.getMaintenanceByComplexAndDate(complexId, selectedDate);

        List<String> timeHeaders = buildTimeHeaders();
        Map<Long, List<FieldScheduleSlot>> scheduleMap = buildScheduleMap(
                fields,
                bookings,
                maintenances,
                selectedDate
        );

        request.setAttribute("complexId", complexId);
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("maxBookingDate", maxBookingDate);
        request.setAttribute("fields", fields);
        request.setAttribute("fieldTypes", fieldTypes);
        request.setAttribute("fieldTypeNameByFieldId", fieldTypeNameByFieldId);
        request.setAttribute("timeHeaders", timeHeaders);
        request.setAttribute("scheduleMap", scheduleMap);
        // Chỉ lấy lỗi từ query string khi trước đó chưa có lỗi validate quan trọng hơn.
        if (request.getAttribute("error") == null) {
            request.setAttribute("error", request.getParameter("error"));
        }

        request.getRequestDispatcher("/WEB-INF/booking/create-booking.jsp").forward(request, response);
    }

    private Map<Long, String> buildFieldTypeNameByFieldId(List<Field> fields, List<FieldType> fieldTypes) {
        Map<Integer, String> fieldTypeNameById = new LinkedHashMap<>();
        // Tạo map loại sân trước để tra cứu nhanh tên loại sân cho từng sân trong cụm.
        if (fieldTypes != null) {
            for (FieldType fieldType : fieldTypes) {
                // Bỏ qua loại sân thiếu khóa để không tạo mapping sai.
                if (fieldType == null || fieldType.getFieldTypeId() == null) {
                    continue;
                }

                fieldTypeNameById.put(fieldType.getFieldTypeId(), resolveFieldTypeName(fieldType));
            }
        }

        Map<Long, String> fieldTypeNameByFieldId = new LinkedHashMap<>();
        // Ghép từng sân với tên loại sân tương ứng để JSP hiển thị dễ đọc.
        if (fields != null) {
            for (Field field : fields) {
                // Sân thiếu id hoặc loại sân không đủ dữ liệu để mapping.
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
        // Nếu loại sân chưa đặt tên thì dùng số người chơi làm nhãn fallback.
        if ((typeName == null || typeName.isEmpty()) && fieldType.getNumberOfPlayers() != null) {
            return "Sân " + fieldType.getNumberOfPlayers();
        }

        return typeName;
    }

    /**
     * Hiển thị trang xác nhận booking sau khi đã parse giờ, kiểm tra slot và tính tiền tạm tính.
     * Nếu Customer nhập voucher thì mã được validate ở đây để preview số tiền giảm.
     */
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


    /**
     * Tạo booking ở trạng thái HOLD sau khi xác nhận cuối cùng.
     * Booking đơn lẻ và booking định kỳ đều dùng cùng context để tránh lệch logic tính tiền/giữ chỗ.
     */
    private void createBookingHoldWithRepeat(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException, ServletException {
        // Business Rule BR-01: Customer phải đăng nhập trước khi hệ thống cho tạo booking HOLD.
        User currentUser = requireLogin(request, response);
        // Nếu chưa đăng nhập thì dừng xử lý để tránh tạo booking không gắn customer.
        if (currentUser == null) {
            return;
        }
        ConfirmationContext context = buildConfirmationContext(
                request,
                currentUser,
                trim(request.getParameter("voucherCode"))
        );
        // Nếu voucher không hợp lệ thì quay lại trang xác nhận, không ghi booking HOLD vào DB.
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
        // Thuê đơn lẻ thì chỉ insert một booking HOLD cho đúng một khung giờ đã chọn.
        if (REPEAT_NONE.equals(context.repeatRequest().repeatType())) {
            Booking booking = buildBooking(
                    currentUser.getUserId(),
                    context.bookingInfo().getComplexId(),
                    context.bookingPreview().getFieldId(),
                    context.bookingPreview().getStartTime(),
                    context.bookingPreview().getEndTime(),
                    context.bookingPreview().getHoldExpiresAt (),
                    context.amounts()
            );
            bookingId = bookingDAO.createBookingHold(
                    booking,
                    currentUser.getUserId(),
                    "Customer created booking hold"
            );
        } else {
            if (context.validBookingSlots().isEmpty()) {
                request.setAttribute("creationError", MONTHLY_NO_AVAILABLE_SLOT);
                forwardConfirmationPage(request, response, context);
                return;
            }

            List<Booking> bookings = new ArrayList<>();
            for (CalculatedBookingSlot slot : context.validBookingSlots()) {
                bookings.add(buildBooking(
                        currentUser.getUserId(),
                        context.bookingInfo().getComplexId(),
                        context.bookingPreview().getFieldId(),
                        slot.slot().startTime(),
                        slot.slot().endTime(),
                        context.bookingPreview().getHoldExpiresAt(),
                        slot.amounts()
                ));
            }
            // Thuê lặp thì insert nhóm recurring và nhiều booking trong cùng transaction.
            RecurringBookingCreationResult creationResult = bookingDAO.createRecurringBookingHolds(
                    bookings,
                    context.repeatRequest().repeatType(),
                    context.repeatRequest().repeatUntil(),
                    currentUser.getUserId(),
                    "Customer created recurring booking hold"
            );
            List<SkippedBookingSlot> finalSkippedSlots =
                    combineSkippedSlots(context.skippedSlots(), creationResult.getSkippedSlots());
            if (!creationResult.hasCreatedBookings()) {
                request.setAttribute("creationError", MONTHLY_NO_AVAILABLE_SLOT);
                request.setAttribute("creationSkippedSlots", finalSkippedSlots);
                forwardConfirmationPage(request, response, context);
                return;
            }

            bookingId = creationResult.getBookingIds().get(0);
            storeRecurringCreationFlash(
                    request,
                    bookingId,
                    creationResult.getCreatedCount(),
                    context.expectedSlots().size(),
                    finalSkippedSlots
            );
        }

        response.sendRedirect(request.getContextPath()
                + "/booking?action=detail&id=" + bookingId + "&success=created");
    }

    /**
     * Hiển thị lịch sử booking của Customer hiện tại.
     * DAO đã giới hạn theo customer_id, controller chỉ tính thêm quyền hủy để JSP render đúng nút thao tác.
     */
    private void showBookingHistory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        // Business Rule BR-01: Customer phải đăng nhập trước khi xem lịch sử đặt sân của chính mình.
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

    /**
     * Hiển thị chi tiết một booking nếu booking thuộc Customer đang đăng nhập.
     * Việc lọc theo customer_id là lớp bảo vệ ownership trước khi cho xem QR, thanh toán hoặc hủy.
     */
    private void showBookingDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        // Business Rule BR-01: Customer phải đăng nhập trước khi xem chi tiết booking và thao tác thanh toán/hủy.
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

        List<BookingView> recurringBookings = List.of();
        if (booking.getRecurringGroupId() != null) {
            recurringBookings = bookingDAO.getBookingsByRecurringGroupIdAndCustomerId(
                    booking.getRecurringGroupId(),
                    currentUser.getUserId()
            );
        }

        // Gan du lieu va thong bao truoc khi render trang chi tiet.
        request.setAttribute("booking", booking);
        request.setAttribute("recurringBookings", recurringBookings);
        request.setAttribute("success", request.getParameter("success"));
        consumeRecurringCreationFlash(request, booking.getBookingId());
        applyCancellationRule(booking);
        request.getRequestDispatcher("/WEB-INF/booking/booking-details.jsp").forward(request, response);
    }

    /**
     * Xử lý Customer hủy booking.
     * Controller kiểm tra quyền sở hữu và rule hủy để phản hồi thân thiện; DAO vẫn khóa bản ghi và kiểm tra lại trong transaction.
     */
    private void cancelBooking(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        // Business Rule BR-01: Customer phải đăng nhập trước khi gửi yêu cầu hủy booking.
        User currentUser = requireLogin(request, response);
        // Nếu chưa đăng nhập thì không cho gửi yêu cầu hủy.
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
        // Business Rule BR-07: Chỉ cho phép Customer hủy khi trạng thái và hạn hủy còn hợp lệ.
        // Nếu vi phạm rule hủy thì quay lại trang chi tiết kèm lý do.
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
            String timeStr = booking.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
            String msg = "Khách hàng " + currentUser.getFullName() + " đã hủy lịch đặt sân " + booking.getFieldName() + " (" + booking.getComplexName() + ") lúc " + timeStr + ". Lý do: " + reason;
            notificationDAO.notifyRole("OWNER", "Khách hàng hủy đặt sân", msg, "BOOKING", bookingId);
            notificationDAO.notifyRole("STAFF", "Khách hàng hủy đặt sân", msg, "BOOKING", bookingId);
        } catch (Exception ignored) {}

        response.sendRedirect(request.getContextPath()
                + "/booking?action=detail&id=" + bookingId + "&success=cancelled");
    }

    /**
     * Gom toàn bộ dữ liệu cần cho trang xác nhận: thông tin sân, slot đơn/định kỳ,
     * trạng thái voucher, VIP discount và thời điểm hết hạn HOLD.
     */
    private ConfirmationContext buildConfirmationContext(
            HttpServletRequest request,
            User currentUser,
            String rawVoucherCode
    ) throws SQLException {
        Long fieldId = parseLong(request.getParameter("fieldId"), "fieldId không hợp lệ.");
        LocalDateTime startTime = parseLocalDateTime(request.getParameter("startTime"), "Giờ bắt đầu không hợp lệ.");
        LocalDateTime endTime = parseLocalDateTime(request.getParameter("endTime"), "Giờ kết thúc không hợp lệ.");
        // Business Rule BR-02: Thời lượng booking được xác định từ startTime/endTime và phải bám block 30 phút.
        validateBookingTimeOrThrow(startTime, endTime);

        RepeatRequest repeatRequest = parseRepeatRequest(
                request.getParameter("repeatType"),
                startTime.toLocalDate()
        );
    
        BookingView bookingInfo = bookingDAO.getBookingPreviewInfoByFieldId(fieldId, currentUser.getUserId());
        if (bookingInfo == null) {
            throw new IllegalArgumentException("Khong tim thay san.");
        }

        boolean activeVip = hasActiveVip(currentUser);
        boolean fullPaymentRequired = REPEAT_MONTHLY.equals(repeatRequest.repeatType());
        List<BookingSlot> expectedSlots = buildBookingSlots(startTime, endTime, repeatRequest);
        List<CalculatedBookingSlot> validBookingSlots = new ArrayList<>();
        List<BookingSlotPreview> slotPreviews = new ArrayList<>();
        List<SkippedBookingSlot> skippedSlots = new ArrayList<>();

        for (BookingSlot slot : expectedSlots) {
            BookingDAO.SlotAvailabilityResult availability =
                    bookingDAO.checkFieldAvailability(fieldId, slot.startTime(), slot.endTime());
            if (!availability.available()) {
                if (REPEAT_NONE.equals(repeatRequest.repeatType())) {
                    throw new IllegalArgumentException(availability.reason());
                }
                SkippedBookingSlot skippedSlot = toSkippedSlot(slot, availability.reason());
                skippedSlots.add(skippedSlot);
                slotPreviews.add(toSlotPreview(slot, BigDecimal.ZERO, false, availability.reason()));
                continue;
            }

            BookingAmounts slotAmounts = calculateBookingAmounts(
                    fieldId,
                    slot.startTime(),
                    slot.endTime(),
                    fullPaymentRequired
            );
            if (activeVip && !REPEAT_NONE.equals(repeatRequest.repeatType())) {
                slotAmounts = applyVipDiscount(slotAmounts, fullPaymentRequired);
            }
            validBookingSlots.add(new CalculatedBookingSlot(slot, slotAmounts));
            slotPreviews.add(toSlotPreview(slot, slotAmounts.finalAmount(), true, null));
        }

        BookingAmounts amounts = calculateAggregateBookingAmounts(validBookingSlots, fullPaymentRequired);

        String voucherCode = trim(rawVoucherCode);
        String voucherError = null;
        String voucherMessage = null;
        // Business Rule BR-10: Một booking chỉ nhận tối đa một voucher được nhập trong request xác nhận.
        if (voucherCode != null && !voucherCode.isEmpty()) {
            if (!REPEAT_NONE.equals(repeatRequest.repeatType())) {
                voucherError = "Mã giảm giá hiện chưa hỗ trợ cho đặt lịch lặp lại.";
            } else {
                // Business Rule BR-09: Voucher phải hợp lệ theo trạng thái, thời gian, số lượng, min order và lịch sử dùng của Customer.
                // Voucher được validate bằng giá gốc của đơn, customer_id và số lượt dùng trước khi áp dụng vào preview.
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
        if (activeVip && REPEAT_NONE.equals(repeatRequest.repeatType())) {
            amounts = applyVipDiscount(amounts, fullPaymentRequired);
        }

        // Business Rule BR-04: hold_expires_at bằng thời điểm tạo preview cộng 15 phút giữ chỗ.
        // HOLD chỉ giữ chỗ tạm thời; thanh toán thành công mới chuyển booking sang CONFIRMED.
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
        Booking bookingPreview = new Booking();
        bookingPreview.setFieldId(fieldId);
        bookingPreview.setComplexId(bookingInfo.getComplexId());
        bookingPreview.setCustomerId(currentUser.getUserId());
        bookingPreview.setStartTime(startTime);
        bookingPreview.setEndTime(endTime);
        bookingPreview.setVoucherId(amounts.voucherId());
        bookingPreview.setOriginalPrice(amounts.originalPrice());
        bookingPreview.setDiscountAmount(amounts.discountAmount());
        bookingPreview.setTotalAmount(amounts.totalAmount());
        bookingPreview.setFinalAmount(amounts.finalAmount());
        bookingPreview.setDepositAmount(amounts.depositAmount());
        // Business Rule BR-24: Trạng thái HOLD là trạng thái đầu tiên trong luồng booking đã triển khai.
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
        // Business Rule BR-24: View xác nhận cũng dùng HOLD để hiển thị đúng trạng thái giữ chỗ tạm thời.
        bookingInfo.setStatus(STATUS_HOLD);
        bookingInfo.setHoldExpiresAt(holdExpiresAt);

        return new ConfirmationContext(
                bookingInfo,
                bookingPreview,
                repeatRequest,
                expectedSlots,
                validBookingSlots,
                slotPreviews,
                skippedSlots,
                amounts,
                voucherCode,
                voucherMessage,
                voucherError,
                activeVip
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
        request.setAttribute("recurringCount", context.validBookingSlots().size());
        request.setAttribute("slotPreviews", context.slotPreviews());
        request.setAttribute("validSlots", context.slotPreviews().stream()
                .filter(BookingSlotPreview::isAvailable)
                .toList());
        request.setAttribute("skippedSlots", context.skippedSlots());
        request.setAttribute("totalExpectedSlots", context.expectedSlots().size());
        request.setAttribute("validSlotCount", context.validBookingSlots().size());
        request.setAttribute("skippedSlotCount", context.skippedSlots().size());
        request.setAttribute("totalAmount", context.amounts().finalAmount());
        request.setAttribute("voucherCode", context.voucherCode());
        request.setAttribute("voucherMessage", context.voucherMessage());
        request.setAttribute("voucherError", context.voucherError());
        request.getRequestDispatcher("/WEB-INF/booking/booking-confirm.jsp").forward(request, response);
    }

    private Booking buildBooking(
            Long customerId,
            Long complexId,
            Long fieldId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime holdExpiresAt,
            BookingAmounts amounts
    ) {
        // Hàm helper dùng object Booking thống nhất cho cả thuê đơn và thuê lặp.
        Booking booking = new Booking();
        booking.setBookingCode(generateBookingCode());
        booking.setCustomerId(customerId);
        booking.setComplexId(complexId);
        booking.setFieldId(fieldId);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setVoucherId(amounts.voucherId());
        booking.setOriginalPrice(amounts.originalPrice());
        booking.setDiscountAmount(amounts.discountAmount());
        booking.setTotalAmount(amounts.totalAmount());
        booking.setFinalAmount(amounts.finalAmount());
        booking.setDepositAmount(amounts.depositAmount());
        // Business Rule BR-24: Booking mới được dựng với trạng thái HOLD trước khi DAO ghi DB.
        booking.setStatus(STATUS_HOLD);
        booking.setHoldExpiresAt(holdExpiresAt);
        return booking;
    }

    private BookingSlotPreview toSlotPreview(
            BookingSlot slot,
            BigDecimal price,
            boolean available,
            String reason
    ) {
        return new BookingSlotPreview(
                slot.startTime().toLocalDate(),
                slot.startTime().toLocalTime(),
                slot.endTime().toLocalTime(),
                available ? money(price) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                available,
                available ? SLOT_STATUS_AVAILABLE : SLOT_STATUS_SKIPPED,
                reason
        );
    }

    private SkippedBookingSlot toSkippedSlot(BookingSlot slot, String reason) {
        return new SkippedBookingSlot(
                slot.startTime().toLocalDate(),
                slot.startTime().toLocalTime(),
                slot.endTime().toLocalTime(),
                reason
        );
    }

    private List<SkippedBookingSlot> combineSkippedSlots(
            List<SkippedBookingSlot> previewSkippedSlots,
            List<SkippedBookingSlot> creationSkippedSlots
    ) {
        List<SkippedBookingSlot> combined = new ArrayList<>();
        if (previewSkippedSlots != null) {
            combined.addAll(previewSkippedSlots);
        }
        if (creationSkippedSlots != null) {
            combined.addAll(creationSkippedSlots);
        }
        return combined;
    }

    private void storeRecurringCreationFlash(
            HttpServletRequest request,
            Long representativeBookingId,
            int createdCount,
            int totalExpectedSlots,
            List<SkippedBookingSlot> skippedSlots
    ) {
        int skippedCount = skippedSlots == null ? 0 : skippedSlots.size();
        String message = skippedCount == 0
                ? "Đặt sân theo tháng thành công với " + createdCount + " buổi."
                : "Đặt sân thành công " + createdCount + "/" + totalExpectedSlots
                + " buổi. Có " + skippedCount + " buổi bị bỏ qua do không khả dụng.";

        HttpSession session = request.getSession();
        session.setAttribute("recurringRepresentativeBookingId", representativeBookingId);
        session.setAttribute("recurringCreatedCount", createdCount);
        session.setAttribute("recurringTotalExpectedSlots", totalExpectedSlots);
        session.setAttribute("recurringSuccessMessage", message);
        session.setAttribute("recurringSkippedSlots", skippedSlots);
    }

    @SuppressWarnings("unchecked")
    private void consumeRecurringCreationFlash(HttpServletRequest request, Long bookingId) {
        HttpSession session = request.getSession(false);
        if (session == null || bookingId == null) {
            return;
        }

        String message = (String) session.getAttribute("recurringSuccessMessage");
        if (message == null || message.isBlank()) {
            return;
        }
        Long representativeBookingId = (Long) session.getAttribute("recurringRepresentativeBookingId");
        if (representativeBookingId != null && !representativeBookingId.equals(bookingId)) {
            return;
        }

        request.setAttribute("recurringSuccessMessage", message);
        request.setAttribute("recurringCreatedCount", session.getAttribute("recurringCreatedCount"));
        request.setAttribute("recurringTotalExpectedSlots", session.getAttribute("recurringTotalExpectedSlots"));
        Object skipped = session.getAttribute("recurringSkippedSlots");
        if (skipped instanceof List<?>) {
            request.setAttribute("recurringSkippedSlots", (List<SkippedBookingSlot>) skipped);
        }

        session.removeAttribute("recurringRepresentativeBookingId");
        session.removeAttribute("recurringCreatedCount");
        session.removeAttribute("recurringTotalExpectedSlots");
        session.removeAttribute("recurringSuccessMessage");
        session.removeAttribute("recurringSkippedSlots");
    }

    private RepeatRequest parseRepeatRequest(String rawRepeatType, LocalDate bookingDate) {
        String repeatType = trim(rawRepeatType);
        // Không chọn loại lặp thì mặc định là thuê một lần.
        if (repeatType == null || repeatType.isEmpty()) {
            repeatType = REPEAT_NONE;
        }
        repeatType = repeatType.toUpperCase(Locale.ROOT);

        // Hiện tại chỉ hỗ trợ không lặp hoặc lặp theo tháng.
        if (!REPEAT_NONE.equals(repeatType) && !REPEAT_MONTHLY.equals(repeatType)) {
            throw new IllegalArgumentException("Ch\u1ec9 h\u1ed7 tr\u1ee3 thu\u00ea \u0111\u01a1n l\u1ebb ho\u1eb7c thu\u00ea theo th\u00e1ng.");
        }

        // Thuê đơn thì không cần ngày kết thúc lặp.
        if (REPEAT_NONE.equals(repeatType)) {
            return new RepeatRequest(REPEAT_NONE, null);
        }

        // Với thuê theo tháng, giới hạn nghiệp vụ là ngày cuối cùng của chính tháng chứa ngày đầu tiên.
        LocalDate repeatUntil = bookingDate.withDayOfMonth(bookingDate.lengthOfMonth());

        return new RepeatRequest(repeatType, repeatUntil);
    }

    /**
     * Sinh các lần đặt cần tạo từ lựa chọn ban đầu.
     * Thuê đơn tạo một slot, còn thuê theo tháng tạo các slot hằng tuần cho tới giới hạn ngày đặt sân.
     */
    private List<BookingSlot> buildBookingSlots(
            LocalDateTime startTime,
            LocalDateTime endTime,
            RepeatRequest repeatRequest
    ) {
        List<BookingSlot> slots = new ArrayList<>();
        LocalDateTime currentStart = startTime;
        LocalDateTime currentEnd = endTime;

        while (true) {
            if (repeatRequest.repeatUntil() != null
                    && currentStart.toLocalDate().isAfter(repeatRequest.repeatUntil())) {
                break;
            }

            // Monthly đã được kiểm tra ngày đầu theo MAX_BOOKING_DAYS_AHEAD; các lần lặp chỉ bị giới hạn bởi cuối tháng.
            validateBookingTimeOrThrow(
                    currentStart,
                    currentEnd,
                    REPEAT_NONE.equals(repeatRequest.repeatType())
            );
            slots.add(new BookingSlot(currentStart, currentEnd));

            // Thuê đơn thì chỉ cần một slot.
            if (REPEAT_NONE.equals(repeatRequest.repeatType())) {
                break;
            }
            // Chặn tạo quá nhiều booking lặp trong một lần submit để tránh ghi hàng loạt ngoài kiểm soát.
            if (slots.size() >= MAX_RECURRING_BOOKINGS) {
                throw new IllegalArgumentException("Khung giờ lặp lại vượt quá giới hạn " + MAX_RECURRING_BOOKINGS + " lần.");
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

        // Thiếu giờ bắt đầu thì không đủ dữ liệu để xét quyền hủy.
        if (startTime == null) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Booking thieu thoi gian bat dau.");
            return;
        }

        String status = booking.getStatus();
        // Business Rule BR-07: Các trạng thái kết thúc hoặc đang sử dụng sân không được Customer hủy nữa.
        // Các trạng thái kết thúc hoặc đang sử dụng sân không được hủy nữa.
        if (STATUS_CANCELLED.equals(status) || STATUS_COMPLETED.equals(status) || STATUS_CHECKED_IN.equals(status)) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Booking khong the huy voi trang thai hien tai.");
            return;
        }

        // Business Rule BR-07: Booking đã bắt đầu thì Customer không còn được hủy.
        // Booking đã bắt đầu thì customer không còn được hủy.
        if (!now.isBefore(startTime)) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Booking da bat dau nen khong the huy.");
            return;
        }

        // Business Rule BR-07: Nếu đã quá sát giờ đá so với cấu hình thì chặn hủy.
        // Nếu đã quá sát giờ đá so với cấu hình thì chặn hủy.
        if (now.isAfter(startTime.minusHours(cancelBeforeHours))) {
            booking.setCanCancel(false);
            booking.setCancelReasonMessage("Chi co the huy truoc gio bat dau toi thieu "
                    + cancelBeforeHours + " gio.");
            return;
        }

        // Qua hết các điều kiện chặn thì booking được phép hủy.
        booking.setCanCancel(true);
        booking.setCancelReasonMessage("Co the huy booking.");
    }
    private Long getComplexIdFromRequest(HttpServletRequest request) throws SQLException {
        String complexIdRaw = request.getParameter("complexId");
        String fieldIdRaw = request.getParameter("fieldId");

        // Ưu tiên complexId nếu request đã truyền trực tiếp.
        if (complexIdRaw != null && !complexIdRaw.trim().isEmpty()) {
            return Long.parseLong(complexIdRaw.trim());
        }

        // Nếu chỉ có fieldId thì suy ra complexId từ DB.
        if (fieldIdRaw != null && !fieldIdRaw.trim().isEmpty()) {
            return bookingDAO.getComplexIdByFieldId(Long.parseLong(fieldIdRaw.trim()));
        }

        return null;
    }

    /**
     * Chuyển dữ liệu sân, booking và bảo trì thành lưới slot 30 phút cho màn hình chọn giờ.
     * Mỗi slot chỉ mang trạng thái hiển thị; không được xem là bằng chứng cuối cùng rằng sân còn trống.
     */
    private Map<Long, List<FieldScheduleSlot>> buildScheduleMap(
            List<Field> fields,
            List<Booking> bookings,
            List<FieldMaintenanceSchedule> maintenances,
            LocalDate selectedDate
    ) {
        Map<Long, List<FieldScheduleSlot>> scheduleMap = new LinkedHashMap<>();

        // Mỗi sân có một danh sách slot riêng để JSP render thành từng hàng lịch.
        for (Field field : fields) {
            List<FieldScheduleSlot> slots = new ArrayList<>();

            LocalTime current = GRID_START_TIME;

            // Sinh tuần tự các slot 30 phút từ giờ mở lưới đến slot cuối trong ngày.
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
        // Không truyền ngày thì mặc định xem lịch hôm nay.
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return LocalDate.now();
        }

        // Parse ngày từ query string; dữ liệu sai format sẽ fallback về hôm nay thay vì làm hỏng màn hình.
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private LocalDate getMaxBookingDate() {
        int maxDays = 30; // default 30 days
        java.util.Optional<com.swp.model.SystemSetting> setting = systemSettingDAO.getSettingByKey("MAX_BOOKING_DAYS_AHEAD");
        // Nếu admin đã cấu hình số ngày đặt trước thì dùng cấu hình thay cho mặc định 30 ngày.
        if (setting.isPresent()) {
            try {
                maxDays = Integer.parseInt(setting.get().getSettingValue());
            } catch (NumberFormatException ignored) {}
        }
        return LocalDate.now().plusDays(maxDays);
    }

    private List<String> buildTimeHeaders() {
        List<String> headers = new ArrayList<>();

        LocalTime current = GRID_START_TIME;

        // Header thời gian phải khớp đúng số slot dùng trong scheduleMap.
        while (!current.isAfter(GRID_LAST_SLOT_START)) {
            headers.add(current.toString());
            current = current.plusMinutes(SLOT_MINUTES);
        }

        return headers;
    }

    /**
     * Xác định trạng thái hiển thị của một ô giờ theo thứ tự ưu tiên:
     * quá khứ/sân khóa, lịch bảo trì, booking trùng, rồi mới đến còn trống.
     */
    private String getSlotStatus(
            Field field,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            List<Booking> bookings,
            List<FieldMaintenanceSchedule> maintenances
    ) {
        // Slot trong quá khứ bị khóa để Customer không chọn giờ đã qua.
        if (slotStart != null && slotStart.isBefore(LocalDateTime.now())) {
            return "DISABLED";
        }

        // Sân không ở trạng thái AVAILABLE thì toàn bộ slot của sân đó không cho đặt.
        if (field.getStatus() == null || !"AVAILABLE".equalsIgnoreCase(field.getStatus())) {
            return "DISABLED";
        }

        // Lịch bảo trì có độ ưu tiên cao hơn booking vì sân không thể phục vụ trong khoảng này.
        for (FieldMaintenanceSchedule maintenance : maintenances) {
            // Nếu slot giao với lịch bảo trì của cùng sân thì đánh dấu bảo trì.
            if (maintenance.getFieldId().equals(field.getFieldId())
                    && isOverlap(slotStart, slotEnd, maintenance.getStartTime(), maintenance.getEndTime())) {
                return "MAINTENANCE";
            }
        }

        // Sau bảo trì, kiểm tra booking đang chiếm khung giờ để khóa slot trên giao diện.
        for (Booking booking : bookings) {
            // Slot giao với booking của cùng sân thì không còn khả dụng.
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
        // Thiếu mốc thời gian thì không thể kết luận overlap, trả về false để caller xử lý an toàn.
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private String getSlotTitle(String status) {
        // Status null được xem là không khả dụng để tránh hiển thị slot có thể đặt nhầm.
        if (status == null) {
            return "Không khả dụng";
        }

        // Chuyển mã trạng thái kỹ thuật thành nhãn tiếng Việt cho tooltip/lịch đặt sân.
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

        // Chưa có session đăng nhập thì chuyển về login và dừng luồng booking.
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
        validateBookingTimeOrThrow(startTime, endTime, true);
    }

    private void validateBookingTimeOrThrow(
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean enforceMaxBookingDate
    ) {
        String errorMessage = validateBookingTime(startTime, endTime, enforceMaxBookingDate);
        // Có lỗi validate thì ném exception để caller thống nhất cách redirect/hiển thị lỗi.
        if (errorMessage != null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Kiểm tra các ràng buộc thời gian trước khi tính tiền hoặc tạo booking:
     * không đặt quá khứ, cùng ngày, đúng block 30 phút và nằm trong giờ hoạt động của sân.
     */
    private String validateBookingTime(LocalDateTime startTime, LocalDateTime endTime, boolean enforceMaxBookingDate) {
        // Thiếu giờ hoặc giờ bắt đầu không trước giờ kết thúc thì booking không có thời lượng hợp lệ.
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return "Gi\u1edd b\u1eaft \u0111\u1ea7u ph\u1ea3i nh\u1ecf h\u01a1n gi\u1edd k\u1ebft th\u00fac.";
        }

        // Không cho đặt slot đã bắt đầu trong quá khứ theo thời điểm xử lý request.
        if (startTime.isBefore(LocalDateTime.now())) {
            return "Kh\u00f4ng th\u1ec3 \u0111\u1eb7t s\u00e2n trong qu\u00e1 kh\u1ee9.";
        }

        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = getMaxBookingDate();
        LocalDate bookingDate = startTime.toLocalDate();
        // Chặn thêm lần nữa theo ngày để tránh trường hợp giờ đã parse nhưng ngày bị lùi.
        if (bookingDate.isBefore(today)) {
            return "Kh\u00f4ng th\u1ec3 \u0111\u1eb7t s\u00e2n trong qu\u00e1 kh\u1ee9.";
        }
        // Một số flow preview định kỳ có thể bỏ qua giới hạn này, còn booking chính thì phải tuân thủ cấu hình.
        if (enforceMaxBookingDate && bookingDate.isAfter(maxBookingDate)) {
            return "Ch\u1ec9 cho ph\u00e9p \u0111\u1eb7t s\u00e2n trong th\u00e1ng n\u00e0y v\u00e0 th\u00e1ng sau.";
        }

        // Booking không được kéo qua ngày khác để công thức giá và slot grid luôn nhất quán.
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            return "Th\u1eddi gian \u0111\u1eb7t s\u00e2n ph\u1ea3i trong c\u00f9ng m\u1ed9t ng\u00e0y.";
        }

        // Business Rule BR-02: Cả giờ bắt đầu và giờ kết thúc đều phải nằm trên mốc 00 hoặc 30 phút.
        if (!isOnThirtyMinuteBlock(startTime) || !isOnThirtyMinuteBlock(endTime)) {
            return "Th\u1eddi gian \u0111\u1eb7t s\u00e2n ph\u1ea3i theo block 30 ph\u00fat.";
        }

        LocalTime start = startTime.toLocalTime();
        LocalTime end = endTime.toLocalTime();
        // Khung giờ phải nằm trong giờ vận hành đã định nghĩa cho lưới đặt sân.
        if (start.isBefore(GRID_START_TIME) || end.isAfter(GRID_END_TIME)) {
            return "Th\u1eddi gian \u0111\u1eb7t s\u00e2n ph\u1ea3i n\u1eb1m trong gi\u1edd ho\u1ea1t \u0111\u1ed9ng t\u1eeb 05:00 \u0111\u1ebfn 21:00.";
        }

        return null;
    }

    private boolean isOnThirtyMinuteBlock(LocalDateTime dateTime) {
        // Thiếu thời điểm thì không thể xác nhận mốc 30 phút.
        if (dateTime == null) {
            return false;
        }

        int minute = dateTime.getMinute();
        return dateTime.getSecond() == 0
                && dateTime.getNano() == 0
                && (minute == 0 || minute == 30);
    }

    private boolean hasActiveVip(User currentUser) {
        // Không có user hợp lệ thì không áp dụng ưu đãi VIP.
        if (currentUser == null || currentUser.getUserId() == null) {
            return false;
        }

        User latestUser = userDAO.getUserById(currentUser.getUserId()).orElse(currentUser);
        return latestUser.isVip()
                && latestUser.getVipValidUntil() != null
                && latestUser.getVipValidUntil().isAfter(LocalDateTime.now());
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

        // Giá gốc được lấy từ DAO theo sân và khung giờ trước khi áp voucher/VIP/cọc.
        BigDecimal originalPrice = bookingDAO.calculatePrice(fieldId, startTime, endTime)
                .setScale(2, RoundingMode.HALF_UP);
        return createBookingAmounts(
                originalPrice,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                fullPaymentRequired,
                null,
                null
        );
    }

    private BookingAmounts calculateAggregateBookingAmounts(
            List<CalculatedBookingSlot> bookingSlots,
            boolean fullPaymentRequired
    ) {
        BigDecimal originalPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal vipDiscountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Cộng tiền từng slot hợp lệ để ra tổng tiền cho nhóm booking định kỳ.
        for (CalculatedBookingSlot slot : bookingSlots) {
            BookingAmounts slotAmounts = slot.amounts();
            originalPrice = originalPrice.add(slotAmounts.originalPrice());
            discountAmount = discountAmount.add(slotAmounts.discountAmount());
            vipDiscountAmount = vipDiscountAmount.add(slotAmounts.vipDiscountAmount());
            totalAmount = totalAmount.add(slotAmounts.totalAmount());
            finalAmount = finalAmount.add(slotAmounts.finalAmount());
        }

        BigDecimal depositAmount = calculateDepositAmount(finalAmount, fullPaymentRequired);

        return new BookingAmounts(
                originalPrice,
                discountAmount,
                vipDiscountAmount,
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
        // Business Rule BR-10: Discount từ voucher được đưa vào phép tính nhưng sẽ bị giới hạn không vượt quá đơn hàng.
        BigDecimal discountAmount = money(validationResult.getDiscountAmount());
        return createBookingAmounts(
                baseAmounts.originalPrice(),
                discountAmount,
                baseAmounts.vipDiscountAmount(),
                fullPaymentRequired,
                validationResult.getVoucher().getId(),
                validationResult.getVoucher().getCode()
        );
    }

    private BookingAmounts applyVipDiscount(
            BookingAmounts baseAmounts,
            boolean fullPaymentRequired
    ) {
        // Ưu đãi VIP tính trên giá gốc của booking trước khi tạo lại bộ số tiền cuối.
        BigDecimal vipDiscountAmount = money(baseAmounts.originalPrice().multiply(VIP_DISCOUNT_RATE));
        return createBookingAmounts(
                baseAmounts.originalPrice(),
                baseAmounts.discountAmount(),
                vipDiscountAmount,
                fullPaymentRequired,
                baseAmounts.voucherId(),
                baseAmounts.voucherCode()
        );
    }

    private BookingAmounts createBookingAmounts(
            BigDecimal originalPrice,
            BigDecimal discountAmount,
            BigDecimal vipDiscountAmount,
            boolean fullPaymentRequired,
            Integer voucherId,
            String voucherCode
    ) {
        // Chuẩn hóa toàn bộ số tiền về scale 2 để tránh lệch số khi lưu DB hoặc hiển thị.
        BigDecimal safeOriginalPrice = money(originalPrice);
        // Business Rule BR-10: Giới hạn discount voucher để final amount không thể âm.
        BigDecimal safeDiscountAmount = capDiscount(discountAmount, safeOriginalPrice);
        BigDecimal afterVoucher = safeOriginalPrice.subtract(safeDiscountAmount).setScale(2, RoundingMode.HALF_UP);
        // Giới hạn ưu đãi VIP theo số tiền còn lại sau voucher để final amount không âm.
        BigDecimal safeVipDiscountAmount = capDiscount(vipDiscountAmount, afterVoucher);
        BigDecimal finalAmount = afterVoucher.subtract(safeVipDiscountAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal depositAmount = calculateDepositAmount(finalAmount, fullPaymentRequired);

        return new BookingAmounts(
                safeOriginalPrice,
                safeDiscountAmount,
                safeVipDiscountAmount,
                finalAmount,
                finalAmount,
                depositAmount,
                voucherId,
                voucherCode
        );
    }

    private BigDecimal capDiscount(BigDecimal discountAmount, BigDecimal maxAmount) {
        BigDecimal safeDiscountAmount = money(discountAmount);
        BigDecimal safeMaxAmount = money(maxAmount);
        // Discount âm không hợp lệ nên được đưa về 0 trước khi tính final amount.
        if (safeDiscountAmount.signum() < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // Discount vượt quá số tiền tối đa thì chỉ giảm đến mức bằng đơn hàng.
        if (safeDiscountAmount.compareTo(safeMaxAmount) > 0) {
            return safeMaxAmount;
        }
        return safeDiscountAmount;
    }

    private BigDecimal calculateDepositAmount(BigDecimal finalAmount, boolean fullPaymentRequired) {
        BigDecimal safeFinalAmount = money(finalAmount);
        // Business Rule BR-06: Booking thường lấy 30% final amount, booking yêu cầu full payment thì lấy toàn bộ.
        // Toán tử ba ngôi tách rõ booking cần thanh toán đủ và booking chỉ cần đặt cọc.
        return fullPaymentRequired
                ? safeFinalAmount
                : safeFinalAmount.multiply(DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        // Giá trị tiền null được xem như 0 để các phép cộng/trừ không phát sinh lỗi.
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

        String complexId = trim(request.getParameter("complexId"));
        // Nếu request lỗi không mang complexId, thử suy ra từ fieldId để quay lại đúng cụm sân.
        if (complexId == null || complexId.isEmpty()) {
            Long fieldId = null;
            // FieldId sai định dạng thì bỏ qua, vì redirect lỗi vẫn có thể quay về trang tạo booking chung.
            try {
                fieldId = parseLong(request.getParameter("fieldId"), "fieldId không hợp lệ.");
            } catch (IllegalArgumentException ignored) {
            }
            // Chỉ gọi DB khi fieldId parse được để tránh exception không cần thiết trong luồng xử lý lỗi.
            if (fieldId != null) {
                try {
                    Long complex = bookingDAO.getComplexIdByFieldId(fieldId);
                    // Có complex tương ứng thì giữ lại để màn hình create mở đúng cụm sân ban đầu.
                    if (complex != null) {
                        complexId = complex.toString();
                    }
                } catch (SQLException ignored) {
                }
            }
        }

        // Gắn complexId vào URL lỗi để Customer không phải chọn lại cụm sân.
        if (complexId != null && !complexId.isEmpty()) {
            url.append("&complexId=").append(complexId);
        }

        String startTime = trim(request.getParameter("startTime"));
        // Giữ lại ngày từ startTime để lịch quay về đúng ngày Customer vừa chọn.
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
            BigDecimal vipDiscountAmount,
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
            List<BookingSlot> expectedSlots,
            List<CalculatedBookingSlot> validBookingSlots,
            List<BookingSlotPreview> slotPreviews,
            List<SkippedBookingSlot> skippedSlots,
            BookingAmounts amounts,
            String voucherCode,
            String voucherMessage,
            String voucherError,
            boolean activeVip
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

    private record CalculatedBookingSlot(
            BookingSlot slot,
            BookingAmounts amounts
    ) {
    }
}

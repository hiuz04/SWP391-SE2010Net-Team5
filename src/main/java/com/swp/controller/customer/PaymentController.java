package com.swp.controller.customer;

import com.swp.dao.PaymentDAO;
import com.swp.dao.PaymentDAO.GatewayPaymentView;
import com.swp.dao.PaymentDAO.PaymentUpdateResult;
import com.swp.model.Payment;
import com.swp.model.PaymentMethod;
import com.swp.model.User;
import com.swp.model.dto.BookingView;
import com.swp.model.dto.InvoiceView;
import com.swp.model.dto.PaymentView;
import com.swp.dao.SystemSettingDAO;
import com.swp.model.SystemSetting;
import java.util.Optional;
import com.swp.util.VNPayConfig;
import com.swp.util.VNPayUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Điều phối các luồng thanh toán của Customer: chọn phương thức, tạo payment đặt cọc,
 * thanh toán invoice checkout, thanh toán membership, xử lý VNPay Return/IPN và xem lịch sử giao dịch.
 */
@WebServlet(name = "PaymentController", urlPatterns = {
        "/payment",
        "/payment/vnpay-return",
        "/payment/vnpay-ipn"
})
public class PaymentController extends HttpServlet {

    private static final String GATEWAY_VNPAY = "VNPAY";
    private static final String MODE_VNPAY = "VNPAY";
    private static final String MODE_SIMULATED = "SIMULATED";
    private static final String METHOD_CASH = "CASH";
    private static final String PAYMENT_TYPE_CHECKOUT = "CHECKOUT";
    private static final String PAYMENT_TYPE_MEMBERSHIP = "MEMBERSHIP";

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final SystemSettingDAO systemSettingDAO = new SystemSettingDAO();

    private java.math.BigDecimal getVipPrice() {
        Optional<SystemSetting> vipSetting = systemSettingDAO.getSettingByKey("VIP_SUBSCRIPTION_PRICE_MONTHLY");
        // Dùng giá VIP trong system setting nếu admin đã cấu hình.
        if (vipSetting.isPresent() && vipSetting.get().getSettingValue() != null) {
            // Parse setting dạng text/số để lấy phần numeric làm amount thanh toán.
            try {
                return new java.math.BigDecimal(vipSetting.get().getSettingValue().replaceAll("[^0-9]", ""));
            } catch(Exception e) {
                // Setting sai format thì bỏ qua và dùng giá VIP mặc định bên dưới.
            }
        }
        return new java.math.BigDecimal("199000");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String servletPath = request.getServletPath();

        // Bọc toàn bộ GET payment để gom lỗi theo đúng loại request: web page hoặc VNPay IPN JSON.
        try {
            // VNPay Return là callback qua trình duyệt, cần xử lý trước action thông thường.
            if ("/payment/vnpay-return".equals(servletPath)) {
                handleVNPayReturn(request, response);
                return;
            }
            // VNPay IPN là callback server-to-server nên trả JSON theo chuẩn gateway.
            if ("/payment/vnpay-ipn".equals(servletPath)) {
                handleVNPayIpn(request, response);
                return;
            }

            String action = trim(request.getParameter("action"));
            switch (action == null || action.isEmpty() ? "history" : action) {
                case "method" -> showPaymentMethod(request, response);
                case "result" -> showPaymentResult(request, response);
                case "history" -> showPaymentHistory(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            // Lỗi validate ở IPN phải được format thành RspCode VNPay thay vì trang lỗi HTML.
            if (isVNPayIpn(request)) {
                handleVNPayIpnError(response, e);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        } catch (SQLException e) {
            // Lỗi DB của IPN vẫn phải trả JSON để VNPay hiểu kết quả xử lý.
            if (isVNPayIpn(request)) {
                handleVNPayIpnError(response, e);
            } else {
                handleDatabaseError(response, e);
            }
        } catch (RuntimeException e) {
            // Runtime exception được log tập trung và không làm lộ stacktrace ra Customer.
            if (isVNPayIpn(request)) {
                handleVNPayIpnError(response, e);
            } else {
                handleUnexpectedError(response, e);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String action = trim(request.getParameter("action"));
        // POST payment chỉ nhận action=pay để tránh submit nhầm endpoint.
        if (!"pay".equals(action)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Gom lỗi submit để redirect về màn chọn phương thức hoặc trả lỗi hệ thống phù hợp.
        try {
            processPayment(request, response);
        } catch (IllegalArgumentException e) {
            redirectToMethodWithError(request, response, e.getMessage());
        } catch (SQLException e) {
            handleDatabaseError(response, e);
        } catch (RuntimeException e) {
            handleUnexpectedError(response, e);
        }
    }

    /**
     * Hiển thị màn hình chọn phương thức thanh toán theo ngữ cảnh: đặt cọc, checkout hoặc membership.
     * Mỗi ngữ cảnh đều load lại dữ liệu từ DB để kiểm tra ownership và số tiền cần trả.
     */
    private void showPaymentMethod(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        // Business Rule BR-01: Customer phải đăng nhập trước khi chọn phương thức thanh toán booking/checkout/membership.
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        String type = trim(request.getParameter("type"));
        // Nếu là checkout thì chuyển sang flow thanh toán hóa đơn còn lại.
        if (PAYMENT_TYPE_CHECKOUT.equalsIgnoreCase(type)) {
            showCheckoutPaymentMethod(request, response, currentUser);
            return;
        }
        // Nếu là membership thì không cần booking, chỉ hiển thị amount gói VIP.
        if (PAYMENT_TYPE_MEMBERSHIP.equalsIgnoreCase(type)) {
            showMembershipPaymentMethod(request, response, currentUser);
            return;
        }

        long bookingId = parsePositiveLong(firstNonBlank(
                request.getParameter("bookingId"),
                request.getParameter("id")
        ), "bookingId khong hop le.");
        BookingView booking = paymentDAO.getBookingForPayment(bookingId, currentUser.getUserId());
        // Business Rule BR-01: Query payment đã giới hạn booking theo customer_id để Customer không thanh toán booking của người khác.
        if (booking == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Khong tim thay booking con hieu luc de thanh toan.");
            return;
        }

        List<PaymentMethod> methods = paymentDAO.getActiveOnlinePaymentMethods();
        request.setAttribute("booking", booking);
        request.setAttribute("paymentContext", "DEPOSIT");
        request.setAttribute("paymentMethods", methods);
        request.setAttribute("error", request.getParameter("error"));
        request.getRequestDispatcher("/WEB-INF/payment/payment.jsp").forward(request, response);
    }

    private void showMembershipPaymentMethod(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, ServletException, SQLException {
        List<PaymentMethod> methods = paymentDAO.getActiveOnlinePaymentMethods();
        request.setAttribute("paymentContext", PAYMENT_TYPE_MEMBERSHIP);
        request.setAttribute("amountToPay", getVipPrice());
        request.setAttribute("paymentMethods", methods);
        request.setAttribute("error", request.getParameter("error"));
        request.getRequestDispatcher("/WEB-INF/payment/payment.jsp").forward(request, response);
    }

    /**
     * Hiển thị phương thức thanh toán cho hóa đơn checkout.
     * Invoice phải thuộc Customer hiện tại và đang ở trạng thái PENDING.
     */
    private void showCheckoutPaymentMethod(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, ServletException, SQLException {
        long invoiceId = parsePositiveLong(request.getParameter("invoiceId"),
                "invoiceId khong hop le.");
        InvoiceView invoice = paymentDAO.getCheckoutInvoiceForPayment(invoiceId, currentUser.getUserId());
        // Business Rule BR-20: Chỉ invoice checkout PENDING mới cần Customer thanh toán phần còn lại.
        if (invoice == null || !"PENDING".equals(invoice.getInvoiceStatus())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Khong tim thay hoa don checkout dang cho thanh toan.");
            return;
        }

        List<PaymentMethod> methods = paymentDAO.getActiveOnlinePaymentMethods();
        request.setAttribute("paymentContext", PAYMENT_TYPE_CHECKOUT);
        request.setAttribute("invoice", invoice);
        request.setAttribute("amountToPay", invoice.getTotalAmount());
        request.setAttribute("paymentMethods", methods);
        request.setAttribute("error", request.getParameter("error"));
        request.getRequestDispatcher("/WEB-INF/payment/payment.jsp").forward(request, response);
    }

    /**
     * Nhận submit thanh toán và chuyển tiếp sang đúng flow theo paymentType.
     * Controller chỉ chọn mode thanh toán; DAO mới tạo transaction với số tiền đọc từ DB.
     */
    private void processPayment(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        // Business Rule BR-01: Customer phải đăng nhập trước khi submit thanh toán.
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        String paymentType = trim(request.getParameter("paymentType"));
        // Submit checkout được xử lý riêng vì amount và trạng thái lấy từ invoice.
        if (PAYMENT_TYPE_CHECKOUT.equalsIgnoreCase(paymentType)) {
            processCheckoutPayment(request, response, currentUser);
            return;
        }
        // Submit membership được xử lý riêng vì không gắn với booking sân.
        if (PAYMENT_TYPE_MEMBERSHIP.equalsIgnoreCase(paymentType)) {
            processMembershipPayment(request, response, currentUser);
            return;
        }

        long bookingId = parsePositiveLong(request.getParameter("bookingId"),
                "bookingId khong hop le.");
        int paymentMethodId = parsePositiveInt(request.getParameter("paymentMethodId"),
                "Vui long chon phuong thuc thanh toan.");

        PaymentMethod selectedMethod = paymentDAO.getPaymentMethodById(paymentMethodId);
        validateOnlinePaymentMethod(selectedMethod);

        // paymentMode quyết định đi VNPay thật hay mô phỏng; payment method vẫn phải ACTIVE trong DB.
        String paymentMode = trim(request.getParameter("paymentMode"));
        String simulateStatus = trim(request.getParameter("simulateStatus"));
        boolean explicitSimulated = MODE_SIMULATED.equalsIgnoreCase(paymentMode)
                || (simulateStatus != null && !simulateStatus.isBlank());
        boolean selectedVNPayMethod = GATEWAY_VNPAY.equalsIgnoreCase(selectedMethod.getMethodCode());
        boolean useVNPay = !explicitSimulated
                && (MODE_VNPAY.equalsIgnoreCase(paymentMode) || selectedVNPayMethod);

        // Người dùng bấm nút VNPay thì method được chọn cũng phải là VNPay.
        if (MODE_VNPAY.equalsIgnoreCase(paymentMode) && !selectedVNPayMethod) {
            throw new IllegalArgumentException("Vui long chon phuong thuc VNPay Sandbox.");
        }

        // Business Rule BR-22: Với VNPay thật, tạo payment PENDING trước để callback có transactionRef đối chiếu.
        if (useVNPay) {
            processVNPayPayment(request, response, bookingId, currentUser.getUserId(), paymentMethodId);
            return;
        }

        processSimulatedPayment(request, response, bookingId, currentUser.getUserId(), paymentMethodId, simulateStatus);
    }

    /**
     * Xử lý submit thanh toán invoice checkout, bao gồm kiểm tra phương thức và chọn VNPay/mô phỏng.
     */
    private void processCheckoutPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            User currentUser
    ) throws IOException, SQLException {
        long invoiceId = parsePositiveLong(request.getParameter("invoiceId"),
                "invoiceId khong hop le.");
        int paymentMethodId = parsePositiveInt(request.getParameter("paymentMethodId"),
                "Vui long chon phuong thuc thanh toan.");

        PaymentMethod selectedMethod = paymentDAO.getPaymentMethodById(paymentMethodId);
        validateOnlinePaymentMethod(selectedMethod);

        String paymentMode = trim(request.getParameter("paymentMode"));
        String simulateStatus = trim(request.getParameter("simulateStatus"));
        boolean explicitSimulated = MODE_SIMULATED.equalsIgnoreCase(paymentMode)
                || (simulateStatus != null && !simulateStatus.isBlank());
        boolean selectedVNPayMethod = GATEWAY_VNPAY.equalsIgnoreCase(selectedMethod.getMethodCode());
        boolean useVNPay = !explicitSimulated
                && (MODE_VNPAY.equalsIgnoreCase(paymentMode) || selectedVNPayMethod);

        // Không cho gửi VNPay bằng một payment method online khác để tránh transaction sai gateway.
        if (MODE_VNPAY.equalsIgnoreCase(paymentMode) && !selectedVNPayMethod) {
            throw new IllegalArgumentException("Vui long chon phuong thuc VNPay Sandbox.");
        }

        // Business Rule BR-22: Payment checkout VNPay phải có transactionRef để callback xác minh amount/trạng thái.
        if (useVNPay) {
            processCheckoutVNPayPayment(request, response, invoiceId, currentUser.getUserId(), paymentMethodId);
            return;
        }

        processCheckoutSimulatedPayment(request, response, invoiceId, currentUser.getUserId(), paymentMethodId, simulateStatus);
    }

    /**
     * Xử lý thanh toán membership VIP.
     * Amount được lấy từ system setting trong server, không đọc từ request.
     */
    private void processMembershipPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            User currentUser
    ) throws IOException, SQLException {
        int paymentMethodId = parsePositiveInt(request.getParameter("paymentMethodId"),
                "Vui long chon phuong thuc thanh toan.");

        PaymentMethod selectedMethod = paymentDAO.getPaymentMethodById(paymentMethodId);
        validateOnlinePaymentMethod(selectedMethod);

        String paymentMode = trim(request.getParameter("paymentMode"));
        String simulateStatus = trim(request.getParameter("simulateStatus"));
        boolean explicitSimulated = MODE_SIMULATED.equalsIgnoreCase(paymentMode)
                || (simulateStatus != null && !simulateStatus.isBlank());
        boolean selectedVNPayMethod = GATEWAY_VNPAY.equalsIgnoreCase(selectedMethod.getMethodCode());
        boolean useVNPay = !explicitSimulated
                && (MODE_VNPAY.equalsIgnoreCase(paymentMode) || selectedVNPayMethod);

        // Membership VNPay cũng phải dùng đúng method VNPay để callback đối chiếu gateway rõ ràng.
        if (MODE_VNPAY.equalsIgnoreCase(paymentMode) && !selectedVNPayMethod) {
            throw new IllegalArgumentException("Vui long chon phuong thuc VNPay Sandbox.");
        }

        // VNPay membership cũng đi qua payment PENDING để callback được xác minh nhất quán.
        if (useVNPay) {
            processMembershipVNPayPayment(request, response, currentUser.getUserId(), paymentMethodId);
            return;
        }

        processMembershipSimulatedPayment(request, response, currentUser.getUserId(), paymentMethodId, simulateStatus);
    }

    /**
     * Tạo payment PENDING cho invoice checkout rồi chuyển Customer sang URL VNPay.
     */
    private void processCheckoutVNPayPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long invoiceId,
            long customerId,
            int paymentMethodId
    ) throws IOException, SQLException {
        // Kiểm tra cấu hình VNPay trước khi tạo redirect URL cho Customer.
        try {
            VNPayConfig.validateRequired();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("VNPay chua duoc cau hinh day du trong vnpay.properties.", e);
        }

        // Payment pending được tạo trước để VNPay callback có transactionRef đối chiếu về DB.
        Payment payment = paymentDAO.createPendingCheckoutPayment(
                invoiceId,
                customerId,
                paymentMethodId
        );
        VNPayUtil.PaymentUrlDebug paymentUrlDebug = VNPayUtil.buildPaymentUrlDebug(payment, payment.getBookingId(), request);
        logVNPayPaymentUrl(payment, payment.getBookingId(), paymentUrlDebug);
        response.sendRedirect(paymentUrlDebug.paymentUrl());
    }

    /**
     * Mô phỏng thanh toán checkout để demo/test mà vẫn đi qua cùng DAO cập nhật trạng thái.
     */
    private void processCheckoutSimulatedPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long invoiceId,
            long customerId,
            int paymentMethodId,
            String simulateStatus
    ) throws IOException, SQLException {
        boolean simulateFailure = "FAILED".equalsIgnoreCase(simulateStatus);

        Payment payment = paymentDAO.createPendingCheckoutPayment(
                invoiceId,
                customerId,
                paymentMethodId
        );
        String transactionRef = payment.getTransactionRef();
        String resultStatus = simulateFailure ? "FAILED" : "SUCCESS";
        String rawPayload = "{\"gateway\":\"SIMULATED\",\"paymentType\":\"CHECKOUT\",\"status\":\"" + resultStatus
                + "\",\"transactionRef\":\"" + transactionRef + "\"}";
        String signature = "SIM-" + transactionRef;

        // Demo thất bại chỉ cập nhật payment FAILED, không hoàn tất invoice/booking.
        if (simulateFailure) {
            paymentDAO.markPaymentFailed(transactionRef, rawPayload, signature);
        } else {
            // Demo thành công đi cùng đường success callback để giữ logic checkout nhất quán.
            String gatewayTransactionId = "SIM"
                    + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
            // Business Rule BR-21: Checkout payment thành công sẽ cập nhật payment/invoice/booking trong DAO.
            paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature
            );
        }

        redirectToPaymentResult(request, response, transactionRef);
    }

    /**
     * Tạo payment PENDING cho membership rồi chuyển Customer sang VNPay.
     */
    private void processMembershipVNPayPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long customerId,
            int paymentMethodId
    ) throws IOException, SQLException {
        // Membership VNPay cần đủ cấu hình gateway trước khi tạo payment URL.
        try {
            VNPayConfig.validateRequired();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("VNPay chua duoc cau hinh day du trong vnpay.properties.", e);
        }

        Payment payment = paymentDAO.createPendingMembershipPayment(
                customerId,
                paymentMethodId,
                getVipPrice()
        );
        VNPayUtil.PaymentUrlDebug paymentUrlDebug = VNPayUtil.buildPaymentUrlDebug(payment, 0L, request);
        logVNPayPaymentUrl(payment, 0L, paymentUrlDebug);
        response.sendRedirect(paymentUrlDebug.paymentUrl());
    }

    private void processMembershipSimulatedPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long customerId,
            int paymentMethodId,
            String simulateStatus
    ) throws IOException, SQLException {
        boolean simulateFailure = "FAILED".equalsIgnoreCase(simulateStatus);

        Payment payment = paymentDAO.createPendingMembershipPayment(
                customerId,
                paymentMethodId,
                getVipPrice()
        );
        String transactionRef = payment.getTransactionRef();
        String resultStatus = simulateFailure ? "FAILED" : "SUCCESS";
        String rawPayload = "{\"gateway\":\"SIMULATED\",\"paymentType\":\"MEMBERSHIP\",\"status\":\"" + resultStatus
                + "\",\"transactionRef\":\"" + transactionRef + "\"}";
        String signature = "SIM-" + transactionRef;

        // Demo membership thất bại giữ payment ở FAILED để người dùng có thể thử lại.
        if (simulateFailure) {
            paymentDAO.markPaymentFailed(transactionRef, rawPayload, signature);
        } else {
            // Demo membership thành công dùng chung hàm success để gia hạn VIP trong DAO.
            String gatewayTransactionId = "SIM"
                    + java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(java.time.LocalDateTime.now());
            // Payment membership thành công dùng chung callback success nhưng không gắn booking sân.
            paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature
            );
        }

        redirectToPaymentResult(request, response, transactionRef);
    }

    /**
     * Tạo payment PENDING cho tiền cọc booking rồi chuyển Customer sang VNPay.
     */
    private void processVNPayPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long bookingId,
            long customerId,
            int paymentMethodId
    ) throws IOException, SQLException {
        // Cấu hình thiếu thì dừng sớm, không tạo payment PENDING bị kẹt ở DB.
        try {
            VNPayConfig.validateRequired();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("VNPay chua duoc cau hinh day du trong vnpay.properties.", e);
        }

        Payment payment = paymentDAO.createPendingDepositPayment(
                bookingId,
                customerId,
                paymentMethodId
        );
        VNPayUtil.PaymentUrlDebug paymentUrlDebug = VNPayUtil.buildPaymentUrlDebug(payment, bookingId, request);
        logVNPayPaymentUrl(payment, bookingId, paymentUrlDebug);
        response.sendRedirect(paymentUrlDebug.paymentUrl());
    }

    /**
     * Mô phỏng thanh toán tiền cọc.
     * Dù là demo, flow vẫn gọi DAO success/fail để cập nhật booking và voucher giống callback thật.
     */
    private void processSimulatedPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long bookingId,
            long customerId,
            int paymentMethodId,
            String simulateStatus
    ) throws IOException, SQLException {
        boolean simulateFailure = "FAILED".equalsIgnoreCase(simulateStatus);

        Payment payment = paymentDAO.createPendingDepositPayment(
                bookingId,
                customerId,
                paymentMethodId
        );
        String transactionRef = payment.getTransactionRef();
        String resultStatus = simulateFailure ? "FAILED" : "SUCCESS";
        String rawPayload = "{\"gateway\":\"SIMULATED\",\"status\":\"" + resultStatus
                + "\",\"transactionRef\":\"" + transactionRef + "\"}";
        String signature = "SIM-" + transactionRef;

        // Demo thất bại đánh dấu FAILED giống callback gateway trả lỗi.
        if (simulateFailure) {
            paymentDAO.markPaymentFailed(transactionRef, rawPayload, signature);
        } else {
            // Demo thành công xác nhận booking; nếu DAO không xác nhận được thì chuyển payment sang FAILED.
            String gatewayTransactionId = "SIM"
                    + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
            // Business Rule BR-08: Thanh toán đặt cọc thành công cho booking HOLD sẽ xác nhận booking trong DAO.
            boolean success = paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature
            );
            // Khi booking hết hạn/trạng thái sai, payment mô phỏng không được để SUCCESS.
            if (!success) {
                paymentDAO.markPaymentFailed(
                        transactionRef,
                        rawPayload.replace("\"SUCCESS\"", "\"FAILED\""),
                        signature
                );
            }
        }

        redirectToPaymentResult(request, response, transactionRef);
    }

    /**
     * Xử lý Return URL khi trình duyệt Customer quay lại từ VNPay.
     * Return phục vụ hiển thị kết quả, nhưng vẫn kiểm chữ ký và cập nhật payment nếu đây là callback đầu tiên.
     */
    private void handleVNPayReturn(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        Map<String, String> params = VNPayUtil.extractParams(request);
        String transactionRef = trim(params.get("vnp_TxnRef"));
        // Không có transactionRef thì hệ thống không thể map callback về payment nào.
        if (transactionRef == null || transactionRef.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thieu ma giao dich VNPay.");
            return;
        }

        String rawPayload = VNPayUtil.buildRawPayload(params);
        String signature = trim(params.get("vnp_SecureHash"));
        // Business Rule BR-22: Không tin bất kỳ tham số callback nào cho tới khi chữ ký VNPay khớp với hashSecret.
        VNPayUtil.SignatureDebug signatureDebug = VNPayUtil.verifySignatureDebug(params);
        // Chữ ký sai vẫn được lưu callback để audit, nhưng không cập nhật payment.
        if (!signatureDebug.valid()) {
            logVNPaySignatureFailure("RETURN", transactionRef, signatureDebug, params);
            paymentDAO.savePaymentCallbackByTransactionRef(
                    transactionRef,
                    GATEWAY_VNPAY,
                    rawPayload,
                    signature,
                    false
            );
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Chu ky VNPay khong hop le.");
            return;
        }

        String gatewayTransactionId = trim(params.get("vnp_TransactionNo"));
        String responseCode = trim(params.get("vnp_ResponseCode"));
        String transactionStatus = trim(params.get("vnp_TransactionStatus"));

        // Business Rule BR-23: DAO xử lý idempotent nên Return/IPN lặp không tạo thêm thay đổi trạng thái.
        // VNPay trả 00/00 mới xem là thanh toán thành công.
        if (isVNPaySuccess(responseCode, transactionStatus)) {
            paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature,
                    GATEWAY_VNPAY
            );
        } else {
            // Mọi mã khác 00/00 đều được ghi nhận là payment failed.
            paymentDAO.markPaymentFailed(
                    transactionRef,
                    rawPayload,
                    signature,
                    GATEWAY_VNPAY
            );
        }

        forwardToPaymentResult(request, response, transactionRef);
    }

    /**
     * Xử lý IPN server-to-server của VNPay.
     * IPN trả mã RspCode theo chuẩn VNPay và kiểm thêm số tiền để tránh xác nhận sai giao dịch.
     */
    private void handleVNPayIpn(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        response.setContentType("application/json;charset=UTF-8");

        Map<String, String> params = VNPayUtil.extractParams(request);
        String transactionRef = trim(params.get("vnp_TxnRef"));
        String signature = trim(params.get("vnp_SecureHash"));
        String rawPayload = VNPayUtil.buildRawPayload(params);

        // Business Rule BR-22: IPN server-to-server cũng bắt buộc kiểm tra chữ ký/hash trước khi đọc amount.
        VNPayUtil.SignatureDebug signatureDebug = VNPayUtil.verifySignatureDebug(params);
        // Signature không hợp lệ thì chỉ lưu audit nếu có transactionRef.
        if (!signatureDebug.valid()) {
            logVNPaySignatureFailure("IPN", transactionRef, signatureDebug, params);
            if (transactionRef != null && !transactionRef.isBlank()) {
                paymentDAO.savePaymentCallbackByTransactionRef(
                        transactionRef,
                        GATEWAY_VNPAY,
                        rawPayload,
                        signature,
                        false
                );
            }
            writeVNPayIpnResponse(response, "97", "Invalid signature");
            return;
        }

        // VNPay IPN thiếu transactionRef thì coi như không tìm thấy order.
        if (transactionRef == null || transactionRef.isBlank()) {
            writeVNPayIpnResponse(response, "01", "Order not found");
            return;
        }

        GatewayPaymentView payment = paymentDAO.findPaymentByTransactionRef(transactionRef);
        // Không tìm thấy payment PENDING/SUCCESS/FAILED tương ứng trong DB.
        if (payment == null) {
            writeVNPayIpnResponse(response, "01", "Order not found");
            return;
        }

        // Business Rule BR-22: Số tiền VNPay gửi về phải khớp amount trong DB, không dựa vào dữ liệu từ trình duyệt.
        String requestAmount = trim(params.get("vnp_Amount"));
        String expectedAmount = VNPayUtil.toVNPayAmount(payment.amount());
        // Amount lệch thì lưu callback nhưng không xác nhận giao dịch.
        if (!expectedAmount.equals(requestAmount)) {
            paymentDAO.savePaymentCallbackByTransactionRef(
                    transactionRef,
                    GATEWAY_VNPAY,
                    rawPayload,
                    signature,
                    false
            );
            writeVNPayIpnResponse(response, "04", "Invalid amount");
            return;
        }

        String responseCode = trim(params.get("vnp_ResponseCode"));
        String transactionStatus = trim(params.get("vnp_TransactionStatus"));
        String gatewayTransactionId = trim(params.get("vnp_TransactionNo"));

        // Business Rule BR-23: DAO xử lý idempotent, callback lặp không làm đổi lại booking/invoice đã xử lý.
        PaymentUpdateResult result;
        // IPN success đi vào nhánh hoàn tất payment; các trạng thái còn lại đánh dấu thất bại.
        if (isVNPaySuccess(responseCode, transactionStatus)) {
            result = paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature,
                    GATEWAY_VNPAY
            );
        } else {
            // Gateway trả lỗi hoặc giao dịch bị hủy thì cập nhật payment failed theo transactionRef.
            result = paymentDAO.markPaymentFailed(
                    transactionRef,
                    rawPayload,
                    signature,
                    GATEWAY_VNPAY
            );
        }

        switch (result) {
            case UPDATED_SUCCESS, UPDATED_FAILED -> writeVNPayIpnResponse(response, "00", "Confirm Success");
            case ALREADY_SUCCESS -> writeVNPayIpnResponse(response, "02", "Order already confirmed");
            case ALREADY_FAILED -> writeVNPayIpnResponse(response, "02", "Order already processed");
            case NOT_FOUND -> writeVNPayIpnResponse(response, "01", "Order not found");
            case INVALID_STATE -> writeVNPayIpnResponse(response, "99", "Invalid order state");
        }
    }

    /**
     * Hiển thị kết quả payment cho Customer đang đăng nhập.
     */
    private void showPaymentResult(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        User currentUser = requireLogin(request, response);
        // Không có session thì requireLogin đã redirect, dừng render result.
        if (currentUser == null) {
            return;
        }

        String transactionRef = requireText(request.getParameter("transactionRef"),
                "Ma giao dich khong hop le.");
        PaymentView payment = paymentDAO.getPaymentResult(transactionRef, currentUser.getUserId());
        // Result page chỉ hiển thị giao dịch thuộc Customer hiện tại.
        if (payment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay giao dich.");
            return;
        }

        request.setAttribute("payment", payment);
        request.getRequestDispatcher("/WEB-INF/payment/payment-result.jsp").forward(request, response);
    }

    private void forwardToPaymentResult(
            HttpServletRequest request,
            HttpServletResponse response,
            String transactionRef
    ) throws IOException, ServletException, SQLException {
        PaymentView payment = paymentDAO.getPaymentResultByTransactionRef(transactionRef);
        // Return URL có thể mở trực tiếp sau callback, nên lookup theo transactionRef.
        if (payment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay giao dich.");
            return;
        }

        request.setAttribute("payment", payment);
        request.getRequestDispatcher("/WEB-INF/payment/payment-result.jsp").forward(request, response);
    }

    /**
     * Hiển thị lịch sử thanh toán của Customer hiện tại.
     */
    private void showPaymentHistory(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        User currentUser = requireLogin(request, response);
        // History chỉ dành cho Customer đã đăng nhập.
        if (currentUser == null) {
            return;
        }

        request.setAttribute("payments", paymentDAO.getPaymentHistory(currentUser.getUserId()));
        request.getRequestDispatcher("/WEB-INF/payment/payment-history.jsp").forward(request, response);
    }

    private User requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        // Không có user trong session thì chuyển về login trước khi cho thanh toán.
        if (session == null || !(session.getAttribute("user") instanceof User user)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        return user;
    }

    private void validateOnlinePaymentMethod(PaymentMethod selectedMethod) {
        // Method phải tồn tại và ACTIVE để tránh submit id cũ/ẩn từ client.
        if (selectedMethod == null || !"ACTIVE".equalsIgnoreCase(selectedMethod.getStatus())) {
            throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
        }
        // Flow Customer online không nhận CASH; tiền mặt chỉ do Staff ghi nhận tại quầy checkout.
        if (METHOD_CASH.equalsIgnoreCase(selectedMethod.getMethodCode())) {
            throw new IllegalArgumentException("Phuong thuc tien mat chi duoc Staff ghi nhan tai quay Check-out.");
        }
    }

    private void redirectToMethodWithError(
            HttpServletRequest request,
            HttpServletResponse response,
            String message
    ) throws IOException {
        String paymentType = trim(request.getParameter("paymentType"));
        // Nếu lỗi ở checkout, redirect về đúng invoice để Customer sửa chọn method.
        if (PAYMENT_TYPE_CHECKOUT.equalsIgnoreCase(paymentType)) {
            String rawInvoiceId = trim(request.getParameter("invoiceId"));
            // invoiceId không hợp lệ thì không dựng URL redirect từ input lỗi.
            if (rawInvoiceId == null || !rawInvoiceId.matches("\\d+")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                return;
            }
            response.sendRedirect(request.getContextPath()
                    + "/payment?action=method&type=checkout&invoiceId=" + rawInvoiceId
                    + "&error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
            return;
        }
        // Membership không cần id nên redirect về màn chọn gói.
        if (PAYMENT_TYPE_MEMBERSHIP.equalsIgnoreCase(paymentType)) {
            response.sendRedirect(request.getContextPath()
                    + "/payment?action=method&type=membership"
                    + "&error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
            return;
        }

        String rawBookingId = trim(request.getParameter("bookingId"));
        // Deposit cần bookingId hợp lệ để quay lại đúng booking đang thanh toán.
        if (rawBookingId == null || !rawBookingId.matches("\\d+")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            return;
        }

        response.sendRedirect(request.getContextPath()
                + "/payment?action=method&bookingId=" + rawBookingId
                + "&error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private void redirectToPaymentResult(
            HttpServletRequest request,
            HttpServletResponse response,
            String transactionRef
    ) throws IOException {
        response.sendRedirect(request.getContextPath()
                + "/payment?action=result&transactionRef="
                + URLEncoder.encode(transactionRef, StandardCharsets.UTF_8));
    }

    private void handleDatabaseError(HttpServletResponse response, SQLException error) throws IOException {
        getServletContext().log("Payment processing failed", error);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Khong the xu ly thanh toan luc nay.");
    }

    private void handleUnexpectedError(HttpServletResponse response, RuntimeException error) throws IOException {
        getServletContext().log("Unexpected payment error", error);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Khong the xu ly thanh toan luc nay.");
    }

    private void handleVNPayIpnError(HttpServletResponse response, Exception error) throws IOException {
        getServletContext().log("VNPay IPN processing failed", error);
        response.setContentType("application/json;charset=UTF-8");
        writeVNPayIpnResponse(response, "99", "Unknown error");
    }

    private void logVNPayPaymentUrl(
            Payment payment,
            long bookingId,
            VNPayUtil.PaymentUrlDebug debug
    ) {
        StringBuilder log = new StringBuilder();
        log.append("[VNPAY_CREATE] bookingId=").append(bookingId)
                .append(", paymentId=").append(payment.getPaymentId())
                .append(", transactionRef=").append(payment.getTransactionRef())
                .append(", amount=").append(payment.getAmount())
                .append(", tmnCode=").append(VNPayConfig.getTmnCode())
                .append(", hashSecretLength=").append(VNPayConfig.getHashSecret().length())
                .append(", payUrl=").append(VNPayConfig.getPayUrl())
                .append(", sortedParams=").append(debug.sortedParams())
                .append(", query=").append(debug.query())
                .append(", hashData=").append(debug.hashData())
                .append(", secureHash=").append(debug.secureHash())
                .append(", redirectUrl=").append(maskSecureHash(debug.paymentUrl()));
        getServletContext().log(log.toString());
    }

    private void logVNPaySignatureFailure(
            String source,
            String transactionRef,
            VNPayUtil.SignatureDebug debug,
            Map<String, String> params
    ) {
        StringBuilder log = new StringBuilder();
        log.append("[VNPAY_").append(source).append("_INVALID_SIGNATURE]")
                .append(" transactionRef=").append(transactionRef)
                .append(", params=").append(params)
                .append(", hashData=").append(debug.hashData())
                .append(", expectedHash=").append(debug.expectedHash())
                .append(", receivedHash=").append(debug.receivedHash());
        getServletContext().log(log.toString());
    }

    private void writeVNPayIpnResponse(HttpServletResponse response, String code, String message)
            throws IOException {
        response.getWriter().write("{\"RspCode\":\"" + escapeJson(code)
                + "\",\"Message\":\"" + escapeJson(message) + "\"}");
    }

    private boolean isVNPaySuccess(String responseCode, String transactionStatus) {
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }

    private boolean isVNPayIpn(HttpServletRequest request) {
        return "/payment/vnpay-ipn".equals(request.getServletPath());
    }

    private long parsePositiveLong(String rawValue, String message) {
        String value = requireText(rawValue, message);
        // Parse id dạng long và chặn số âm/0 ngay tại controller.
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private int parsePositiveInt(String rawValue, String message) {
        String value = requireText(rawValue, message);
        // Parse id dạng int cho paymentMethodId và chặn giá trị không dương.
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private String requireText(String rawValue, String message) {
        String value = trim(rawValue);
        // Các tham số bắt buộc không được null hoặc chuỗi rỗng sau trim.
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String firstNonBlank(String first, String second) {
        String value = trim(first);
        return value == null || value.isEmpty() ? second : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String escapeJson(String value) {
        // Null được encode thành chuỗi rỗng để response JSON VNPay luôn hợp lệ.
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String maskSecureHash(String url) {
        // Không log chữ ký thật của VNPay ra servlet log.
        if (url == null) {
            return "";
        }
        return url.replaceAll("(vnp_SecureHash=)[^&]+", "$1***");
    }
}

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

@WebServlet(name = "PaymentController", urlPatterns = {
        "/payment",
        "/payment/vnpay-return",
        "/payment/vnpay-ipn"
})
public class PaymentController extends HttpServlet {

    private static final String GATEWAY_VNPAY = "VNPAY";
    private static final String MODE_VNPAY = "VNPAY";
    private static final String MODE_SIMULATED = "SIMULATED";
    private static final String PAYMENT_TYPE_CHECKOUT = "CHECKOUT";
    private static final String PAYMENT_TYPE_MEMBERSHIP = "MEMBERSHIP";

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final SystemSettingDAO systemSettingDAO = new SystemSettingDAO();

    private java.math.BigDecimal getVipPrice() {
        Optional<SystemSetting> vipSetting = systemSettingDAO.getSettingByKey("VIP_SUBSCRIPTION_PRICE_MONTHLY");
        if (vipSetting.isPresent() && vipSetting.get().getSettingValue() != null) {
            try {
                return new java.math.BigDecimal(vipSetting.get().getSettingValue().replaceAll("[^0-9]", ""));
            } catch(Exception e) {}
        }
        return new java.math.BigDecimal("199000");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String servletPath = request.getServletPath();

        try {
            if ("/payment/vnpay-return".equals(servletPath)) {
                handleVNPayReturn(request, response);
                return;
            }
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
            if (isVNPayIpn(request)) {
                handleVNPayIpnError(response, e);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        } catch (SQLException e) {
            if (isVNPayIpn(request)) {
                handleVNPayIpnError(response, e);
            } else {
                handleDatabaseError(response, e);
            }
        } catch (RuntimeException e) {
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
        if (!"pay".equals(action)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

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

    private void showPaymentMethod(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        String type = trim(request.getParameter("type"));
        if (PAYMENT_TYPE_CHECKOUT.equalsIgnoreCase(type)) {
            showCheckoutPaymentMethod(request, response, currentUser);
            return;
        }
        if (PAYMENT_TYPE_MEMBERSHIP.equalsIgnoreCase(type)) {
            showMembershipPaymentMethod(request, response, currentUser);
            return;
        }

        long bookingId = parsePositiveLong(firstNonBlank(
                request.getParameter("bookingId"),
                request.getParameter("id")
        ), "bookingId khong hop le.");
        BookingView booking = paymentDAO.getBookingForPayment(bookingId, currentUser.getUserId());
        if (booking == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Khong tim thay booking con hieu luc de thanh toan.");
            return;
        }

        List<PaymentMethod> methods = paymentDAO.getActivePaymentMethods();
        request.setAttribute("booking", booking);
        request.setAttribute("paymentContext", "DEPOSIT");
        request.setAttribute("paymentMethods", methods);
        request.setAttribute("error", request.getParameter("error"));
        request.getRequestDispatcher("/WEB-INF/payment/payment.jsp").forward(request, response);
    }

    private void showMembershipPaymentMethod(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, ServletException, SQLException {
        List<PaymentMethod> methods = paymentDAO.getActivePaymentMethods();
        request.setAttribute("paymentContext", PAYMENT_TYPE_MEMBERSHIP);
        request.setAttribute("amountToPay", getVipPrice());
        request.setAttribute("paymentMethods", methods);
        request.setAttribute("error", request.getParameter("error"));
        request.getRequestDispatcher("/WEB-INF/payment/payment.jsp").forward(request, response);
    }

    private void showCheckoutPaymentMethod(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException, ServletException, SQLException {
        long invoiceId = parsePositiveLong(request.getParameter("invoiceId"),
                "invoiceId khong hop le.");
        InvoiceView invoice = paymentDAO.getCheckoutInvoiceForPayment(invoiceId, currentUser.getUserId());
        if (invoice == null || !"PENDING".equals(invoice.getInvoiceStatus())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Khong tim thay hoa don checkout dang cho thanh toan.");
            return;
        }

        List<PaymentMethod> methods = paymentDAO.getActivePaymentMethods();
        request.setAttribute("paymentContext", PAYMENT_TYPE_CHECKOUT);
        request.setAttribute("invoice", invoice);
        request.setAttribute("amountToPay", invoice.getTotalAmount());
        request.setAttribute("paymentMethods", methods);
        request.setAttribute("error", request.getParameter("error"));
        request.getRequestDispatcher("/WEB-INF/payment/payment.jsp").forward(request, response);
    }

    private void processPayment(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        String paymentType = trim(request.getParameter("paymentType"));
        if (PAYMENT_TYPE_CHECKOUT.equalsIgnoreCase(paymentType)) {
            processCheckoutPayment(request, response, currentUser);
            return;
        }
        if (PAYMENT_TYPE_MEMBERSHIP.equalsIgnoreCase(paymentType)) {
            processMembershipPayment(request, response, currentUser);
            return;
        }

        long bookingId = parsePositiveLong(request.getParameter("bookingId"),
                "bookingId khong hop le.");
        int paymentMethodId = parsePositiveInt(request.getParameter("paymentMethodId"),
                "Vui long chon phuong thuc thanh toan.");

        PaymentMethod selectedMethod = paymentDAO.getPaymentMethodById(paymentMethodId);
        if (selectedMethod == null || !"ACTIVE".equalsIgnoreCase(selectedMethod.getStatus())) {
            throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
        }

        String paymentMode = trim(request.getParameter("paymentMode"));
        String simulateStatus = trim(request.getParameter("simulateStatus"));
        boolean explicitSimulated = MODE_SIMULATED.equalsIgnoreCase(paymentMode)
                || (simulateStatus != null && !simulateStatus.isBlank());
        boolean selectedVNPayMethod = GATEWAY_VNPAY.equalsIgnoreCase(selectedMethod.getMethodCode());
        boolean useVNPay = !explicitSimulated
                && (MODE_VNPAY.equalsIgnoreCase(paymentMode) || selectedVNPayMethod);

        if (MODE_VNPAY.equalsIgnoreCase(paymentMode) && !selectedVNPayMethod) {
            throw new IllegalArgumentException("Vui long chon phuong thuc VNPay Sandbox.");
        }

        if (useVNPay) {
            processVNPayPayment(request, response, bookingId, currentUser.getUserId(), paymentMethodId);
            return;
        }

        processSimulatedPayment(request, response, bookingId, currentUser.getUserId(), paymentMethodId, simulateStatus);
    }

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
        if (selectedMethod == null || !"ACTIVE".equalsIgnoreCase(selectedMethod.getStatus())) {
            throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
        }

        String paymentMode = trim(request.getParameter("paymentMode"));
        String simulateStatus = trim(request.getParameter("simulateStatus"));
        boolean explicitSimulated = MODE_SIMULATED.equalsIgnoreCase(paymentMode)
                || (simulateStatus != null && !simulateStatus.isBlank());
        boolean selectedVNPayMethod = GATEWAY_VNPAY.equalsIgnoreCase(selectedMethod.getMethodCode());
        boolean useVNPay = !explicitSimulated
                && (MODE_VNPAY.equalsIgnoreCase(paymentMode) || selectedVNPayMethod);

        if (MODE_VNPAY.equalsIgnoreCase(paymentMode) && !selectedVNPayMethod) {
            throw new IllegalArgumentException("Vui long chon phuong thuc VNPay Sandbox.");
        }

        if (useVNPay) {
            processCheckoutVNPayPayment(request, response, invoiceId, currentUser.getUserId(), paymentMethodId);
            return;
        }

        processCheckoutSimulatedPayment(request, response, invoiceId, currentUser.getUserId(), paymentMethodId, simulateStatus);
    }

    private void processMembershipPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            User currentUser
    ) throws IOException, SQLException {
        int paymentMethodId = parsePositiveInt(request.getParameter("paymentMethodId"),
                "Vui long chon phuong thuc thanh toan.");

        PaymentMethod selectedMethod = paymentDAO.getPaymentMethodById(paymentMethodId);
        if (selectedMethod == null || !"ACTIVE".equalsIgnoreCase(selectedMethod.getStatus())) {
            throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
        }

        String paymentMode = trim(request.getParameter("paymentMode"));
        String simulateStatus = trim(request.getParameter("simulateStatus"));
        boolean explicitSimulated = MODE_SIMULATED.equalsIgnoreCase(paymentMode)
                || (simulateStatus != null && !simulateStatus.isBlank());
        boolean selectedVNPayMethod = GATEWAY_VNPAY.equalsIgnoreCase(selectedMethod.getMethodCode());
        boolean useVNPay = !explicitSimulated
                && (MODE_VNPAY.equalsIgnoreCase(paymentMode) || selectedVNPayMethod);

        if (MODE_VNPAY.equalsIgnoreCase(paymentMode) && !selectedVNPayMethod) {
            throw new IllegalArgumentException("Vui long chon phuong thuc VNPay Sandbox.");
        }

        if (useVNPay) {
            processMembershipVNPayPayment(request, response, currentUser.getUserId(), paymentMethodId);
            return;
        }

        processMembershipSimulatedPayment(request, response, currentUser.getUserId(), paymentMethodId, simulateStatus);
    }

    private void processCheckoutVNPayPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long invoiceId,
            long customerId,
            int paymentMethodId
    ) throws IOException, SQLException {
        try {
            VNPayConfig.validateRequired();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("VNPay chua duoc cau hinh day du trong vnpay.properties.", e);
        }

        Payment payment = paymentDAO.createPendingCheckoutPayment(
                invoiceId,
                customerId,
                paymentMethodId
        );
        VNPayUtil.PaymentUrlDebug paymentUrlDebug = VNPayUtil.buildPaymentUrlDebug(payment, payment.getBookingId(), request);
        logVNPayPaymentUrl(payment, payment.getBookingId(), paymentUrlDebug);
        response.sendRedirect(paymentUrlDebug.paymentUrl());
    }

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

        if (simulateFailure) {
            paymentDAO.markPaymentFailed(transactionRef, rawPayload, signature);
        } else {
            String gatewayTransactionId = "SIM"
                    + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
            paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature
            );
        }

        redirectToPaymentResult(request, response, transactionRef);
    }

    private void processMembershipVNPayPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long customerId,
            int paymentMethodId
    ) throws IOException, SQLException {
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

        if (simulateFailure) {
            paymentDAO.markPaymentFailed(transactionRef, rawPayload, signature);
        } else {
            String gatewayTransactionId = "SIM"
                    + java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(java.time.LocalDateTime.now());
            paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature
            );
        }

        redirectToPaymentResult(request, response, transactionRef);
    }

    private void processVNPayPayment(
            HttpServletRequest request,
            HttpServletResponse response,
            long bookingId,
            long customerId,
            int paymentMethodId
    ) throws IOException, SQLException {
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

        if (simulateFailure) {
            paymentDAO.markPaymentFailed(transactionRef, rawPayload, signature);
        } else {
            String gatewayTransactionId = "SIM"
                    + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
            boolean success = paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature
            );
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

    private void handleVNPayReturn(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        Map<String, String> params = VNPayUtil.extractParams(request);
        String transactionRef = trim(params.get("vnp_TxnRef"));
        if (transactionRef == null || transactionRef.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thieu ma giao dich VNPay.");
            return;
        }

        String rawPayload = VNPayUtil.buildRawPayload(params);
        String signature = trim(params.get("vnp_SecureHash"));
        VNPayUtil.SignatureDebug signatureDebug = VNPayUtil.verifySignatureDebug(params);
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

        if (isVNPaySuccess(responseCode, transactionStatus)) {
            paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature,
                    GATEWAY_VNPAY
            );
        } else {
            paymentDAO.markPaymentFailed(
                    transactionRef,
                    rawPayload,
                    signature,
                    GATEWAY_VNPAY
            );
        }

        forwardToPaymentResult(request, response, transactionRef);
    }

    private void handleVNPayIpn(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        response.setContentType("application/json;charset=UTF-8");

        Map<String, String> params = VNPayUtil.extractParams(request);
        String transactionRef = trim(params.get("vnp_TxnRef"));
        String signature = trim(params.get("vnp_SecureHash"));
        String rawPayload = VNPayUtil.buildRawPayload(params);

        VNPayUtil.SignatureDebug signatureDebug = VNPayUtil.verifySignatureDebug(params);
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

        if (transactionRef == null || transactionRef.isBlank()) {
            writeVNPayIpnResponse(response, "01", "Order not found");
            return;
        }

        GatewayPaymentView payment = paymentDAO.findPaymentByTransactionRef(transactionRef);
        if (payment == null) {
            writeVNPayIpnResponse(response, "01", "Order not found");
            return;
        }

        String requestAmount = trim(params.get("vnp_Amount"));
        String expectedAmount = VNPayUtil.toVNPayAmount(payment.amount());
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

        PaymentUpdateResult result;
        if (isVNPaySuccess(responseCode, transactionStatus)) {
            result = paymentDAO.markPaymentSuccessAndConfirmBooking(
                    transactionRef,
                    gatewayTransactionId,
                    rawPayload,
                    signature,
                    GATEWAY_VNPAY
            );
        } else {
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

    private void showPaymentResult(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        String transactionRef = requireText(request.getParameter("transactionRef"),
                "Ma giao dich khong hop le.");
        PaymentView payment = paymentDAO.getPaymentResult(transactionRef, currentUser.getUserId());
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
        if (payment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay giao dich.");
            return;
        }

        request.setAttribute("payment", payment);
        request.getRequestDispatcher("/WEB-INF/payment/payment-result.jsp").forward(request, response);
    }

    private void showPaymentHistory(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        request.setAttribute("payments", paymentDAO.getPaymentHistory(currentUser.getUserId()));
        request.getRequestDispatcher("/WEB-INF/payment/payment-history.jsp").forward(request, response);
    }

    private User requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof User user)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        return user;
    }

    private void redirectToMethodWithError(
            HttpServletRequest request,
            HttpServletResponse response,
            String message
    ) throws IOException {
        String paymentType = trim(request.getParameter("paymentType"));
        if (PAYMENT_TYPE_CHECKOUT.equalsIgnoreCase(paymentType)) {
            String rawInvoiceId = trim(request.getParameter("invoiceId"));
            if (rawInvoiceId == null || !rawInvoiceId.matches("\\d+")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                return;
            }
            response.sendRedirect(request.getContextPath()
                    + "/payment?action=method&type=checkout&invoiceId=" + rawInvoiceId
                    + "&error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
            return;
        }

        String rawBookingId = trim(request.getParameter("bookingId"));
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
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String maskSecureHash(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(vnp_SecureHash=)[^&]+", "$1***");
    }
}

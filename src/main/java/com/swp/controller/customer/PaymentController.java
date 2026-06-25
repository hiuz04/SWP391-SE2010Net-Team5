package com.swp.controller.customer;

import com.swp.dao.PaymentDAO;
import com.swp.model.Payment;
import com.swp.model.PaymentMethod;
import com.swp.model.User;
import com.swp.model.dto.BookingView;
import com.swp.model.dto.PaymentView;
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
import java.util.Locale;

@WebServlet(name = "PaymentController", urlPatterns = {"/payment"})
public class PaymentController extends HttpServlet {

    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            String action = trim(request.getParameter("action"));
            switch (action == null || action.isEmpty() ? "history" : action) {
                case "method" -> showPaymentMethod(request, response);
                case "result" -> showPaymentResult(request, response);
                case "history" -> showPaymentHistory(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            handleDatabaseError(response, e);
        } catch (RuntimeException e) {
            handleUnexpectedError(response, e);
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

        long bookingId = parsePositiveLong(firstNonBlank(
                request.getParameter("bookingId"),
                request.getParameter("id")
        ), "bookingId kh\u00f4ng h\u1ee3p l\u1ec7.");
        BookingView booking = paymentDAO.getBookingForPayment(bookingId, currentUser.getUserId());
        if (booking == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Kh\u00f4ng t\u00ecm th\u1ea5y booking c\u00f2n hi\u1ec7u l\u1ef1c \u0111\u1ec3 thanh to\u00e1n.");
            return;
        }

        List<PaymentMethod> methods = paymentDAO.getActivePaymentMethods();
        request.setAttribute("booking", booking);
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

        long bookingId = parsePositiveLong(request.getParameter("bookingId"),
                "bookingId kh\u00f4ng h\u1ee3p l\u1ec7.");
        int paymentMethodId = parsePositiveInt(request.getParameter("paymentMethodId"),
                "Vui l\u00f2ng ch\u1ecdn ph\u01b0\u01a1ng th\u1ee9c thanh to\u00e1n.");
        String simulateStatus = trim(request.getParameter("simulateStatus"));
        boolean simulateFailure = "FAILED".equalsIgnoreCase(simulateStatus);

        Payment payment = paymentDAO.createPendingDepositPayment(
                bookingId,
                currentUser.getUserId(),
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
            if (success) {
                try {
                    com.swp.dao.NotificationDAO notificationDAO = new com.swp.dao.NotificationDAO();
                    com.swp.model.Notification notification = new com.swp.model.Notification();
                    notification.setUserId(currentUser.getUserId());
                    notification.setTitle("Đặt sân thành công");
                    notification.setMessage("Bạn đã thanh toán thành công và đặt sân hoàn tất. Mã giao dịch: " + transactionRef);
                    notification.setNotificationType("BOOKING");
                    notification.setReferenceId(bookingId);
                    notificationDAO.insertNotification(notification);
                } catch (Exception ignored) {
                }
            } else {
                paymentDAO.markPaymentFailed(
                        transactionRef,
                        rawPayload.replace("\"SUCCESS\"", "\"FAILED\""),
                        signature
                );
            }
        }

        response.sendRedirect(request.getContextPath()
                + "/payment?action=result&transactionRef="
                + URLEncoder.encode(transactionRef, StandardCharsets.UTF_8));
    }

    private void showPaymentResult(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        User currentUser = requireLogin(request, response);
        if (currentUser == null) {
            return;
        }

        String transactionRef = requireText(request.getParameter("transactionRef"),
                "M\u00e3 giao d\u1ecbch kh\u00f4ng h\u1ee3p l\u1ec7.");
        PaymentView payment = paymentDAO.getPaymentResult(transactionRef, currentUser.getUserId());
        if (payment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Kh\u00f4ng t\u00ecm th\u1ea5y giao d\u1ecbch.");
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
        String rawBookingId = trim(request.getParameter("bookingId"));
        if (rawBookingId == null || !rawBookingId.matches("\\d+")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            return;
        }

        response.sendRedirect(request.getContextPath()
                + "/payment?action=method&bookingId=" + rawBookingId
                + "&error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private void handleDatabaseError(HttpServletResponse response, SQLException error) throws IOException {
        getServletContext().log("Payment processing failed", error);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Kh\u00f4ng th\u1ec3 x\u1eed l\u00fd thanh to\u00e1n l\u00fac n\u00e0y.");
    }

    private void handleUnexpectedError(HttpServletResponse response, RuntimeException error) throws IOException {
        getServletContext().log("Unexpected payment error", error);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Kh\u00f4ng th\u1ec3 x\u1eed l\u00fd thanh to\u00e1n l\u00fac n\u00e0y.");
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
}

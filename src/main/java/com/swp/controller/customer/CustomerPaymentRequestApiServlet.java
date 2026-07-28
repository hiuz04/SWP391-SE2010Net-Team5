package com.swp.controller.customer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.swp.dao.PaymentDAO;
import com.swp.model.User;
import com.swp.model.dto.CheckoutPaymentRequestView;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API polling nhẹ để Customer nhận popup thanh toán checkout khi Staff gửi yêu cầu online.
 */
@WebServlet("/api/customer/pending-payment-requests")
public class CustomerPaymentRequestApiServlet extends HttpServlet {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = getSessionUser(request);
        // API polling chỉ trả dữ liệu cho Customer đã đăng nhập.
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        // Đọc danh sách payment request và serialize JSON cho popup checkout.
        try {
            List<CheckoutPaymentRequestView> requests =
                    paymentDAO.getPendingCheckoutPaymentRequests(user.getUserId());
            Map<String, Object> payload = new HashMap<>();
            payload.put("requests", requests);
            response.getWriter().write(gson.toJson(payload));
        } catch (SQLException e) {
            // Lỗi DB được log và API trả JSON lỗi chung.
            getServletContext().log("Cannot load pending checkout payment requests", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal Server Error\"}");
        }
    }

    private User getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}

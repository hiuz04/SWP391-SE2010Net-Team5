package com.swp.controller.customer;

import com.google.gson.*;
import com.swp.model.User;
import com.swp.model.dto.UserVoucherDTO;
import com.swp.model.dto.VoucherExchangeDTO;
import com.swp.service.VoucherUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@WebServlet("/vouchers-api")
public class VoucherAPIController extends HttpServlet {

    private static final VoucherUserService voucherService = new VoucherUserService();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalTime.class,
                    (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                            src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString()))
            .create();

    @Override
    /**
     * Trả dữ liệu JSON cho kho voucher đổi điểm hoặc danh sách voucher của Customer.
     * Request phải có session đăng nhập; dữ liệu lọc theo tham số `to` và `status/type`.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String page = req.getParameter("to");

        if("center".equals(page)) {
            getVoucherExchange(req,resp);
        }

        if("owned".equals(page)) {
            getMyVoucher(req,resp);
        }

    }

    /**
     * Lấy danh sách voucher còn hiệu lực để Customer có thể đổi bằng điểm thưởng.
     * Kết quả trả về kèm số điểm hiện tại của user trong session.
     */
    private void getVoucherExchange(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User user = (User) req.getSession().getAttribute("user");

        // Business Rule BR-01: Customer phải đăng nhập trước khi xem kho voucher đổi điểm.
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Bạn chưa đăng nhập.\"}");
            return;
        }

        String targetUser = req.getParameter("type");
        if (targetUser == null || targetUser.isBlank()) {
            targetUser = "ALL_TYPE";
        }

        try {
            List<VoucherExchangeDTO> vouchers = voucherService.getExchangeVouchers(targetUser);

            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("point", user.getRewardPoints());
            json.add("data", GSON.toJsonTree(vouchers));

            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Đã có lỗi xảy ra.\"}");
        }
    }

    /**
     * Lấy các voucher đã thuộc về Customer hiện tại.
     * Tham số status chỉ lọc dữ liệu hiển thị, không thay đổi trạng thái voucher.
     */
    private void getMyVoucher(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        User user = (User) req.getSession().getAttribute("user");

        // Business Rule BR-01: Customer phải đăng nhập trước khi xem voucher thuộc tài khoản mình.
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Bạn chưa đăng nhập.\"}");
            return;
        }

        String status = req.getParameter("status");
        if (status == null || status.isBlank()) {
            status = "ALL";
        }

        try {
            List<UserVoucherDTO> vouchers = voucherService.getUserVouchers(user.getUserId(), status);

            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.add("data", GSON.toJsonTree(vouchers));

            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Đã có lỗi xảy ra.\"}");
        }
    }

}

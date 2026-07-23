package com.swp.controller.customer;

import com.google.gson.JsonObject;
import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.service.VoucherUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/vouchers")
public class VoucherController extends HttpServlet {

    private static final VoucherUserService voucherService = new VoucherUserService();
    private static final UserDAO userDao = new UserDAO();

    @Override
    /**
     * Hiển thị trang kho voucher hoặc voucher cá nhân của Customer.
     * Method chỉ forward JSP sau khi session đăng nhập đã được xác nhận.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Business Rule BR-01: Customer phải đăng nhập trước khi mở kho hoặc danh sách voucher cá nhân.
        User currentUser = requireLogin(req, resp);
        // Nếu chưa đăng nhập thì requireLogin đã chuyển hướng sang trang login.
        if (currentUser == null) {
            return;
        }

        String page = req.getParameter("to");

        if("center".equals(page)) {
            req.getRequestDispatcher("/WEB-INF/customer/voucher-center.jsp")
                    .forward(req, resp);
        }

        if("owned".equals(page)) {
            req.getRequestDispatcher("/WEB-INF/customer/my-voucher.jsp")
                    .forward(req, resp);
        }

    }

    @Override
    /**
     * Nhận thao tác đổi voucher bằng điểm thưởng.
     * Method validate đăng nhập và chuyển phần kiểm tra điểm/số lượng xuống service/DAO.
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Business Rule BR-01: Customer phải đăng nhập trước khi đổi voucher.
        User currentUser = requireLogin(req, resp);
        // Nếu chưa đăng nhập thì requireLogin đã chuyển hướng sang trang login.
        if (currentUser == null) {
            return;
        }

        String action = req.getParameter("action");

        if("redeem".equals(action)) {
            redeemVoucher(req, resp, currentUser);
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

    /**
     * Parse voucherId và trả JSON kết quả đổi voucher.
     * Nếu đổi thành công, điểm thưởng mới được nạp lại vào session để navbar/UI hiển thị đúng.
     */
    private void redeemVoucher(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject json = new JsonObject();

        long voucherId;
        try {
            String voucherIdParam = request.getParameter("voucherId");
            if (voucherIdParam == null || voucherIdParam.isBlank()) {
                throw new NumberFormatException("missing voucherId");
            }
            voucherId = Long.parseLong(voucherIdParam);
            if (voucherId <= 0) {
                throw new NumberFormatException("invalid voucherId");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "voucherId không hợp lệ.");
            response.getWriter().write(json.toString());
            return;
        }

        try {
            boolean success = voucherService.redeemVoucher(currentUser, voucherId);

            if (success) {
                int updatedPoints = userDao.getAvailableRewardPoints(currentUser.getUserId());
                currentUser.setRewardPoints(updatedPoints);
                request.getSession().setAttribute("user", currentUser);

                json.addProperty("success", true);
                json.addProperty("newPoints", updatedPoints);
                json.addProperty("message", "Đổi voucher thành công.");
            } else {
                json.addProperty("success", false);
                json.addProperty("message", "Không thể đổi voucher.");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // Log chi tiết ở server, không trả message gốc ra ngoài
            e.printStackTrace();
            json.addProperty("success", false);
            json.addProperty("message", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.getWriter().write(json.toString());
    }
}

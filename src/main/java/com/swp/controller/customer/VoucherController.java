package com.swp.controller.customer;

import com.google.gson.JsonObject;
import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.model.dto.VoucherRedeemResult;
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
        if (!"CUSTOMER".equalsIgnoreCase(currentUser.getRoleName())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập kho voucher.");
            return;
        }

        // Cập nhật lại điểm thưởng mới nhất từ CSDL vào session để tránh trễ điểm khi có sự thay đổi
        int updatedPoints = userDao.getAvailableRewardPoints(currentUser.getUserId());
        currentUser.setRewardPoints(updatedPoints);
        req.getSession().setAttribute("user", currentUser);

        String page = req.getParameter("to");

        // to=center mở kho voucher đổi điểm cho Customer.
        if("center".equals(page)) {
            req.getRequestDispatcher("/WEB-INF/customer/voucher-center.jsp")
                    .forward(req, resp);
        }

        // to=owned mở danh sách voucher Customer đã đổi/sở hữu.
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
        if (!"CUSTOMER".equalsIgnoreCase(currentUser.getRoleName())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền đổi voucher.");
            return;
        }

        String action = req.getParameter("action");

        // POST hiện chỉ hỗ trợ action redeem để đổi voucher bằng điểm thưởng.
        if("redeem".equals(action)) {
            redeemVoucher(req, resp, currentUser);
        }
    }

    private User requireLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        // Không có session/user thì redirect login và trả null cho caller dừng flow.
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
        // voucherId từ request phải parse được và là số dương trước khi gọi service.
        try {
            String voucherIdParam = request.getParameter("voucherId");
            // Thiếu voucherId thì coi như input không hợp lệ.
            if (voucherIdParam == null || voucherIdParam.isBlank()) {
                throw new NumberFormatException("missing voucherId");
            }
            voucherId = Long.parseLong(voucherIdParam);
            // Id <= 0 không thể là voucher hợp lệ trong DB.
            if (voucherId <= 0) {
                throw new NumberFormatException("invalid voucherId");
            }
        } catch (NumberFormatException e) {
            // Lỗi parse trả BAD_REQUEST dạng JSON để fetch phía client xử lý được.
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("success", false);
            json.addProperty("message", "voucherId không hợp lệ.");
            response.getWriter().write(json.toString());
            return;
        }

        // Service/DAO xử lý kiểm điểm, số lượng, VIP và cấp voucher trong transaction.
        try {
            VoucherRedeemResult result = voucherService.redeemVoucher(currentUser, voucherId);

            // Đổi thành công thì refresh điểm thưởng trong session để navbar/modal hiển thị đúng.
            if (result.isSuccess()) {
                int updatedPoints = userDao.getAvailableRewardPoints(currentUser.getUserId());
                currentUser.setRewardPoints(updatedPoints);
                request.getSession().setAttribute("user", currentUser);

                json.addProperty("success", true);
                json.addProperty("newPoints", updatedPoints);
                json.addProperty("message", result.getMessage());
            } else {
                // Đổi thất bại do rule nghiệp vụ vẫn trả HTTP 200 kèm success=false.
                json.addProperty("success", false);
                json.addProperty("message", result.getMessage());
            }

        } catch (Exception e) {
            // Lỗi ngoài dự kiến trả message chung, không lộ chi tiết DB/stacktrace ra client.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // Log chi tiết ở server, không trả message gốc ra ngoài
            e.printStackTrace();
            json.addProperty("success", false);
            json.addProperty("message", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.getWriter().write(json.toString());
    }
}

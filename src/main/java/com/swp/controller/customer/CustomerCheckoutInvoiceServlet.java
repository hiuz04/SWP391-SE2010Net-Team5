package com.swp.controller.customer;

import com.swp.dao.StaffBillingDAO;
import com.swp.model.User;
import com.swp.model.dto.InvoiceView;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Cho phép Customer mở hóa đơn checkout từ notification hoặc lịch sử booking.
 * Servlet chỉ render invoice thuộc đúng Customer đang đăng nhập.
 */
@WebServlet("/customer/checkout-invoice")
public class CustomerCheckoutInvoiceServlet extends HttpServlet {

    private final StaffBillingDAO billingDAO = new StaffBillingDAO();

    @Override
    /**
     * Tải invoice checkout và kiểm tra ownership trước khi chuyển tới trang thanh toán.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User user = getSessionUser(req);
        // Customer phải đăng nhập mới được mở hóa đơn checkout.
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long invoiceId = parsePositiveLong(req.getParameter("id"));
        // id không hợp lệ thì dừng trước khi query DB.
        if (invoiceId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invoiceId khong hop le.");
            return;
        }

        // Query invoice và bắt lỗi DB để không lộ chi tiết lỗi ra trình duyệt.
        try {
            InvoiceView invoice = billingDAO.getInvoiceByInvoiceId(invoiceId);
            // Customer chỉ được xem hóa đơn của chính mình và invoice checkout còn ý nghĩa thanh toán/đối soát.
            // Nếu invoice không thuộc Customer hiện tại hoặc trạng thái không phù hợp thì trả 404.
            if (invoice == null
                    || invoice.getCustomerId() == null
                    || !invoice.getCustomerId().equals(user.getUserId())
                    || (!"PENDING".equals(invoice.getInvoiceStatus()) && !"PAID".equals(invoice.getInvoiceStatus()))) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay hoa don.");
                return;
            }

            req.setAttribute("invoice", invoice);
            req.getRequestDispatcher("/WEB-INF/customer/checkout-invoice.jsp").forward(req, resp);
        } catch (SQLException e) {
            // Lỗi tải hóa đơn được log server-side và trả message chung cho Customer.
            getServletContext().log("Cannot load checkout invoice", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Khong the tai hoa don.");
        }
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    private Long parsePositiveLong(String raw) {
        // Thiếu id thì xem như request không hợp lệ.
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // Parse id và chỉ nhận số dương.
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            // Chuỗi không phải số sẽ trả null để caller trả BAD_REQUEST.
            return null;
        }
    }
}

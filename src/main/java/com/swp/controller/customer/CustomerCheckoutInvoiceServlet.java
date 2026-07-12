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

@WebServlet("/customer/checkout-invoice")
public class CustomerCheckoutInvoiceServlet extends HttpServlet {

    private final StaffBillingDAO billingDAO = new StaffBillingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User user = getSessionUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long invoiceId = parsePositiveLong(req.getParameter("id"));
        if (invoiceId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invoiceId khong hop le.");
            return;
        }

        try {
            InvoiceView invoice = billingDAO.getInvoiceByInvoiceId(invoiceId);
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
            getServletContext().log("Cannot load checkout invoice", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Khong the tai hoa don.");
        }
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    private Long parsePositiveLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

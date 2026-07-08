package com.swp.controller.staff;

import com.swp.dao.StaffBillingDAO;
import com.swp.model.User;
import com.swp.model.dto.CheckoutResult;
import com.swp.model.dto.CheckoutView;
import com.swp.model.dto.InvoiceView;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;

@WebServlet({"/staff/checkout", "/staff/invoice", "/api/staff/checkout"})
public class StaffBillingServlet extends HttpServlet {

    private static final int ROLE_OWNER = 2;
    private static final int ROLE_STAFF = 3;
    private static final int MAX_NOTE_LENGTH = 500;

    private final StaffBillingDAO billingDAO = new StaffBillingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User user = getSessionUser(req);
        if (!ensurePageAccess(req, resp, user)) {
            return;
        }

        String path = getPath(req);
        if (path.startsWith("/staff/checkout")) {
            showCheckout(req, resp, user);
            return;
        }
        if (path.startsWith("/staff/invoice")) {
            showInvoice(req, resp, user);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        User user = getSessionUser(req);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(resp, false, "Chưa đăng nhập");
            return;
        }
        if (!isStaffOrOwner(user)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeJson(resp, false, "Không có quyền truy cập");
            return;
        }

        String path = getPath(req);
        if (!path.startsWith("/api/staff/checkout")) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJson(resp, false, "Không tìm thấy API");
            return;
        }

        try {
            long bookingId = requireLong(req.getParameter("bookingId"), "Mã đặt sân không được bỏ trống");
            BigDecimal surchargeAmount = parseMoney(req.getParameter("surchargeAmount"), "Phụ phí không hợp lệ");
            BigDecimal discountAmount = parseMoney(req.getParameter("discountAmount"), "Giảm giá không hợp lệ");
            String surchargeReason = trimToLength(req.getParameter("surchargeReason"), "Lý do phụ phí", MAX_NOTE_LENGTH);
            String note = trimToLength(req.getParameter("note"), "Ghi chú", MAX_NOTE_LENGTH);

            CheckoutResult result = billingDAO.completeCheckout(
                    bookingId,
                    user.getUserId(),
                    user.getRoleId() == ROLE_STAFF,
                    surchargeAmount,
                    discountAmount,
                    surchargeReason,
                    note
            );

            writeSuccess(resp, result);
        } catch (SecurityException e) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeJson(resp, false, e.getMessage());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, e.getMessage());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, false, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void showCheckout(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Long bookingId = parsePageBookingId(req, idParam);
        if (bookingId != null) {
            try {
                CheckoutView checkout = billingDAO.getCheckoutView(bookingId);
                if (checkout == null) {
                    req.setAttribute("error", "Không tìm thấy lịch đặt sân");
                } else if (!"CHECKED_IN".equals(checkout.getStatus())) {
                    req.setAttribute("error", "Chỉ booking đã check-in mới được checkout");
                } else if (user.getRoleId() == ROLE_STAFF
                        && !billingDAO.canStaffCheckoutFacility(user.getUserId(), checkout.getFacilityId())) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.setAttribute("error", "Bạn không có ca làm việc đang hoạt động tại cơ sở của booking này");
                } else {
                    req.setAttribute("checkout", checkout);
                }
            } catch (SQLException e) {
                req.setAttribute("error", "Không thể tải thông tin checkout. Vui lòng thử lại.");
            }
        }
        req.getRequestDispatcher("/WEB-INF/staff/checkout.jsp").forward(req, resp);
    }

    private void showInvoice(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Long bookingId = parsePageBookingId(req, idParam);
        if (bookingId != null) {
            try {
                InvoiceView invoice = billingDAO.getInvoiceByBookingId(bookingId);
                if (invoice == null) {
                    req.setAttribute("error", "Không tìm thấy hóa đơn");
                } else if (user.getRoleId() == ROLE_STAFF
                        && !billingDAO.canStaffViewFacilityToday(user.getUserId(), invoice.getFacilityId())) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.setAttribute("error", "Bạn không có quyền xem hóa đơn của cơ sở này");
                } else {
                    req.setAttribute("invoice", invoice);
                }
            } catch (SQLException e) {
                req.setAttribute("error", "Không thể tải thông tin hóa đơn. Vui lòng thử lại.");
            }
        }
        req.getRequestDispatcher("/WEB-INF/staff/invoice.jsp").forward(req, resp);
    }

    private Long parsePageBookingId(HttpServletRequest req, String value) {
        if (value == null || value.trim().isEmpty()) {
            req.setAttribute("error", "Mã đặt sân không được bỏ trống");
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            req.setAttribute("error", "Định dạng mã đặt sân không hợp lệ");
            return null;
        }
    }

    private boolean ensurePageAccess(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        if (!isStaffOrOwner(user)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập.");
            return false;
        }
        return true;
    }

    private long requireLong(String value, String missingMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(missingMessage);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Định dạng mã đặt sân không hợp lệ");
        }
    }

    private BigDecimal parseMoney(String value, String invalidMessage) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal amount = new BigDecimal(value.trim().replace(",", ""));
            if (amount.signum() < 0) {
                throw new IllegalArgumentException(invalidMessage);
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(invalidMessage);
        }
    }

    private String trimToLength(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " không được vượt quá " + maxLength + " ký tự");
        }
        return normalized;
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return (session != null) ? (User) session.getAttribute("user") : null;
    }

    private boolean isStaffOrOwner(User user) {
        return user.getRoleId() == ROLE_STAFF || user.getRoleId() == ROLE_OWNER;
    }

    private String getPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        return uri.substring(contextPath.length());
    }

    private void writeSuccess(HttpServletResponse resp, CheckoutResult result) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":true"
                + ",\"invoiceId\":" + result.getInvoiceId()
                + ",\"bookingId\":" + result.getBookingId()
                + ",\"invoiceCode\":\"" + escapeJson(result.getInvoiceCode()) + "\""
                + "}");
        out.flush();
    }

    private void writeJson(HttpServletResponse resp, boolean success, String message) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":" + success + ",\"error\":\"" + escapeJson(message) + "\"}");
        out.flush();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

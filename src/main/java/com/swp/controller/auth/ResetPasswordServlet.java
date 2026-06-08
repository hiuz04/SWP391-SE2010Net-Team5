package com.swp.controller.auth;

import com.swp.dao.ActivityLogDAO;
import com.swp.dao.PasswordResetTokenDAO;
import com.swp.dao.UserDAO;
import com.swp.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();
    private final ActivityLogDAO logDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("verifiedUserId") == null) {
            response.sendRedirect(request.getContextPath() + "/forgot-password");
            return;
        }
        request.getRequestDispatcher("/reset-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("verifiedUserId") == null) {
            response.sendRedirect(request.getContextPath() + "/forgot-password");
            return;
        }

        long userId = (Long) session.getAttribute("verifiedUserId");
        long tokenId = (Long) session.getAttribute("verifiedTokenId");

        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        String passwordError = PasswordUtil.validatePassword(newPassword);
        if (passwordError != null) {
            request.setAttribute("error", passwordError);
            request.getRequestDispatcher("/reset-password.jsp").forward(request, response);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            request.getRequestDispatcher("/reset-password.jsp").forward(request, response);
            return;
        }

        try {
            // Update password in DB
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            userDAO.updatePassword(userId, hashedPassword);

            // Mark OTP as used
            tokenDAO.markAsUsed(tokenId);

            // Log activity
            logDAO.insertLog(userId, "PASSWORD_RESET", "Người dùng đã đặt lại mật khẩu thành công qua OTP.");

            // Clear session variables
            session.removeAttribute("resetEmail");
            session.removeAttribute("verifiedUserId");
            session.removeAttribute("verifiedTokenId");

            // Redirect to login
            response.sendRedirect(request.getContextPath() + "/login?resetSuccess=1");

        } catch (RuntimeException e) {
            e.printStackTrace(); // In ra console để xem lỗi chi tiết
            request.setAttribute("error", "Lỗi DB: " + e.getMessage());
            request.getRequestDispatcher("/reset-password.jsp").forward(request, response);
        }
    }
}

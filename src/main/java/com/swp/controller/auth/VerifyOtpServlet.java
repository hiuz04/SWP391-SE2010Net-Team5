package com.swp.controller.auth;

import com.swp.dao.PasswordResetTokenDAO;
import com.swp.dao.UserDAO;
import com.swp.model.PasswordResetToken;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/verify-otp")
public class VerifyOtpServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("resetEmail") == null) {
            response.sendRedirect(request.getContextPath() + "/forgot-password");
            return;
        }
        request.getRequestDispatcher("/verify-otp.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("resetEmail") == null) {
            response.sendRedirect(request.getContextPath() + "/forgot-password");
            return;
        }

        String inputEmail = (String) session.getAttribute("resetEmail");
        String otpCode = request.getParameter("otpCode");

        if (otpCode == null || otpCode.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập mã OTP.");
            request.getRequestDispatcher("/verify-otp.jsp").forward(request, response);
            return;
        }
        
        otpCode = otpCode.trim();

        try {
            Optional<User> userOpt = userDAO.findByEmailOrPhone(inputEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Optional<PasswordResetToken> tokenOpt = tokenDAO.findValidOtp(user.getUserId(), otpCode);
                
                if (tokenOpt.isPresent()) {
                    // OTP hợp lệ -> Lưu lại session để cho phép đổi mật khẩu
                    session.setAttribute("verifiedUserId", user.getUserId());
                    session.setAttribute("verifiedTokenId", tokenOpt.get().getTokenId());
                    response.sendRedirect(request.getContextPath() + "/reset-password");
                    return;
                }
            }
            request.setAttribute("error", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            request.getRequestDispatcher("/verify-otp.jsp").forward(request, response);

        } catch (RuntimeException e) {
            request.setAttribute("error", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/verify-otp.jsp").forward(request, response);
        }
    }
}

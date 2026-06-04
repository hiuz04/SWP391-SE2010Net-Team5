package com.swp.controller.auth;

import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.util.MailUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 10;
    private final SecureRandom random = new SecureRandom();

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String input = trim(request.getParameter("contact"));

        // Validate đầu vào
        if (input == null || input.isBlank()) {
            request.setAttribute("error", "Vui lòng nhập email hoặc số điện thoại.");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
            return;
        }

        try {
            Optional<User> userOpt = userDAO.findByEmailOrPhone(input);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Kiểm tra user có email hay không
                String toEmail = user.getEmail();
                if (toEmail == null || toEmail.isBlank()) {
                    // Tài khoản chỉ có số điện thoại, không có email → thông báo chung
                    request.setAttribute("success",
                            "Nếu thông tin khớp với tài khoản, mật khẩu mới đã được gửi đến email của bạn.");
                    request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
                    return;
                }

                // Sinh mật khẩu ngẫu nhiên
                String newPassword = generatePassword();

                // Cập nhật mật khẩu trong DB
                userDAO.updatePassword(user.getUserId(), newPassword);

                // Gửi email
                if (MailUtil.isConfigured()) {
                    String subject = "Mật khẩu mới – Sport Field Booking";
                    String body = MailUtil.buildNewPasswordEmail(user.getFullName(), newPassword);
                    MailUtil.sendHtml(toEmail, subject, body);
                } else {
                    // Mail chưa cấu hình → log để dev biết (production không nên xảy ra)
                    System.err.println("[ForgotPassword] Mail chưa cấu hình. Mật khẩu mới cho "
                            + toEmail + ": " + newPassword);
                    request.setAttribute("error",
                            "Hệ thống email chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
                    request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
                    return;
                }
            }
            // Luôn trả về thông báo chung dù có hay không có tài khoản (tránh lộ thông tin)
            request.setAttribute("success",
                    "Nếu email hoặc số điện thoại khớp với tài khoản, mật khẩu mới đã được gửi đến địa chỉ email của bạn.");

        } catch (RuntimeException e) {
            System.err.println("[ForgotPassword] Lỗi DB: " + e.getMessage());
            request.setAttribute("error", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
        } catch (MessagingException e) {
            System.err.println("[ForgotPassword] Lỗi gửi email: " + e.getMessage());
            request.setAttribute("error",
                    "Không thể gửi email lúc này. Vui lòng kiểm tra cấu hình mail hoặc thử lại sau.");
        }

        request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
    }

    /** Sinh mật khẩu ngẫu nhiên an toàn gồm chữ và số. */
    private String generatePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

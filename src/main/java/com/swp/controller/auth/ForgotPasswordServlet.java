/*
 * Author: Tran Bao Long
 * 4/6/2026
 */
package com.swp.controller.auth;

import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.model.User;
import com.swp.util.MailUtil;
import com.swp.util.PasswordUtil;
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

    private static final String DIGITS = "0123456789";
    private static final int OTP_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();
    
    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final java.util.regex.Pattern PHONE_PATTERN =
            java.util.regex.Pattern.compile("^0\\d{9,10}$");

    private final UserDAO userDAO = new UserDAO();
    private final com.swp.dao.PasswordResetTokenDAO tokenDAO = new com.swp.dao.PasswordResetTokenDAO();

    /**
     hien thi reset mk
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu POST: xử lý yêu cầu đặt lại mật khẩu.
     * Nhận email hoặc số điện thoại, tìm user trong DB,
     * sinh mã OTP mới, cập nhật vào DB và gửi qua email.
     * Thông báo lỗi nếu tài khoản không tồn tại.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String input = trim(request.getParameter("contact"));

        // Validate đầu vào trống
        if (input == null || input.isBlank()) {
            request.setAttribute("error", "Vui lòng nhập email hoặc số điện thoại.");
            request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
            return;
        }

        // Validate định dạng email / SĐT
        boolean isEmail = EMAIL_PATTERN.matcher(input).matches();
        boolean isPhone = PHONE_PATTERN.matcher(input).matches();
        if (!isEmail && !isPhone) {
            request.setAttribute("error", "Định dạng không hợp lệ. Vui lòng nhập đúng email (vd: name@example.com) hoặc số điện thoại (bắt đầu bằng 0, 10-11 số).");
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
                    request.setAttribute("error",
                            "Tài khoản không được liên kết với email nào.");
                    request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
                    return;
                }

                // Vô hiệu hóa các OTP cũ của user này
                tokenDAO.invalidateOldTokens(user.getUserId());

                // Sinh mã OTP ngẫu nhiên
                String otpCode = generateOtp();
                java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusMinutes(5);

                // Lưu OTP vào DB
                tokenDAO.insertToken(user.getUserId(), otpCode, expiresAt);

                // Gửi email
                if (MailUtil.isConfigured()) {
                    String subject = "Mã OTP Đặt lại mật khẩu – Sport Field Booking";
                    String body = MailUtil.buildOtpEmail(user.getFullName(), otpCode);
                    MailUtil.sendHtml(toEmail, subject, body);
                } else {
                    // Mail chưa cấu hình → log để dev biết
                    System.err.println("[ForgotPassword] Mail chưa cấu hình. OTP mới cho "
                            + toEmail + ": " + otpCode);
                    request.setAttribute("error",
                            "Hệ thống email chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
                    request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
                    return;
                }
                
                // Lưu lại session để check bước tiếp theo, chuyển hướng sang nhập OTP
                request.getSession().setAttribute("resetEmail", input);
                response.sendRedirect(request.getContextPath() + "/verify-otp");
                return;
            } else {
                request.setAttribute("error", "Email hoặc số điện thoại không tồn tại trong hệ thống.");
                request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
                return;
            }

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

    /**
     * Sinh mã OTP 6 số ngẫu nhiên.
     */
    private String generateOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }

    /**
     * Loại bỏ khoảng trắng đầu/cuối chuỗi. Trả về null nếu đầu vào là null.
     *
     * @param value chuỗi cần trim
     * @return chuỗi đã trim, hoặc null
     */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

package com.swp.controller.auth;

import com.swp.dao.UserDAO;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/verify-registration")
public class VerifyRegistrationServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("registrationUser") == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }
        request.getRequestDispatcher("/verify-registration.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("registrationUser") == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        String otpCode = request.getParameter("otpCode");
        String sessionOtp = (String) session.getAttribute("registrationOtp");
        LocalDateTime expiryTime = (LocalDateTime) session.getAttribute("registrationExpiry");
        User user = (User) session.getAttribute("registrationUser");

        if (otpCode == null || otpCode.trim().isEmpty()) {
            request.setAttribute("error", "Vui lòng nhập mã OTP.");
            request.getRequestDispatcher("/verify-registration.jsp").forward(request, response);
            return;
        }

        otpCode = otpCode.trim();

        if (LocalDateTime.now().isAfter(expiryTime)) {
            request.setAttribute("error", "Mã OTP đã hết hạn. Vui lòng đăng ký lại.");
            session.removeAttribute("registrationUser");
            session.removeAttribute("registrationOtp");
            session.removeAttribute("registrationExpiry");
            request.getRequestDispatcher("/verify-registration.jsp").forward(request, response);
            return;
        }

        if (otpCode.equals(sessionOtp)) {
            try {
                // OTP đúng -> Lưu user vào DB
                userDAO.insert(user);
                
                // Xóa session
                session.removeAttribute("registrationUser");
                session.removeAttribute("registrationOtp");
                session.removeAttribute("registrationExpiry");

                // Thành công
                response.sendRedirect(request.getContextPath() + "/login?registered=1");
            } catch (Exception e) {
                request.setAttribute("error", "Đã xảy ra lỗi hệ thống khi tạo tài khoản. Vui lòng thử lại sau.");
                request.getRequestDispatcher("/verify-registration.jsp").forward(request, response);
            }
        } else {
            request.setAttribute("error", "Mã OTP không hợp lệ.");
            request.getRequestDispatcher("/verify-registration.jsp").forward(request, response);
        }
    }
}

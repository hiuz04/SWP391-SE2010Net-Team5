package com.swp.controller.auth;

import com.swp.dao.RoleDAO;
import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.util.GoogleConfig;
import com.swp.util.RegisterValidator;
import com.swp.util.ValidationResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        request.setAttribute("googleEnabled", GoogleConfig.isConfigured());
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));
        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        preserveForm(request, fullName, phone, email);

        ValidationResult validation = RegisterValidator.validate(
                fullName, phone, email, password, confirmPassword);

        if (!validation.isValid()) {
            request.setAttribute("fieldErrors", validation.getFieldErrors());
            request.setAttribute("error", validation.getGeneralError());
            forward(request, response);
            return;
        }

        try {
            if (userDAO.existsByEmail(email)) {
                validation.addFieldError("email", "Email này đã được đăng ký.");
                request.setAttribute("fieldErrors", validation.getFieldErrors());
                request.setAttribute("error", validation.getGeneralError());
                forward(request, response);
                return;
            }
            if (userDAO.existsByPhone(phone)) {
                validation.addFieldError("phone", "Số điện thoại này đã được đăng ký.");
                request.setAttribute("fieldErrors", validation.getFieldErrors());
                request.setAttribute("error", validation.getGeneralError());
                forward(request, response);
                return;
            }

            int roleId = roleDAO.findRoleIdByName(RegisterValidator.DEFAULT_ROLE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Vai trò CUSTOMER chưa có trong bảng roles. Chạy script INSERT roles trước."));

            User user = new User();
            user.setRoleId(roleId);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPasswordHash(password);

            userDAO.insert(user);
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (RuntimeException e) {
            request.setAttribute("error", "Không thể đăng ký. Kiểm tra kết nối database và bảng roles.");
            forward(request, response);
        }
    }

    private void preserveForm(HttpServletRequest request, String fullName, String phone, String email) {
        request.setAttribute("fullName", fullName);
        request.setAttribute("phone", phone);
        request.setAttribute("email", email);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("googleEnabled", GoogleConfig.isConfigured());
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

package com.swp.controller;

import com.swp.dao.RoleDAO;
import com.swp.dao.UserDAO;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9,10}$");

    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
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
        String roleName = trim(request.getParameter("role"));

        preserveForm(request, fullName, phone, email, roleName);

        if (fullName == null || fullName.isBlank()) {
            request.setAttribute("error", "Vui lòng nhập họ tên.");
            forward(request, response);
            return;
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            request.setAttribute("error", "Email không hợp lệ.");
            forward(request, response);
            return;
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            request.setAttribute("error", "Số điện thoại phải bắt đầu bằng 0 và có 10–11 chữ số.");
            forward(request, response);
            return;
        }
        if (password == null || password.length() < 6) {
            request.setAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            forward(request, response);
            return;
        }
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp.");
            forward(request, response);
            return;
        }
        if (roleName == null || roleName.isBlank()) {
            request.setAttribute("error", "Vui lòng chọn vai trò.");
            forward(request, response);
            return;
        }

        try {
            if (userDAO.existsByEmail(email)) {
                request.setAttribute("error", "Email đã được sử dụng.");
                forward(request, response);
                return;
            }
            if (userDAO.existsByPhone(phone)) {
                request.setAttribute("error", "Số điện thoại đã được sử dụng.");
                forward(request, response);
                return;
            }

            int roleId = roleDAO.findRoleIdByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Vai trò không tồn tại trong database: " + roleName));

            User user = new User();
            user.setRoleId(roleId);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPasswordHash(password);

            userDAO.insert(user);
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (RuntimeException e) {
            request.setAttribute("error", "Không thể đăng ký. Kiểm tra database hoặc dữ liệu vai trò (roles).");
            forward(request, response);
        }
    }

    private void preserveForm(HttpServletRequest request, String fullName, String phone, String email, String role) {
        request.setAttribute("fullName", fullName);
        request.setAttribute("phone", phone);
        request.setAttribute("email", email);
        request.setAttribute("role", role);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

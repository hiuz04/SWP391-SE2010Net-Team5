package com.swp.controller.admin;

import com.swp.dao.RoleDAO;
import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/users")
public class AdminUsersServlet extends HttpServlet {

    private UserDAO userDAO;
    private RoleDAO roleDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        roleDAO = new RoleDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong co quyen truy cap.");
            return;
        }

        // Lấy danh sách người dùng
        List<User> userList = userDAO.getAllUsers();
        request.setAttribute("userList", userList);

        // Forward to JSP
        request.getRequestDispatcher("/WEB-INF/admin/users.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = session == null ? null : (User) session.getAttribute("user");

        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong co quyen truy cap.");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/admin/users");
            return;
        }

        try {
            switch (action) {
                case "add":
                    addUser(request);
                    break;
                case "edit":
                    editUser(request);
                    break;
                case "ban":
                    changeStatus(request, "BANNED");
                    break;
                case "unban":
                case "approve":
                    changeStatus(request, "ACTIVE");
                    break;
                case "delete":
                    deleteUser(request);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle error, maybe set session attribute for error message
        }

        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    private void addUser(HttpServletRequest request) {
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String roleName = request.getParameter("roleName");
        String status = request.getParameter("status");
        String password = request.getParameter("password");

        User user = new User();
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(status != null ? status : "ACTIVE");
        
        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(PasswordUtil.hashPassword(password));
        } else {
            user.setPasswordHash(PasswordUtil.hashPassword("123456")); // default password
        }

        roleDAO.findRoleIdByName(roleName).ifPresent(user::setRoleId);

        userDAO.insert(user);
    }

    private void editUser(HttpServletRequest request) {
        long userId = Long.parseLong(request.getParameter("userId"));
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String roleName = request.getParameter("roleName");
        String status = request.getParameter("status");
        String password = request.getParameter("password");

        User user = new User();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(status);

        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(PasswordUtil.hashPassword(password));
        }

        roleDAO.findRoleIdByName(roleName).ifPresent(user::setRoleId);

        userDAO.updateUserByAdmin(user);
    }

    private void changeStatus(HttpServletRequest request, String status) {
        long userId = Long.parseLong(request.getParameter("userId"));
        userDAO.updateUserStatus(userId, status);
    }

    private void deleteUser(HttpServletRequest request) {
        long userId = Long.parseLong(request.getParameter("userId"));
        userDAO.deleteUser(userId);
    }
}

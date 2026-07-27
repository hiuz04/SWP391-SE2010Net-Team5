package com.swp.controller.admin;

import com.swp.dao.RoleDAO;
import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.util.PasswordUtil;
import com.swp.util.RegisterValidator;
import com.swp.util.ValidationResult;
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

        // Bước 1: Lấy các tham số tìm kiếm và bộ lọc từ request (VD: tên/email, role, trạng thái)
        String search = request.getParameter("search");
        String role = request.getParameter("role");
        String status = request.getParameter("status");
        String joinDate = request.getParameter("joinDate");
        
        // Bước 2: Khởi tạo giá trị phân trang mặc định (trang 1, mỗi trang 10 dòng)
        int page = 1;
        int limit = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException e) {
            page = 1;
        }
        
        int offset = (page - 1) * limit;
        
        // Bước 3: Gọi DAO lấy danh sách người dùng theo bộ lọc và giới hạn phân trang
        List<User> userList = userDAO.getUsersPaginated(search, role, status, joinDate, offset, limit);
        
        // Bước 4: Lấy tổng số lượng người dùng thỏa mãn điều kiện để tính tổng số trang
        int totalUsers = userDAO.countUsers(search, role, status, joinDate);
        int totalPages = (int) Math.ceil((double) totalUsers / limit);
        
        // Bước 5: Truyền toàn bộ dữ liệu (danh sách user, phân trang, bộ lọc hiện tại) sang JSP
        request.setAttribute("userList", userList);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("search", search);
        request.setAttribute("role", role);
        request.setAttribute("status", status);
        request.setAttribute("joinDate", joinDate);

        // Forward to JSP để hiển thị danh sách tài khoản
        request.getRequestDispatcher("/WEB-INF/admin/users.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (User) session.getAttribute("user");

        // Bước 1: Kiểm tra hành động admin muốn thực hiện (Thêm, Sửa, Khóa, Mở khóa, Xóa)
        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/admin/users");
            return;
        }

        try {
            // Bước 2: Route request tới các hàm xử lý tương ứng
            switch (action) {
                case "add":
                    addUser(request, session);
                    break;
                case "edit":
                    editUser(request, session);
                    break;
                case "ban":
                    // Khóa tài khoản
                    changeStatus(request, session, currentUser.getUserId(), "BANNED");
                    break;
                case "unban":
                case "approve":
                    // Mở khóa tài khoản
                    changeStatus(request, session, currentUser.getUserId(), "ACTIVE");
                    break;
                case "delete":
                    deleteUser(request, session, currentUser.getUserId());
                    break;
            }
        } catch (IllegalArgumentException e) {
            // Bắt lỗi logic (VD: trùng email, trùng số điện thoại) và gửi lại giao diện
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
        }

        // Bước 3: Hoàn tất xử lý, reload lại trang danh sách người dùng
        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    private void addUser(HttpServletRequest request, HttpSession session) throws IllegalArgumentException {
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String roleName = request.getParameter("roleName");
        String status = request.getParameter("status");
        String password = request.getParameter("password");
        
        ValidationResult vr = RegisterValidator.validate(fullName, phone, email, 
                (password != null && !password.isEmpty()) ? password : "DefaultPassword1@",
                (password != null && !password.isEmpty()) ? password : "DefaultPassword1@");
        
        if (!vr.isValid()) {
            String errorMsg = vr.getFieldErrors().isEmpty() ? vr.getGeneralError() : vr.getFieldErrors().values().iterator().next();
            throw new IllegalArgumentException(errorMsg);
        }
        
        if (userDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng bới tài khoản khác.");
        }
        if (userDAO.existsByPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng bới tài khoản khác.");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(status != null ? status : "ACTIVE");
        
        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(PasswordUtil.hashPassword(password));
        } else {
            user.setPasswordHash(PasswordUtil.hashPassword("123456")); // default password
            session.setAttribute("successMessage", "Tạo tài khoản thành công! Mật khẩu mặc định là 123456.");
        }

        int roleId = roleDAO.findRoleIdByName(roleName).orElse(-1);
        if (roleId == -1) {
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        user.setRoleId(roleId);

        userDAO.insert(user);
        if (session.getAttribute("successMessage") == null) {
            session.setAttribute("successMessage", "Thêm người dùng thành công.");
        }
    }

    private void editUser(HttpServletRequest request, HttpSession session) throws IllegalArgumentException {
        long userId = Long.parseLong(request.getParameter("userId"));
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String roleName = request.getParameter("roleName");
        String status = request.getParameter("status");
        String password = request.getParameter("password");
        
        ValidationResult vr = RegisterValidator.validate(fullName, phone, email, 
                (password != null && !password.isEmpty()) ? password : "DefaultPassword1@",
                (password != null && !password.isEmpty()) ? password : "DefaultPassword1@");
        
        if (!vr.isValid()) {
            String errorMsg = vr.getFieldErrors().isEmpty() ? vr.getGeneralError() : vr.getFieldErrors().values().iterator().next();
            throw new IllegalArgumentException(errorMsg);
        }
        
        if (userDAO.existsByEmailExcludeUser(email, userId)) {
            throw new IllegalArgumentException("Email đã được sử dụng bới tài khoản khác.");
        }
        if (userDAO.existsByPhoneExcludeUser(phone, userId)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng bới tài khoản khác.");
        }

        User user = new User();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(status);

        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(PasswordUtil.hashPassword(password));
        }

        int roleId = roleDAO.findRoleIdByName(roleName).orElse(-1);
        if (roleId == -1) {
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        user.setRoleId(roleId);

        userDAO.updateUserByAdmin(user);
        session.setAttribute("successMessage", "Cập nhật người dùng thành công.");
    }

    private void changeStatus(HttpServletRequest request, HttpSession session, long currentAdminId, String status) throws IllegalArgumentException {
        long userId = Long.parseLong(request.getParameter("userId"));
        if (userId == currentAdminId) {
            throw new IllegalArgumentException("Bạn không thể khóa/mở khóa chính mình.");
        }
        userDAO.updateUserStatus(userId, status);
        session.setAttribute("successMessage", "Đã cập nhật trạng thái người dùng thành " + status);
    }

    private void deleteUser(HttpServletRequest request, HttpSession session, long currentAdminId) throws IllegalArgumentException {
        long userId = Long.parseLong(request.getParameter("userId"));
        if (userId == currentAdminId) {
            throw new IllegalArgumentException("Bạn không thể xóa chính mình.");
        }
        userDAO.deleteUser(userId);
        session.setAttribute("successMessage", "Đã xóa người dùng thành công.");
    }
}

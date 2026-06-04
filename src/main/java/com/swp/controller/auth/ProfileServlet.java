/*
 * Author: Tran Bao Long
 * 3/6/2026
 */
package com.swp.controller.auth;

import com.swp.dao.UserDAO;
import com.swp.dao.BookingDAO;
import com.swp.model.User;
import com.swp.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern FULL_NAME_PATTERN =
            Pattern.compile("^[\\p{L}][\\p{L}\\s'.]{1,98}[\\p{L}.]$|^[\\p{L}]{2,}$");

    /**
     * Xử lý yêu cầu GET: hiển thị trang hồ sơ người dùng.
     * Lấy thông tin mới nhất của user từ DB và cập nhật vào session.
     * Nếu chưa đăng nhập, chuyển hướng về trang login.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User sessionUser = (User) session.getAttribute("user");
        Optional<User> freshUserOpt = userDAO.findByEmail(sessionUser.getEmail());
        if (freshUserOpt.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }

        User user = freshUserOpt.get();
        session.setAttribute("user", user);

        int bookingCount = 0;
        try {
            bookingCount = bookingDAO.getBookingCountByCustomerId(user.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        request.setAttribute("bookingCount", bookingCount);
        request.setAttribute("currentUser", user);
        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu POST: cập nhật thông tin cá nhân.
     * Validate các trường họ tên, email, số điện thoại và mật khẩu mới (nếu có).
     * Kiểm tra email/SĐT không trùng với tài khoản khác, sau đó lưu vào DB và cập nhật session.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User sessionUser = (User) session.getAttribute("user");
        long userId = sessionUser.getUserId();

        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));
        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");

        Map<String, String> errors = new HashMap<>();

        if (fullName == null || fullName.isBlank()) {
            errors.put("fullName", "Họ tên không được để trống.");
        } else if (fullName.length() < 2 || fullName.length() > 100) {
            errors.put("fullName", "Họ tên phải từ 2 đến 100 ký tự.");
        } else if (!FULL_NAME_PATTERN.matcher(fullName).matches()) {
            errors.put("fullName", "Họ tên chỉ được chứa chữ cái và khoảng trắng.");
        }

        if (email == null || email.isBlank()) {
            errors.put("email", "Email không được để trống.");
        } else if (email.length() > 100) {
            errors.put("email", "Email không được vượt quá 100 ký tự.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "Email không đúng định dạng.");
        } else if (userDAO.existsByEmailExcludeUser(email, userId)) {
            errors.put("email", "Email này đã được sử dụng bởi tài khoản khác.");
        }

        if (phone == null || phone.isBlank()) {
            errors.put("phone", "Số điện thoại không được để trống.");
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.put("phone", "Số điện thoại phải bắt đầu bằng 0 và có 10–11 chữ số.");
        } else if (userDAO.existsByPhoneExcludeUser(phone, userId)) {
            errors.put("phone", "Số điện thoại này đã được sử dụng bởi tài khoản khác.");
        }

        boolean changePassword = false;
        if (password != null && !password.isEmpty()) {
            changePassword = true;
            if (password.length() < 6) {
                errors.put("password", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            } else if (password.length() > 64) {
                errors.put("password", "Mật khẩu mới không được vượt quá 64 ký tự.");
            } else if (!containsLetter(password) || !containsDigit(password)) {
                errors.put("password", "Mật khẩu mới phải có ít nhất 1 chữ cái và 1 chữ số.");
            }
        }

        Optional<User> freshUserOpt = userDAO.findByEmail(sessionUser.getEmail());
        if (freshUserOpt.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }
        User user = freshUserOpt.get();

        if (!errors.isEmpty()) {
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setEmail(email);

            int bookingCount = 0;
            try {
                bookingCount = bookingDAO.getBookingCountByCustomerId(user.getUserId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            request.setAttribute("bookingCount", bookingCount);
            request.setAttribute("errors", errors);
            request.setAttribute("currentUser", user);
            request.getRequestDispatcher("/profile.jsp").forward(request, response);
            return;
        }

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        if (changePassword) {
            user.setPasswordHash(PasswordUtil.hashPassword(password));
        }

        try {
            userDAO.updateProfile(user);
            session.setAttribute("user", user);

            request.setAttribute("success", "Cập nhật hồ sơ thành công!");
        } catch (RuntimeException e) {
            request.setAttribute("error", "Lỗi hệ thống: Không thể cập nhật thông tin.");
        }

        int bookingCount = 0;
        try {
            bookingCount = bookingDAO.getBookingCountByCustomerId(user.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        request.setAttribute("bookingCount", bookingCount);
        request.setAttribute("currentUser", user);
        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    /**
     * Kiểm tra chuỗi có chứa ít nhất một ký tự chữ cái không.
     *
     * @param value chuỗi cần kiểm tra
     * @return true nếu có chữa ít nhất một chữ cái
     */
    private boolean containsLetter(String value) {
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra chuỗi có chứa ít nhất một ký tự số không.
     *
     * @param value chuỗi cần kiểm tra
     * @return true nếu có chứa ít nhất một chữ số
     */
    private boolean containsDigit(String value) {
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
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

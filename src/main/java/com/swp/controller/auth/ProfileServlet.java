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
            Pattern.compile("^[a-zA-Z0-9.]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[35789]\\d{8}$");
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
        
        // Bước 1: Kiểm tra xem người dùng đã đăng nhập chưa, nếu chưa thì đẩy về trang login
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Bước 2: Lấy thông tin user hiện tại và query Database để lấy dữ liệu mới nhất (phòng trường hợp vừa bị đổi bởi admin)
        User sessionUser = (User) session.getAttribute("user");
        Optional<User> freshUserOpt = userDAO.findByEmail(sessionUser.getEmail());
        if (freshUserOpt.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }

        User user = freshUserOpt.get();
        // Cập nhật lại session với dữ liệu mới
        session.setAttribute("user", user);

        // Bước 3: Thống kê số lượng lịch đặt sân (Booking) của người dùng này
        int bookingCount = 0;
        try {
            bookingCount = bookingDAO.getBookingCountByCustomerId(user.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Bước 4: Đẩy dữ liệu ra view profile.jsp
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
        
        // Bước 1: Kiểm tra đăng nhập
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        User sessionUser = (User) session.getAttribute("user");
        long userId = sessionUser.getUserId();

        // Bước 2: Lấy dữ liệu gửi lên từ form chỉnh sửa hồ sơ
        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));
        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");

        Map<String, String> errors = new HashMap<>();

        // Business Rule BR-26: Hồ sơ cũng áp dụng giới hạn họ tên 2-50 ký tự theo mẫu tên hệ thống.
        // Bước 3: Validate dữ liệu đầu vào (Tên, Email, SDT)
        if (fullName == null || fullName.isBlank()) {
            errors.put("fullName", "Họ tên không được để trống.");
        } else if (fullName.length() < 2 || fullName.length() > 50) {
            errors.put("fullName", "Họ tên phải từ 2 đến 50 ký tự.");
        } else if (!FULL_NAME_PATTERN.matcher(fullName).matches()) {
            errors.put("fullName", "Họ tên chỉ được chứa chữ cái và khoảng trắng.");
        }

        // Business Rule BR-27: Email hồ sơ phải đúng định dạng, không quá 50 ký tự và không trùng tài khoản khác.
        if (email == null || email.isBlank()) {
            errors.put("email", "Email không được để trống.");
        } else if (email.length() > 50) {
            errors.put("email", "Email không được vượt quá 50 ký tự.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "Email không đúng định dạng (vd: name@example.com).");
        } else if (userDAO.existsByEmailExcludeUser(email, userId)) {
            // Đảm bảo không đổi sang email của người khác
            errors.put("email", "Email này đã được sử dụng bởi tài khoản khác.");
        }

        // Business Rule BR-28: Số điện thoại hồ sơ phải khớp regex hệ thống và không trùng tài khoản khác.
        if (phone == null || phone.isBlank()) {
            errors.put("phone", "Số điện thoại không được để trống.");
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.put("phone", "Số điện thoại không đúng định dạng (10 số, mạng VN).");
        } else if (userDAO.existsByPhoneExcludeUser(phone, userId)) {
            // Đảm bảo không đổi sang SĐT của người khác
            errors.put("phone", "Số điện thoại này đã được sử dụng bởi tài khoản khác.");
        }

        // Bước 4: Validate Mật khẩu mới (Nếu có đổi)
        boolean changePassword = false;
        if (password != null && !password.isEmpty()) {
            changePassword = true;
            String passwordError = PasswordUtil.validatePassword(password);
            if (passwordError != null) {
                errors.put("password", passwordError);
            }
        }

        // Lấy lại user cũ từ CSDL để update
        Optional<User> freshUserOpt = userDAO.findByEmail(sessionUser.getEmail());
        if (freshUserOpt.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }
        User user = freshUserOpt.get();

        // Bước 5: Nếu có lỗi validation, trả về trang kèm thông báo lỗi
        if (!errors.isEmpty()) {
            request.setAttribute("submittedFullName", fullName);
            request.setAttribute("submittedPhone", phone);
            request.setAttribute("submittedEmail", email);

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

        // Bước 6: Nếu hợp lệ, gán các trường và cập nhật xuống Database
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        if (changePassword) {
            user.setPasswordHash(PasswordUtil.hashPassword(password));
        }

        try {
            userDAO.updateProfile(user);
            session.setAttribute("user", user); // update session

            request.setAttribute("success", "Cập nhật hồ sơ thành công!");
        } catch (RuntimeException e) {
            request.setAttribute("error", "Lỗi hệ thống: Không thể cập nhật thông tin.");
        }

        // Bước 7: Thống kê lại booking để hiển thị dashboard cá nhân
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
     * Loại bỏ khoảng trắng đầu/cuối chuỗi. Trả về null nếu đầu vào là null.
     *
     * @param value chuỗi cần trim
     * @return chuỗi đã trim, hoặc null
     */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

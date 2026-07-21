/*
 * Author: Tran Bao Long
 * 31/5/2026
 */
package com.swp.controller.auth;

import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.util.AuthUtil;
import com.swp.util.GoogleConfig;
import com.swp.util.LoginAttemptUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    /**
     * hien thi trang dăng nhap
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User loggedIn = (User) session.getAttribute("user");
            response.sendRedirect(request.getContextPath() + AuthUtil.dashboardPath(loggedIn.getRoleName()));
            return;
        }
        prepareLoginPage(request);
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu POST: thực hiện xác thực đăng nhập.
     * Nhận email/SĐT và mật khẩu, kiểm tra với database.
     * Nếu hợp lệ, tạo session và chuyển hướng về trang chủ.
     * Nếu không hợp lệ, hiển thị lại trang login kèm thông báo lỗi.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Bước 1: Kiểm tra bảo mật CSRF Token để chống giả mạo request
        HttpSession currentSession = request.getSession(false);
        String sessionCsrf = currentSession != null ? (String) currentSession.getAttribute("csrfToken") : null;
        String requestCsrf = request.getParameter("csrfToken");
        if (sessionCsrf == null || !sessionCsrf.equals(requestCsrf)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
            return;
        }

        // Bước 2: Nhận dữ liệu đầu vào từ form
        String login = trim(request.getParameter("login"));
        String password = request.getParameter("password");

        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("error", "Vui lòng nhập đầy đủ thông tin đăng nhập.");
            request.setAttribute("login", login);
            prepareLoginPage(request);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        // Bước 3: Kiểm tra cơ chế khóa tài khoản tạm thời (chống brute-force)
        if (LoginAttemptUtil.isLocked(login)) {
            long remaining = LoginAttemptUtil.getRemainingLockTimeInMinutes(login);
            request.setAttribute("error",
                    "Tài khoản của bạn đã bị khóa do nhập sai quá nhiều lần. Vui lòng thử lại sau " + remaining
                            + " phút.");
            request.setAttribute("login", login);
            prepareLoginPage(request);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try {
            // Bước 4: Gọi Database (UserDAO) để xác thực tài khoản và mật khẩu
            Optional<User> user = userDAO.findByLoginAndPassword(login, password);
            if (user.isPresent()) {
                User loggedIn = user.get();
                
                // Bước 5: Kiểm tra trạng thái tài khoản (chỉ cho phép ACTIVE)
                if (!"ACTIVE".equals(loggedIn.getStatus())) {
                    request.setAttribute("error", "Tài khoản của bạn đã bị khóa .");
                    request.setAttribute("login", login);
                    prepareLoginPage(request);
                    request.getRequestDispatcher("/login.jsp").forward(request, response);
                    return;
                }

                // Bước 6: Đăng nhập thành công -> Xóa bộ đếm sai mật khẩu
                LoginAttemptUtil.loginSucceeded(login);
                
                // Bước 7: Khởi tạo Session và lưu thông tin người dùng
                HttpSession session = request.getSession(true);
                session.setAttribute("user", loggedIn);
                session.setAttribute("navRole", AuthUtil.toNavRole(loggedIn.getRoleName()));

                // Bước 8: Xử lý chức năng "Ghi nhớ đăng nhập" (Remember Me)
                if (request.getParameter("remember") != null) {
                    com.swp.util.RememberMeUtil.setRememberMeCookie(response, loggedIn);
                }

                // Bước 9: Chuyển hướng người dùng về trang đích (Dashboard) tương ứng với chức vụ (Role)
                response.sendRedirect(request.getContextPath() + AuthUtil.dashboardPath(loggedIn.getRoleName()));
                return;
            }

            // Bước 10: Nếu sai thông tin đăng nhập -> Tăng bộ đếm và khóa nếu quá 5 lần
            LoginAttemptUtil.loginFailed(login);
            if (LoginAttemptUtil.isLocked(login)) {
                request.setAttribute("error", "Bạn đã nhập sai 5 lần liên tiếp. Tài khoản bị khóa trong 30 phút.");
            } else {
                request.setAttribute("error", "Sai email/số điện thoại hoặc mật khẩu!");
            }
            request.setAttribute("login", login);
        } catch (RuntimeException e) {
            request.setAttribute("error", "Không kết nối được database. Kiểm tra db.properties và SQL Server.");
            request.setAttribute("login", login);
        }

        // Bước 11: Render lại trang đăng nhập kèm lỗi (nếu có)
        prepareLoginPage(request);
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    /**
     * Chuẩn bị các attribute cần thiết trước khi hiển thị trang login:
     * thông báo đăng ký thành công (nếu có), lỗi từ Google OAuth (nếu có),
     * và trạng thái bật/tắt nút đăng nhập Google.
     */
    private void prepareLoginPage(HttpServletRequest request) {
        if ("1".equals(request.getParameter("registered"))) {
            request.setAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
        } else if ("1".equals(request.getParameter("resetSuccess"))) {
            request.setAttribute("success", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.");
        }
        if (request.getAttribute("error") == null) {
            String googleError = request.getParameter("googleError");
            if (googleError != null && !googleError.isBlank()) {
                request.setAttribute("error", resolveGoogleError(googleError));
            }
        }
        request.setAttribute("googleEnabled", GoogleConfig.isConfigured());

        HttpSession session = request.getSession(true);
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
        }
    }

    /**
     * Chuyển đổi mã lỗi Google OAuth sang thông báo tiếng Việt thân thiện.
     *
     * @param code mã lỗi từ query parameter googleError
     * @return chuỗi thông báo lỗi hiển thị cho người dùng
     */
    private String resolveGoogleError(String code) {
        if ("not_configured".equals(code)) {
            return "Đăng nhập Google chưa được cấu hình. Kiểm tra file google.properties.";
        }
        if ("cancelled".equals(code)) {
            return "Bạn đã hủy đăng nhập Google.";
        }
        if ("invalid_state".equals(code)) {
            return "Phiên đăng nhập Google không hợp lệ. Thử lại.";
        }
        if ("no_code".equals(code)) {
            return "Google không trả về mã xác thực.";
        }
        return "Đăng nhập Google thất bại. Vui lòng thử lại.";
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

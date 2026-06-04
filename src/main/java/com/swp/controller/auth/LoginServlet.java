package com.swp.controller.auth;

import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.util.AuthUtil;
import com.swp.util.GoogleConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        prepareLoginPage(request);
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String login = trim(request.getParameter("login"));
        String password = request.getParameter("password");

        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("error", "Vui lòng nhập đầy đủ thông tin đăng nhập.");
            request.setAttribute("login", login);
            prepareLoginPage(request);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try {
            Optional<User> user = userDAO.findByLoginAndPassword(login, password);
            if (user.isPresent()) {
                HttpSession session = request.getSession(true);
                User loggedIn = user.get();
                session.setAttribute("user", loggedIn);
                session.setAttribute("navRole", AuthUtil.toNavRole(loggedIn.getRoleName()));
                response.sendRedirect(request.getContextPath() + "/");
                return;
            }
            request.setAttribute("error", "Sai email/số điện thoại hoặc mật khẩu!");
            request.setAttribute("login", login);
        } catch (RuntimeException e) {
            request.setAttribute("error", "Không kết nối được database. Kiểm tra db.properties và SQL Server.");
            request.setAttribute("login", login);
        }

        prepareLoginPage(request);
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    private void prepareLoginPage(HttpServletRequest request) {
        if ("1".equals(request.getParameter("registered"))) {
            request.setAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
        }
        if (request.getAttribute("error") == null) {
            String googleError = request.getParameter("googleError");
            if (googleError != null && !googleError.isBlank()) {
                request.setAttribute("error", resolveGoogleError(googleError));
            }
        }
        request.setAttribute("googleEnabled", GoogleConfig.isConfigured());
    }

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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

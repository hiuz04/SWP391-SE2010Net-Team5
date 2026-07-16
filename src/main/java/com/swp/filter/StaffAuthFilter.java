package com.swp.filter;

import com.swp.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter({"/staff/*", "/api/staff/*"})
public class StaffAuthFilter implements Filter {

    protected static final String STAFF_ROLE_NAME = "STAFF";
    protected static final String OWNER_ROLE_NAME = "OWNER";

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);

        // Chưa đăng nhập
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        String roleName = user.getRoleName();

        // Không phải Staff hoặc Owner
        if (!STAFF_ROLE_NAME.equalsIgnoreCase(roleName) && !OWNER_ROLE_NAME.equalsIgnoreCase(roleName)) {
            // Đối với API request, có thể trả về 403 thay vì redirect, nhưng để nhất quán ta dùng 403 page/status.
            if (req.getRequestURI().startsWith(req.getContextPath() + "/api/")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập.");
            } else {
                resp.sendRedirect(req.getContextPath() + "/error/403.jsp");
            }
            return;
        }

        // Cho phép request đi tiếp
        chain.doFilter(request, response);
    }
}

package com.swp.filter;

import com.swp.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/owner/*")
public class OwnerAuthFilter implements Filter {

    protected static final String OWNER_ROLE_NAME = "OWNER";

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);

        boolean isApiRequest = req.getRequestURI().contains("/api/")
                || "XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"))
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        // Business Rule BR-30: Chỉ user đã đăng nhập mới được vào nhóm chức năng /owner, gồm Manage Voucher.
        if (session == null || session.getAttribute("user") == null) {
            if (isApiRequest) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Chưa đăng nhập\"}");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login");
            }
            return;
        }

        User user = (User) session.getAttribute("user");

        // Business Rule BR-30: Manage Voucher chỉ cho phép role OWNER tiếp tục xử lý request.
        if (!OWNER_ROLE_NAME.equalsIgnoreCase(user.getRoleName())) {
            if (isApiRequest) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Không có quyền truy cập\"}");
            } else {
                resp.sendRedirect(req.getContextPath() + "/error/403.jsp");
            }
            return;
        }

        // Cho phép request đi tiếp
        chain.doFilter(request, response);
    }
}

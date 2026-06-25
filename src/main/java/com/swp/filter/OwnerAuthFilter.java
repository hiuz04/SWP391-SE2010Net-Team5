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

        // Chưa đăng nhập
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");

        // Không phải Owner
        if (!OWNER_ROLE_NAME.equals(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/error/403.jsp");
            return;
        }

        // Cho phép request đi tiếp
        chain.doFilter(request, response);
    }
}
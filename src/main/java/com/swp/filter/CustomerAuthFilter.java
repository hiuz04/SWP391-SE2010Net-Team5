package com.swp.filter;

import com.swp.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/customer/*")
public class CustomerAuthFilter implements Filter {

    protected static final String CUSTOMER_ROLE_NAME = "CUSTOMER";

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

        // Không phải Customer
        if (!CUSTOMER_ROLE_NAME.equalsIgnoreCase(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/error/403.jsp");
            return;
        }

        // Cho phép request đi tiếp
        chain.doFilter(request, response);
    }
}

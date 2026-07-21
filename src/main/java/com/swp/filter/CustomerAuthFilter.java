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

        // Business Rule BR-01: Customer phải đăng nhập trước khi vào các trang/chức năng trong /customer.
        // Chưa đăng nhập.
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");

        // Business Rule BR-01: Route Customer chỉ cho phép role CUSTOMER tiếp tục xử lý request.
        // Không phải Customer.
        if (!CUSTOMER_ROLE_NAME.equalsIgnoreCase(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/error/403.jsp");
            return;
        }

        // Cho phép request đi tiếp
        chain.doFilter(request, response);
    }
}

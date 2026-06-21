package com.swp.filter;

import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.util.AuthUtil;
import com.swp.util.RememberMeUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

public class AuthFilter implements Filter {

    private UserDAO userDAO;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        // Check if user is already logged in
        if (session == null || session.getAttribute("user") == null) {
            String token = RememberMeUtil.getRememberMeCookie(req);
            if (token != null) {
                String userIdStr = RememberMeUtil.extractUserId(token);
                if (userIdStr != null) {
                    try {
                        long userId = Long.parseLong(userIdStr);
                        Optional<User> userOpt = userDAO.getUserById(userId);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            if ("ACTIVE".equals(user.getStatus()) && RememberMeUtil.verifyToken(token, user)) {
                                // Auto login
                                session = req.getSession(true);
                                session.setAttribute("user", user);
                                session.setAttribute("navRole", AuthUtil.toNavRole(user.getRoleName()));
                            } else {
                                RememberMeUtil.clearRememberMeCookie(res);
                            }
                        } else {
                            RememberMeUtil.clearRememberMeCookie(res);
                        }
                    } catch (NumberFormatException e) {
                        RememberMeUtil.clearRememberMeCookie(res);
                    }
                }
            }
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}

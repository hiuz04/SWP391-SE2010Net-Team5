package com.swp.filter;

import com.swp.dao.SystemSettingDAO;
import com.swp.model.SystemSetting;
import com.swp.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;
import java.util.Optional;

@WebFilter("/*")
public class MaintenanceFilter implements Filter {

    private SystemSettingDAO systemSettingDAO;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        systemSettingDAO = new SystemSettingDAO();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getServletPath();

        // Luôn cho phép truy cập tài nguyên tĩnh
        if (path.startsWith("/assets") || path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images")) {
            chain.doFilter(request, response);
            return;
        }

        // Bỏ qua trang bảo trì để tránh loop
        if (path.equals("/maintenance.jsp") || path.equals("/maintenance")) {
            chain.doFilter(request, response);
            return;
        }

        // Kiểm tra cài đặt bảo trì
        Optional<SystemSetting> maintenanceOpt = systemSettingDAO.getSettingByKey("MAINTENANCE_MODE");
        boolean isMaintenanceMode = false;
        if (maintenanceOpt.isPresent() && "true".equals(maintenanceOpt.get().getSettingValue())) {
            isMaintenanceMode = true;
        }

        if (isMaintenanceMode) {
            // Cho phép các endpoint auth cơ bản để Admin có thể đăng nhập/đăng xuất
            if (path.equals("/login") || path.equals("/login.jsp") || path.equals("/logout") || path.startsWith("/api/auth")) {
                chain.doFilter(request, response);
                return;
            }

            HttpSession session = req.getSession(false);
            boolean isAdmin = false;
            
            if (session != null && session.getAttribute("user") != null) {
                User user = (User) session.getAttribute("user");
                if ("ADMIN".equals(user.getRoleName())) {
                    isAdmin = true;
                }
            }

            // Nếu không phải Admin, chuyển hướng tới trang bảo trì
            if (!isAdmin) {
                res.sendRedirect(req.getContextPath() + "/maintenance.jsp");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}

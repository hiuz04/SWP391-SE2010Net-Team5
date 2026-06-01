package com.swp.controller.owner;

import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/owner")
public class OwnerDashboardServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        // Chưa đăng nhập
//        if(session == null || session.getAttribute("user") == null) {
//            resp.sendRedirect(req.getContextPath() + "/login.jsp");
//            return;
//        }

        // Không phải là Owner
//        User user = (User) session.getAttribute("user");
//        if(!Constant.OWNER_ROLE_NAME.equals(user.getRoleName())) {
//            resp.sendRedirect(req.getContextPath() + "/error/error-403.jsp");
//            return;
//        }

        // Chuyển hướng tới trang Dashboard dành cho Owner
        req.getRequestDispatcher(
        "/owner/dashboard.jsp")
            .forward(req, resp);
    }
}

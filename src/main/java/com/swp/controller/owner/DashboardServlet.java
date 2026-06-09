/**
 * Module: Owner Dashboard
 * File: DashboardServlet.java
 * Description: Xử lý điều hướng người dùng đến trang quản trị của chủ sở hữu (Owner Dashboard).
 *
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 04/06/2026
 */
package com.swp.controller.owner;

import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet({"/owner", "/owner/dashboard"})
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        // Chưa đăng nhập
        if(session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Không phải là Owner
        User user = (User) session.getAttribute("user");
        if(!Constant.OWNER_ROLE_NAME.equals(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }

        req.getRequestDispatcher("/WEB-INF/owner/dashboard.jsp").forward(req, resp);
    }
}

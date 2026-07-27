package com.swp.controller.admin;

import com.swp.dao.NotificationDAO;
import com.swp.model.Notification;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/notifications")
public class AdminNotificationServlet extends HttpServlet {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        List<Notification> notifications = notificationDAO.getGlobalNotifications();
        req.setAttribute("notifications", notifications);

        req.getRequestDispatcher("/WEB-INF/admin/notifications.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        String action = req.getParameter("action");
        if ("delete".equals(action)) {
            String title = req.getParameter("title");
            String message = req.getParameter("message");
            String type = req.getParameter("type");
            
            if (title != null && message != null && type != null) {
                // Normalize newlines in case the browser converts \n to \r\n upon form submission
                message = message.replace("\r\n", "\n");
                boolean success = notificationDAO.deleteGlobalNotification(title, message, type);
                if (success) {
                    req.getSession().setAttribute("msgSuccess", "Đã xóa thông báo thành công.");
                } else {
                    req.getSession().setAttribute("msgError", "Không thể xóa thông báo.");
                }
            }
        }
        
        resp.sendRedirect(req.getContextPath() + "/admin/notifications");
    }
}

package com.swp.controller.user;

import com.swp.dao.NotificationDAO;
import com.swp.model.Notification;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {

    private NotificationDAO notificationDAO;

    @Override
    public void init() throws ServletException {
        notificationDAO = new NotificationDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = (User) request.getSession().getAttribute("user");
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            List<Notification> notifications = notificationDAO.getNotificationsByUserId(user.getUserId());
            request.setAttribute("notifications", notifications);
            
            request.getRequestDispatcher("/WEB-INF/user/notifications.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain");
            response.getWriter().write("Error: " + e.toString() + "\n");
            for (StackTraceElement element : e.getStackTrace()) {
                response.getWriter().write(element.toString() + "\n");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if ("mark_all".equals(action)) {
            notificationDAO.markAllAsRead(user.getUserId());
        } else if ("mark_read".equals(action)) {
            try {
                long notificationId = Long.parseLong(request.getParameter("id"));
                notificationDAO.markAsRead(notificationId, user.getUserId());
            } catch (NumberFormatException ignored) {
            }
        }
        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}

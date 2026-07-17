package com.swp.controller;

import com.google.gson.Gson;
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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/notifications")
public class NotificationAPI extends HttpServlet {

    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, 
                (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) -> 
                    new com.google.gson.JsonPrimitive(src.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Unauthorized\"}");
            out.flush();
            return;
        }

        User user = (User) session.getAttribute("user");
        long userId = user.getUserId();

        try {
            int unreadCount = notificationDAO.countUnread(userId);
            List<Notification> notifications = notificationDAO.getNotificationsByUserId(userId);

            // Limit to top 10 for dropdown
            if (notifications.size() > 10) {
                notifications = notifications.subList(0, 10);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("unreadCount", unreadCount);
            responseData.put("notifications", notifications);

            out.print(gson.toJson(responseData));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Internal Server Error\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Unauthorized\"}");
            out.flush();
            return;
        }

        User user = (User) session.getAttribute("user");
        long userId = user.getUserId();
        String action = req.getParameter("action");

        try {
            if ("mark_read".equals(action)) {
                long notifId = Long.parseLong(req.getParameter("id"));
                boolean success = notificationDAO.markAsRead(notifId, userId);
                out.print("{\"success\":" + success + "}");
            } else if ("mark_all_read".equals(action)) {
                boolean success = notificationDAO.markAllAsRead(userId);
                out.print("{\"success\":" + success + "}");
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Invalid action\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Internal Server Error\"}");
        }
        out.flush();
    }
}

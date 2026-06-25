package com.swp.controller.api;

import com.google.gson.Gson;
import com.swp.dao.NotificationDAO;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/notifications/*")
public class NotificationApiServlet extends HttpServlet {

    private NotificationDAO notificationDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        notificationDAO = new NotificationDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return;
        }

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();

        if ("/unread-count".equals(pathInfo)) {
            int count = notificationDAO.countUnread(user.getUserId());
            result.put("count", count);
            out.write(gson.toJson(result));
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write("{\"error\": \"Not Found\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return;
        }

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();

        if ("/mark-read".equals(pathInfo)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                try {
                    long id = Long.parseLong(idStr);
                    boolean success = notificationDAO.markAsRead(id, user.getUserId());
                    result.put("success", success);
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("error", "Invalid ID");
                }
            } else {
                result.put("success", false);
            }
            out.write(gson.toJson(result));
        } else if ("/mark-all-read".equals(pathInfo)) {
            boolean success = notificationDAO.markAllAsRead(user.getUserId());
            result.put("success", success);
            out.write(gson.toJson(result));
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write("{\"error\": \"Not Found\"}");
        }
    }
}

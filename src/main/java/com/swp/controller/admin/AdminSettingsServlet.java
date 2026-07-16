package com.swp.controller.admin;

import com.swp.dao.SystemSettingDAO;
import com.swp.model.SystemSetting;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/admin/settings")
public class AdminSettingsServlet extends HttpServlet {

    private SystemSettingDAO systemSettingDAO;

    @Override
    public void init() throws ServletException {
        systemSettingDAO = new SystemSettingDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<SystemSetting> settings = systemSettingDAO.getAllSettings();
            
            // Convert to map for easy access in JSP, handling null values and duplicates
            Map<String, String> settingsMap = settings.stream()
                    .collect(Collectors.toMap(
                            SystemSetting::getSettingKey,
                            s -> s.getSettingValue() == null ? "" : s.getSettingValue(),
                            (v1, v2) -> v1 // In case of duplicate keys, keep the first one
                    ));
            
            request.setAttribute("settings", settingsMap);
            request.getRequestDispatcher("/WEB-INF/admin/settings.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi xử lý cài đặt: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // Predefined setting keys
        String[] keys = {
            "MAINTENANCE_MODE",
            "CONTACT_EMAIL",
            "CONTACT_PHONE",
            "MAX_BOOKING_DAYS_AHEAD",
            "MIN_CANCELLATION_HOURS"
        };

        for (String key : keys) {
            String value = request.getParameter(key);
            if (value != null) {
                // If it's a checkbox (e.g., MAINTENANCE_MODE), it will be submitted as "true" or "on". 
                // We handle that in the JSP, but let's just save the string value.
                if (value.equals("on")) {
                    value = "true";
                }
                systemSettingDAO.updateSetting(key, value.trim());
            } else if (key.equals("MAINTENANCE_MODE")) {
                // If checkbox is unchecked, it won't be sent in the request
                systemSettingDAO.updateSetting(key, "false");
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/settings?success=1");
    }
}

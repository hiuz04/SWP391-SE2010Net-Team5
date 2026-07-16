package com.swp.controller.api;

import com.swp.dao.SystemSettingDAO;
import com.swp.model.SystemSetting;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/public/settings")
public class SettingsApiServlet extends HttpServlet {
    private SystemSettingDAO dao;

    @Override
    public void init() throws ServletException {
        dao = new SystemSettingDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        List<SystemSetting> settings = dao.getAllSettings();
        
        String email = "tranbaolong.280904@gmail.com"; // Default fallback
        String phone = "0385028924"; // Default fallback
        
        for (SystemSetting s : settings) {
            if ("CONTACT_EMAIL".equals(s.getSettingKey()) && s.getSettingValue() != null && !s.getSettingValue().isEmpty()) {
                email = s.getSettingValue();
            }
            if ("CONTACT_PHONE".equals(s.getSettingKey()) && s.getSettingValue() != null && !s.getSettingValue().isEmpty()) {
                phone = s.getSettingValue();
            }
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"email\":\"").append(email.replace("\"", "\\\"")).append("\",");
        json.append("\"phone\":\"").append(phone.replace("\"", "\\\"")).append("\"");
        json.append("}");

        PrintWriter out = resp.getWriter();
        out.print(json.toString());
        out.flush();
    }
}

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
            // Bước 1: Gọi DAO lấy toàn bộ các dòng cấu hình từ database bảng system_settings
            List<SystemSetting> settings = systemSettingDAO.getAllSettings();
            
            // Bước 2: Chuyển đổi List thành Map (key là tên cấu hình, value là giá trị) để JSP dễ truy xuất
            Map<String, String> settingsMap = settings.stream()
                    .collect(Collectors.toMap(
                            SystemSetting::getSettingKey,
                            s -> s.getSettingValue() == null ? "" : s.getSettingValue(),
                            (v1, v2) -> v1 // In case of duplicate keys, keep the first one
                    ));
            
            // Bước 3: Đẩy Map cấu hình sang View (settings.jsp) để hiển thị lên Form
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

        // Bước 1: Khai báo mảng chứa các Key cấu hình hệ thống cần cập nhật
        String[] keys = {
            "MAINTENANCE_MODE",
            "CONTACT_EMAIL",
            "CONTACT_PHONE",
            "MAX_BOOKING_DAYS_AHEAD",
            "MIN_CANCELLATION_HOURS",
            "VIP_SUBSCRIPTION_PRICE_MONTHLY",
            "VIP_DISCOUNT_PERCENTAGE",
            "DEPOSIT_PERCENTAGE",
            "BOOKING_HOLD_MINUTES"
        };

        // Bước 2: Duyệt qua từng Key và lấy giá trị tương ứng từ request (form submit)
        for (String key : keys) {
            String value = request.getParameter(key);
            if (value != null) {
                // Xử lý riêng cho checkbox: HTML form gửi "on" nếu được check
                if (value.equals("on")) {
                    value = "true";
                }
                // Bước 3: Gọi DAO cập nhật giá trị vào CSDL
                systemSettingDAO.updateSetting(key, value.trim());
            } else if (key.equals("MAINTENANCE_MODE")) {
                // Nếu là checkbox (ví dụ Chế độ bảo trì) mà không được tick -> form sẽ không gửi tham số này
                // Khi đó cần chủ động cập nhật thành false
                systemSettingDAO.updateSetting(key, "false");
            }
        }

        // Bước 4: Hoàn thành, chuyển hướng lại trang cấu hình kèm cờ thành công
        response.sendRedirect(request.getContextPath() + "/admin/settings?success=1");
    }
}

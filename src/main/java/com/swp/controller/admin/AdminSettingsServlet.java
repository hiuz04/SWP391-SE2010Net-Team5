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
    private com.swp.dao.NotificationDAO notificationDAO;

    @Override
    public void init() throws ServletException {
        systemSettingDAO = new SystemSettingDAO();
        notificationDAO = new com.swp.dao.NotificationDAO();
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
            "BOOKING_HOLD_MINUTES",
            "REWARD_POINTS_PERCENTAGE"
        };

        // Bước 2: Duyệt qua từng Key và lấy giá trị tương ứng từ request, thực hiện Validate
        java.util.Map<String, String> newValues = new java.util.HashMap<>();
        java.util.Map<String, String> errors = new java.util.HashMap<>();
        
        for (String key : keys) {
            String value = request.getParameter(key);
            if (value != null) {
                if (value.equals("on")) {
                    value = "true";
                }
                value = value.trim();
            } else if (key.equals("MAINTENANCE_MODE")) {
                value = "false";
            }
            newValues.put(key, value);

            // Validation logic
            if (value != null && !value.isEmpty()) {
                try {
                    switch (key) {
                        case "CONTACT_EMAIL":
                            if (!value.matches("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
                                errors.put(key, "Email không hợp lệ.");
                            }
                            break;
                        case "CONTACT_PHONE":
                            if (!value.matches("^0\\d{9,10}$")) {
                                errors.put(key, "Số điện thoại phải bắt đầu bằng 0 và có 10-11 số.");
                            }
                            break;
                        case "MAX_BOOKING_DAYS_AHEAD":
                            int maxDays = Integer.parseInt(value);
                            if (maxDays < 1 || maxDays > 365) errors.put(key, "Phải từ 1 đến 365 ngày.");
                            break;
                        case "MIN_CANCELLATION_HOURS":
                        case "VIP_SUBSCRIPTION_PRICE_MONTHLY":
                            if (Integer.parseInt(value) < 0) errors.put(key, "Không được nhỏ hơn 0.");
                            break;
                        case "VIP_DISCOUNT_PERCENTAGE":
                        case "DEPOSIT_PERCENTAGE":
                        case "REWARD_POINTS_PERCENTAGE":
                            int pct = Integer.parseInt(value);
                            if (pct < 0 || pct > 100) errors.put(key, "Phải từ 0 đến 100%.");
                            break;
                        case "BOOKING_HOLD_MINUTES":
                            if (Integer.parseInt(value) < 1) errors.put(key, "Phải lớn hơn 0.");
                            break;
                    }
                } catch (NumberFormatException e) {
                    errors.put(key, "Phải là số nguyên hợp lệ.");
                }
            }
        }

        // Bước 3: Nếu có lỗi -> Trả về giao diện kèm thông báo lỗi
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("settings", newValues); // Giữ lại giá trị vừa nhập
            request.setAttribute("errorMsg", "Vui lòng kiểm tra lại dữ liệu đầu vào.");
            request.getRequestDispatcher("/WEB-INF/admin/settings.jsp").forward(request, response);
            return;
        }

        // Bước 4: So sánh và Lưu nếu hợp lệ
        java.util.List<com.swp.model.SystemSetting> oldSettingsList = systemSettingDAO.getAllSettings();
        java.util.Map<String, String> oldSettings = new java.util.HashMap<>();
        for (com.swp.model.SystemSetting s : oldSettingsList) {
            oldSettings.put(s.getSettingKey(), s.getSettingValue());
        }

        java.util.Map<String, String> keyLabels = new java.util.HashMap<>();
        keyLabels.put("MAINTENANCE_MODE", "Chế độ bảo trì hệ thống");
        keyLabels.put("CONTACT_EMAIL", "Email liên hệ");
        keyLabels.put("CONTACT_PHONE", "Số điện thoại hỗ trợ");
        keyLabels.put("MAX_BOOKING_DAYS_AHEAD", "Giới hạn đặt trước (ngày)");
        keyLabels.put("MIN_CANCELLATION_HOURS", "Thời gian hủy miễn phí (giờ)");
        keyLabels.put("VIP_SUBSCRIPTION_PRICE_MONTHLY", "Giá gói VIP 1 tháng");
        keyLabels.put("VIP_DISCOUNT_PERCENTAGE", "Giảm giá VIP (%)");
        keyLabels.put("DEPOSIT_PERCENTAGE", "Tỉ lệ đặt cọc (%)");
        keyLabels.put("BOOKING_HOLD_MINUTES", "Thời gian giữ chỗ (phút)");
        keyLabels.put("REWARD_POINTS_PERCENTAGE", "Tỉ lệ tích điểm (%)");

        StringBuilder changes = new StringBuilder("Admin vừa cập nhật cài đặt hệ thống:\n");
        boolean hasChanges = false;

        for (String key : keys) {
            String value = newValues.get(key);
            if (value != null) {
                String oldValue = oldSettings.get(key);
                if (oldValue == null) oldValue = "";
                
                if (!oldValue.equals(value)) {
                    systemSettingDAO.updateSetting(key, value);
                    
                    String displayOld = oldValue;
                    String displayNew = value;
                    if ("MAINTENANCE_MODE".equals(key)) {
                        displayOld = "true".equals(oldValue) ? "Bật" : "Tắt";
                        displayNew = "true".equals(value) ? "Bật" : "Tắt";
                    } else {
                        if (displayOld.isEmpty()) displayOld = "trống";
                    }

                    String label = keyLabels.getOrDefault(key, key);
                    changes.append("- ").append(label).append(": từ [").append(displayOld).append("] thành [").append(displayNew).append("]\n");
                    hasChanges = true;
                }
            }
        }

        // Bước 5: Thông báo cho Owner về việc thay đổi hệ thống nếu có thay đổi
        if (hasChanges) {
            notificationDAO.notifyRole("OWNER", "Thay đổi hệ thống", changes.toString().trim(), "SYSTEM", null);
            notificationDAO.notifyRole("ADMIN", "Thay đổi hệ thống", changes.toString().trim(), "SYSTEM", null);
        }

        // Bước 6: Hoàn thành, chuyển hướng lại trang cấu hình kèm cờ thành công
        response.sendRedirect(request.getContextPath() + "/admin/settings?success=1");
    }
}

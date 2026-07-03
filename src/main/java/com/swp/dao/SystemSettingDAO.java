package com.swp.dao;

import com.swp.model.SystemSetting;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SystemSettingDAO {

    public List<SystemSetting> getAllSettings() {
        List<SystemSetting> settings = new ArrayList<>();
        String sql = "SELECT * FROM system_settings";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SystemSetting setting = new SystemSetting(
                        rs.getLong("setting_id"),
                        rs.getString("setting_key"),
                        rs.getString("setting_value"),
                        rs.getString("description"),
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
                );
                settings.add(setting);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settings;
    }

    public Optional<SystemSetting> getSettingByKey(String key) {
        String sql = "SELECT * FROM system_settings WHERE setting_key = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SystemSetting setting = new SystemSetting(
                            rs.getLong("setting_id"),
                            rs.getString("setting_key"),
                            rs.getString("setting_value"),
                            rs.getString("description"),
                            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
                    );
                    return Optional.of(setting);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void updateSetting(String key, String value) {
        String sql = "UPDATE system_settings SET setting_value = ?, updated_at = GETDATE() WHERE setting_key = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

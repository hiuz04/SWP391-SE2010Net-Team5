package com.swp.dao;

import com.swp.model.ActivityLog;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ActivityLogDAO {

    public void insertLog(long userId, String action, String description) {
        String sql = """
                INSERT INTO activity_logs (user_id, action, description, created_at)
                VALUES (?, ?, ?, GETDATE())
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, action);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu nhật ký hoạt động: " + e.getMessage(), e);
        }
    }
}

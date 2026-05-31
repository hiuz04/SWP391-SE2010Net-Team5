package com.swp.dao;

import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class RoleDAO {

    public Optional<Integer> findRoleIdByName(String roleName) {
        String sql = "SELECT role_id FROM roles WHERE role_name = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("role_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn vai trò: " + e.getMessage(), e);
        }
        return Optional.empty();
    }
}

package com.swp.dao;

import com.swp.model.User;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDAO {

    private static final String FIND_BY_EMAIL_AND_PASSWORD = """
            SELECT user_id, role_id, full_name, email, phone, password_hash,
                   avatar_url, google_id, status, created_at, updated_at
            FROM users
            WHERE email = ? AND password_hash = ? AND status = 'ACTIVE'
            """;

    public Optional<User> findByEmailAndPassword(String email, String password) {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL_AND_PASSWORD)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn người dùng: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setRoleId(rs.getInt("role_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setGoogleId(rs.getString("google_id"));
        user.setStatus(rs.getString("status"));
        user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        user.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return user;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}

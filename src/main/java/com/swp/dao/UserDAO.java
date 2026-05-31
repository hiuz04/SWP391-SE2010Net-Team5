package com.swp.dao;

import com.swp.model.User;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDAO {

    private static final String FIND_BY_LOGIN_AND_PASSWORD = """
            SELECT u.user_id, u.role_id, u.full_name, u.email, u.phone, u.password_hash,
                   u.avatar_url, u.google_id, u.status, u.created_at, u.updated_at,
                   r.role_name
            FROM users u
            INNER JOIN roles r ON u.role_id = r.role_id
            WHERE (u.email = ? OR u.phone = ?) AND u.password_hash = ? AND u.status = 'ACTIVE'
            """;

    public Optional<User> findByLoginAndPassword(String login, String password) {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_LOGIN_AND_PASSWORD)) {
            ps.setString(1, login);
            ps.setString(2, login);
            ps.setString(3, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    user.setRoleName(rs.getString("role_name"));
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn người dùng: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    public boolean existsByPhone(String phone) {
        return exists("SELECT 1 FROM users WHERE phone = ?", phone);
    }

    public long insert(User user) {
        String sql = """
                INSERT INTO users (role_id, full_name, email, phone, password_hash, status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, user.getRoleId());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo tài khoản: " + e.getMessage(), e);
        }
        return -1;
    }

    private boolean exists(String sql, String value) {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra người dùng: " + e.getMessage(), e);
        }
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

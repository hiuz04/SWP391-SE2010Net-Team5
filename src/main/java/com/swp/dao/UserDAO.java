package com.swp.dao;

import com.swp.model.User;
import com.swp.util.DBContext;
import com.swp.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDAO {

    private static final String USER_SELECT = """
            SELECT u.user_id, u.role_id, u.full_name, u.email, u.phone, u.password_hash,
                   u.avatar_url, u.google_id, u.status, u.created_at, u.updated_at, u.available_reward_points,
                   u.is_vip, u.vip_valid_until, r.role_name
            FROM users u
            INNER JOIN roles r ON u.role_id = r.role_id
            """;

    private static final String FIND_BY_GOOGLE_ID = USER_SELECT + """
            WHERE u.google_id = ? AND u.status = 'ACTIVE'
            """;

    private static final String FIND_BY_EMAIL = USER_SELECT + """
            WHERE u.email = ? AND u.status = 'ACTIVE'
            """;

    private static final String FIND_BY_EMAIL_OR_PHONE = USER_SELECT + """
            WHERE (u.email = ? OR u.phone = ?) AND u.status = 'ACTIVE'
            """;

    /**
     * Xác thực đăng nhập: tìm user theo email hoặc số điện thoại kết hợp với mật
     * khẩu.(user : active)
     * Mật khẩu được kiểm tra an toàn bằng băm BCrypt qua PasswordUtil.
     * 
     * @return {@code Optional<User>} chứa user nếu thông tin hợp lệ, ngược lại
     *         {@code Optional.empty()}
     */
    public Optional<User> findByLoginAndPassword(String login, String password) {
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL_OR_PHONE)) {
            ps.setString(1, login);
            ps.setString(2, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    if (PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                        return Optional.of(user);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn người dùng: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<User> findByGoogleId(String googleId) {
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_BY_GOOGLE_ID)) {
            ps.setString(1, googleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn Google ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn email: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tìm user theo email HOẶC số điện thoại (dùng cho chức năng quên mật khẩu).
     */
    public Optional<User> findByEmailOrPhone(String input) {
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL_OR_PHONE)) {
            ps.setString(1, input);
            ps.setString(2, input);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm user theo email/phone: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Cập nhật mật khẩu mới cho user (plain text, phù hợp với cách lưu hiện tại).
     */
    public void updatePassword(long userId, String newPassword) {
        String sql = """
                UPDATE users
                SET password_hash = ?, updated_at = GETDATE()
                WHERE user_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật mật khẩu: " + e.getMessage(), e);
        }
    }

    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    public boolean existsByPhone(String phone) {
        return exists("SELECT 1 FROM users WHERE phone = ?", phone);
    }

    public boolean existsByEmailExcludeUser(String email, long userId) {
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE email = ? AND user_id != ?")) {
            ps.setString(1, email);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra trùng email: " + e.getMessage(), e);
        }
    }

    public boolean existsByPhoneExcludeUser(String phone, long userId) {
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE phone = ? AND user_id != ?")) {
            ps.setString(1, phone);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra trùng số điện thoại: " + e.getMessage(), e);
        }
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
            return readGeneratedKey(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo tài khoản: " + e.getMessage(), e);
        }
    }

    public long insertGoogleUser(User user) {
        String sql = """
                INSERT INTO users (role_id, full_name, email, phone, password_hash, avatar_url, google_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, user.getRoleId());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getAvatarUrl());
            ps.setString(7, user.getGoogleId());
            ps.executeUpdate();
            return readGeneratedKey(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo tài khoản Google: " + e.getMessage(), e);
        }
    }

    public void linkGoogleAccount(long userId, String googleId, String avatarUrl) {
        String sql = """
                UPDATE users
                SET google_id = ?, avatar_url = COALESCE(?, avatar_url), updated_at = GETDATE()
                WHERE user_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, googleId);
            ps.setString(2, avatarUrl);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi liên kết tài khoản Google: " + e.getMessage(), e);
        }
    }

    private long readGeneratedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
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

    public void updateProfile(User user) {
        String sql = """
                UPDATE users
                SET full_name = ?, phone = ?, email = ?, password_hash = ?, updated_at = GETDATE()
                WHERE user_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setLong(5, user.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật hồ sơ: " + e.getMessage(), e);
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
        user.setRoleName(rs.getString("role_name"));
        user.setVip(rs.getBoolean("is_vip"));
        user.setVipValidUntil(toLocalDateTime(rs.getTimestamp("vip_valid_until")));
        user.setRewardPoints(rs.getInt("available_reward_points"));
        return user;
    }

    // lay danh sach user trong admindashboard
    public java.util.List<User> getAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(USER_SELECT + " ORDER BY u.created_at DESC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách người dùng: " + e.getMessage(), e);
        }
        return users;
    }

    public java.util.List<User> getUsersPaginated(String search, String role, String status, int offset, int limit) {
        java.util.List<User> users = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder(USER_SELECT + " WHERE 1=1 ");

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (u.full_name LIKE ? OR u.email LIKE ? OR u.phone LIKE ?) ");
        }
        if (role != null && !role.trim().isEmpty()) {
            sql.append(" AND r.role_name = ? ");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND u.status = ? ");
        }
        sql.append(" ORDER BY u.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY ");

        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String likeSearch = "%" + search.trim() + "%";
                ps.setString(paramIndex++, likeSearch);
                ps.setString(paramIndex++, likeSearch);
                ps.setString(paramIndex++, likeSearch);
            }
            if (role != null && !role.trim().isEmpty()) {
                ps.setString(paramIndex++, role.trim());
            }
            if (status != null && !status.trim().isEmpty()) {
                ps.setString(paramIndex++, status.trim());
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex++, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn danh sách người dùng phân trang: " + e.getMessage(), e);
        }
        return users;
    }

    public int countUsers(String search, String role, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM users u INNER JOIN roles r ON u.role_id = r.role_id WHERE 1=1 ");

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (u.full_name LIKE ? OR u.email LIKE ? OR u.phone LIKE ?) ");
        }
        if (role != null && !role.trim().isEmpty()) {
            sql.append(" AND r.role_name = ? ");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND u.status = ? ");
        }

        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String likeSearch = "%" + search.trim() + "%";
                ps.setString(paramIndex++, likeSearch);
                ps.setString(paramIndex++, likeSearch);
                ps.setString(paramIndex++, likeSearch);
            }
            if (role != null && !role.trim().isEmpty()) {
                ps.setString(paramIndex++, role.trim());
            }
            if (status != null && !status.trim().isEmpty()) {
                ps.setString(paramIndex++, status.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đếm số lượng người dùng: " + e.getMessage(), e);
        }
        return 0;
    }

    public Optional<User> getUserById(long userId) {
        String sql = USER_SELECT + " WHERE u.user_id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn người dùng theo ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void updateUserStatus(long userId, String status) {
        String sql = "UPDATE users SET status = ?, updated_at = GETDATE() WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật trạng thái người dùng: " + e.getMessage(), e);
        }
    }

    public void deleteUser(long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa người dùng: " + e.getMessage(), e);
        }
    }

    public void updateUserByAdmin(User user) {
        String sql = """
                UPDATE users
                SET full_name = ?, phone = ?, email = ?, role_id = ?, status = ?,
                    password_hash = COALESCE(NULLIF(?, ''), password_hash),
                    updated_at = GETDATE()
                WHERE user_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setInt(4, user.getRoleId());
            ps.setString(5, user.getStatus());
            ps.setString(6, user.getPasswordHash());
            ps.setLong(7, user.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật thông tin người dùng bởi admin: " + e.getMessage(), e);
        }
    }

    public void updateVipStatus(long userId, LocalDateTime newValidUntil) {
        String sql = "UPDATE users SET is_vip = 1, vip_valid_until = ?, updated_at = GETDATE() WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (newValidUntil != null) {
                ps.setTimestamp(1, Timestamp.valueOf(newValidUntil));
            } else {
                ps.setNull(1, java.sql.Types.TIMESTAMP);
            }
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật trạng thái VIP: " + e.getMessage(), e);
        }
    }

    public int getRewardPoint(long userId) {
        String sql = """
            SELECT available_reward_points
            FROM users
            WHERE user_id = ?
            """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("available_reward_points");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int getAvailableRewardPoints(long userId) {
        String sql = "SELECT available_reward_points FROM users WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("available_reward_points") : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}

package com.swp.dao;

import com.swp.model.PasswordResetToken;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetTokenDAO {

    public void insertToken(long userId, String otpCode, LocalDateTime expiresAt) {
        String sql = """
                INSERT INTO password_reset_tokens (user_id, otp_code, expires_at, used, created_at)
                VALUES (?, ?, ?, 0, GETDATE())
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, otpCode);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu OTP reset password: " + e.getMessage(), e);
        }
    }

    public Optional<PasswordResetToken> findValidOtp(long userId, String otpCode) {
        String sql = """
                SELECT token_id, user_id, token, otp_code, expires_at, used, created_at
                FROM password_reset_tokens
                WHERE user_id = ? AND otp_code = ? AND used = 0 AND expires_at > GETDATE()
                ORDER BY created_at DESC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, otpCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn OTP: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void markAsUsed(long tokenId) {
        String sql = "UPDATE password_reset_tokens SET used = 1 WHERE token_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tokenId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đánh dấu OTP: " + e.getMessage(), e);
        }
    }

    public void invalidateOldTokens(long userId) {
        String sql = "UPDATE password_reset_tokens SET used = 1 WHERE user_id = ? AND used = 0";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi vô hiệu hóa OTP cũ: " + e.getMessage(), e);
        }
    }

    private PasswordResetToken mapRow(ResultSet rs) throws SQLException {
        PasswordResetToken t = new PasswordResetToken();
        t.setTokenId(rs.getLong("token_id"));
        t.setUserId(rs.getLong("user_id"));
        t.setToken(rs.getString("token"));
        t.setOtpCode(rs.getString("otp_code"));
        t.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        t.setUsed(rs.getBoolean("used"));
        t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return t;
    }
}

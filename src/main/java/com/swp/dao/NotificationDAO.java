package com.swp.dao;

import com.swp.model.Notification;
import com.swp.util.DBContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> getNotificationsByUserId(long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countUnread(long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean markAsRead(long notificationId, long userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ? AND user_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, notificationId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAllAsRead(long userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean insertNotification(Notification n) {
        String sql = "INSERT INTO notifications (user_id, title, message, notification_type, reference_id, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, n.getUserId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getNotificationType());
            if (n.getReferenceId() != null) {
                ps.setLong(5, n.getReferenceId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setBoolean(6, n.getIsRead() != null ? n.getIsRead() : false);
            ps.setTimestamp(7, n.getCreatedAt() != null ? Timestamp.valueOf(n.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        n.setNotificationId(rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void notifyRole(String roleName, String title, String message, String type, Long referenceId) {
        String sql = "INSERT INTO notifications (user_id, title, message, notification_type, reference_id, is_read, created_at) " +
                     "SELECT u.user_id, ?, ?, ?, ?, 0, GETDATE() " +
                     "FROM users u JOIN roles r ON u.role_id = r.role_id " +
                     "WHERE UPPER(r.role_name) = UPPER(?)";
        try (Connection conn = DBContext.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, type);
            if (referenceId != null) {
                ps.setLong(4, referenceId);
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, roleName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendBookingReminders() {
        String sql = "INSERT INTO notifications (user_id, title, message, notification_type, reference_id, is_read, created_at) " +
                     "SELECT b.customer_id, N'Sắp đến giờ đá!', " +
                     "N'Bạn có lịch đặt sân vào lúc ' + FORMAT(b.start_time, 'HH:mm dd/MM/yyyy') + N'. Vui lòng đến trước 10 phút để nhận sân.', " +
                     "'REMINDER', b.booking_id, 0, GETDATE() " +
                     "FROM bookings b " +
                     "WHERE b.status = 'CONFIRMED' " +
                     "AND b.start_time BETWEEN DATEADD(minute, 50, GETDATE()) AND DATEADD(minute, 70, GETDATE()) " +
                     "AND NOT EXISTS (" +
                     "   SELECT 1 FROM notifications n " +
                     "   WHERE n.reference_id = b.booking_id AND n.notification_type = 'REMINDER'" +
                     ")";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendMembershipReminders() {
        String sql = "INSERT INTO notifications (user_id, title, message, notification_type, reference_id, is_read, created_at) " +
                     "SELECT u.user_id, N'Gia hạn Gói Hội Viên', " +
                     "N'Gói Hội Viên VIP của bạn sẽ hết hạn vào ' + FORMAT(u.vip_valid_until, 'dd/MM/yyyy HH:mm') + N'. Vui lòng gia hạn để không bị gián đoạn đặc quyền.', " +
                     "'SYSTEM', NULL, 0, GETDATE() " +
                     "FROM users u " +
                     "WHERE u.is_vip = 1 AND u.vip_valid_until IS NOT NULL " +
                     "AND u.vip_valid_until BETWEEN GETDATE() AND DATEADD(day, 3, GETDATE()) " +
                     "AND NOT EXISTS (" +
                     "   SELECT 1 FROM notifications n " +
                     "   WHERE n.user_id = u.user_id AND n.title = N'Gia hạn Gói Hội Viên' " +
                     "   AND n.created_at >= DATEADD(day, -3, GETDATE())" +
                     ")";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("notification_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setNotificationType(rs.getString("notification_type"));
        
        long refId = rs.getLong("reference_id");
        if (!rs.wasNull()) {
            n.setReferenceId(refId);
        }
        
        n.setIsRead(rs.getBoolean("is_read"));
        
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            n.setCreatedAt(created.toLocalDateTime());
        }
        return n;
    }

    public List<Notification> getGlobalNotifications() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT title, message, notification_type, MAX(created_at) as created_at, MAX(reference_id) as reference_id " +
                     "FROM notifications " +
                     "GROUP BY title, message, notification_type " +
                     "ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Notification n = new Notification();
                n.setTitle(rs.getString("title"));
                n.setMessage(rs.getString("message"));
                n.setNotificationType(rs.getString("notification_type"));
                
                long refId = rs.getLong("reference_id");
                if (!rs.wasNull()) {
                    n.setReferenceId(refId);
                }
                
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    n.setCreatedAt(created.toLocalDateTime());
                }
                list.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteGlobalNotification(String title, String message, String type) {
        String sql = "DELETE FROM notifications WHERE title = ? AND message = ? AND notification_type = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, type);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

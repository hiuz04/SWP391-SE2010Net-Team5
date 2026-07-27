package com.swp.dao;

import com.swp.model.WorkShift;
import com.swp.model.User;
import com.swp.util.DBContext;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkShiftDAO - Data Access Object phụ trách thao tác dữ liệu Ca làm việc (work_shifts)
 * và Phân công ca trực (shift_assignments) cho chủ cơ sở (Owner) và nhân viên (Staff).
 */
public class WorkShiftDAO {

    /**
     * Chuyển đổi từ java.sql.Timestamp sang java.time.LocalDateTime.
     */
    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /**
     * Tự động hợp nhất các bản ghi ca trực trùng lặp (cùng cơ sở, cùng ngày, cùng khung giờ)
     * thành 1 ca duy nhất và gom tất cả nhân viên được phân công vào ca duy nhất đó.
     */
    public void consolidateDuplicateShifts() {
        String sql = """
            SELECT ws1.shift_id AS keep_id, ws2.shift_id AS delete_id
            FROM work_shifts ws1
            JOIN work_shifts ws2 ON ws1.complex_id = ws2.complex_id
                                AND ws1.shift_date = ws2.shift_date
                                AND ws1.start_time = ws2.start_time
                                AND ws1.end_time = ws2.end_time
                                AND ws1.shift_id < ws2.shift_id
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long keepId = rs.getLong("keep_id");
                long deleteId = rs.getLong("delete_id");

                // Chuyển tất cả nhân viên phân công ở ca trùng (deleteId) sang ca chuẩn (keepId)
                String moveAssignSql = """
                    UPDATE shift_assignments
                    SET shift_id = ?
                    WHERE shift_id = ?
                      AND staff_id NOT IN (SELECT staff_id FROM shift_assignments WHERE shift_id = ?)
                    """;
                try (PreparedStatement psMove = conn.prepareStatement(moveAssignSql)) {
                    psMove.setLong(1, keepId);
                    psMove.setLong(2, deleteId);
                    psMove.setLong(3, keepId);
                    psMove.executeUpdate();
                }

                // Xóa bản ghi phân công thừa của ca trùng
                try (PreparedStatement psDelAssign = conn.prepareStatement("DELETE FROM shift_assignments WHERE shift_id = ?")) {
                    psDelAssign.setLong(1, deleteId);
                    psDelAssign.executeUpdate();
                }

                // Xóa thông báo liên quan ca trùng
                try (PreparedStatement psNotif = conn.prepareStatement("DELETE FROM notifications WHERE reference_id = ?")) {
                    psNotif.setLong(1, deleteId);
                    psNotif.executeUpdate();
                }

                // Xóa bản ghi ca trùng khỏi work_shifts
                try (PreparedStatement psDelShift = conn.prepareStatement("DELETE FROM work_shifts WHERE shift_id = ?")) {
                    psDelShift.setLong(1, deleteId);
                    psDelShift.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
        }
    }

    /**
     * Tự động dọn dẹp các ca trực trùng lặp rác (cùng cơ sở, ngày, giờ bắt đầu & kết thúc)
     * mà không có nhân viên nào được phân công.
     */
    public void cleanAllDuplicateShifts() {
        consolidateDuplicateShifts();
        String sql = """
            DELETE FROM work_shifts
            WHERE shift_id IN (
                SELECT ws.shift_id FROM work_shifts ws
                JOIN work_shifts ws2 ON ws2.complex_id = ws.complex_id
                                    AND ws2.shift_date = ws.shift_date
                                    AND ws2.start_time = ws.start_time
                                    AND ws2.end_time = ws.end_time
                                    AND ws2.shift_id < ws.shift_id
                WHERE ws.shift_id NOT IN (SELECT shift_id FROM shift_assignments)
            )
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    /**
     * Lấy toàn bộ danh sách tất cả các ca làm việc trong hệ thống (Sắp xếp ngày giảm dần, giờ tăng dần).
     */
    public List<WorkShift> getAllShifts() {
        cleanAllDuplicateShifts();
        List<WorkShift> list = new ArrayList<>();
        String sql = "SELECT * FROM work_shifts ORDER BY shift_date DESC, start_time ASC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapWorkShift(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách ca làm việc: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Lấy danh sách ca làm việc theo ID cơ sở/cụm sân bóng.
     */
    public List<WorkShift> getShiftsByComplex(long complexId) {
        List<WorkShift> list = new ArrayList<>();
        String sql = "SELECT * FROM work_shifts WHERE complex_id = ? ORDER BY shift_date DESC, start_time ASC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWorkShift(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy ca làm việc theo cụm sân: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Lấy thông tin chi tiết một ca làm việc theo shift_id.
     */
    public WorkShift getShiftById(long shiftId) {
        String sql = "SELECT * FROM work_shifts WHERE shift_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapWorkShift(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy ca làm việc theo ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Thêm mới một ca làm việc vào bảng work_shifts.
     * @return shift_id vừa tạo, hoặc -1 nếu thất bại.
     */
    public long insertShift(WorkShift shift) {
        String sql = "INSERT INTO work_shifts (complex_id, shift_name, shift_date, start_time, end_time, created_at) VALUES (?, ?, ?, ?, ?, GETDATE())";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, shift.getComplexId());
            ps.setString(2, shift.getShiftName());
            ps.setDate(3, Date.valueOf(shift.getShiftDate()));
            ps.setTime(4, Time.valueOf(shift.getStartTime()));
            ps.setTime(5, Time.valueOf(shift.getEndTime()));
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm ca làm việc: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Cập nhật thông tin ca làm việc (tên ca, ngày trực, giờ bắt đầu/kết thúc).
     */
    public boolean updateShift(WorkShift shift) {
        String sql = "UPDATE work_shifts SET complex_id = ?, shift_name = ?, shift_date = ?, start_time = ?, end_time = ? WHERE shift_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shift.getComplexId());
            ps.setString(2, shift.getShiftName());
            ps.setDate(3, Date.valueOf(shift.getShiftDate()));
            ps.setTime(4, Time.valueOf(shift.getStartTime()));
            ps.setTime(5, Time.valueOf(shift.getEndTime()));
            ps.setLong(6, shift.getShiftId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật ca làm việc: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa vĩnh viễn (Hard Delete) ca làm việc khỏi Cơ sở dữ liệu:
     * 1. Xóa các thông báo liên quan trong bảng notifications
     * 2. Xóa các bản ghi phân công trong bảng shift_assignments
     * 3. Xóa bản ghi ca trực trong bảng work_shifts
     */
    public boolean deleteShift(long shiftId) {
        String deleteNotifSql = "DELETE FROM notifications WHERE reference_id = ?";
        String deleteAssignSql = "DELETE FROM shift_assignments WHERE shift_id = ?";
        String deleteShiftSql = "DELETE FROM work_shifts WHERE shift_id = ?";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps0 = conn.prepareStatement(deleteNotifSql)) {
                    ps0.setLong(1, shiftId);
                    ps0.executeUpdate();
                }
                try (PreparedStatement ps1 = conn.prepareStatement(deleteAssignSql)) {
                    ps1.setLong(1, shiftId);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = conn.prepareStatement(deleteShiftSql)) {
                    ps2.setLong(1, shiftId);
                    int affected = ps2.executeUpdate();
                    conn.commit();
                    return affected > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa ca làm việc: " + e.getMessage(), e);
        }
    }

    /**
     * Dọn dẹp ca trùng lặp cùng khung giờ ở cùng cơ sở mà không có nhân viên trực.
     */
    public void cleanDuplicateShifts(long complexId, LocalDate shiftDate, LocalTime startTime, LocalTime endTime, long keepShiftId) {
        String sql = """
            DELETE FROM work_shifts
            WHERE complex_id = ?
              AND shift_date = ?
              AND start_time = CAST(? AS TIME)
              AND end_time = CAST(? AS TIME)
              AND shift_id <> ?
              AND shift_id NOT IN (SELECT shift_id FROM shift_assignments)
            """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            ps.setDate(2, Date.valueOf(shiftDate));
            ps.setObject(3, Time.valueOf(startTime));
            ps.setObject(4, Time.valueOf(endTime));
            ps.setLong(5, keepShiftId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy danh sách tất cả nhân viên (Role Staff = 3) đang hoạt động (STATUS = 'ACTIVE').
     */
    public List<User> getAllActiveStaff() {
        List<User> list = new ArrayList<>();
        String sql = """
                SELECT u.user_id, u.role_id, u.full_name, u.email, u.phone, u.password_hash,
                       u.avatar_url, u.google_id, u.status, u.created_at, u.updated_at,
                       r.role_name
                FROM users u
                INNER JOIN roles r ON u.role_id = r.role_id
                WHERE u.role_id = 3 AND u.status = 'ACTIVE'
                ORDER BY u.full_name ASC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setRoleId(rs.getInt("role_id"));
                
                String rawName = rs.getString("full_name");
                user.setFullName(rawName != null ? rawName.trim() : "");
                
                String rawEmail = rs.getString("email");
                user.setEmail(rawEmail != null ? rawEmail.trim() : "");
                
                String rawPhone = rs.getString("phone");
                user.setPhone(rawPhone != null ? rawPhone.trim() : "");
                
                user.setPasswordHash(rs.getString("password_hash"));
                user.setAvatarUrl(rs.getString("avatar_url"));
                user.setGoogleId(rs.getString("google_id"));
                
                String rawStatus = rs.getString("status");
                user.setStatus(rawStatus != null ? rawStatus.trim() : "");
                
                user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                user.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
                
                String rawRole = rs.getString("role_name");
                user.setRoleName(rawRole != null ? rawRole.trim() : "");
                list.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách nhân viên: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Lấy danh sách nhân viên được phân công vào một ca trực cụ thể.
     */
    public List<User> getStaffAssignedToShift(long shiftId) {
        List<User> list = new ArrayList<>();
        String sql = """
                SELECT u.user_id, u.role_id, u.full_name, u.email, u.phone, u.avatar_url, u.status
                FROM users u
                INNER JOIN shift_assignments sa ON u.user_id = sa.staff_id
                WHERE sa.shift_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getLong("user_id"));
                    user.setRoleId(rs.getInt("role_id"));
                    
                    String rawName = rs.getString("full_name");
                    user.setFullName(rawName != null ? rawName.trim() : "");
                    
                    String rawEmail = rs.getString("email");
                    user.setEmail(rawEmail != null ? rawEmail.trim() : "");
                    
                    String rawPhone = rs.getString("phone");
                    user.setPhone(rawPhone != null ? rawPhone.trim() : "");
                    
                    user.setAvatarUrl(rs.getString("avatar_url"));
                    
                    String rawStatus = rs.getString("status");
                    user.setStatus(rawStatus != null ? rawStatus.trim() : "");
                    list.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy nhân viên được phân ca: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Lấy thông tin ngắn của một nhân viên theo user_id.
     */
    public User getStaffById(long staffId) {
        String sql = "SELECT user_id, role_id, full_name, email, phone, avatar_url, status FROM users WHERE user_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getLong("user_id"));
                    user.setRoleId(rs.getInt("role_id"));
                    user.setFullName(rs.getString("full_name"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setAvatarUrl(rs.getString("avatar_url"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Phân công nhân viên vào ca trực. Ngăn ngừa trùng lặp bản ghi phân công.
     */
    public boolean assignStaffToShift(long shiftId, long staffId) {
        String checkSql = "SELECT COUNT(*) FROM shift_assignments WHERE shift_id = ? AND staff_id = ?";
        String insertSql = "INSERT INTO shift_assignments (shift_id, staff_id, status) VALUES (?, ?, 'ASSIGNED')";
        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setLong(1, shiftId);
                psCheck.setLong(2, staffId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true; // Đã phân công từ trước
                    }
                }
            }
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psInsert.setLong(1, shiftId);
                psInsert.setLong(2, staffId);
                return psInsert.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi phân ca làm việc cho nhân viên: " + e.getMessage(), e);
        }
    }

    /**
     * Hủy phân công nhân viên khỏi ca trực.
     */
    public boolean removeStaffFromShift(long shiftId, long staffId) {
        String sql = "DELETE FROM shift_assignments WHERE shift_id = ? AND staff_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shiftId);
            ps.setLong(2, staffId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi hủy phân ca cho nhân viên: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra nhân viên có bị trùng ca làm việc ở một cơ sở khác trong cùng khung giờ hay không.
     * Hỗ trợ ca đêm vượt mốc 00:00 (Ví dụ: 17:00 - 24:00).
     */
    public boolean hasOverlappingShift(long staffId, LocalDate date, LocalTime start, LocalTime end, Long excludeShiftId) {
        WorkShift excludeShift = excludeShiftId != null ? getShiftById(excludeShiftId) : null;

        String sql = "SELECT ws.* " +
                     "FROM work_shifts ws " +
                     "JOIN shift_assignments sa ON ws.shift_id = sa.shift_id " +
                     "WHERE sa.staff_id = ? AND ws.shift_date BETWEEN ? AND ?";
        List<WorkShift> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             ps.setLong(1, staffId);
             ps.setDate(2, Date.valueOf(date.minusDays(1)));
             ps.setDate(3, Date.valueOf(date.plusDays(1)));
             try (ResultSet rs = ps.executeQuery()) {
                 while (rs.next()) {
                     list.add(mapWorkShift(rs));
                 }
             }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra trùng lịch nhân viên: " + e.getMessage(), e);
        }

        LocalDateTime newStart = LocalDateTime.of(date, start);
        LocalDateTime newEnd = end.isBefore(start) ? LocalDateTime.of(date.plusDays(1), end) : LocalDateTime.of(date, end);

        Long currentComplexId = excludeShift != null ? excludeShift.getComplexId() : null;

        for (WorkShift ws : list) {
            // Bỏ qua chính ca trực đang chỉnh sửa
            if (excludeShiftId != null && excludeShiftId.equals(ws.getShiftId())) {
                continue;
            }
            // Bỏ qua các ca trực trùng ở CÙNG cơ sở và CÙNG ngày (đã được làm sạch / quản lý độc lập)
            if (currentComplexId != null && currentComplexId.equals(ws.getComplexId()) && date.equals(ws.getShiftDate())) {
                continue;
            }

            LocalDateTime existingStart = LocalDateTime.of(ws.getShiftDate(), ws.getStartTime());
            LocalDateTime existingEnd = ws.getEndTime().isBefore(ws.getStartTime())
                ? LocalDateTime.of(ws.getShiftDate().plusDays(1), ws.getEndTime())
                : LocalDateTime.of(ws.getShiftDate(), ws.getEndTime());

            if (newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra cơ sở/cụm sân bóng đã có ca trực trùng khung giờ trong ngày hay chưa.
     * Hỗ trợ ca đêm vượt mốc 00:00.
     */
    public boolean hasOverlappingShiftAtComplex(long complexId, LocalDate date, LocalTime start, LocalTime end, Long excludeShiftId) {
        String sql = "SELECT * FROM work_shifts WHERE complex_id = ? AND shift_date BETWEEN ? AND ?";
        List<WorkShift> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             ps.setLong(1, complexId);
             ps.setDate(2, Date.valueOf(date.minusDays(1)));
             ps.setDate(3, Date.valueOf(date.plusDays(1)));
             try (ResultSet rs = ps.executeQuery()) {
                 while (rs.next()) {
                     list.add(mapWorkShift(rs));
                 }
             }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra trùng lịch cụm sân: " + e.getMessage(), e);
        }

        LocalDateTime newStart = LocalDateTime.of(date, start);
        LocalDateTime newEnd = end.isBefore(start) ? LocalDateTime.of(date.plusDays(1), end) : LocalDateTime.of(date, end);

        for (WorkShift ws : list) {
            if (excludeShiftId != null && excludeShiftId.equals(ws.getShiftId())) {
                continue;
            }
            LocalDateTime existingStart = LocalDateTime.of(ws.getShiftDate(), ws.getStartTime());
            LocalDateTime existingEnd = ws.getEndTime().isBefore(ws.getStartTime())
                ? LocalDateTime.of(ws.getShiftDate().plusDays(1), ws.getEndTime())
                : LocalDateTime.of(ws.getShiftDate(), ws.getEndTime());

            if (newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Map ResultSet thành đối tượng WorkShift.
     */
    private WorkShift mapWorkShift(ResultSet rs) throws SQLException {
        WorkShift ws = new WorkShift();
        ws.setShiftId(rs.getLong("shift_id"));
        ws.setComplexId(rs.getLong("complex_id"));
        ws.setShiftName(rs.getString("shift_name"));
        ws.setShiftDate(rs.getDate("shift_date").toLocalDate());
        ws.setStartTime(rs.getTime("start_time").toLocalTime());
        ws.setEndTime(rs.getTime("end_time").toLocalTime());
        ws.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return ws;
    }
}

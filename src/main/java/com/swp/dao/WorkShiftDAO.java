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

public class WorkShiftDAO {

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public List<WorkShift> getAllShifts() {
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
            throw new RuntimeException("Lỗi khi lấy ca làm việc theo cơ sở: " + e.getMessage(), e);
        }
        return list;
    }

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

    public boolean deleteShift(long shiftId) {
        // First delete assignments for this shift
        String deleteAssignSql = "DELETE FROM shift_assignments WHERE shift_id = ?";
        String deleteShiftSql = "DELETE FROM work_shifts WHERE shift_id = ?";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
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

    public List<User> getAllActiveStaff() {
        List<User> list = new ArrayList<>();
        // Query users where role_id = 3 (Staff) and status = 'ACTIVE'
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

    public boolean assignStaffToShift(long shiftId, long staffId) {
        // Check if already assigned to avoid duplication
        String checkSql = "SELECT COUNT(*) FROM shift_assignments WHERE shift_id = ? AND staff_id = ?";
        String insertSql = "INSERT INTO shift_assignments (shift_id, staff_id, status) VALUES (?, ?, 'ASSIGNED')";
        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setLong(1, shiftId);
                psCheck.setLong(2, staffId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true; // Already assigned
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

    public boolean hasOverlappingShift(long staffId, java.time.LocalDate date, java.time.LocalTime start, java.time.LocalTime end, Long excludeShiftId) {
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
            throw new RuntimeException("Lỗi kiểm tra trùng lịch: " + e.getMessage(), e);
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

    public boolean hasOverlappingShiftAtComplex(long complexId, java.time.LocalDate date, java.time.LocalTime start, java.time.LocalTime end, Long excludeShiftId) {
        String sql = "SELECT * FROM work_shifts " +
                     "WHERE complex_id = ? AND shift_date BETWEEN ? AND ?";
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
            throw new RuntimeException("Lỗi kiểm tra trùng lịch cơ sở: " + e.getMessage(), e);
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

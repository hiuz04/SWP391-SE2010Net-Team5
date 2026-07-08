package com.swp.dao;

import com.swp.model.Field;
import com.swp.model.dto.TopFieldSummary;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FieldDAO {

    public void addField(Field f) {
        String sql = "INSERT INTO fields(facility_id, field_type_id, field_name, description, status) VALUES (?,?,?,?,?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, f.getFacilityId());
            ps.setInt(2, f.getFieldTypeId());
            ps.setString(3, f.getFieldName());
            ps.setString(4, f.getDescription());
            ps.setString(5, f.getStatus());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tạo mới dữ liệu: " + e.getMessage(), e);
        }
    }

    public void editField(Field f) {
        String sql = "UPDATE fields SET field_name=?, description=?, field_type_id=?, facility_id=?, status=? WHERE field_id=?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getFieldName());
            ps.setString(2, f.getDescription());
            ps.setInt(3, f.getFieldTypeId());
            ps.setLong(4, f.getFacilityId());
            ps.setString(5, f.getStatus());
            ps.setLong(6, f.getFieldId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thay đổi thông tin dữ liệu: " + e.getMessage(), e);
        }
    }

    public void deleteField(long id) {
        String sql = "DELETE FROM fields WHERE field_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng xóa dữ liệu: " + e.getMessage(), e);
        }
    }

    public void deleteFieldWithFacilityID(long id) {
        String sql = "DELETE FROM fields WHERE facility_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng xóa dữ liệu: " + e.getMessage(), e);
        }
    }

    public Field getFieldByID(long id) {
        Field field = new Field();
        String sql = "SELECT * FROM fields WHERE field_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                field.setFieldId(rs.getLong("field_id"));
                field.setFieldName(rs.getString("field_name"));
                field.setDescription(rs.getString("description"));
                field.setFieldTypeId(rs.getInt("field_type_id"));
                field.setFacilityId(rs.getLong("facility_id"));
                field.setStatus(rs.getString("status"));
                field.setHot(rs.getBoolean("is_hot"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }
        ;
        return field;
    }

    public List<Field> getFieldBelongToThisFacilityId(long id) {
        List<Field> list = new ArrayList<>();

        String sql = "SELECT * FROM fields WHERE facility_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Field(
                        rs.getLong("field_id"),
                        rs.getLong("facility_id"),
                        rs.getInt("field_type_id"),
                        rs.getString("field_name"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getBoolean("is_hot"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Field> getAllField() {
        List<Field> list = new ArrayList<>();
        String sql = "SELECT * FROM fields";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Field(
                        rs.getLong("field_id"),
                        rs.getLong("facility_id"),
                        rs.getInt("field_type_id"),
                        rs.getString("field_name"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getBoolean("is_hot"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }

        return list;
    }

    public int getFieldCountWithFacilityId(long id) {
        String sql = """
                    SELECT COUNT(*) AS total
                    FROM fields
                    WHERE facility_id = ?
                    """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e
            );
        }
    }

    /**
     * Lấy các sân nổi bật (hot).
     * Loại trừ các sân đang bảo trì (status = MAINTENANCE) và phải được đánh dấu là hot (is_hot = 1).
     * Join với bảng bookings để đếm, facility để lấy địa chỉ, field_types để lấy tên loại sân.
     */
    public List<TopFieldSummary> getHotFields() {
        List<TopFieldSummary> list = new ArrayList<>();
        String sql =
            "SELECT " +
            "  f.field_id, f.field_name, f.description, f.status, f.is_hot, f.facility_id, " +
            "  fac.facility_name, fac.address, fac.district, fac.city, " +
            "  COALESCE(ft.type_name, '') AS field_type_name, " +
            "  COUNT(b.booking_id) AS booking_count, " +
            "  fi.image_url " +
            "FROM fields f " +
            "LEFT JOIN bookings b ON b.field_id = f.field_id " +
            "LEFT JOIN facilities fac ON fac.facility_id = f.facility_id " +
            "LEFT JOIN field_types ft ON ft.field_type_id = f.field_type_id " +
            "OUTER APPLY (SELECT TOP 1 image_url FROM facility_images fi2 WHERE fi2.facility_id = f.facility_id ORDER BY thumbnail DESC, image_id DESC) fi " +
            "WHERE f.status <> 'MAINTENANCE' AND f.is_hot = 1 " +
            "GROUP BY f.field_id, f.field_name, f.description, f.status, f.is_hot, f.facility_id, " +
            "         fac.facility_name, fac.address, fac.district, fac.city, ft.type_name, fi.image_url " +
            "ORDER BY booking_count DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new TopFieldSummary(
                    rs.getLong("field_id"),
                    rs.getString("field_name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("field_type_name"),
                    rs.getBoolean("is_hot"),
                    rs.getLong("facility_id"),
                    rs.getString("facility_name"),
                    rs.getString("address"),
                    rs.getString("district"),
                    rs.getString("city"),
                    rs.getInt("booking_count"),
                    rs.getString("image_url")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy top 3 sân nổi bật: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Tìm kiếm sân theo tên thành phố.
     * Trả về danh sách TopFieldSummary chứa thông tin sân + cơ sở + loại sân.
     */
    public List<TopFieldSummary> searchFieldsByCity(String city) {
        List<TopFieldSummary> list = new ArrayList<>();
        String sql =
            "SELECT " +
            "  f.field_id, f.field_name, f.description, f.status, f.is_hot, f.facility_id, " +
            "  fac.facility_name, fac.address, fac.district, fac.city, " +
            "  COALESCE(ft.type_name, '') AS field_type_name, " +
            "  COUNT(b.booking_id) AS booking_count, " +
            "  fi.image_url " +
            "FROM fields f " +
            "LEFT JOIN bookings b ON b.field_id = f.field_id " +
            "LEFT JOIN facilities fac ON fac.facility_id = f.facility_id " +
            "LEFT JOIN field_types ft ON ft.field_type_id = f.field_type_id " +
            "OUTER APPLY (SELECT TOP 1 image_url FROM facility_images fi2 WHERE fi2.facility_id = f.facility_id ORDER BY thumbnail DESC, image_id DESC) fi " +
            "WHERE f.status <> 'MAINTENANCE' AND fac.city = ? " +
            "GROUP BY f.field_id, f.field_name, f.description, f.status, f.is_hot, f.facility_id, " +
            "         fac.facility_name, fac.address, fac.district, fac.city, ft.type_name, fi.image_url " +
            "ORDER BY booking_count DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, city);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopFieldSummary(
                        rs.getLong("field_id"),
                        rs.getString("field_name"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("field_type_name"),
                        rs.getBoolean("is_hot"),
                        rs.getLong("facility_id"),
                        rs.getString("facility_name"),
                        rs.getString("address"),
                        rs.getString("district"),
                        rs.getString("city"),
                        rs.getInt("booking_count"),
                        rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm sân theo thành phố: " + e.getMessage(), e);
        }

        return list;
    }

    public void updateFieldHotStatus(long fieldId, boolean isHot) {
        String sql = "UPDATE fields SET is_hot = ? WHERE field_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isHot);
            ps.setLong(2, fieldId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái HOT của sân: " + e.getMessage(), e);
        }
    }
}

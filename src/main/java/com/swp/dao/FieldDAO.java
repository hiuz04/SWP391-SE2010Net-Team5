package com.swp.dao;

import com.swp.model.Field;
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
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }

        return list;
    }

}

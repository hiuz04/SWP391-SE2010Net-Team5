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

    public void insertField(Field f) {
        String insertFieldSql =
        """
        INSERT INTO fields
        (complex_id, field_type_id, field_name, description, status)
        VALUES (?, ?, ?, ?, ?)
        """;

        String updateComplexSql =
        """
        UPDATE football_complexes
        SET status = 'ACTIVE'
        WHERE complex_id = ?
          AND status = 'PENDING'
        """;

        Connection conn = null;

        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            // Thêm sân
            try (PreparedStatement ps = conn.prepareStatement(insertFieldSql)) {
                ps.setLong(1, f.getComplexId());
                ps.setInt(2, f.getFieldTypeId());
                ps.setString(3, f.getFieldName());
                ps.setString(4, f.getDescription());
                ps.setString(5, "AVAILABLE");

                ps.executeUpdate();
            }

            // Nếu cụm đang PENDING thì chuyển sang ACTIVE
            try (PreparedStatement ps = conn.prepareStatement(updateComplexSql)) {
                ps.setLong(1, f.getComplexId());
                ps.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("Lỗi khi thêm sân.", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateField(Field f) {
        String sql = "UPDATE fields SET field_name=?, description=?, field_type_id=?, complex_id=? WHERE field_id=?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getFieldName());
            ps.setString(2, f.getDescription());
            ps.setInt(3, f.getFieldTypeId());
            ps.setLong(4, f.getComplexId());
            ps.setLong(5, f.getFieldId());
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
                field.setComplexId(rs.getLong("complex_id"));
                field.setStatus(rs.getString("status"));
                field.setHot(rs.getBoolean("is_hot"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }
        ;
        return field;
    }

    public List<Field> getFieldBelongToThisComplexId(long id) {
        List<Field> list = new ArrayList<>();

        String sql = "SELECT * FROM fields WHERE complex_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Field(
                        rs.getLong("field_id"),
                        rs.getLong("complex_id"),
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
                        rs.getLong("complex_id"),
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

    public int getFieldCountWithComplexId(long id) {
        String sql = """
                    SELECT COUNT(*) AS total
                    FROM fields
                    WHERE complex_id = ?
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
     * Chỉ lấy các sân có trạng thái đang Hoạt động (status = ACTIVE) và phải được đánh dấu là hot (is_hot = 1).
     * Join với bảng bookings để đếm, complex để lấy địa chỉ, field_types để lấy tên loại sân.
     */
    public List<TopFieldSummary> getHotFields() {
        List<TopFieldSummary> list = new ArrayList<>();
        String sql =
            "SELECT " +
            "  f.field_id, f.field_name, f.description, f.status, f.is_hot, f.complex_id, f.field_type_id, " +
            "  fc.complex_name, fc.address, fc.city, " +
            "  COALESCE(ft.type_name, '') AS field_type_name, " +
            "  SUM(CASE WHEN b.status IN ('PAID', 'CONFIRMED', 'COMPLETED') THEN 1 ELSE 0 END) AS booking_count, " +
            "  fi.image_url " +
            "FROM fields f " +
            "LEFT JOIN bookings b ON b.field_id = f.field_id " +
            "LEFT JOIN football_complexes fc ON fc.complex_id = f.complex_id " +
            "LEFT JOIN field_types ft ON ft.field_type_id = f.field_type_id " +
            "OUTER APPLY (SELECT TOP 1 image_url FROM football_complex_images fi2 WHERE fi2.complex_id = f.complex_id ORDER BY thumbnail DESC, image_id DESC) fi " +
            "WHERE f.status = 'AVAILABLE' AND fc.status = 'ACTIVE' AND f.is_hot = 1 " +
            "GROUP BY f.field_id, f.field_name, f.description, f.status, f.is_hot, f.complex_id, f.field_type_id, " +
            "         fc.complex_name, fc.address, fc.city, ft.type_name, fi.image_url " +
            "ORDER BY booking_count DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            PriceRuleDAO priceRuleDAO = new PriceRuleDAO();

            while (rs.next()) {
                long cId = rs.getLong("complex_id");
                long fId = rs.getLong("field_id");
                int ftId = rs.getInt("field_type_id");
                java.math.BigDecimal currentPrice = com.swp.util.PriceCalculator.calculateCurrentPrice(priceRuleDAO.getByComplexId(cId), fId, ftId);

                list.add(new TopFieldSummary(
                    rs.getLong("field_id"),
                    rs.getString("field_name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("field_type_name"),
                    rs.getBoolean("is_hot"),
                    cId,
                    rs.getString("complex_name"),
                    rs.getString("address"),
                    rs.getString("city"),
                    rs.getInt("booking_count"),
                    rs.getString("image_url"),
                    currentPrice
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy top 3 sân nổi bật: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Tìm kiếm sân theo tên thành phố.
     * Trả về danh sách TopFieldSummary chứa thông tin sân + cụm sân + loại sân.
     */
    public List<TopFieldSummary> searchFieldsByCity(String city) {
        List<TopFieldSummary> list = new ArrayList<>();
        String sql =
            "SELECT " +
            "  f.field_id, f.field_name, f.description, f.status, f.is_hot, f.complex_id, f.field_type_id, " +
            "  fc.complex_name, fc.address, fc.city, " +
            "  COALESCE(ft.type_name, '') AS field_type_name, " +
            "  SUM(CASE WHEN b.status IN ('PAID', 'CONFIRMED', 'COMPLETED') THEN 1 ELSE 0 END) AS booking_count, " +
            "  fi.image_url " +
            "FROM fields f " +
            "LEFT JOIN bookings b ON b.field_id = f.field_id " +
            "LEFT JOIN football_complexes fc ON fc.complex_id = f.complex_id " +
            "LEFT JOIN field_types ft ON ft.field_type_id = f.field_type_id " +
            "OUTER APPLY (SELECT TOP 1 image_url FROM football_complex_images fi2 WHERE fi2.complex_id = f.complex_id ORDER BY thumbnail DESC, image_id DESC) fi " +
            "WHERE f.status <> 'MAINTENANCE' AND fc.city = ? " +
            "GROUP BY f.field_id, f.field_name, f.description, f.status, f.is_hot, f.complex_id, f.field_type_id, " +
            "         fc.complex_name, fc.address, fc.city, ft.type_name, fi.image_url " +
            "ORDER BY booking_count DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, city);
            try (ResultSet rs = ps.executeQuery()) {
                PriceRuleDAO priceRuleDAO = new PriceRuleDAO();
                while (rs.next()) {
                    long fId = rs.getLong("field_id");
                    long cId = rs.getLong("complex_id");
                    int ftId = rs.getInt("field_type_id");
                    java.math.BigDecimal currentPrice = com.swp.util.PriceCalculator.calculateCurrentPrice(priceRuleDAO.getByComplexId(cId), fId, ftId);
                    list.add(new TopFieldSummary(
                        rs.getLong("field_id"),
                        rs.getString("field_name"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("field_type_name"),
                        rs.getBoolean("is_hot"),
                        cId,
                        rs.getString("complex_name"),
                        rs.getString("address"),
                        rs.getString("city"),
                        rs.getInt("booking_count"),
                        rs.getString("image_url"),
                        currentPrice
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

    public void updateStatus(long fieldId, String status) {
        String sql = """
        UPDATE fields
        SET status = ?,
            updated_at = GETDATE()
        WHERE field_id = ?
        """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, fieldId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái cụm sân.", e);
        }
    }

    public List<Field> searchField(String fieldName, String status, Long typeId, long complexId) {
        List<Field> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        SELECT *
        FROM fields
        WHERE complex_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(complexId);

        if (fieldName != null && !fieldName.isBlank()) {
            sql.append(" AND field_name LIKE ?");
            params.add("%" + fieldName.trim() + "%");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        if (typeId != null) {
            sql.append(" AND field_type_id = ?");
            params.add(typeId);
        }

        sql.append(" ORDER BY field_name");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Field field = new Field();

                field.setFieldId(rs.getLong("field_id"));
                field.setComplexId(rs.getLong("complex_id"));
                field.setFieldTypeId(rs.getInt("field_type_id"));
                field.setFieldName(rs.getString("field_name"));
                field.setDescription(rs.getString("description"));
                field.setStatus(rs.getString("status"));
                field.setHot(rs.getBoolean("is_hot"));

                list.add(field);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm kiếm sân.", e);
        }

        return list;
    }

    public boolean existsByName(String fieldName, long complexId) {
        String sql = """
        SELECT 1
        FROM fields
        WHERE LOWER(TRIM(field_name)) = LOWER(TRIM(?))
          AND complex_id = ?
        """;

        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, fieldName);
            ps.setLong(2, complexId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsByNameExceptId(String fieldName, long complexId, long fieldId) {
        String sql = """
        SELECT 1
        FROM fields
        WHERE LOWER(TRIM(field_name)) = LOWER(TRIM(?))
          AND complex_id = ?
          AND field_id <> ?
        """;

        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, fieldName);
            ps.setLong(2, complexId);
            ps.setLong(3, fieldId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

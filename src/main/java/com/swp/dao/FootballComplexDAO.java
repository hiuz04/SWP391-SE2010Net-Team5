package com.swp.dao;

import com.swp.model.FootballComplex;
import com.swp.model.FootballComplexImage;
import com.swp.model.PriceRule;
import com.swp.util.DBContext;
import com.swp.util.PriceCalculator;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FootballComplexDAO {

    public long addComplex(FootballComplex fc) {
        String sql = "INSERT INTO football_complexes (" +
                "complex_name, " +
                "description, " +
                "address, " +
                "ward, " +
                "city, " +
                "latitude, " +
                "longitude, " +
                "hotline, " +
                "opening_time, " +
                "closing_time, " +
                "general_rules, " +
                "status " +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fc.getComplexName());
            ps.setString(2, fc.getDescription());
            ps.setString(3, fc.getAddress());
            ps.setString(4, fc.getWard());
            ps.setString(5, "TP Hà Nội");

            if(fc.getLatitude() != null) {
                ps.setBigDecimal(6, fc.getLatitude());
            } else {ps.setNull(6, Types.DECIMAL);}

            if(fc.getLongitude() != null) {
                ps.setBigDecimal(7, fc.getLongitude());
            } else {ps.setNull(7, Types.DECIMAL);}

            ps.setString(8, fc.getHotline());

            ps.setTime(9,
                    fc.getOpeningTime() != null
                            ? Time.valueOf(fc.getOpeningTime())
                            : null
            );

            ps.setTime(10,
                    fc.getClosingTime() != null
                            ? Time.valueOf(fc.getClosingTime())
                            : null
            );

            ps.setString(11, fc.getGeneralRules());
            ps.setString(12, "PENDING");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();

            if(rs.next()){
                return rs.getLong(1);
            }
            throw new RuntimeException("Không lấy được complex_id");
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tạo mới dữ liệu: " + e.getMessage(), e);
        }
    }

    public void editFootballComplex(FootballComplex fc) {
        String sql = "UPDATE football_complexes SET " +
                "complex_name=?, " +
                "description=?, " +
                "address=?, " +
                "ward=?, " +
                "city=?, " +
                "latitude=?, " +
                "longitude=?, " +
                "hotline=?, " +
                "opening_time=?, " +
                "closing_time=?, " +
                "general_rules=? " +
                "WHERE complex_id=?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fc.getComplexName());
            ps.setString(2, fc.getDescription());
            ps.setString(3, fc.getAddress());
            ps.setString(4, fc.getWard());
            ps.setString(5, "TP Hà Nội");

            if(fc.getLatitude() != null) {
                ps.setBigDecimal(6, fc.getLatitude());
            } else {ps.setNull(6, Types.DECIMAL);}

            if(fc.getLongitude() != null) {
                ps.setBigDecimal(7, fc.getLongitude());
            } else {ps.setNull(7, Types.DECIMAL);}

            ps.setString(8, fc.getHotline());

            ps.setTime(9,
                    fc.getOpeningTime() != null
                            ? Time.valueOf(fc.getOpeningTime())
                            : null
            );

            ps.setTime(10,
                    fc.getClosingTime() != null
                            ? Time.valueOf(fc.getClosingTime())
                            : null
            );

            ps.setString(11, fc.getGeneralRules());
            ps.setLong(12, fc.getComplexId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật dữ liệu: " + e.getMessage(), e);
        }
    }

    public void deleteFootballComplex(long id) {
        String sql = "DELETE FROM football_complexes WHERE complex_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng xóa dữ liệu: " + e.getMessage(), e);
        }
    }

    public FootballComplex getFootballComplexDataByID(long id) {
        String sql = "SELECT * FROM football_complexes WHERE complex_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new FootballComplex(
                        rs.getLong("complex_id"),
                        rs.getString("complex_name"),
                        rs.getString("description"),
                        rs.getString("address"),
                        rs.getString("ward"),
                        rs.getString("city"),
                        rs.getBigDecimal("latitude"),
                        rs.getBigDecimal("longitude"),
                        rs.getString("hotline"),
                        rs.getTime("opening_time") != null
                                ? rs.getTime("opening_time").toLocalTime()
                                : null,
                        rs.getTime("closing_time") != null
                                ? rs.getTime("closing_time").toLocalTime()
                                : null,
                        rs.getString("general_rules"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                );
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
    }

    public List<FootballComplex> getAllActiveComplex() {
        List<FootballComplex> list = new ArrayList<>();
        String sql = "SELECT * FROM football_complexes WHERE status = 'ACTIVE'";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new FootballComplex(
                        rs.getLong("complex_id"),
                        rs.getString("complex_name"),
                        rs.getString("description"),
                        rs.getString("address"),
                        rs.getString("ward"),
                        rs.getString("city"),
                        rs.getBigDecimal("latitude"),
                        rs.getBigDecimal("longitude"),
                        rs.getString("hotline"),
                        rs.getTime("opening_time") != null
                                ? rs.getTime("opening_time").toLocalTime()
                                : null,
                        rs.getTime("closing_time") != null
                                ? rs.getTime("closing_time").toLocalTime()
                                : null,
                        rs.getString("general_rules"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
        return list;
    }

    public List<FootballComplex> getAllComplexExceptDeleteOne() {
        List<FootballComplex> list = new ArrayList<>();
        String sql = "SELECT * FROM football_complexes";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new FootballComplex(
                        rs.getLong("complex_id"),
                        rs.getString("complex_name"),
                        rs.getString("description"),
                        rs.getString("address"),
                        rs.getString("ward"),
                        rs.getString("city"),
                        rs.getBigDecimal("latitude"),
                        rs.getBigDecimal("longitude"),
                        rs.getString("hotline"),
                        rs.getTime("opening_time") != null
                                ? rs.getTime("opening_time").toLocalTime()
                                : null,
                        rs.getTime("closing_time") != null
                                ? rs.getTime("closing_time").toLocalTime()
                                : null,
                        rs.getString("general_rules"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
        return list;
    }

    public void addImage(FootballComplexImage img) {
        String sql = "INSERT INTO football_complex_images(" +
                "complex_id," +
                "image_url," +
                "thumbnail," +
                "public_id" +
                ") VALUES (?, ?, ?, ?)";

        try(Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, img.getComplexId());
            ps.setString(2, img.getImageUrl());
            ps.setBoolean(3, img.getThumbnail());
            ps.setString(4, img.getPublicId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tạo mới dữ liệu: " + e.getMessage(), e);
        }
    }

    public void updateImage(long id, boolean isThumbnail){
        String sql = "UPDATE football_complex_images SET thumbnail=? WHERE image_id=?";

        try(Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isThumbnail);
            ps.setLong(2, id);

            ps.executeUpdate();
        } catch (SQLException e){
            throw new RuntimeException("Lỗi khi cập nhập dữ liệu: " + e.getMessage(), e);
        }
    }

    public void deleteImage(long id) {
        String sql = "DELETE FROM football_complex_images WHERE image_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng xóa dữ liệu: " + e.getMessage(), e);
        }
    }

    public List<FootballComplexImage> getAllImage(long fcId) {
        List<FootballComplexImage> list = new ArrayList<>();
        String sql = "SELECT * FROM football_complex_images WHERE complex_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fcId);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                list.add(new FootballComplexImage(
                        rs.getLong("image_id"),
                        rs.getLong("complex_id"),
                        rs.getString("image_url"),
                        rs.getBoolean("thumbnail"),
                        rs.getString("public_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
        return list;
    }

    public void deleteAllImageRelatedToFootballComplex(long id) {
        String sql = """
            DELETE FROM football_complex_images
            WHERE complex_id = ?
            """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Lỗi khi xóa toàn bộ ảnh của fc: " + e.getMessage(),
                    e
            );
        }
    }

    public FootballComplexImage getThumbnail(long fcId) {
        String sql = "SELECT * FROM football_complex_images " +
                              "WHERE complex_id = ? " +
                              "AND thumbnail = 1";
        try(Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setLong(1, fcId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return new FootballComplexImage(
                        rs.getLong("image_id"),
                        rs.getLong("complex_id"),
                        rs.getString("image_url"),
                        rs.getBoolean("thumbnail"),
                        rs.getString("public_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    );
            }

            return null;
        }catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
    }

    public FootballComplexImage getImgById(long imgId) {
        String sql = "SELECT * FROM football_complex_images " +
                              "WHERE image_id = ?";
        try(Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setLong(1, imgId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return new FootballComplexImage(
                        rs.getLong("image_id"),
                        rs.getLong("complex_id"),
                        rs.getString("image_url"),
                        rs.getBoolean("thumbnail"),
                        rs.getString("public_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    );
            }

            return null;
        }catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
    }

    public List<String> getAllCities() {

        String sql = """
                    SELECT DISTINCT city
                    FROM football_complexes
                    ORDER BY city
                """;

        List<String> cities = new ArrayList<>();

        try (
                Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                cities.add(rs.getString("city"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cities;
    }

    public List<String> getAllWards() {

        String sql = """
            SELECT DISTINCT ward
            FROM football_complexes
            WHERE ward IS NOT NULL AND ward <> ''
            ORDER BY ward
        """;

        List<String> wards = new ArrayList<>();

        try (
                Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                wards.add(rs.getString("ward"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return wards;
    }

    public java.math.BigDecimal getMinPriceForComplex(Long complexId) {
        String sql = "SELECT MIN(price) AS min_price FROM price_rules WHERE complex_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("min_price");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.math.BigDecimal getMaxPriceForComplex(Long complexId) {
        String sql = "SELECT MAX(price) AS max_price FROM price_rules WHERE complex_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("max_price");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BigDecimal getCurrentPriceForComplex(Long complexId) {
        PriceRuleDAO priceRuleDAO = new PriceRuleDAO();
        List<PriceRule> rules = priceRuleDAO.getByComplexId(complexId);

        if (rules == null || rules.isEmpty()) {
            return null;
        }

        return PriceCalculator.calculateCurrentPrice(rules, null, null);
    }

    public void updateStatus(long complexId, String status) {
        String sql = """
        UPDATE football_complexes
        SET status = ?,
            updated_at = GETDATE()
        WHERE complex_id = ?
        """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, complexId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái cụm sân.", e);
        }
    }

    public List<FootballComplex> searchComplex(String keyword, String status) {
        List<FootballComplex> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        SELECT *
        FROM football_complexes
        WHERE 1 = 1
        """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND complex_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY created_at DESC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new FootballComplex(
                        rs.getLong("complex_id"),
                        rs.getString("complex_name"),
                        rs.getString("description"),
                        rs.getString("address"),
                        rs.getString("ward"),
                        rs.getString("city"),
                        rs.getBigDecimal("latitude"),
                        rs.getBigDecimal("longitude"),
                        rs.getString("hotline"),
                        rs.getTime("opening_time") != null
                                ? rs.getTime("opening_time").toLocalTime()
                                : null,
                        rs.getTime("closing_time") != null
                                ? rs.getTime("closing_time").toLocalTime()
                                : null,
                        rs.getString("general_rules"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at") != null
                                ? rs.getTimestamp("created_at").toLocalDateTime()
                                : null,
                        rs.getTimestamp("updated_at") != null
                                ? rs.getTimestamp("updated_at").toLocalDateTime()
                                : null
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm kiếm cơ sở.", e);
        }

        return list;
    }
}

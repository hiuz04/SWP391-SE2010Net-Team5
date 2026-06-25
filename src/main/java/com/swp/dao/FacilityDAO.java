package com.swp.dao;

import com.swp.model.Facility;
import com.swp.model.FacilityImage;
import com.swp.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacilityDAO {

    public long addFacility(Facility facility) {
        String sql = "INSERT INTO facilities (" +
                "facility_name, " +
                "description, " +
                "address, " +
                "ward, " +
                "district, " +
                "city, " +
                "latitude, " +
                "longitude, " +
                "hotline, " +
                "opening_time, " +
                "closing_time, " +
                "general_rules, " +
                "status, " +
                "featured" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, facility.getFacilityName());
            ps.setString(2, facility.getDescription());
            ps.setString(3, facility.getAddress());
            ps.setString(4, facility.getWard());
            ps.setString(5, facility.getDistrict());
            ps.setString(6, facility.getCity());

            if(facility.getLatitude() != null) {
                ps.setBigDecimal(7, facility.getLatitude());
            } else {ps.setNull(7, Types.DECIMAL);}

            if(facility.getLongitude() != null) {
                ps.setBigDecimal(8, facility.getLongitude());
            } else {ps.setNull(8, Types.DECIMAL);}

            ps.setString(9, facility.getHotline());

            ps.setTime(10,
                    facility.getOpeningTime() != null
                            ? Time.valueOf(facility.getOpeningTime())
                            : null
            );

            ps.setTime(11,
                    facility.getClosingTime() != null
                            ? Time.valueOf(facility.getClosingTime())
                            : null
            );

            ps.setString(12, facility.getGeneralRules());
            ps.setString(13, facility.getStatus());
            ps.setBoolean(14, facility.getFeatured());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();

            if(rs.next()){
                return rs.getLong(1);
            }
            throw new RuntimeException("Không lấy được facility_id");
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tạo mới dữ liệu: " + e.getMessage(), e);
        }
    }

    public void editFacility(Facility facility) {
        String sql = "UPDATE facilities SET " +
                "facility_name=?, " +
                "description=?, " +
                "address=?, " +
                "ward=?, " +
                "district=?, " +
                "city=?, " +
                "latitude=?, " +
                "longitude=?, " +
                "hotline=?, " +
                "opening_time=?, " +
                "closing_time=?, " +
                "general_rules=?, " +
                "status=?, " +
                "featured=? " +
                "WHERE facility_id=?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, facility.getFacilityName());
            ps.setString(2, facility.getDescription());
            ps.setString(3, facility.getAddress());
            ps.setString(4, facility.getWard());
            ps.setString(5, facility.getDistrict());
            ps.setString(6, facility.getCity());

            if(facility.getLatitude() != null) {
                ps.setBigDecimal(7, facility.getLatitude());
            } else {ps.setNull(7, Types.DECIMAL);}

            if(facility.getLongitude() != null) {
                ps.setBigDecimal(8, facility.getLongitude());
            } else {ps.setNull(8, Types.DECIMAL);}

            ps.setString(9, facility.getHotline());

            ps.setTime(10,
                    facility.getOpeningTime() != null
                            ? Time.valueOf(facility.getOpeningTime())
                            : null
            );

            ps.setTime(11,
                    facility.getClosingTime() != null
                            ? Time.valueOf(facility.getClosingTime())
                            : null
            );

            ps.setString(12, facility.getGeneralRules());
            ps.setString(13, facility.getStatus());
            ps.setBoolean(14, facility.getFeatured());
            ps.setLong(15, facility.getFacilityId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật dữ liệu: " + e.getMessage(), e);
        }
    }

    public void deleteFacility(long id) {
        String sql = "DELETE FROM facilities WHERE facility_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng xóa dữ liệu: " + e.getMessage(), e);
        }
    }

    public Facility getFacilityDataByID(long id) {
        String sql = "SELECT * FROM facilities WHERE facility_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Facility(
                        rs.getLong("facility_id"),
                        rs.getString("facility_name"),
                        rs.getString("description"),
                        rs.getString("address"),
                        rs.getString("ward"),
                        rs.getString("district"),
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
                        rs.getBoolean("featured"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                );
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
    }

    public List<Facility> getAllFacility() {
        List<Facility> list = new ArrayList<>();
        String sql = "SELECT * FROM facilities";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Facility(
                        rs.getLong("facility_id"),
                        rs.getString("facility_name"),
                        rs.getString("description"),
                        rs.getString("address"),
                        rs.getString("ward"),
                        rs.getString("district"),
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
                        rs.getBoolean("featured"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e);
        }
        return list;
    }

    public void addImage(FacilityImage img) {
        String sql = "INSERT INTO facility_images(" +
                "facility_id," +
                "image_url," +
                "thumbnail," +
                "public_id" +
                ") VALUES (?, ?, ?, ?)";

        try(Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, img.getFacilityId());
            ps.setString(2, img.getImageUrl());
            ps.setBoolean(3, img.getThumbnail());
            ps.setString(4, img.getPublicId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tạo mới dữ liệu: " + e.getMessage(), e);
        }
    }

    public void updateImage(long id, boolean isThumbnail){
        String sql = "UPDATE facility_images SET thumbnail=? WHERE image_id=?";

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
        String sql = "DELETE FROM facility_images WHERE image_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cố gắng xóa dữ liệu: " + e.getMessage(), e);
        }
    }

    public List<FacilityImage> getAllImage(long facilityId) {
        List<FacilityImage> list = new ArrayList<>();
        String sql = "SELECT * FROM facility_images WHERE facility_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, facilityId);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                list.add(new FacilityImage(
                        rs.getLong("image_id"),
                        rs.getLong("facility_id"),
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

    public void deleteAllImageRelatedToFacility(long id) {
        String sql = """
            DELETE FROM facility_images
            WHERE facility_id = ?
            """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Lỗi khi xóa toàn bộ ảnh của facility: " + e.getMessage(),
                    e
            );
        }
    }

    public FacilityImage getThumbnail(long facilityId) {
        String sql = "SELECT * FROM facility_images " +
                              "WHERE facility_id = ? " +
                              "AND thumbnail = 1";
        try(Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setLong(1, facilityId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return new FacilityImage(
                        rs.getLong("image_id"),
                        rs.getLong("facility_id"),
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

    public FacilityImage getImgById(long imgId) {
        String sql = "SELECT * FROM facility_images " +
                              "WHERE image_id = ?";
        try(Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setLong(1, imgId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return new FacilityImage(
                        rs.getLong("image_id"),
                        rs.getLong("facility_id"),
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
                    FROM facilities
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
            FROM facilities
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
}

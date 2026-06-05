package com.swp.dao;

import com.swp.model.Facility;
import com.swp.util.DBContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FacilityDAO {

    public void addFacility(Facility facility) {
        String sql = "INSERT INTO facilities (" +
                "facility_name, " +
                "description, " +
                "address, " +
                "ward, " +
                "district, " +
                "city, " +
                "hotline, " +
                "opening_time, " +
                "closing_time, " +
                "general_rules, " +
                "status, " +
                "featured" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, facility.getFacilityName());
            ps.setString(2, facility.getDescription());
            ps.setString(3, facility.getAddress());
            ps.setString(4, facility.getWard());
            ps.setString(5, facility.getDistrict());
            ps.setString(6, facility.getCity());

            ps.setString(7, facility.getHotline());

            ps.setTime(8,
                facility.getOpeningTime() != null
                    ? Time.valueOf(facility.getOpeningTime())
                    : null
            );

            ps.setTime(9,
                facility.getClosingTime() != null
                    ? Time.valueOf(facility.getClosingTime())
                    : null
            );

            ps.setString(10, facility.getGeneralRules());
            ps.setString(11, facility.getStatus());
            ps.setBoolean(12, facility.getFeatured());
            ps.executeUpdate();
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

            ps.setString(7, facility.getHotline());

            ps.setTime(8,
                    facility.getOpeningTime() != null
                            ? Time.valueOf(facility.getOpeningTime())
                            : null
            );

            ps.setTime(9,
                    facility.getClosingTime() != null
                            ? Time.valueOf(facility.getClosingTime())
                            : null
            );

            ps.setString(10, facility.getGeneralRules());
            ps.setString(11, facility.getStatus());
            ps.setBoolean(12, facility.getFeatured());
            ps.setLong(13, facility.getFacilityId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật dữ liệu: " + e.getMessage(), e);
        }
    }

    public void deleteFacility(long id) {
        String sql = "DELETE FROM facilities WHERE facility_id = ?";
        try(Connection conn = DBContext.getConnection();
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

}

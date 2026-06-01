package com.swp.dao;

import com.swp.model.Facility;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FacilityDAO {

    private void addFacility(){

    }

    private void editFacility(){

    }

    private void deleteFacility(){

    }

    public List<Facility> getAllFacility(){
        List<Facility> list = new ArrayList<>();
        String sql = "SELECT * FROM facilities";
        try (Connection conn = DBContext.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
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
                        rs.getObject("opening_time", LocalTime.class),
                        rs.getObject("closing_time", LocalTime.class),
                        rs.getString("general_rules"),
                        rs.getString("status"),
                        rs.getBoolean("featured"),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getObject("updated_at", LocalDateTime.class)
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }
        return list;
    }

}

package com.swp.dao;

import com.swp.model.PriceRule;
import com.swp.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PriceRuleDAO {

    public List<PriceRule> getAllPriceRules() {
        List<PriceRule> list = new ArrayList<>();
        String sql = "SELECT * FROM price_rules ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<PriceRule> getPriceRulesByFacility(long facilityId) {
        List<PriceRule> list = new ArrayList<>();
        String sql = "SELECT * FROM price_rules WHERE facility_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, facilityId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public PriceRule getPriceRuleById(long id) {
        String sql = "SELECT * FROM price_rules WHERE price_rule_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long insert(PriceRule rule) {
        String sql = "INSERT INTO price_rules (" +
                "facility_id, field_type_id, field_id, rule_name, day_of_week, " +
                "specific_date, start_time, end_time, price, rule_type, priority, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
             
            setParameters(ps, rule);
            
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void update(PriceRule rule) {
        String sql = "UPDATE price_rules SET " +
                "facility_id = ?, field_type_id = ?, field_id = ?, rule_name = ?, day_of_week = ?, " +
                "specific_date = ?, start_time = ?, end_time = ?, price = ?, rule_type = ?, priority = ?, status = ? " +
                "WHERE price_rule_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            setParameters(ps, rule);
            ps.setLong(13, rule.getPriceRuleId());
            
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM price_rules WHERE price_rule_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setParameters(PreparedStatement ps, PriceRule rule) throws SQLException {
        if (rule.getFacilityId() != null) ps.setLong(1, rule.getFacilityId()); else ps.setNull(1, Types.BIGINT);
        if (rule.getFieldTypeId() != null) ps.setInt(2, rule.getFieldTypeId()); else ps.setNull(2, Types.INTEGER);
        if (rule.getFieldId() != null) ps.setLong(3, rule.getFieldId()); else ps.setNull(3, Types.BIGINT);
        
        ps.setString(4, rule.getRuleName());
        ps.setString(5, rule.getDayOfWeek());
        
        if (rule.getSpecificDate() != null) ps.setDate(6, Date.valueOf(rule.getSpecificDate())); else ps.setNull(6, Types.DATE);
        if (rule.getStartTime() != null) ps.setTime(7, Time.valueOf(rule.getStartTime())); else ps.setNull(7, Types.TIME);
        if (rule.getEndTime() != null) ps.setTime(8, Time.valueOf(rule.getEndTime())); else ps.setNull(8, Types.TIME);
        
        ps.setBigDecimal(9, rule.getPrice());
        ps.setString(10, rule.getRuleType());
        if (rule.getPriority() != null) ps.setInt(11, rule.getPriority()); else ps.setInt(11, 0);
        ps.setString(12, rule.getStatus() != null ? rule.getStatus() : "ACTIVE");
    }

    private PriceRule mapRow(ResultSet rs) throws SQLException {
        PriceRule rule = new PriceRule();
        rule.setPriceRuleId(rs.getLong("price_rule_id"));
        rule.setFacilityId(rs.getLong("facility_id") == 0 ? null : rs.getLong("facility_id"));
        rule.setFieldTypeId(rs.getInt("field_type_id") == 0 ? null : rs.getInt("field_type_id"));
        rule.setFieldId(rs.getLong("field_id") == 0 ? null : rs.getLong("field_id"));
        rule.setRuleName(rs.getString("rule_name"));
        rule.setDayOfWeek(rs.getString("day_of_week"));
        
        Date specificDate = rs.getDate("specific_date");
        if (specificDate != null) rule.setSpecificDate(specificDate.toLocalDate());
        
        Time startTime = rs.getTime("start_time");
        if (startTime != null) rule.setStartTime(startTime.toLocalTime());
        
        Time endTime = rs.getTime("end_time");
        if (endTime != null) rule.setEndTime(endTime.toLocalTime());
        
        rule.setPrice(rs.getBigDecimal("price"));
        rule.setRuleType(rs.getString("rule_type"));
        rule.setPriority(rs.getInt("priority"));
        rule.setStatus(rs.getString("status"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) rule.setCreatedAt(createdAt.toLocalDateTime());
        
        return rule;
    }
}

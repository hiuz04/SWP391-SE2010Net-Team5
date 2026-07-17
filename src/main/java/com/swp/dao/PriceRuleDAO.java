package com.swp.dao;

import com.swp.model.PriceRule;
import com.swp.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PriceRuleDAO {
    
    public List<PriceRule> getByComplexId(long complexId) {
        List<PriceRule> list = new ArrayList<>();
        String sql = "SELECT * FROM price_rules WHERE complex_id = ? AND status = 'ACTIVE' ORDER BY priority ASC, created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean insert(PriceRule pr) {
        String sql = "INSERT INTO price_rules (complex_id, field_type_id, field_id, rule_name, day_of_week, specific_date, start_time, end_time, price, rule_type, priority, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pr.getComplexId());
            if (pr.getFieldTypeId() != null) ps.setInt(2, pr.getFieldTypeId()); else ps.setNull(2, Types.INTEGER);
            if (pr.getFieldId() != null) ps.setLong(3, pr.getFieldId()); else ps.setNull(3, Types.BIGINT);
            ps.setString(4, pr.getRuleName());
            ps.setString(5, pr.getDayOfWeek());
            if (pr.getSpecificDate() != null) ps.setDate(6, Date.valueOf(pr.getSpecificDate())); else ps.setNull(6, Types.DATE);
            if (pr.getStartTime() != null) ps.setTime(7, Time.valueOf(pr.getStartTime())); else ps.setNull(7, Types.TIME);
            if (pr.getEndTime() != null) ps.setTime(8, Time.valueOf(pr.getEndTime())); else ps.setNull(8, Types.TIME);
            ps.setBigDecimal(9, pr.getPrice());
            ps.setString(10, pr.getRuleType());
            ps.setInt(11, pr.getPriority() != null ? pr.getPriority() : 0);
            ps.setString(12, pr.getStatus() != null ? pr.getStatus() : "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(PriceRule pr) {
        String sql = "UPDATE price_rules SET field_type_id = ?, field_id = ?, rule_name = ?, day_of_week = ?, specific_date = ?, start_time = ?, end_time = ?, price = ?, rule_type = ?, priority = ?, status = ? " +
                     "WHERE price_rule_id = ? AND complex_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (pr.getFieldTypeId() != null) ps.setInt(1, pr.getFieldTypeId()); else ps.setNull(1, Types.INTEGER);
            if (pr.getFieldId() != null) ps.setLong(2, pr.getFieldId()); else ps.setNull(2, Types.BIGINT);
            ps.setString(3, pr.getRuleName());
            ps.setString(4, pr.getDayOfWeek());
            if (pr.getSpecificDate() != null) ps.setDate(5, Date.valueOf(pr.getSpecificDate())); else ps.setNull(5, Types.DATE);
            if (pr.getStartTime() != null) ps.setTime(6, Time.valueOf(pr.getStartTime())); else ps.setNull(6, Types.TIME);
            if (pr.getEndTime() != null) ps.setTime(7, Time.valueOf(pr.getEndTime())); else ps.setNull(7, Types.TIME);
            ps.setBigDecimal(8, pr.getPrice());
            ps.setString(9, pr.getRuleType());
            ps.setInt(10, pr.getPriority() != null ? pr.getPriority() : 0);
            ps.setString(11, pr.getStatus() != null ? pr.getStatus() : "ACTIVE");
            ps.setLong(12, pr.getPriceRuleId());
            ps.setLong(13, pr.getComplexId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(long priceRuleId, long complexId) {
        // Soft delete
        String sql = "UPDATE price_rules SET status = 'INACTIVE' WHERE price_rule_id = ? AND complex_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, priceRuleId);
            ps.setLong(2, complexId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private PriceRule mapRow(ResultSet rs) throws SQLException {
        PriceRule pr = new PriceRule();
        pr.setPriceRuleId(rs.getLong("price_rule_id"));
        pr.setComplexId(rs.getLong("complex_id"));
        
        long fieldTypeId = rs.getLong("field_type_id");
        if (!rs.wasNull()) pr.setFieldTypeId((int) fieldTypeId);
        
        long fieldId = rs.getLong("field_id");
        if (!rs.wasNull()) pr.setFieldId(fieldId);
        
        pr.setRuleName(rs.getString("rule_name"));
        pr.setDayOfWeek(rs.getString("day_of_week"));
        
        Date sd = rs.getDate("specific_date");
        if (sd != null) pr.setSpecificDate(sd.toLocalDate());
        
        Time st = rs.getTime("start_time");
        if (st != null) pr.setStartTime(st.toLocalTime());
        
        Time et = rs.getTime("end_time");
        if (et != null) pr.setEndTime(et.toLocalTime());
        
        pr.setPrice(rs.getBigDecimal("price"));
        pr.setRuleType(rs.getString("rule_type"));
        pr.setPriority(rs.getInt("priority"));
        pr.setStatus(rs.getString("status"));
        
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) pr.setCreatedAt(ca.toLocalDateTime());
        
        return pr;
    }
}

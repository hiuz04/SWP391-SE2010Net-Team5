package com.swp.dao;

import com.swp.model.FieldType;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FieldTypeDAO {

    public List<FieldType> getAllFieldTypes(){
        List<FieldType> list = new ArrayList<>();
        String sql = "SELECT * FROM field_types";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new FieldType(
                    rs.getInt("field_type_id"),
                    rs.getString("type_name"),
                    rs.getInt("number_of_players"),
                    rs.getString("description")
                ));
            }

        } catch (SQLException e)
        {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }

        return list;
    }

}

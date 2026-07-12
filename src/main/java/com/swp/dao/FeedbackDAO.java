package com.swp.dao;

import com.swp.model.Feedback;
import com.swp.model.FeedbackImage;
import com.swp.model.dto.FeedbackDTO;
import com.swp.util.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {

    public List<FeedbackDTO> getAllFeedbackOfThisComplexes(long complexId) {
        List<FeedbackDTO> list = new ArrayList<>();
        String sql = "SELECT " +
                "u.full_name AS userName, " +
                "f.created_at AS createdAt, " +
                "f.updated_at AS updatedAt, " +
                "fa.complex_name AS fieldName, " +
                "f.rating, " +
                "f.description AS feedbackDesc, " +
                "f.owner_reply AS ownerReply, " +
                "f.feedback_id " +
                "FROM feedbacks f " +
                "JOIN users u " +
                "ON f.user_id = u.user_id " +
                "JOIN football_complexes fa " +
                "ON f.complex_id = fa.complex_id " +
                "WHERE f.complex_id = ? " +
                "AND f.status = 'ACTIVE' " +
                "ORDER BY f.created_at DESC; ";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Timestamp createdAt = rs.getTimestamp("createdAt");
                Timestamp updatedAt = rs.getTimestamp("updatedAt");

                list.add(new FeedbackDTO(
                        rs.getLong("feedback_id"),
                        rs.getString("userName"),
                        createdAt == null ? null : createdAt.toLocalDateTime(),
                        updatedAt == null ? null : updatedAt.toLocalDateTime(),
                        rs.getString("fieldName"),
                        rs.getInt("rating"),
                        rs.getString("feedbackDesc"),
                        rs.getString("ownerReply")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }
        return list;
    }

    public List<FeedbackImage> getFeedbackImagesByComplexId(long complexId) {
        List<FeedbackImage> list = new ArrayList<>();

        String sql = """
            SELECT
                fi.image_id,
                fi.feedback_id,
                fi.image_url,
                fi.public_id
            FROM feedback_images fi
            JOIN feedbacks f
                ON fi.feedback_id = f.feedback_id
            WHERE f.complex_id = ?
            AND f.status = 'ACTIVE'
        """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, complexId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new FeedbackImage(
                        rs.getLong("image_id"),
                        rs.getLong("feedback_id"),
                        rs.getString("image_url"),
                        rs.getString("public_id")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public void addFeedback(Feedback f) {
        String sql = "INSERT INTO feedbacks(user_id, complex_id, rating, description, owner_reply, status) " +
                        "VALUE (?,?,?,?,?,?)";


    }
}

package com.swp.dao;

import com.swp.model.Feedback;
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

    public Feedback getFeedbackById(long feedbackId) {
        String sql = "SELECT * FROM feedbacks WHERE feedback_id = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, feedbackId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Feedback feedback = new Feedback();

                    feedback.setFeedbackId(rs.getLong("feedback_id"));
                    feedback.setBookingId(rs.getLong("booking_id"));
                    feedback.setUserId(rs.getLong("user_id"));
                    feedback.setComplexId(rs.getLong("complex_id"));
                    feedback.setRating(rs.getInt("rating"));
                    feedback.setDescription(rs.getString("description"));
                    feedback.setOwnerReply(rs.getString("owner_reply"));
                    feedback.setStatus(rs.getString("status"));
                    feedback.setCreatedAt(
                            rs.getTimestamp("created_at") != null
                                    ? rs.getTimestamp("created_at").toLocalDateTime()
                                    : null
                    );
                    feedback.setUpdatedAt(
                            rs.getTimestamp("updated_at") != null
                                    ? rs.getTimestamp("updated_at").toLocalDateTime()
                                    : null
                    );
                    feedback.setReplyAt(
                            rs.getTimestamp("reply_at") != null
                                    ? rs.getTimestamp("reply_at").toLocalDateTime()
                                    : null
                    );
                    return feedback;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu: " + e.getMessage(), e);
        }

        return null;
    }

    public boolean existsByBookingId(long bookingId) {
        String sql = """
                SELECT 1
                FROM feedbacks
                WHERE booking_id = ?
                  AND status = 'ACTIVE'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi kiểm tra feedback: " + e.getMessage(), e);
        }
    }

    public void addFeedback(Feedback f) {
        String sql = """
                INSERT INTO feedbacks
                (
                    booking_id,
                    user_id,
                    complex_id,
                    rating,
                    description
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, f.getBookingId());
            ps.setLong(2, f.getUserId());
            ps.setLong(3, f.getComplexId());
            ps.setInt(4, f.getRating());
            ps.setString(5, f.getDescription());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm feedback: " + e.getMessage(), e);
        }
    }

    public void updateFeedback(long feedbackId, int rating, String description) {
        String sql = """
                        UPDATE feedbacks
                        SET rating = ?,
                            description = ?,
                            updated_at = GETDATE()
                        WHERE feedback_id = ?
                    """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, rating);
            ps.setString(2, description);
            ps.setLong(3, feedbackId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addReply(long feedbackId, String message) {
        String sql = "UPDATE feedbacks SET owner_reply = ?, reply_at = GETDATE() WHERE feedback_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setLong(2, feedbackId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thay đổi thông tin dữ liệu: " + e.getMessage(), e);
        }
    }
}

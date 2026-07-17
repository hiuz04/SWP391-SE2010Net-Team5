package com.swp.dao;

import com.swp.model.MatchmakingPostResponse;
import com.swp.model.dto.MatchmakingPostResponseDTO;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MatchmakingPostResponseDAO {

    public void createResponse(MatchmakingPostResponse response) {
        String sql = "INSERT INTO matchmaking_post_responses (post_id, responder_id, message, status, created_at) " +
                     "VALUES (?, ?, ?, ?, GETDATE())";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, response.getPostId());
            ps.setLong(2, response.getResponderId());
            ps.setString(3, response.getMessage());
            ps.setString(4, response.getStatus() != null ? response.getStatus() : "PENDING");
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tạo phản hồi bài đăng tìm đối: " + e.getMessage(), e);
        }
    }

    public List<MatchmakingPostResponseDTO> getResponsesByPostId(long postId) {
        List<MatchmakingPostResponseDTO> list = new ArrayList<>();
        String sql = "SELECT mpr.*, u.full_name AS responder_name, u.phone AS responder_phone " +
                     "FROM matchmaking_post_responses mpr " +
                     "LEFT JOIN users u ON mpr.responder_id = u.user_id " +
                     "WHERE mpr.post_id = ? " +
                     "ORDER BY mpr.created_at DESC";
                     
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MatchmakingPostResponse response = new MatchmakingPostResponse();
                response.setResponseId(rs.getLong("response_id"));
                response.setPostId(rs.getLong("post_id"));
                response.setResponderId(rs.getLong("responder_id"));
                response.setMessage(rs.getString("message"));
                response.setStatus(rs.getString("status"));
                response.setCreatedAt(rs.getTimestamp("created_at") != null 
                        ? rs.getTimestamp("created_at").toLocalDateTime() 
                        : null);

                String responderName = rs.getString("responder_name");
                String responderPhone = rs.getString("responder_phone");
                list.add(new MatchmakingPostResponseDTO(response, responderName, responderPhone));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách phản hồi: " + e.getMessage(), e);
        }
        
        return list;
    }

    public void updateResponseStatus(long responseId, String status) {
        String sql = "UPDATE matchmaking_post_responses SET status = ? WHERE response_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, responseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái phản hồi: " + e.getMessage(), e);
        }
    }

    public MatchmakingPostResponse getResponseByPostAndResponder(long postId, long responderId) {
        String sql = "SELECT * FROM matchmaking_post_responses WHERE post_id = ? AND responder_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, responderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MatchmakingPostResponse response = new MatchmakingPostResponse();
                    response.setResponseId(rs.getLong("response_id"));
                    response.setPostId(rs.getLong("post_id"));
                    response.setResponderId(rs.getLong("responder_id"));
                    response.setMessage(rs.getString("message"));
                    response.setStatus(rs.getString("status"));
                    response.setCreatedAt(rs.getTimestamp("created_at") != null 
                            ? rs.getTimestamp("created_at").toLocalDateTime() 
                            : null);
                    return response;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy phản hồi cụ thể: " + e.getMessage(), e);
        }
        return null;
    }

    public void updateResponse(long responseId, String message) {
        String sql = "UPDATE matchmaking_post_responses SET message = ?, created_at = GETDATE() WHERE response_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setLong(2, responseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật nội dung phản hồi: " + e.getMessage(), e);
        }
    }
}

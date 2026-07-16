package com.swp.dao;

import com.swp.model.MatchmakingPost;
import com.swp.model.dto.MatchmakingPostDTO;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MatchmakingPostDAO {

    public List<MatchmakingPostDTO> getAllPosts(String postType, String skillLevel, Long complexId, Long authorId) {
        autoCloseExpiredPosts();
        List<MatchmakingPostDTO> list = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT mp.*, u.full_name AS author_name, f.complex_name, " +
            "(SELECT COUNT(*) FROM matchmaking_post_responses mpr WHERE mpr.post_id = mp.post_id) AS response_count " +
            "FROM matchmaking_posts mp " +
            "LEFT JOIN users u ON mp.author_id = u.user_id " +
            "LEFT JOIN football_complexes f ON mp.complex_id = f.complex_id " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if (postType != null && !postType.trim().isEmpty() && !"ALL".equalsIgnoreCase(postType)) {
            sql.append(" AND mp.post_type = ?");
            params.add(postType);
        }

        if (skillLevel != null && !skillLevel.trim().isEmpty() && !"ALL".equalsIgnoreCase(skillLevel)) {
            sql.append(" AND mp.skill_level = ?");
            params.add(skillLevel);
        }

        if (complexId != null && complexId > 0) {
            sql.append(" AND mp.complex_id = ?");
            params.add(complexId);
        }

        if (authorId != null && authorId > 0) {
            sql.append(" AND mp.author_id = ?");
            params.add(authorId);
        } else {
            sql.append(" AND mp.status = 'OPEN'");
        }

        sql.append(" ORDER BY mp.created_at DESC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MatchmakingPost post = new MatchmakingPost();
                post.setPostId(rs.getLong("post_id"));
                post.setAuthorId(rs.getLong("author_id"));
                post.setPostType(rs.getString("post_type"));
                post.setTitle(rs.getString("title"));
                post.setDescription(rs.getString("description"));
                post.setSkillLevel(rs.getString("skill_level"));
                post.setExpectedTime(rs.getTimestamp("expected_time") != null 
                        ? rs.getTimestamp("expected_time").toLocalDateTime() 
                        : null);
                post.setComplexId(rs.getLong("complex_id") != 0 ? rs.getLong("complex_id") : null);
                post.setContactName(rs.getString("contact_name"));
                post.setContactPhone(rs.getString("contact_phone"));
                post.setStatus(rs.getString("status"));
                post.setCreatedAt(rs.getTimestamp("created_at") != null 
                        ? rs.getTimestamp("created_at").toLocalDateTime() 
                        : null);
                post.setUpdatedAt(rs.getTimestamp("updated_at") != null 
                        ? rs.getTimestamp("updated_at").toLocalDateTime() 
                        : null);

                String authorName = rs.getString("author_name");
                String complexName = rs.getString("complex_name");
                int responseCount = rs.getInt("response_count");

                list.add(new MatchmakingPostDTO(post, authorName, complexName, responseCount));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy cập dữ liệu MatchmakingPost: " + e.getMessage(), e);
        }

        return list;
    }

    public MatchmakingPostDTO getPostById(long postId) {
        autoCloseExpiredPosts();
        String sql = "SELECT mp.*, u.full_name AS author_name, f.complex_name " +
                     "FROM matchmaking_posts mp " +
                     "LEFT JOIN users u ON mp.author_id = u.user_id " +
                     "LEFT JOIN football_complexes f ON mp.complex_id = f.complex_id " +
                     "WHERE mp.post_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                MatchmakingPost post = new MatchmakingPost();
                post.setPostId(rs.getLong("post_id"));
                post.setAuthorId(rs.getLong("author_id"));
                post.setPostType(rs.getString("post_type"));
                post.setTitle(rs.getString("title"));
                post.setDescription(rs.getString("description"));
                post.setSkillLevel(rs.getString("skill_level"));
                post.setExpectedTime(rs.getTimestamp("expected_time") != null 
                        ? rs.getTimestamp("expected_time").toLocalDateTime() 
                        : null);
                post.setComplexId(rs.getLong("complex_id") != 0 ? rs.getLong("complex_id") : null);
                post.setContactName(rs.getString("contact_name"));
                post.setContactPhone(rs.getString("contact_phone"));
                post.setStatus(rs.getString("status"));
                post.setCreatedAt(rs.getTimestamp("created_at") != null 
                        ? rs.getTimestamp("created_at").toLocalDateTime() 
                        : null);
                post.setUpdatedAt(rs.getTimestamp("updated_at") != null 
                        ? rs.getTimestamp("updated_at").toLocalDateTime() 
                        : null);

                String authorName = rs.getString("author_name");
                String complexName = rs.getString("complex_name");

                return new MatchmakingPostDTO(post, authorName, complexName);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy thông tin chi tiết bài viết: " + e.getMessage(), e);
        }
        return null;
    }

    public void createPost(MatchmakingPost post) {
        String sql = "INSERT INTO matchmaking_posts (author_id, post_type, title, description, skill_level, expected_time, complex_id, contact_name, contact_phone, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, post.getAuthorId());
            ps.setString(2, post.getPostType());
            ps.setString(3, post.getTitle());
            ps.setString(4, post.getDescription());
            ps.setString(5, post.getSkillLevel());
            ps.setTimestamp(6, post.getExpectedTime() != null ? Timestamp.valueOf(post.getExpectedTime()) : null);
            if (post.getComplexId() != null) {
                ps.setLong(7, post.getComplexId());
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            ps.setString(8, post.getContactName());
            ps.setString(9, post.getContactPhone());
            ps.setString(10, post.getStatus() != null ? post.getStatus() : "OPEN");
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tạo mới bài đăng tìm đối: " + e.getMessage(), e);
        }
    }

    public void updatePostStatus(long postId, String status) {
        String sql = "UPDATE matchmaking_posts SET status = ?, updated_at = GETDATE() WHERE post_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, postId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái bài đăng: " + e.getMessage(), e);
        }
    }

    public void updatePost(MatchmakingPost post) {
        String sql = "UPDATE matchmaking_posts SET title = ?, description = ?, skill_level = ?, expected_time = ?, facility_id = ?, contact_name = ?, contact_phone = ?, updated_at = GETDATE() WHERE post_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getDescription());
            ps.setString(3, post.getSkillLevel());
            ps.setTimestamp(4, post.getExpectedTime() != null ? Timestamp.valueOf(post.getExpectedTime()) : null);
            if (post.getFacilityId() != null) {
                ps.setLong(5, post.getFacilityId());
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            ps.setString(6, post.getContactName());
            ps.setString(7, post.getContactPhone());
            ps.setLong(8, post.getPostId());
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật bài đăng tìm đối: " + e.getMessage(), e);
        }
    }

    public void deletePost(long postId) {
        String deleteResponsesSql = "DELETE FROM matchmaking_post_responses WHERE post_id = ?";
        String deletePostSql = "DELETE FROM matchmaking_posts WHERE post_id = ?";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(deleteResponsesSql);
                 PreparedStatement ps2 = conn.prepareStatement(deletePostSql)) {
                
                ps1.setLong(1, postId);
                ps1.executeUpdate();
                
                ps2.setLong(1, postId);
                ps2.executeUpdate();
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa bài đăng tìm đối: " + e.getMessage(), e);
        }
    }

    private void autoCloseExpiredPosts() {
        String sql = "UPDATE matchmaking_posts SET status = 'CLOSED' WHERE expected_time < GETDATE() AND status = 'OPEN'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động đóng các bài đăng hết hạn: " + e.getMessage());
        }
    }
}

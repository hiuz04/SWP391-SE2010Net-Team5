package com.swp.controller.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.dao.MatchmakingPostDAO;
import com.swp.dao.MatchmakingPostResponseDAO;
import com.swp.model.MatchmakingPost;
import com.swp.model.MatchmakingPostResponse;
import com.swp.model.User;
import com.swp.model.dto.MatchmakingPostDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet("/api/matchmaking")
public class MatchmakingAPIController extends HttpServlet {

    private final MatchmakingPostDAO postDAO = new MatchmakingPostDAO();
    private final MatchmakingPostResponseDAO responseDAO = new MatchmakingPostResponseDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String action = req.getParameter("action");
        
        if ("get_responses".equals(action)) {
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\":\"Vui lòng đăng nhập để xem phản hồi.\"}");
                out.flush();
                return;
            }

            String postIdStr = req.getParameter("postId");
            if (postIdStr == null || postIdStr.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Thiếu mã bài viết.\"}");
                out.flush();
                return;
            }

            try {
                long postId = Long.parseLong(postIdStr);
                var responses = responseDAO.getResponsesByPostId(postId);
                
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class,
                                (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                                        -> new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))))
                        .create();
                
                out.print(gson.toJson(responses));
            } catch (Exception e) {
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\":\"Lỗi khi lấy danh sách phản hồi: " + e.getMessage() + "\"}");
            }
            out.flush();
            return;
        }

        String postType = req.getParameter("postType");
        String skillLevel = req.getParameter("skillLevel");
        
        Long complexId = null;
        String complexIdStr = req.getParameter("complexId");
        if (complexIdStr != null && !complexIdStr.trim().isEmpty()) {
            try {
                complexId = Long.parseLong(complexIdStr);
            } catch (NumberFormatException e) {
                // Ignore invalid long
            }
        }

        Long authorId = null;
        String myPostsOnlyStr = req.getParameter("myPostsOnly");
        if ("true".equalsIgnoreCase(myPostsOnlyStr)) {
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                User user = (User) session.getAttribute("user");
                authorId = user.getUserId();
            }
        }

        try {
            List<MatchmakingPostDTO> posts = postDAO.getAllPosts(postType, skillLevel, complexId, authorId);
            
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class,
                            (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                                    -> new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))))
                    .create();

            out.print(gson.toJson(posts));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Lỗi hệ thống khi lấy danh sách tin tuyển đối: " + e.getMessage() + "\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Unauthorized - Vui lòng đăng nhập\"}");
            out.flush();
            return;
        }

        User user = (User) session.getAttribute("user");
        long userId = user.getUserId();
        String action = req.getParameter("action");

        try {
            if ("create_post".equals(action)) {
                String postType = req.getParameter("postType");
                String title = req.getParameter("title");
                String description = req.getParameter("description");
                String skillLevel = req.getParameter("skillLevel");
                String expectedTimeStr = req.getParameter("expectedTime");
                String complexIdStr = req.getParameter("complexId");
                String contactName = req.getParameter("contactName");
                String contactPhone = req.getParameter("contactPhone");

                if (title == null || title.trim().isEmpty() ||
                    postType == null || postType.trim().isEmpty() ||
                    contactName == null || contactName.trim().isEmpty() ||
                    contactPhone == null || contactPhone.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thiếu các thông tin bắt buộc!\"}");
                    out.flush();
                    return;
                }

                LocalDateTime expectedTime = null;
                if (expectedTimeStr != null && !expectedTimeStr.trim().isEmpty()) {
                    // input type="datetime-local" sends format like "2026-07-07T19:00"
                    expectedTime = LocalDateTime.parse(expectedTimeStr);
                }

                Long complexId = null;
                if (complexIdStr != null && !complexIdStr.trim().isEmpty()) {
                    complexId = Long.parseLong(complexIdStr);
                }

                MatchmakingPost post = new MatchmakingPost();
                post.setAuthorId(userId);
                post.setPostType(postType);
                post.setTitle(title);
                post.setDescription(description);
                post.setSkillLevel(skillLevel);
                post.setExpectedTime(expectedTime);
                post.setComplexId(complexId);
                post.setContactName(contactName);
                post.setContactPhone(contactPhone);
                post.setStatus("OPEN");

                postDAO.createPost(post);
                out.print("{\"success\":true}");
            } else if ("respond_post".equals(action)) {
                String postIdStr = req.getParameter("postId");
                String message = req.getParameter("message");

                if (postIdStr == null || postIdStr.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thiếu mã bài viết!\"}");
                    out.flush();
                    return;
                }

                long postId = Long.parseLong(postIdStr);
                
                MatchmakingPostResponse response = new MatchmakingPostResponse();
                response.setPostId(postId);
                response.setResponderId(userId);
                response.setMessage(message);
                response.setStatus("PENDING");

                responseDAO.createResponse(response);
                out.print("{\"success\":true}");
            } else if ("close_post".equals(action)) {
                String postIdStr = req.getParameter("postId");
                if (postIdStr == null || postIdStr.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thiếu mã bài viết!\"}");
                    out.flush();
                    return;
                }

                long postId = Long.parseLong(postIdStr);
                MatchmakingPostDTO existing = postDAO.getPostById(postId);
                
                if (existing == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"Bài đăng không tồn tại!\"}");
                    out.flush();
                    return;
                }

                if (existing.getPost().getAuthorId() != userId) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"error\":\"Bạn không có quyền đóng bài viết này!\"}");
                    out.flush();
                    return;
                }

                postDAO.updatePostStatus(postId, "CLOSED");
                out.print("{\"success\":true}");
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Hành động không hợp lệ!\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Lỗi máy chủ: " + e.getMessage() + "\"}");
        }
        out.flush();
    }
}

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
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Mã bài viết phải là dạng số.\"}");
            } catch (Exception e) {
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\":\"Lỗi khi lấy danh sách phản hồi: " + e.getMessage() + "\"}");
            }
            out.flush();
            return;
        } else if ("get_my_response".equals(action)) {
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\":\"Vui lòng đăng nhập.\"}");
                out.flush();
                return;
            }
            User user = (User) session.getAttribute("user");
            long userId = user.getUserId();
            String postIdStr = req.getParameter("postId");
            if (postIdStr == null || postIdStr.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Thiếu mã bài viết.\"}");
                out.flush();
                return;
            }
            try {
                long postId = Long.parseLong(postIdStr);
                MatchmakingPostResponse existingResponse = responseDAO.getResponseByPostAndResponder(postId, userId);
                if (existingResponse != null) {
                    String escapedMsg = existingResponse.getMessage()
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r");
                    out.print("{\"exists\":true,\"message\":\"" + escapedMsg + "\"}");
                } else {
                    out.print("{\"exists\":false}");
                }
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Mã bài viết phải là số.\"}");
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\":\"Lỗi hệ thống: " + e.getMessage() + "\"}");
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

                if (expectedTime != null && expectedTime.isBefore(LocalDateTime.now())) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thời gian dự kiến không được trước ngày và giờ hiện tại!\"}");
                    out.flush();
                    return;
                }

                if (contactPhone == null || !contactPhone.trim().matches("^0\\d{9}$")) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Số điện thoại không đúng định dạng (phải gồm 10 số và bắt đầu bằng số 0)!\"}");
                    out.flush();
                    return;
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

                long postId;
                try {
                    postId = Long.parseLong(postIdStr);
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Mã bài viết phải là số!\"}");
                    out.flush();
                    return;
                }

                MatchmakingPostDTO postDTO = postDAO.getPostById(postId);
                if (postDTO == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"Bài đăng không tồn tại!\"}");
                    out.flush();
                    return;
                }

                if (postDTO.getPost().getAuthorId() == userId) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Bạn không thể tự phản hồi bài đăng của chính mình!\"}");
                    out.flush();
                    return;
                }

                if (message == null || message.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Nội dung lời nhắn không được để trống!\"}");
                    out.flush();
                    return;
                }

                // Check if already responded
                MatchmakingPostResponse existingResponse = responseDAO.getResponseByPostAndResponder(postId, userId);
                if (existingResponse != null) {
                    // Update existing response
                    responseDAO.updateResponse(existingResponse.getResponseId(), message);
                } else {
                    // Create new response
                    MatchmakingPostResponse response = new MatchmakingPostResponse();
                    response.setPostId(postId);
                    response.setResponderId(userId);
                    response.setMessage(message);
                    response.setStatus("PENDING");
                    responseDAO.createResponse(response);
                }
                out.print("{\"success\":true}");
            } else if ("close_post".equals(action)) {
                String postIdStr = req.getParameter("postId");
                if (postIdStr == null || postIdStr.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thiếu mã bài viết!\"}");
                    out.flush();
                    return;
                }

                long postId;
                try {
                    postId = Long.parseLong(postIdStr);
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Mã bài viết phải là số!\"}");
                    out.flush();
                    return;
                }

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
            } else if ("update_post".equals(action)) {
                String postIdStr = req.getParameter("postId");
                String title = req.getParameter("title");
                String description = req.getParameter("description");
                String skillLevel = req.getParameter("skillLevel");
                String expectedTimeStr = req.getParameter("expectedTime");
                String facilityIdStr = req.getParameter("facilityId");
                String contactName = req.getParameter("contactName");
                String contactPhone = req.getParameter("contactPhone");

                if (postIdStr == null || postIdStr.trim().isEmpty() ||
                    title == null || title.trim().isEmpty() ||
                    contactName == null || contactName.trim().isEmpty() ||
                    contactPhone == null || contactPhone.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thiếu các thông tin bắt buộc!\"}");
                    out.flush();
                    return;
                }

                long postId;
                try {
                    postId = Long.parseLong(postIdStr);
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Mã bài viết không hợp lệ!\"}");
                    out.flush();
                    return;
                }

                MatchmakingPostDTO existing = postDAO.getPostById(postId);
                if (existing == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"Bài đăng không tồn tại!\"}");
                    out.flush();
                    return;
                }

                if (existing.getPost().getAuthorId() != userId) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"error\":\"Bạn không có quyền chỉnh sửa bài viết này!\"}");
                    out.flush();
                    return;
                }

                LocalDateTime expectedTime = null;
                if (expectedTimeStr != null && !expectedTimeStr.trim().isEmpty()) {
                    expectedTime = LocalDateTime.parse(expectedTimeStr);
                }

                if (expectedTime != null && expectedTime.isBefore(LocalDateTime.now())) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thời gian dự kiến không được trước ngày và giờ hiện tại!\"}");
                    out.flush();
                    return;
                }

                if (!contactPhone.trim().matches("^0\\d{9}$")) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Số điện thoại không đúng định dạng (phải gồm 10 số và bắt đầu bằng số 0)!\"}");
                    out.flush();
                    return;
                }

                Long facilityId = null;
                if (facilityIdStr != null && !facilityIdStr.trim().isEmpty()) {
                    facilityId = Long.parseLong(facilityIdStr);
                }

                MatchmakingPost post = existing.getPost();
                post.setTitle(title);
                post.setDescription(description);
                post.setSkillLevel(skillLevel);
                post.setExpectedTime(expectedTime);
                post.setFacilityId(facilityId);
                post.setContactName(contactName);
                post.setContactPhone(contactPhone);

                postDAO.updatePost(post);
                out.print("{\"success\":true}");
            } else if ("delete_post".equals(action)) {
                String postIdStr = req.getParameter("postId");
                if (postIdStr == null || postIdStr.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Thiếu mã bài viết!\"}");
                    out.flush();
                    return;
                }

                long postId;
                try {
                    postId = Long.parseLong(postIdStr);
                } catch (NumberFormatException e) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Mã bài viết phải là số!\"}");
                    out.flush();
                    return;
                }

                MatchmakingPostDTO existing = postDAO.getPostById(postId);
                if (existing == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"Bài đăng không tồn tại!\"}");
                    out.flush();
                    return;
                }

                if (existing.getPost().getAuthorId() != userId) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"error\":\"Bạn không có quyền xóa bài viết này!\"}");
                    out.flush();
                    return;
                }

                if (!"CLOSED".equalsIgnoreCase(existing.getPost().getStatus())) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Chỉ có thể xóa bài viết đã đóng tuyển!\"}");
                    out.flush();
                    return;
                }

                postDAO.deletePost(postId);
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

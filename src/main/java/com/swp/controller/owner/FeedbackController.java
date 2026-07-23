package com.swp.controller.owner;

import com.swp.service.FeedbackService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/owner/feedback")
public class FeedbackController extends HttpServlet {

    private static final FeedbackService feedbackService = new FeedbackService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String action = req.getParameter("action");
        switch (action == null ? "" : action) {
            case "reply":
                reply(req, resp);
                break;
            default:
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"status\":\"error\",\"message\":\"Hành động không hợp lệ\"}");
                break;
        }
    }

    private void reply(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            long feedbackId = Long.parseLong(req.getParameter("feedbackId"));
            String message = req.getParameter("message");
            if (message == null || message.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"status\":\"error\",\"message\":\"Nội dung phản hồi không được để trống\"}");
                return;
            }
            feedbackService.ownerReplyToFeedback(feedbackId, message.trim());
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"status\":\"success\",\"message\":\"Phản hồi thành công\"}");
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Mã đánh giá không hợp lệ\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Lỗi hệ thống.\"}");
        }
    }

}

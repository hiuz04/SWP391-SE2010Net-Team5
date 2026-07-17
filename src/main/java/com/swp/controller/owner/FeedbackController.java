package com.swp.controller.owner;

import com.swp.service.FeedbackService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/feedback-owner")
public class FeedbackController extends HttpServlet {

    private static final FeedbackService feedbackService = new FeedbackService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        switch (action == null ? "" : action) {
            case "reply":
                reply(req, resp);
                break;
            case "edit":
                break;
        }
    }

    private void reply(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            long feedbackId = Long.parseLong(req.getParameter("feedbackId"));
            String message = req.getParameter("message");
            feedbackService.ownerReplyToFeedback(feedbackId, message);
        }
        catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");

            resp.getWriter().write("Lỗi hệ thống.");
        }
    }

}

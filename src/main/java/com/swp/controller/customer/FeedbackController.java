package com.swp.controller.customer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.Feedback;
import com.swp.model.User;
import com.swp.service.FeedbackService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/feedback-user")
public class FeedbackController extends HttpServlet {

    private static final FeedbackService feedbackService = new FeedbackService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        User currentUser = requireLogin(req, resp);
        if (currentUser == null) {
            return;
        }

        String action = req.getParameter("action");

        if ("get".equals(action)) {
            getInfo(req, resp);
            return;
        }

        if ("create".equals(action)) {
            long bookingId = Long.parseLong(req.getParameter("bookingId"));
            req.setAttribute("bookingId", bookingId);
        }

        if ("edit".equals(action)) {
            long feedbackId = Long.parseLong(req.getParameter("id"));
            req.setAttribute("feedbackId", feedbackId);
        }

        req.getRequestDispatcher("/WEB-INF/customer/feedback-form.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        switch (action == null ? "" : action) {
            case "add":
                createFeedback(req, resp);
                break;
            case "update":
                editFeedback(req, resp);
                break;
        }
    }

    private void createFeedback(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            long bookingId = Long.parseLong(request.getParameter("bookingId"));
            int rating = Integer.parseInt(request.getParameter("rating"));
            String description = request.getParameter("description");

            Feedback feedback = new Feedback();
            feedback.setBookingId(bookingId);
            feedback.setRating(rating);
            feedback.setDescription(description);

            feedbackService.createFeedback(feedback);

            response.sendRedirect(request.getContextPath()
                    + "/booking?action=history");

        } catch (NumberFormatException e) {
            request.setAttribute("error", "Dữ liệu không hợp lệ.");
            request.getRequestDispatcher("/WEB-INF/customer/feedback-form.jsp")
                    .forward(request, response);

        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/customer/feedback-form.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void editFeedback(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User user = (User) request.getSession().getAttribute("user");

            long feedbackId = Long.parseLong(request.getParameter("feedbackId"));
            int rating = Integer.parseInt(request.getParameter("rating"));
            String description = request.getParameter("description");

            feedbackService.updateFeedback(
                    user.getUserId(),
                    feedbackId,
                    rating,
                    description
            );

            response.sendRedirect(request.getContextPath()
                    + "/booking?action=history");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/customer/feedback-form.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void getInfo(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            long id = Long.parseLong(req.getParameter("id"));
            Feedback feedback = feedbackService.getFeedbackById(id);
            if (feedback == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Feedback not found");
                return;
            }

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(
                            LocalDateTime.class,
                            (JsonSerializer<LocalDateTime>) (src, t, c)
                                    -> new JsonPrimitive(src.toString())
                    )
                    .create();

            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(gson.toJson(feedback));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid feedback id");
        }
    }

    private User requireLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        return (User) session.getAttribute("user");
    }
}

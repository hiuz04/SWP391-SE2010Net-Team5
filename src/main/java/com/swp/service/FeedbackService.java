package com.swp.service;

import com.swp.dao.BookingDAO;
import com.swp.dao.FeedbackDAO;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.UserDAO;
import com.swp.model.Booking;
import com.swp.model.Feedback;
import com.swp.model.dto.FeedbackDTO;

import java.util.List;

public class FeedbackService {

    private static FeedbackDAO feedbackDAO = new FeedbackDAO();
    private static BookingDAO bookingDAO = new BookingDAO();
    private static UserDAO userDAO = new UserDAO();
    private static FootballComplexDAO complexDAO = new FootballComplexDAO();

    public List<FeedbackDTO> getAllFeedbackOfThisComplexes(long complexId) {
        List<FeedbackDTO> feedbacks = feedbackDAO.getAllFeedbackOfThisComplexes(complexId);

        return feedbacks;
    }

    public void createFeedback(Feedback feedback) {

        if (feedback == null) {
            throw new IllegalArgumentException("Feedback không được để trống.");
        }

        // Rating bắt buộc
        if (feedback.getRating() < 1 || feedback.getRating() > 5) {
            throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao.");
        }

        // Booking phải tồn tại
        Booking booking = bookingDAO.getBookingById(feedback.getBookingId());

        if (booking == null) {
            throw new IllegalArgumentException("Booking không tồn tại.");
        }

        // Booking phải Completed
        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá booking đã hoàn thành.");
        }

        // Booking chưa được đánh giá
        if (feedbackDAO.existsByBookingId(feedback.getBookingId())) {
            throw new IllegalArgumentException("Booking này đã được đánh giá.");
        }

        // Chuẩn hóa dữ liệu
        if (feedback.getDescription() != null) {
            feedback.setDescription(feedback.getDescription().trim());

            if (feedback.getDescription().length() > 1000) {
                throw new IllegalArgumentException("Mô tả không được vượt quá 1000 ký tự.");
            }
        }

        // Không tin dữ liệu từ client
        feedback.setUserId(booking.getCustomerId());
        feedback.setComplexId(booking.getComplexId());
        feedback.setStatus("ACTIVE");

        feedbackDAO.addFeedback(feedback);
    }

    public void updateFeedback(long userId, long feedbackId, int rating, String description) {
        Feedback feedback = feedbackDAO.getFeedbackById(feedbackId);

        if (feedback == null) {
            throw new IllegalArgumentException("Đánh giá không tồn tại.");
        }

        if (feedback.getUserId() != userId) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa đánh giá này.");
        }

        if (!"ACTIVE".equals(feedback.getStatus())) {
            throw new IllegalArgumentException("Đánh giá không hợp lệ.");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5.");
        }

        feedbackDAO.updateFeedback(
                feedbackId,
                rating,
                description
        );
    }

    public void ownerReplyToFeedback(long feedbackId, String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Reply message cannot be empty.");
        }

        Feedback feedback = feedbackDAO.getFeedbackById(feedbackId);

        if (feedback == null) {
            throw new IllegalArgumentException("Feedback does not exist.");
        }

        if (!"ACTIVE".equals(feedback.getStatus())) {
            throw new IllegalStateException("Cannot reply to a hidden feedback.");
        }

        if (feedback.getOwnerReply() != null) {
            throw new IllegalStateException("Feedback has already been replied.");
        }

        feedbackDAO.addReply(feedbackId, message);
    }

    public Feedback getFeedbackById(long feedbackId) {
        return feedbackDAO.getFeedbackById(feedbackId);
    }
}

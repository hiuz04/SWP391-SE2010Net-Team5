package com.swp.model;

import java.time.LocalDateTime;

public class Feedback {

    private long feedbackId;
    private long userId;
    private long complexId;
    private long bookingId;
    private int rating;
    private String description;
    private String ownerReply;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime replyAt;

    public Feedback() {
    }

    public Feedback(long feedbackId, long userId, long complexId, long bookingId, int rating, String description, String ownerReply, String status, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime replyAt) {
        this.feedbackId = feedbackId;
        this.userId = userId;
        this.complexId = complexId;
        this.bookingId = bookingId;
        this.rating = rating;
        this.description = description;
        this.ownerReply = ownerReply;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.replyAt = replyAt;
    }

    public long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getComplexId() {
        return complexId;
    }

    public void setComplexId(long complexId) {
        this.complexId = complexId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerReply() {
        return ownerReply;
    }

    public void setOwnerReply(String ownerReply) {
        this.ownerReply = ownerReply;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getReplyAt() {
        return replyAt;
    }

    public void setReplyAt(LocalDateTime replyAt) {
        this.replyAt = replyAt;
    }
}

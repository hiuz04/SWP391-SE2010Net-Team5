package com.swp.model.dto;

import java.time.LocalDateTime;

public class FeedbackDTO {

    private long feedbackId;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String fieldName;
    private int rating;
    private String feedbackDesc;
    private String ownerReply;

    public FeedbackDTO() {
    }

    public FeedbackDTO(long feedbackId, String userName, LocalDateTime createdAt, LocalDateTime updatedAt, String fieldName, int rating, String feedbackDesc, String ownerReply) {
        this.feedbackId = feedbackId;
        this.userName = userName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fieldName = fieldName;
        this.rating = rating;
        this.feedbackDesc = feedbackDesc;
        this.ownerReply = ownerReply;
    }

    public long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getFeedbackDesc() {
        return feedbackDesc;
    }

    public void setFeedbackDesc(String feedbackDesc) {
        this.feedbackDesc = feedbackDesc;
    }

    public String getOwnerReply() {
        return ownerReply;
    }

    public void setOwnerReply(String ownerReply) {
        this.ownerReply = ownerReply;
    }
}

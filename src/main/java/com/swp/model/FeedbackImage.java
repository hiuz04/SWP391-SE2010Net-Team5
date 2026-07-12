package com.swp.model;

import java.time.LocalDateTime;

public class FeedbackImage {

    private long imageId;
    private long feedbackId;
    private String url;
    private String publicId;
    private LocalDateTime createdAt;

    public FeedbackImage() {
    }

    public FeedbackImage(long imageId, long feedbackId, String url, String publicId) {
        this.imageId = imageId;
        this.feedbackId = feedbackId;
        this.url = url;
        this.publicId = publicId;
    }

    public long getImageId() {
        return imageId;
    }

    public void setImageId(long imageId) {
        this.imageId = imageId;
    }

    public long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MatchmakingPostResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long responseId;
    private Long postId;
    private Long responderId;
    private String message;
    private String status;
    private LocalDateTime createdAt;

    public MatchmakingPostResponse() {
    }

    public MatchmakingPostResponse(Long responseId, Long postId, Long responderId, String message, String status, LocalDateTime createdAt) {
        this.responseId = responseId;
        this.postId = postId;
        this.responderId = responderId;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getResponseId() {
        return responseId;
    }

    public void setResponseId(Long responseId) {
        this.responseId = responseId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getResponderId() {
        return responderId;
    }

    public void setResponderId(Long responderId) {
        this.responderId = responderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
}

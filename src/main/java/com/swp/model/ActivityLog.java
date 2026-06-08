package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ActivityLog implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long logId;
    private Long userId;
    private String action;
    private String description;
    private LocalDateTime createdAt;

    public ActivityLog() {
    }

    public ActivityLog(Long logId, Long userId, String action, String description, LocalDateTime createdAt) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

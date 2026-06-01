package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MatchmakingPost implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long postId;
    private Long authorId;
    private String postType;
    private String title;
    private String description;
    private String skillLevel;
    private LocalDateTime expectedTime;
    private Long facilityId;
    private String contactName;
    private String contactPhone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MatchmakingPost() {
    }

    public MatchmakingPost(Long postId, Long authorId, String postType, String title, String description, String skillLevel, LocalDateTime expectedTime, Long facilityId, String contactName, String contactPhone, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.postId = postId;
        this.authorId = authorId;
        this.postType = postType;
        this.title = title;
        this.description = description;
        this.skillLevel = skillLevel;
        this.expectedTime = expectedTime;
        this.facilityId = facilityId;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public LocalDateTime getExpectedTime() {
        return expectedTime;
    }

    public void setExpectedTime(LocalDateTime expectedTime) {
        this.expectedTime = expectedTime;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
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
}
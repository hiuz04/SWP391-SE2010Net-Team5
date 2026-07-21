package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Integer roleId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String avatarUrl;
    private String googleId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String roleName;
    private boolean isVip;
    private LocalDateTime vipValidUntil;
    private int rewardPoints;

    public User() {
    }

    public User(int rewardPoints, LocalDateTime vipValidUntil, boolean isVip, String roleName, LocalDateTime updatedAt, LocalDateTime createdAt, String status, String googleId, String avatarUrl, String passwordHash, String phone, String email, String fullName, Integer roleId, Long userId) {
        this.rewardPoints = rewardPoints;
        this.vipValidUntil = vipValidUntil;
        this.isVip = isVip;
        this.roleName = roleName;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.status = status;
        this.googleId = googleId;
        this.avatarUrl = avatarUrl;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.email = email;
        this.fullName = fullName;
        this.roleId = roleId;
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
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

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isVip() {
        return isVip;
    }

    public void setVip(boolean vip) {
        isVip = vip;
    }

    public LocalDateTime getVipValidUntil() {
        return vipValidUntil;
    }

    public void setVipValidUntil(LocalDateTime vipValidUntil) {
        this.vipValidUntil = vipValidUntil;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }
}
package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PasswordResetToken implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long tokenId;
    private Long userId;
    private String token;
    private String otpCode;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime createdAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(Long tokenId, Long userId, String token, String otpCode, LocalDateTime expiresAt, Boolean used, LocalDateTime createdAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.token = token;
        this.otpCode = otpCode;
        this.expiresAt = expiresAt;
        this.used = used;
        this.createdAt = createdAt;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
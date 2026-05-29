package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SystemSetting implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long settingId;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;

    public SystemSetting() {
    }

    public SystemSetting(Long settingId, String settingKey, String settingValue, String description, LocalDateTime updatedAt) {
        this.settingId = settingId;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public Long getSettingId() {
        return settingId;
    }

    public void setSettingId(Long settingId) {
        this.settingId = settingId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

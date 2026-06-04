package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class FieldMaintenanceSchedule implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long maintenanceId;
    private Long fieldId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    public FieldMaintenanceSchedule() {
    }

    public FieldMaintenanceSchedule(Long maintenanceId, Long fieldId, LocalDateTime startTime, LocalDateTime endTime, String reason, String status, LocalDateTime createdAt) {
        this.maintenanceId = maintenanceId;
        this.fieldId = fieldId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getMaintenanceId() {
        return maintenanceId;
    }

    public void setMaintenanceId(Long maintenanceId) {
        this.maintenanceId = maintenanceId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
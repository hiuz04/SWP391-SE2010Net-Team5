package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Field implements Serializable {
//    private static final long serialVersionUID = 1L;
    private Long fieldId;
    private Long complexId;
    private Integer fieldTypeId;
    private String fieldName;
    private String description;
    private String status;
    private boolean isHot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Field() {
    }

    public Field(Long fieldId, Long complexId, Integer fieldTypeId, String fieldName, String description, String status, boolean isHot, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.fieldId = fieldId;
        this.complexId = complexId;
        this.fieldTypeId = fieldTypeId;
        this.fieldName = fieldName;
        this.description = description;
        this.status = status;
        this.isHot = isHot;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public Long getComplexId() {
        return complexId;
    }

    public void setComplexId(Long complexId) {
        this.complexId = complexId;
    }

    public Integer getFieldTypeId() {
        return fieldTypeId;
    }

    public void setFieldTypeId(Integer fieldTypeId) {
        this.fieldTypeId = fieldTypeId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHot() {
        return isHot;
    }

    public void setHot(boolean isHot) {
        this.isHot = isHot;
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

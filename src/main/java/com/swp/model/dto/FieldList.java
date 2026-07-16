package com.swp.model.dto;

public class FieldList {

    private long fieldId;
    private String fieldName;
    private String type;
    private String complexName;
    private String description;
    private String status;
    private boolean isHot;

    public FieldList(long fieldId, String fieldName, String type, String complexName, String description, String status, boolean isHot) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.type = type;
        this.complexName = complexName;
        this.description = description;
        this.status = status;
        this.isHot = isHot;
    }

    public long getFieldId() {
        return fieldId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getType() {
        return type;
    }

    public String getComplexName() {
        return complexName;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public boolean isHot() {
        return isHot;
    }
}

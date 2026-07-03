package com.swp.model.dto;

public class FieldList {

    private long fieldId;
    private String fieldName;
    private String type;
    private String facilityName;
    private String description;
    private String status;
    private boolean isHot;

    public FieldList(long fieldId, String fieldName, String type, String facilityName, String description, String status, boolean isHot) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.type = type;
        this.facilityName = facilityName;
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

    public String getFacilityName() {
        return facilityName;
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

package com.swp.model.dto;

import com.swp.model.Feedback;
import com.swp.model.Field;
import com.swp.model.FieldType;

import java.time.LocalTime;
import java.util.List;

public class FieldDetail {

    private long complexId;
    private String complexName;
    private String complexAddress;
    private List<FieldType> fieldTypeList;
    private List<Field> fields;
    private String description;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String hotline;
    private List<FeedbackDTO> feedbacks;

    public FieldDetail() {
    }

    public FieldDetail(long complexId, String complexName, String complexAddress, List<FieldType> fieldTypeList, List<Field> fields, String description, LocalTime openingTime, LocalTime closingTime, String hotline, List<FeedbackDTO> feedbacks) {
        this.complexId = complexId;
        this.complexName = complexName;
        this.complexAddress = complexAddress;
        this.fieldTypeList = fieldTypeList;
        this.fields = fields;
        this.description = description;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.hotline = hotline;
        this.feedbacks = feedbacks;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public String getHotline() {
        return hotline;
    }

    public void setHotline(String hotline) {
        this.hotline = hotline;
    }

    public long getComplexId() {
        return complexId;
    }

    public void setComplexId(long complexId) {
        this.complexId = complexId;
    }

    public String getComplexName() {
        return complexName;
    }

    public void setComplexName(String complexName) {
        this.complexName = complexName;
    }

    public String getComplexAddress() {
        return complexAddress;
    }

    public void setComplexAddress(String complexAddress) {
        this.complexAddress = complexAddress;
    }

    public List<FieldType> getFieldTypeList() {
        return fieldTypeList;
    }

    public void setFieldTypeList(List<FieldType> fieldTypeList) {
        this.fieldTypeList = fieldTypeList;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public List<FeedbackDTO> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<FeedbackDTO> feedbacks) {
        this.feedbacks = feedbacks;
    }
}

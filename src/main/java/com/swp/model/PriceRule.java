package com.swp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class PriceRule implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long priceRuleId;
    private Long facilityId;
    private Integer fieldTypeId;
    private Long fieldId;
    private String ruleName;
    private String dayOfWeek;
    private LocalDate specificDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private String ruleType;
    private Integer priority;
    private String status;
    private LocalDateTime createdAt;

    public PriceRule() {
    }

    public PriceRule(Long priceRuleId, Long facilityId, Integer fieldTypeId, Long fieldId, String ruleName, String dayOfWeek, LocalDate specificDate, LocalTime startTime, LocalTime endTime, BigDecimal price, String ruleType, Integer priority, String status, LocalDateTime createdAt) {
        this.priceRuleId = priceRuleId;
        this.facilityId = facilityId;
        this.fieldTypeId = fieldTypeId;
        this.fieldId = fieldId;
        this.ruleName = ruleName;
        this.dayOfWeek = dayOfWeek;
        this.specificDate = specificDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.ruleType = ruleType;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getPriceRuleId() {
        return priceRuleId;
    }

    public void setPriceRuleId(Long priceRuleId) {
        this.priceRuleId = priceRuleId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public Integer getFieldTypeId() {
        return fieldTypeId;
    }

    public void setFieldTypeId(Integer fieldTypeId) {
        this.fieldTypeId = fieldTypeId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalDate getSpecificDate() {
        return specificDate;
    }

    public void setSpecificDate(LocalDate specificDate) {
        this.specificDate = specificDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

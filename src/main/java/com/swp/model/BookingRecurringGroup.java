package com.swp.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingRecurringGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long recurringGroupId;
    private Long customerId;
    private String repeatType;
    private LocalDate repeatUntil;
    private LocalDateTime createdAt;

    public BookingRecurringGroup() {
    }

    public BookingRecurringGroup(Long recurringGroupId, Long customerId, String repeatType, LocalDate repeatUntil, LocalDateTime createdAt) {
        this.recurringGroupId = recurringGroupId;
        this.customerId = customerId;
        this.repeatType = repeatType;
        this.repeatUntil = repeatUntil;
        this.createdAt = createdAt;
    }

    public Long getRecurringGroupId() {
        return recurringGroupId;
    }

    public void setRecurringGroupId(Long recurringGroupId) {
        this.recurringGroupId = recurringGroupId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public LocalDate getRepeatUntil() {
        return repeatUntil;
    }

    public void setRepeatUntil(LocalDate repeatUntil) {
        this.repeatUntil = repeatUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

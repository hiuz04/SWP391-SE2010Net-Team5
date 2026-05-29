package com.swp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private String bookingCode;
    private Long customerId;
    private Long facilityId;
    private Long fieldId;
    private Long recurringGroupId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private String status;
    private LocalDateTime holdExpiresAt;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private String qrCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Booking() {
    }

    public Booking(Long bookingId, String bookingCode, Long customerId, Long facilityId, Long fieldId, Long recurringGroupId, LocalDateTime startTime, LocalDateTime endTime, BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal totalAmount, BigDecimal depositAmount, String status, LocalDateTime holdExpiresAt, String cancellationReason, LocalDateTime cancelledAt, String qrCode, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.customerId = customerId;
        this.facilityId = facilityId;
        this.fieldId = fieldId;
        this.recurringGroupId = recurringGroupId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.originalPrice = originalPrice;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.depositAmount = depositAmount;
        this.status = status;
        this.holdExpiresAt = holdExpiresAt;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
        this.qrCode = qrCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public Long getRecurringGroupId() {
        return recurringGroupId;
    }

    public void setRecurringGroupId(Long recurringGroupId) {
        this.recurringGroupId = recurringGroupId;
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

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
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
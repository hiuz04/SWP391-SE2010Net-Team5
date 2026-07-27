package com.swp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity booking lưu trạng thái đặt sân, thời gian giữ chỗ HOLD, tiền cọc,
 * voucher áp dụng và thông tin hủy để các DAO cập nhật xuyên suốt booking/payment/checkout.
 */
public class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private String bookingCode;
    private Long customerId;
    private Long complexId;
    private Long fieldId;
    private Long recurringGroupId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer voucherId;
    private Long userVoucherId;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private BigDecimal depositAmount;
    private String status;
    private LocalDateTime holdExpiresAt;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Booking() {
    }

    public Booking(Long bookingId, String bookingCode, Long customerId, Long complexId, Long fieldId, Long recurringGroupId, LocalDateTime startTime, LocalDateTime endTime,Integer voucherId, BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal totalAmount, BigDecimal finalAmount, BigDecimal depositAmount, String status, LocalDateTime holdExpiresAt, String cancellationReason, LocalDateTime cancelledAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.customerId = customerId;
        this.complexId = complexId;
        this.fieldId = fieldId;
        this.recurringGroupId = recurringGroupId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.voucherId = voucherId;
        this.originalPrice = originalPrice;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.finalAmount = finalAmount;
        this.depositAmount = depositAmount;
        this.status = status;
        this.holdExpiresAt = holdExpiresAt;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
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

    public Long getComplexId() {
        return complexId;
    }

    public void setComplexId(Long complexId) {
        this.complexId = complexId;
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

    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
    }

    public Long getUserVoucherId() {
        return userVoucherId;
    }

    public void setUserVoucherId(Long userVoucherId) {
        this.userVoucherId = userVoucherId;
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

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
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

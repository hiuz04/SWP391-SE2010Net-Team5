package com.swp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CheckoutView {
    private Long bookingId;
    private String bookingCode;
    private Long customerId;
    private Long facilityId;
    private Long fieldId;
    private String status;
    private String customerName;
    private String customerPhone;
    private String facilityName;
    private String facilityAddress;
    private String fieldName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal fieldFee = BigDecimal.ZERO;
    private BigDecimal depositAmount = BigDecimal.ZERO;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getFacilityAddress() {
        return facilityAddress;
    }

    public void setFacilityAddress(String facilityAddress) {
        this.facilityAddress = facilityAddress;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
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

    public BigDecimal getFieldFee() {
        return fieldFee;
    }

    public void setFieldFee(BigDecimal fieldFee) {
        this.fieldFee = fieldFee != null ? fieldFee : BigDecimal.ZERO;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount != null ? depositAmount : BigDecimal.ZERO;
    }

    public BigDecimal getBaseRemainingAmount() {
        BigDecimal remaining = fieldFee.subtract(depositAmount);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }
}

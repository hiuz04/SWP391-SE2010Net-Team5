package com.swp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO preview checkout cho Staff/Owner, gồm tiền sân, phụ phí quá giờ, tiền cọc và số còn phải thanh toán.
 */
public class CheckoutView {
    private Long bookingId;
    private String bookingCode;
    private Long customerId;
    private Long complexId;
    private Long fieldId;
    private String status;
    private String customerName;
    private String customerPhone;
    private String complexName;
    private String complexAddress;
    private String fieldName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkoutTime;
    private BigDecimal fieldFee = BigDecimal.ZERO;
    private BigDecimal depositAmount = BigDecimal.ZERO;
    private long overtimeMinutes;
    private BigDecimal overtimeFeePerMinute = BigDecimal.ZERO;
    private BigDecimal overtimeFee = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    private BigDecimal paidAmountBeforeCheckout = BigDecimal.ZERO;
    private boolean checkoutAllowed;
    private String checkoutBlockedReason;
    private Long existingInvoiceId;
    private String existingInvoiceStatus;
    private Long pendingPaymentRequestId;
    private String pendingPaymentRequestStatus;

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

    public LocalDateTime getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(LocalDateTime checkoutTime) {
        this.checkoutTime = checkoutTime;
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

    public long getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public void setOvertimeMinutes(long overtimeMinutes) {
        this.overtimeMinutes = Math.max(0, overtimeMinutes);
    }

    public BigDecimal getOvertimeFeePerMinute() {
        return overtimeFeePerMinute;
    }

    public void setOvertimeFeePerMinute(BigDecimal overtimeFeePerMinute) {
        this.overtimeFeePerMinute = overtimeFeePerMinute != null ? overtimeFeePerMinute : BigDecimal.ZERO;
    }

    public BigDecimal getOvertimeFee() {
        return overtimeFee;
    }

    public void setOvertimeFee(BigDecimal overtimeFee) {
        this.overtimeFee = overtimeFee != null ? overtimeFee : BigDecimal.ZERO;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount != null ? finalAmount : BigDecimal.ZERO;
    }

    public BigDecimal getPaidAmountBeforeCheckout() {
        return paidAmountBeforeCheckout;
    }

    public void setPaidAmountBeforeCheckout(BigDecimal paidAmountBeforeCheckout) {
        this.paidAmountBeforeCheckout = paidAmountBeforeCheckout != null ? paidAmountBeforeCheckout : BigDecimal.ZERO;
    }

    public boolean isCheckoutAllowed() {
        return checkoutAllowed;
    }

    public void setCheckoutAllowed(boolean checkoutAllowed) {
        this.checkoutAllowed = checkoutAllowed;
    }

    public String getCheckoutBlockedReason() {
        return checkoutBlockedReason;
    }

    public void setCheckoutBlockedReason(String checkoutBlockedReason) {
        this.checkoutBlockedReason = checkoutBlockedReason;
    }

    public Long getExistingInvoiceId() {
        return existingInvoiceId;
    }

    public void setExistingInvoiceId(Long existingInvoiceId) {
        this.existingInvoiceId = existingInvoiceId;
    }

    public String getExistingInvoiceStatus() {
        return existingInvoiceStatus;
    }

    public void setExistingInvoiceStatus(String existingInvoiceStatus) {
        this.existingInvoiceStatus = existingInvoiceStatus;
    }

    public Long getPendingPaymentRequestId() {
        return pendingPaymentRequestId;
    }

    public void setPendingPaymentRequestId(Long pendingPaymentRequestId) {
        this.pendingPaymentRequestId = pendingPaymentRequestId;
    }

    public String getPendingPaymentRequestStatus() {
        return pendingPaymentRequestStatus;
    }

    public void setPendingPaymentRequestStatus(String pendingPaymentRequestStatus) {
        this.pendingPaymentRequestStatus = pendingPaymentRequestStatus;
    }

    /**
     * Tính phần còn lại sau khi trừ cọc nhưng chưa cộng phụ phí quá giờ.
     */
    public BigDecimal getBaseRemainingAmount() {
        BigDecimal remaining = fieldFee.subtract(depositAmount);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }
}

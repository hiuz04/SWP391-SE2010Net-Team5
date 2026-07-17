package com.swp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserVoucherDTO {

    private long userVoucherId;
    private long voucherId;
    private String voucherCode;
    private String voucherName;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrder;
    private int exchangePoints;
    private LocalDateTime receivedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime usedAt;
    private String effectiveStatus;

    public UserVoucherDTO() {
    }

    public UserVoucherDTO(long userVoucherId, long voucherId, String voucherCode, String voucherName, String discountType, BigDecimal discountValue, BigDecimal minOrder, int exchangePoints, LocalDateTime receivedAt, LocalDateTime expiredAt, LocalDateTime usedAt, String effectiveStatus) {
        this.userVoucherId = userVoucherId;
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.voucherName = voucherName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrder = minOrder;
        this.exchangePoints = exchangePoints;
        this.receivedAt = receivedAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
        this.effectiveStatus = effectiveStatus;
    }

    public long getUserVoucherId() {
        return userVoucherId;
    }

    public void setUserVoucherId(long userVoucherId) {
        this.userVoucherId = userVoucherId;
    }

    public long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getVoucherName() {
        return voucherName;
    }

    public void setVoucherName(String voucherName) {
        this.voucherName = voucherName;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrder() {
        return minOrder;
    }

    public void setMinOrder(BigDecimal minOrder) {
        this.minOrder = minOrder;
    }

    public int getExchangePoints() {
        return exchangePoints;
    }

    public void setExchangePoints(int exchangePoints) {
        this.exchangePoints = exchangePoints;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public String getEffectiveStatus() {
        return effectiveStatus;
    }

    public void setEffectiveStatus(String effectiveStatus) {
        this.effectiveStatus = effectiveStatus;
    }
}

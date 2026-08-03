package com.swp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VoucherExchangeDTO {

    private long id;
    private String code;
    private String name;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrder;
    private int quantity;
    private int used;
    private int exchangePoints;
    private LocalDateTime endDate;
    private String targetUser;
    private String distributionType;

    public VoucherExchangeDTO() {
    }

    public VoucherExchangeDTO(long id, String code, String name, String discountType, BigDecimal discountValue, BigDecimal minOrder, int quantity, int used, int exchangePoints, LocalDateTime endDate, String targetUser) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrder = minOrder;
        this.quantity = quantity;
        this.used = used;
        this.exchangePoints = exchangePoints;
        this.endDate = endDate;
        this.targetUser = targetUser;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUsed() {
        return used;
    }

    public void setUsed(int used) {
        this.used = used;
    }

    public int getExchangePoints() {
        return exchangePoints;
    }

    public void setExchangePoints(int exchangePoints) {
        this.exchangePoints = exchangePoints;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(String distributionType) {
        this.distributionType = distributionType;
    }
}

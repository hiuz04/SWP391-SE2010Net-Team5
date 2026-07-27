package com.swp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity voucher lưu cấu hình giảm giá, thời gian hiệu lực, số lượng phát hành,
 * loại phát hành và trạng thái bật/tắt do Owner quản lý.
 *
 * Cột used được giữ để tương thích dữ liệu cũ:
 * - PUBLIC_CODE: số lượt đã dùng thành công sau payment.
 * - REWARD_VOUCHER: số lượt đã được khách hàng đổi/claim.
 */
public class Voucher implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DISTRIBUTION_PUBLIC_CODE = "PUBLIC_CODE";
    public static final String DISTRIBUTION_REWARD_VOUCHER = "REWARD_VOUCHER";
    public static final String TARGET_ALL = "ALL";
    public static final String TARGET_MEMBER = "MEMBER";

    private int id;
    private String code;
    private String name;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrder;
    private int quantity;
    private int used;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private String distributionType;
    private int exchangePoint;
    private String targetUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Voucher() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(String distributionType) {
        this.distributionType = distributionType;
    }

    public int getExchangePoint() {
        return exchangePoint;
    }

    public void setExchangePoint(int exchangePoint) {
        this.exchangePoint = exchangePoint;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
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

    public boolean isPublicCode() {
        return DISTRIBUTION_PUBLIC_CODE.equalsIgnoreCase(distributionType);
    }

    public boolean isRewardVoucher() {
        return DISTRIBUTION_REWARD_VOUCHER.equalsIgnoreCase(distributionType);
    }
}

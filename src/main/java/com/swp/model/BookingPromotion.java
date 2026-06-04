package com.swp.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class BookingPromotion implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long bookingId;
    private Long promotionId;
    private BigDecimal discountAmount;

    public BookingPromotion() {
    }

    public BookingPromotion(Long bookingId, Long promotionId, BigDecimal discountAmount) {
        this.bookingId = bookingId;
        this.promotionId = promotionId;
        this.discountAmount = discountAmount;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}
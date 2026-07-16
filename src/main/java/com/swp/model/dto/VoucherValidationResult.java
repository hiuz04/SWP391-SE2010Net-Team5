package com.swp.model.dto;

import com.swp.model.Voucher;

import java.math.BigDecimal;

public class VoucherValidationResult {
    private boolean valid;
    private String message;
    private Voucher voucher;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public VoucherValidationResult() {
    }

    public static VoucherValidationResult invalid(String message) {
        VoucherValidationResult result = new VoucherValidationResult();
        result.setValid(false);
        result.setMessage(message);
        result.setDiscountAmount(BigDecimal.ZERO);
        result.setFinalAmount(BigDecimal.ZERO);
        return result;
    }

    public static VoucherValidationResult valid(
            Voucher voucher,
            BigDecimal discountAmount,
            BigDecimal finalAmount
    ) {
        VoucherValidationResult result = new VoucherValidationResult();
        result.setValid(true);
        result.setMessage("Mã giảm giá hợp lệ.");
        result.setVoucher(voucher);
        result.setDiscountAmount(discountAmount);
        result.setFinalAmount(finalAmount);
        return result;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }
}

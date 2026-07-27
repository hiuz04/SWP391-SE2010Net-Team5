package com.swp.model.dto;

public class VoucherRedeemResult {

    private final boolean success;
    private final String message;

    private VoucherRedeemResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static VoucherRedeemResult success(String message) {
        return new VoucherRedeemResult(true, message);
    }

    public static VoucherRedeemResult failure(String message) {
        return new VoucherRedeemResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}

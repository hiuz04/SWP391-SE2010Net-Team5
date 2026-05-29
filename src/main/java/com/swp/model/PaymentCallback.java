package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PaymentCallback implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long callbackId;
    private Long paymentId;
    private String gatewayCode;
    private String rawPayload;
    private String signature;
    private Boolean valid;
    private LocalDateTime receivedAt;

    public PaymentCallback() {
    }

    public PaymentCallback(Long callbackId, Long paymentId, String gatewayCode, String rawPayload, String signature, Boolean valid, LocalDateTime receivedAt) {
        this.callbackId = callbackId;
        this.paymentId = paymentId;
        this.gatewayCode = gatewayCode;
        this.rawPayload = rawPayload;
        this.signature = signature;
        this.valid = valid;
        this.receivedAt = receivedAt;
    }

    public Long getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(Long callbackId) {
        this.callbackId = callbackId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}

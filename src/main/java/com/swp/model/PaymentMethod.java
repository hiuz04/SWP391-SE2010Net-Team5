package com.swp.model;

import java.io.Serializable;

/**
 * Entity phương thức thanh toán đang được hệ thống bật/tắt, ví dụ VNPay hoặc phương thức mô phỏng.
 */
public class PaymentMethod implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer paymentMethodId;
    private String methodCode;
    private String methodName;
    private String status;

    public PaymentMethod() {
    }

    public PaymentMethod(Integer paymentMethodId, String methodCode, String methodName, String status) {
        this.paymentMethodId = paymentMethodId;
        this.methodCode = methodCode;
        this.methodName = methodName;
        this.status = status;
    }

    public Integer getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getMethodCode() {
        return methodCode;
    }

    public void setMethodCode(String methodCode) {
        this.methodCode = methodCode;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

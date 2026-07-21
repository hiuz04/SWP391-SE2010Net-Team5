package com.swp.model.dto;

/**
 * DTO kết quả khi Staff/Owner tạo hoặc gửi lại invoice checkout cho Customer.
 */
public class CheckoutResult {
    private final long invoiceId;
    private final long bookingId;
    private final String invoiceCode;
    private final String message;

    public CheckoutResult(long invoiceId, long bookingId, String invoiceCode) {
        this(invoiceId, bookingId, invoiceCode, "Da gui yeu cau thanh toan cho khach.");
    }

    public CheckoutResult(long invoiceId, long bookingId, String invoiceCode, String message) {
        this.invoiceId = invoiceId;
        this.bookingId = bookingId;
        this.invoiceCode = invoiceCode;
        this.message = message;
    }

    public long getInvoiceId() {
        return invoiceId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public String getMessage() {
        return message;
    }
}

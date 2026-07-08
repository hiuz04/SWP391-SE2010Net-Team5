package com.swp.model.dto;

public class CheckoutResult {
    private final long invoiceId;
    private final long bookingId;
    private final String invoiceCode;

    public CheckoutResult(long invoiceId, long bookingId, String invoiceCode) {
        this.invoiceId = invoiceId;
        this.bookingId = bookingId;
        this.invoiceCode = invoiceCode;
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
}

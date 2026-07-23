package com.swp.model.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingSlotPreview implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private boolean available;
    private String status;
    private String reason;

    public BookingSlotPreview() {
    }

    public BookingSlotPreview(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal price,
            boolean available,
            String status,
            String reason
    ) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.available = available;
        this.status = status;
        this.reason = reason;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

package com.swp.model.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public class SkippedBookingSlot implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;

    public SkippedBookingSlot() {
    }

    public SkippedBookingSlot(LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

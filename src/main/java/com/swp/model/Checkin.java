package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Checkin implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long checkinId;
    private Long bookingId;
    private Long staffId;
    private LocalDateTime checkinTime;
    private String note;

    public Checkin() {
    }

    public Checkin(Long checkinId, Long bookingId, Long staffId, LocalDateTime checkinTime, String note) {
        this.checkinId = checkinId;
        this.bookingId = bookingId;
        this.staffId = staffId;
        this.checkinTime = checkinTime;
        this.note = note;
    }

    public Long getCheckinId() {
        return checkinId;
    }

    public void setCheckinId(Long checkinId) {
        this.checkinId = checkinId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime) {
        this.checkinTime = checkinTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

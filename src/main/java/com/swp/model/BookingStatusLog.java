package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity audit log cho các lần đổi trạng thái booking như HOLD, CONFIRMED, CANCELLED,
 * PENDING_CHECKOUT_PAYMENT và COMPLETED.
 */
public class BookingStatusLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long logId;
    private Long bookingId;
    private String oldStatus;
    private String newStatus;
    private Long changedBy;
    private String note;
    private LocalDateTime createdAt;

    public BookingStatusLog() {
    }

    public BookingStatusLog(Long logId, Long bookingId, String oldStatus, String newStatus, Long changedBy, String note, LocalDateTime createdAt) {
        this.logId = logId;
        this.bookingId = bookingId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

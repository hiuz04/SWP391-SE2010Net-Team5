package com.swp.model.dto;

import java.time.LocalDateTime;

/**
 * DTO biểu diễn một ô 30 phút trên lịch sân, dùng cho màn hình chọn khung giờ đặt sân.
 */
public class FieldScheduleSlot {
    private Long fieldId;
    private String fieldName;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private String status;
    private String title;

    public FieldScheduleSlot() {
    }

    public FieldScheduleSlot(Long fieldId, String fieldName, LocalDateTime slotStart,
                             LocalDateTime slotEnd, String status, String title) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.status = status;
        this.title = title;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public LocalDateTime getSlotStart() {
        return slotStart;
    }

    public void setSlotStart(LocalDateTime slotStart) {
        this.slotStart = slotStart;
    }

    public LocalDateTime getSlotEnd() {
        return slotEnd;
    }

    public void setSlotEnd(LocalDateTime slotEnd) {
        this.slotEnd = slotEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Chuyển trạng thái nghiệp vụ của slot sang CSS class để JSP không phải lặp lại mapping hiển thị.
     */
    public String getCssClass() {
        if (status == null) {
            return "slot-disabled";
        }

        switch (status) {
            case "AVAILABLE":
                return "slot-available";
            case "BOOKED":
                return "slot-booked";
            case "MAINTENANCE":
                return "slot-maintenance";
            case "DISABLED":
                return "slot-disabled";
            default:
                return "slot-disabled";
        }
    }
}

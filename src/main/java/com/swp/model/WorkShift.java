package com.swp.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class WorkShift implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long shiftId;
    private Long facilityId;
    private String shiftName;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;

    public WorkShift() {
    }

    public WorkShift(Long shiftId, Long facilityId, String shiftName, LocalDate shiftDate, LocalTime startTime, LocalTime endTime, LocalDateTime createdAt) {
        this.shiftId = shiftId;
        this.facilityId = facilityId;
        this.shiftName = shiftName;
        this.shiftDate = shiftDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

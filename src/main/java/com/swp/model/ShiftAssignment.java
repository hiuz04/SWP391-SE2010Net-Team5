package com.swp.model;

import java.io.Serializable;

public class ShiftAssignment implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long assignmentId;
    private Long shiftId;
    private Long staffId;
    private String status;

    public ShiftAssignment() {
    }

    public ShiftAssignment(Long assignmentId, Long shiftId, Long staffId, String status) {
        this.assignmentId = assignmentId;
        this.shiftId = shiftId;
        this.staffId = staffId;
        this.status = status;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

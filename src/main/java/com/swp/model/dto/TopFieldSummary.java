/*
 * Author: Tran Bao Long
 * 5/6/2026
 */
package com.swp.model.dto;

/**
 * DTO chứa thông tin tóm tắt của sân nổi bật dùng để hiển thị trên trang chủ.
 */
public class TopFieldSummary {

    private long fieldId;
    private String fieldName;
    private String description;
    private String status;
    private String fieldTypeName;

    private long facilityId;
    private String facilityName;
    private String address;
    private String district;
    private String city;

    private int bookingCount;

    public TopFieldSummary() {
    }

    public TopFieldSummary(long fieldId, String fieldName, String description, String status,
                           String fieldTypeName, long facilityId, String facilityName,
                           String address, String district, String city, int bookingCount) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.description = description;
        this.status = status;
        this.fieldTypeName = fieldTypeName;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.address = address;
        this.district = district;
        this.city = city;
        this.bookingCount = bookingCount;
    }

    public long getFieldId() { return fieldId; }
    public void setFieldId(long fieldId) { this.fieldId = fieldId; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFieldTypeName() { return fieldTypeName; }
    public void setFieldTypeName(String fieldTypeName) { this.fieldTypeName = fieldTypeName; }

    public long getFacilityId() { return facilityId; }
    public void setFacilityId(long facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getBookingCount() { return bookingCount; }
    public void setBookingCount(int bookingCount) { this.bookingCount = bookingCount; }

    /** Trả về địa chỉ đầy đủ gồm district + city */
    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        if (district != null && !district.isBlank()) sb.append(district);
        if (city != null && !city.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        return sb.toString();
    }
}

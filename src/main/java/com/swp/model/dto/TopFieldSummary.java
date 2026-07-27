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
    private boolean isHot;

    private long complexId;
    private String complexName;
    private String address;
    private String city;

    private int bookingCount;
    private String imageUrl;
    private java.math.BigDecimal currentPrice;

    public TopFieldSummary() {
    }

    public TopFieldSummary(long fieldId, String fieldName, String description, String status,
                           String fieldTypeName, boolean isHot, long complexId, String complexName,
                           String address, String city, int bookingCount, String imageUrl,
                           java.math.BigDecimal currentPrice) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.description = description;
        this.status = status;
        this.fieldTypeName = fieldTypeName;
        this.isHot = isHot;
        this.complexId = complexId;
        this.complexName = complexName;
        this.address = address;
        this.city = city;
        this.bookingCount = bookingCount;
        this.imageUrl = imageUrl;
        this.currentPrice = currentPrice;
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

    public boolean isHot() { return isHot; }
    public void setHot(boolean isHot) { this.isHot = isHot; }

    public long getComplexId() { return complexId; }
    public void setComplexId(long complexId) { this.complexId = complexId; }

    public String getComplexName() { return complexName; }
    public void setComplexName(String complexName) { this.complexName = complexName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getBookingCount() { return bookingCount; }
    public void setBookingCount(int bookingCount) { this.bookingCount = bookingCount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public java.math.BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(java.math.BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    /** Trả về địa chỉ đầy đủ gồm address + city */
    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isBlank()) sb.append(address);
        if (city != null && !city.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        return sb.toString();
    }
}

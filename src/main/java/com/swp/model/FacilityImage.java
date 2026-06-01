package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class FacilityImage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long imageId;
    private Long facilityId;
    private String imageUrl;
    private Boolean thumbnail;
    private LocalDateTime createdAt;

    public FacilityImage() {
    }

    public FacilityImage(Long imageId, Long facilityId, String imageUrl, Boolean thumbnail, LocalDateTime createdAt) {
        this.imageId = imageId;
        this.facilityId = facilityId;
        this.imageUrl = imageUrl;
        this.thumbnail = thumbnail;
        this.createdAt = createdAt;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
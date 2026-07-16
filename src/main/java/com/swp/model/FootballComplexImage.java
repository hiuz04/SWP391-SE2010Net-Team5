package com.swp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class FootballComplexImage implements Serializable {
    //    private static final long serialVersionUID = 1L;
    private Long imageId;
    private Long complexId;
    private String imageUrl;
    private Boolean thumbnail;
    private String publicId;
    private LocalDateTime createdAt;

    public FootballComplexImage() {
    }

    public FootballComplexImage(Long imageId, Long complexId, String imageUrl, Boolean thumbnail, String publicId, LocalDateTime createdAt) {
        this.imageId = imageId;
        this.complexId = complexId;
        this.imageUrl = imageUrl;
        this.thumbnail = thumbnail;
        this.publicId = publicId;
        this.createdAt = createdAt;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getComplexId() {
        return complexId;
    }

    public void setComplexId(Long complexId) {
        this.complexId = complexId;
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

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
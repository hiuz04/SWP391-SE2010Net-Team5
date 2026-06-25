package com.swp.model.dto;

public class CloudinaryResponse {

    private String imgUrl;
    private String publicId;

    public CloudinaryResponse() {
    }

    public CloudinaryResponse(String imgUrl, String publicId) {
        this.imgUrl = imgUrl;
        this.publicId = publicId;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }
}

package com.swp.model.dto;

import com.swp.model.FieldType;

import java.time.LocalTime;
import java.util.List;

public class ComplexCard {
    private long complexId;
    private String thumbnail;
    private String complexName;
    private String address;
    private String city;
    private String ward;
    private List<FieldType> fieldTypeList;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String thumbnailUrl;

    public ComplexCard() {
    }

    public ComplexCard(long complexId, String thumbnail, String complexName, String address, String city, String ward, List<FieldType> fieldTypeList, LocalTime openingTime, LocalTime closingTime, String thumbnailUrl) {
        this.complexId = complexId;
        this.thumbnail = thumbnail;
        this.complexName = complexName;
        this.address = address;
        this.city = city;
        this.ward = ward;
        this.fieldTypeList = fieldTypeList;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.thumbnailUrl = thumbnailUrl;
    }

    public long getComplexId() {
        return complexId;
    }

    public void setComplexId(long complexId) {
        this.complexId = complexId;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getComplexName() {
        return complexName;
    }

    public void setComplexName(String complexName) {
        this.complexName = complexName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public List<FieldType> getFieldTypeList() {
        return fieldTypeList;
    }

    public void setFieldTypeList(List<FieldType> fieldTypeList) {
        this.fieldTypeList = fieldTypeList;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}

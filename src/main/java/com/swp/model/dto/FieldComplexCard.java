package com.swp.model.dto;

import com.swp.model.FieldType;

import java.time.LocalTime;
import java.util.List;

public class FieldComplexCard {
    private long facilityId;
    private String thumbnail;
    private String facilityName;
    private String address;
    private String city;
    private String ward;
    private List<FieldType> fieldTypeList;
    private LocalTime openingTime;
    private LocalTime closingTime;

    public FieldComplexCard() {
    }

    public FieldComplexCard(long facilityId, String thumbnail, String facilityName, String address, String city, String ward, List<FieldType> fieldTypeList, LocalTime openingTime, LocalTime closingTime) {
        this.facilityId = facilityId;
        this.thumbnail = thumbnail;
        this.facilityName = facilityName;
        this.address = address;
        this.city = city;
        this.ward = ward;
        this.fieldTypeList = fieldTypeList;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }

    public long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(long facilityId) {
        this.facilityId = facilityId;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
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
}

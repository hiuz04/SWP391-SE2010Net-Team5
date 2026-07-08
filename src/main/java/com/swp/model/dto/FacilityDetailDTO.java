package com.swp.model.dto;

import com.swp.model.Facility;
import com.swp.model.FacilityImage;

import java.util.List;

public class FacilityDetailDTO {

    private Facility facility;
    private List<FacilityImage> img;

    public FacilityDetailDTO() {
    }

    public FacilityDetailDTO(Facility facility, List<FacilityImage> img) {
        this.facility = facility;
        this.img = img;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public List<FacilityImage> getImg() {
        return img;
    }

    public void setImg(List<FacilityImage> img) {
        this.img = img;
    }
}

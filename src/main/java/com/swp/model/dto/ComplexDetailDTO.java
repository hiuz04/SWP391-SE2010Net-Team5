package com.swp.model.dto;

import com.swp.model.FootballComplex;
import com.swp.model.FootballComplexImage;

import java.util.List;

public class ComplexDetailDTO {

    private FootballComplex complex;
    private List<FootballComplexImage> img;

    public ComplexDetailDTO() {
    }

    public ComplexDetailDTO(FootballComplex complex, List<FootballComplexImage> img) {
        this.complex = complex;
        this.img = img;
    }

    public FootballComplex getComplex() {
        return complex;
    }

    public void setComplex(FootballComplex complex) {
        this.complex = complex;
    }

    public List<FootballComplexImage> getImg() {
        return img;
    }

    public void setImg(List<FootballComplexImage> img) {
        this.img = img;
    }
}

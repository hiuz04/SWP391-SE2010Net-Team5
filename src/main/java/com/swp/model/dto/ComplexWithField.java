package com.swp.model.dto;

import com.swp.model.Field;
import com.swp.model.FootballComplex;

import java.util.List;

public class ComplexWithField {
    private FootballComplex complex;
    private List<Field> fields;

    public ComplexWithField(FootballComplex complex, List<Field> fields) {
        this.complex = complex;
        this.fields = fields;
    }

    public List<Field> getFields() {
        return fields;
    }

    public FootballComplex getComplex() {
        return complex;
    }
}

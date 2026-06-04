package com.swp.model.dto;

import com.swp.model.Facility;
import com.swp.model.Field;

import java.util.List;

public class FacilityWithField {
    private Facility facility;
    private List<Field> fields;

    public FacilityWithField(Facility facility, List<Field> fields) {
        this.facility = facility;
        this.fields = fields;
    }

    public List<Field> getFields() {
        return fields;
    }

    public Facility getFacility() {
        return facility;
    }
}

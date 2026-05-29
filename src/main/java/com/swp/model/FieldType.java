package com.swp.model;

import java.io.Serializable;

public class FieldType implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer fieldTypeId;
    private String typeName;
    private Integer numberOfPlayers;
    private String description;

    public FieldType() {
    }

    public FieldType(Integer fieldTypeId, String typeName, Integer numberOfPlayers, String description) {
        this.fieldTypeId = fieldTypeId;
        this.typeName = typeName;
        this.numberOfPlayers = numberOfPlayers;
        this.description = description;
    }

    public Integer getFieldTypeId() {
        return fieldTypeId;
    }

    public void setFieldTypeId(Integer fieldTypeId) {
        this.fieldTypeId = fieldTypeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public void setNumberOfPlayers(Integer numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
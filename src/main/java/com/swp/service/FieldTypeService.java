package com.swp.service;

import com.swp.dao.FieldTypeDAO;
import com.swp.model.FieldType;

import java.util.List;

public class FieldTypeService {

    private static final FieldTypeDAO fieldTypeDao = new FieldTypeDAO();

    public List<FieldType> getAllType() {
        return fieldTypeDao.getAllFieldTypes();
    }

}

package com.swp.controller.owner;

import com.swp.dao.FacilityDAO;
import com.swp.dao.FieldDAO;
import com.swp.dao.FieldTypeDAO;

public class Constant {
    protected static final String OWNER_ROLE_NAME = "OWNER";
    protected static final FieldDAO fieldDAO = new FieldDAO();
    protected static final FacilityDAO facilityDAO = new FacilityDAO();
    protected static final FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();
}

/**
 * Module: Field Management
 * File: FieldAPIController.java
 * Description: Xử lý chức năng lấy toàn bộ thông tin sân bóng để hiển thị dữ liệu trên front-end.
 *
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 */
package com.swp.controller.owner;

import com.google.gson.Gson;
import com.swp.model.Facility;
import com.swp.model.Field;
import com.swp.model.FieldType;
import com.swp.model.dto.FieldList;
import com.swp.service.owner.FacilityService;
import com.swp.service.owner.FieldService;
import com.swp.service.owner.FieldTypeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@WebServlet("/api/fields")
public class FieldAPIController extends HttpServlet {

    private static final FieldService fieldService = new FieldService();
    private static final FacilityService facilityService = new FacilityService();
    private static final FieldTypeService fieldTypeService = new FieldTypeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        List<Field> fields = fieldService.getAllField();

        List<Facility> facilities = facilityService.getListFacility();
        Map<Long, Facility> facilityMap = facilities.stream()
                .collect(Collectors.toMap(Facility::getFacilityId, Function.identity()));

        List<FieldType> fieldTypes = fieldTypeService.getAllType();
        Map<Integer, FieldType> fieldTypeMap = fieldTypes.stream()
                .collect(Collectors.toMap(FieldType::getFieldTypeId, Function.identity()));
        List<FieldList> lists = new ArrayList<>();

        for(Field f : fields) {
            Facility fac = facilityMap.get(f.getFacilityId());
            FieldType fT = fieldTypeMap.get(f.getFieldTypeId());

            lists.add(new FieldList(
                    f.getFieldId(),
                    f.getFieldName(),
                    fT.getTypeName(),
                    fac.getFacilityName(),
                    f.getDescription(),
                    f.getStatus()
            ));
        }

        resp.setContentType("application/json;charset=UTF-8");

        Gson gson = new Gson();

        resp.getWriter().write(gson.toJson(lists));
    }
}

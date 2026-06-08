package com.swp.controller.customer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.dao.FacilityDAO;
import com.swp.dao.FieldDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.model.Facility;
import com.swp.model.Field;
import com.swp.model.FieldType;
import com.swp.model.dto.FieldDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@WebServlet("/field")
public class GetFieldDetails extends HttpServlet {

    private static final FacilityDAO facilityDao = new FacilityDAO();
    private static final FieldDAO fieldDAO = new FieldDAO();
    private static final FieldTypeDAO fieldTypeDao = new FieldTypeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long facilityId = Long.parseLong(req.getParameter("id"));
        Facility facility = facilityDao.getFacilityDataByID(facilityId);
        List<Field> fields = fieldDAO.getFieldBelongToThisFacilityId(facilityId);
        List<FieldType> fieldTypes = fieldTypeDao.getAllFieldTypes();
        FieldDetail detail = new FieldDetail();

        Map<Integer, FieldType> fieldTypeMap = fieldTypes.stream()
                .collect(Collectors.toMap(
                        FieldType::getFieldTypeId,
                        Function.identity()
                ));
        List<FieldType> typeOfFac = fields.stream()
                .map(Field::getFieldTypeId)
                .distinct()
                .map(fieldTypeMap::get)
                .filter(Objects::nonNull)
                .toList();

        detail.setFacilityId(facilityId);
        detail.setFacilityName(facility.getFacilityName());
        detail.setComplexAddress(String.join(", ",
                facility.getAddress(),
                facility.getWard(),
                facility.getDistrict(),
                facility.getCity()
        ));
        detail.setDescription(facility.getDescription());
        detail.setFieldTypeList(typeOfFac);
        detail.setOpeningTime(facility.getOpeningTime());
        detail.setClosingTime(facility.getClosingTime());
        detail.setFields(fields);
        detail.setHotline(facility.getHotline());

        resp.setContentType("application/json;charset=UTF-8");
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString()))
                .create();

        resp.getWriter().write(gson.toJson(detail));
    }
}

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
import com.swp.model.dto.FieldComplexCard;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@WebServlet("/field-list")
public class GetFieldList extends HttpServlet {

    private static final FacilityDAO facilityDao = new FacilityDAO();
    private static final FieldDAO fieldDAO = new FieldDAO();
    private static final FieldTypeDAO fieldTypeDao = new FieldTypeDAO();

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {

//        String keyword = req.getParameter("keyword");
        String province = req.getParameter("province");
        String ward = req.getParameter("ward");
        String fieldTypeId = req.getParameter("fieldTypeId");

        List<Facility> facilities = facilityDao.getAllFacility();
        List<Field> fields = fieldDAO.getAllField();
        List<FieldType> fieldTypes = fieldTypeDao.getAllFieldTypes();

        List<FieldComplexCard> lists = new ArrayList<>();

        Map<Integer, FieldType> fieldTypeMap = fieldTypes.stream()
                .collect(Collectors.toMap(
                        FieldType::getFieldTypeId,
                        Function.identity()
                ));

        for (Facility fac : facilities) {

            List<FieldType> typeOfFac = fields.stream()
                    .filter(f -> f.getFacilityId() == fac.getFacilityId())
                    .map(Field::getFieldTypeId)
                    .distinct()
                    .map(fieldTypeMap::get)
                    .filter(Objects::nonNull)
                    .toList();

//            // Search theo keyword
//            if (keyword != null && !keyword.isBlank()) {
//
//                String kw = keyword.trim().toLowerCase();
//
//                boolean match =
//                        fac.getFacilityName().toLowerCase().contains(kw)
//                                || fac.getAddress().toLowerCase().contains(kw);
//
//                if (!match) {
//                    continue;
//                }
//            }

            // Search theo province
            if (province != null
                    && !province.isBlank()
                    && !province.equalsIgnoreCase(fac.getCity())) {
                continue;
            }

            // Search theo ward
            if (ward != null
                    && !ward.isBlank()
                    && !ward.equalsIgnoreCase(fac.getWard())) {
                continue;
            }

            // Search theo loại sân
            if (fieldTypeId != null && !fieldTypeId.isBlank()) {

                int typeId = Integer.parseInt(fieldTypeId);

                boolean hasType = typeOfFac.stream()
                        .anyMatch(t -> t.getFieldTypeId() == typeId);

                if (!hasType) {
                    continue;
                }
            }

            FieldComplexCard card = new FieldComplexCard();

            card.setFacilityId(fac.getFacilityId());
            card.setFacilityName(fac.getFacilityName());
            card.setAddress(fac.getAddress());
            card.setCity(fac.getCity());
            card.setWard(fac.getWard());
            card.setFieldTypeList(typeOfFac);
            card.setOpeningTime(fac.getOpeningTime());
            card.setClosingTime(fac.getClosingTime());

            lists.add(card);
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString())
                )
                .create();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write(gson.toJson(lists));
    }
}

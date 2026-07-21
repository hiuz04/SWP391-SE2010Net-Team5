package com.swp.controller.customer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.FieldDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.model.*;
import com.swp.model.dto.ComplexCard;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@WebServlet("/field-list")
public class GetFieldList extends HttpServlet {

    private static final FootballComplexDAO FOOTBALL_COMPLEX_DAO = new FootballComplexDAO();
    private static final FieldDAO fieldDAO = new FieldDAO();
    private static final FieldTypeDAO fieldTypeDao = new FieldTypeDAO();

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String province = req.getParameter("province");
            String ward = req.getParameter("ward");
            String fieldTypeId = req.getParameter("fieldTypeId");
            String sortOrder = req.getParameter("sortOrder");

            List<FootballComplex> complexes = FOOTBALL_COMPLEX_DAO.getAllActiveComplex();
            List<Field> fields = fieldDAO.getAllField();
            List<FieldType> fieldTypes = fieldTypeDao.getAllFieldTypes();

            List<ComplexCard> lists = new ArrayList<>();

            Map<Integer, FieldType> fieldTypeMap = fieldTypes.stream()
                    .collect(Collectors.toMap(
                            FieldType::getFieldTypeId,
                            Function.identity()
                    ));

            for (FootballComplex fc : complexes) {
                try {
                    System.out.println("Đang xử lý Complex ID = " + fc.getComplexId());

                    FootballComplexImage thumbnail =
                            FOOTBALL_COMPLEX_DAO.getThumbnail(fc.getComplexId());

                    List<FieldType> typeOfFc = fields.stream()
                            .filter(f -> f.getComplexId() == fc.getComplexId())
                            .map(Field::getFieldTypeId)
                            .distinct()
                            .map(fieldTypeMap::get)
                            .filter(Objects::nonNull)
                            .toList();

                    // Filter province
                    if (province != null
                            && !province.isBlank()
                            && !province.equalsIgnoreCase(fc.getCity())) {
                        continue;
                    }

                    // Filter ward
                    if (ward != null
                            && !ward.isBlank()
                            && !ward.equalsIgnoreCase(fc.getWard())) {
                        continue;
                    }

                    // Filter field type
                    if (fieldTypeId != null && !fieldTypeId.isBlank()) {

                        int typeId = Integer.parseInt(fieldTypeId);

                        boolean hasType = typeOfFc.stream()
                                .anyMatch(t -> t.getFieldTypeId() == typeId);

                        if (!hasType) {
                            continue;
                        }
                    }

                    ComplexCard card = new ComplexCard();

                    card.setComplexId(fc.getComplexId());
                    card.setComplexName(fc.getComplexName());
                    card.setAddress(fc.getAddress());
                    card.setCity(fc.getCity());
                    card.setWard(fc.getWard());
                    card.setFieldTypeList(typeOfFc);
                    card.setOpeningTime(fc.getOpeningTime());
                    card.setClosingTime(fc.getClosingTime());

                    // Thumbnail
                    if (thumbnail != null) {
                        card.setThumbnailUrl(thumbnail.getImageUrl());
                    }

                    // Giá
                    System.out.println("Đang lấy giá cho Complex " + fc.getComplexId());

                    card.setCurrentPrice(
                            FOOTBALL_COMPLEX_DAO.getCurrentPriceForComplex(fc.getComplexId())
                    );

                    lists.add(card);

                } catch (Exception ex) {
                    System.err.println("======================================");
                    System.err.println("Lỗi tại Complex ID: " + fc.getComplexId());
                    System.err.println("Tên: " + fc.getComplexName());
                    ex.printStackTrace();
                    System.err.println("======================================");
                }
            }

            if (sortOrder != null && !sortOrder.isBlank()) {
                if ("price_asc".equals(sortOrder)) {
                    lists.sort((c1, c2) -> {
                        if (c1.getCurrentPrice() == null && c2.getCurrentPrice() == null) return 0;
                        if (c1.getCurrentPrice() == null) return 1;
                        if (c2.getCurrentPrice() == null) return -1;
                        return c1.getCurrentPrice().compareTo(c2.getCurrentPrice());
                    });
                } else if ("price_desc".equals(sortOrder)) {
                    lists.sort((c1, c2) -> {
                        if (c1.getCurrentPrice() == null && c2.getCurrentPrice() == null) return 0;
                        if (c1.getCurrentPrice() == null) return 1;
                        if (c2.getCurrentPrice() == null) return -1;
                        return c2.getCurrentPrice().compareTo(c1.getCurrentPrice());
                    });
                }
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

        } catch (Exception e) {
            System.err.println("========== API ERROR ==========");
            e.printStackTrace();
            System.err.println("===============================");

            resp.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }
}

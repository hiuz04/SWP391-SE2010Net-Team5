/**
 * Module: Facility Management
 * File: FacilityAPIController.java
 * Description: Lấy toàn bộ thông tin cơ sở để hiển thị dữ liệu lên front-end.
 *
 * Author: Dương Hải Anh
 * Version: 1.1
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 * Update Notes: Cập nhật hiển thị thêm số lượng sân của từng cơ sở.
 */
package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.dto.FacilityWithField;
import com.swp.model.Facility;
import com.swp.model.Field;
import com.swp.service.owner.FacilityService;
import com.swp.service.owner.FieldService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/facilities")
public class FacilityAPIController extends HttpServlet {

    private static final FieldService fieldService = new FieldService();
    private static final FacilityService facilityService = new FacilityService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        List<FacilityWithField> lists = new ArrayList<>();
        List<Field> fields = fieldService.getAllField();
        List<Facility> facilities = facilityService.getListFacility();

        for (Facility fac : facilities) {
            List<Field> facilityFields = fields.stream()
                    .filter(f -> fac.getFacilityId() == f.getFacilityId())
                    .toList();

            lists.add(new FacilityWithField(fac, facilityFields));
        }

        resp.setContentType("application/json;charset=UTF-8");

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString()))
                .create();

        resp.getWriter().write(gson.toJson(lists));
    }
}

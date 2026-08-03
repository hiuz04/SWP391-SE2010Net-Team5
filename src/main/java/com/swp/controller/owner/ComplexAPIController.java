/**
 * Module: Complex Management
 * File: ComplexAPIController.java
 * Description: Lấy toàn bộ thông tin cụm sân để hiển thị dữ liệu lên front-end.
 *
 * Author: Dương Hải Anh
 * Version: 1.1
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 * Update Notes: Cập nhật hiển thị thêm số lượng sân của từng cụm sân.
 */
package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.FootballComplex;
import com.swp.model.dto.ComplexWithField;
import com.swp.model.Field;
import com.swp.service.FootballComplexService;
import com.swp.service.FieldService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/complexes")
public class ComplexAPIController extends HttpServlet {

    private static final FieldService fieldService = new FieldService();
    private static final FootballComplexService FOOTBALL_COMPLEX_SERVICE = new FootballComplexService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String keyword = req.getParameter("keyword");
        String status = req.getParameter("status");

        List<ComplexWithField> lists = new ArrayList<>();
        List<Field> fields = fieldService.getAllField();
        List<FootballComplex> complexes = FOOTBALL_COMPLEX_SERVICE.searchComplex(keyword, status);

        for (FootballComplex fc : complexes) {
            List<Field> complexFields = fields.stream()
                    .filter(f -> fc.getComplexId() == f.getComplexId())
                    .toList();

            lists.add(new ComplexWithField(fc, complexFields));
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

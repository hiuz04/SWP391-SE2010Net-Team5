/**
 * Module: Field Management
 * File: FieldAPIController.java
 * Description: Xử lý chức năng lấy toàn bộ thông tin sân bóng để hiển thị dữ liệu trên front-end.
 * <p>
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 */
package com.swp.controller.owner;

import com.google.gson.Gson;
import com.swp.model.Field;
import com.swp.model.FieldType;
import com.swp.model.FootballComplex;
import com.swp.model.dto.FieldList;
import com.swp.service.FootballComplexService;
import com.swp.service.FieldService;
import com.swp.service.FieldTypeService;
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

@WebServlet("/owner/api/fields")
public class FieldAPIController extends HttpServlet {

    private static final FieldService fieldService = new FieldService();
    private static final FootballComplexService complexService = new FootballComplexService();
    private static final FieldTypeService fieldTypeService = new FieldTypeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {

            long complexId = Long.parseLong(req.getParameter("complexId"));

            String keyword = req.getParameter("keyword");

            String status = req.getParameter("status");

            String fieldType = req.getParameter("fieldTypeId");

            Long typeId = null;

            if (fieldType != null && !fieldType.isBlank()) {
                typeId = Long.parseLong(fieldType);
            }

            List<Field> fields = fieldService.searchField(
                    keyword,
                    status,
                    typeId,
                    complexId
            );
            FootballComplex complex = complexService.getFootballComplexInfo(complexId);
            List<FieldType> fieldTypes = fieldTypeService.getAllType();

            Map<Integer, FieldType> fieldTypeMap = fieldTypes.stream()
                    .collect(Collectors.toMap(FieldType::getFieldTypeId, Function.identity(), (a, b) -> a));

            List<FieldList> lists = new ArrayList<>();
            String complexName = complex != null ? complex.getComplexName() : "";

            for (Field f : fields) {
                FieldType ft = fieldTypeMap.get(f.getFieldTypeId());
                String typeName = ft != null ? ft.getTypeName() : "Khác";

                lists.add(new FieldList(
                        f.getFieldId(),
                        f.getFieldName(),
                        typeName,
                        complexName,
                        f.getDescription(),
                        f.getStatus(),
                        f.isHot()
                ));
            }

            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(new Gson().toJson(lists));

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "complexId không hợp lệ.");

        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi hệ thống.");
        }
    }
}

package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.Field;
import com.swp.model.FootballComplex;
import com.swp.service.FieldService;
import com.swp.service.FootballComplexService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/owner/field")
public class FieldController extends HttpServlet {

    private static final FieldService fieldService = new FieldService();
    private static final FootballComplexService complexService = new FootballComplexService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("get".equals(action)) {
            getInfo(req, resp);
            return;
        }

        String complexId = req.getParameter("complexId");

        if (complexId == null || complexId.isBlank()) {
            List<FootballComplex> complexes = complexService.getListFootballComplex();

            if (!complexes.isEmpty()) {
                resp.sendRedirect(
                        req.getContextPath()
                                + "/owner/field?complexId="
                                + complexes.get(0).getComplexId()
                );
                return;
            }
        }

        req.getRequestDispatcher("/WEB-INF/owner/field-list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        switch (action == null ? "" : action) {
            case "add":
                add(req, resp);
                break;
            case "edit":
                edit(req, resp);
                break;
            case "delete":
                delete(req, resp);
                break;
            case "status":
                changeStatus(req, resp);
                break;
        }
    }

    protected void add(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String fieldName = req.getParameter("fieldName");
            String fieldDesc = req.getParameter("description");
            int fieldTypeID = Integer.parseInt(req.getParameter("fieldTypeID"));
            long complexId = Long.parseLong(req.getParameter("complexId"));

            // Kiểm tra trùng tên trong cùng một cụm sân
            if (fieldService.existsByName(fieldName, complexId)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("""
                {
                    "success": false,
                    "message": "Tên sân đã tồn tại! Vui lòng nhập tên khác!"
                }
                """);
                return;
            }

            Field field = new Field();
            field.setFieldName(fieldName);
            field.setDescription(fieldDesc);
            field.setFieldTypeId(fieldTypeID);
            field.setComplexId(complexId);

            fieldService.insertField(field);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("SUCCESS");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("""
            {
                "success": false,
                "message": "%s"
            }
            """.formatted(
                    e.getMessage() != null ? e.getMessage() : "Không thể cập nhật sân bóng!"
            ));
        }
    }

    protected void edit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            long fieldId = Long.parseLong(req.getParameter("fieldID"));
            String fieldName = req.getParameter("fieldName");
            String fieldDesc = req.getParameter("description");
            int fieldTypeID = Integer.parseInt(req.getParameter("fieldTypeID"));
            long complexId = Long.parseLong(req.getParameter("complexId"));
            String status = req.getParameter("status");

            // Kiểm tra trùng tên trong cùng cụm sân (bỏ qua chính sân đang sửa)
            if (fieldService.existsByNameExceptId(fieldName, complexId, fieldId)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("""
                {
                    "success": false,
                    "message": "Tên sân đã tồn tại! Vui lòng nhập tên khác!"
                }
                """);
                return;
            }

            Field f = new Field();
            f.setFieldId(fieldId);
            f.setFieldName(fieldName);
            f.setDescription(fieldDesc);
            f.setFieldTypeId(fieldTypeID);
            f.setComplexId(complexId);

            if (status != null && !status.isBlank()) {
                f.setStatus(status);
            }

            fieldService.updateField(f);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("SUCCESS");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("""
            {
                "success": false,
                "message": "%s"
            }
            """.formatted(
                    e.getMessage() != null ? e.getMessage() : "Không thể cập nhật sân bóng!"
            ));
        }
    }

    protected void delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {

            long id = Long.parseLong(
                    req.getParameter("id")
            );

            fieldService.deleteField(id);

            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (IllegalStateException e) {

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain;charset=UTF-8");

            resp.getWriter().write(e.getMessage());

        } catch (Exception e) {

            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");

            resp.getWriter().write("Lỗi hệ thống.");
        }
    }
    protected void getInfo(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = Long.parseLong(req.getParameter("id"));
        Field f = fieldService.getFieldInfo(id);
        resp.setContentType("application/json;charset=UTF-8");

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, t, c)
                                -> new JsonPrimitive(src.toString())
                )
                .create();

        resp.getWriter().write(gson.toJson(f));
    }

    private void changeStatus(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            long fieldId = Long.parseLong(req.getParameter("fieldId"));
            String status = req.getParameter("status");

            fieldService.changeStatus(fieldId, status);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("SUCCESS");

        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Có lỗi xảy ra khi cập nhật trạng thái.");
        }
    }
}

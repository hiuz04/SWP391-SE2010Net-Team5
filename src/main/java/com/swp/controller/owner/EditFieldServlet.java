package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.Field;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/field/edit")
public class EditFieldServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = Long.parseLong(req.getParameter("id"));
        Field f = Constant.fieldDAO.getFieldByID(id);
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long fieldId = Long.parseLong(req.getParameter("fieldID"));
        String fieldName = req.getParameter("fieldName");
        String fieldDesc = req.getParameter("description");
        int fieldTypeID = Integer.parseInt(req.getParameter("fieldTypeID"));
        long facilityID = Long.parseLong(req.getParameter("facilityId"));
        String status = req.getParameter("status");

        Field f = new Field();
        f.setFieldId(fieldId);
        f.setFieldName(fieldName);
        f.setDescription(fieldDesc);
        f.setFieldTypeId(fieldTypeID);
        f.setFacilityId(facilityID);
        f.setStatus(status);

        Constant.fieldDAO.editField(f);
    }
}

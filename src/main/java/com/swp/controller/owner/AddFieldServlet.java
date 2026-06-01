package com.swp.controller.owner;

import com.swp.model.Field;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/field/add")
public class AddFieldServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String fieldName = req.getParameter("fieldName");
        String fieldDesc = req.getParameter("description");
        int fieldTypeID = Integer.parseInt(req.getParameter("fieldTypeID"));
        long facilityID = Long.parseLong(req.getParameter("facilityId"));
        String status = req.getParameter("status");

        Field field = new Field();
        field.setFieldName(fieldName);
        field.setDescription(fieldDesc);
        field.setFieldTypeId(fieldTypeID);
        field.setFacilityId(facilityID);
        field.setStatus(status);

        Constant.fieldDAO.addField(field);
        resp.sendRedirect(req.getContextPath() + "/owner/field-list");
    }
}

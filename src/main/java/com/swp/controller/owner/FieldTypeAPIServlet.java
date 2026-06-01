package com.swp.controller.owner;

import com.google.gson.Gson;
import com.swp.model.FieldType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/field-type")
public class FieldTypeAPIServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<FieldType> fieldTypes = Constant.fieldTypeDAO.getAllFieldTypes();
        resp.setContentType("application/json;charset=UTF-8");

        Gson gson = new Gson();
        resp.getWriter().write(gson.toJson(fieldTypes));
    }
}

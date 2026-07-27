package com.swp.controller.customer;

import com.swp.dao.FieldTypeDAO;
import com.swp.model.FieldType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet("/search")
public class SearchPage extends HttpServlet {
    private final FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<FieldType> fieldTypes;
        try {
            fieldTypes = fieldTypeDAO.getAllFieldTypes();
        } catch (Exception e) {
            e.printStackTrace();
            fieldTypes = Collections.emptyList();
        }
        req.setAttribute("fieldTypes", fieldTypes);

        req.getRequestDispatcher("/WEB-INF/customer/field-list.jsp")
                .forward(req, resp);
    }
}

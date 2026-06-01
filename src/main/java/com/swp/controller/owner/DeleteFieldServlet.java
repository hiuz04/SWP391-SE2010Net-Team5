package com.swp.controller.owner;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/field/delete")
public class DeleteFieldServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long fieldId = Long.parseLong(req.getParameter("id"));
        Constant.fieldDAO.deleteField(fieldId);
        resp.sendRedirect(req.getContextPath() + "/owner/field-list");
    }

}

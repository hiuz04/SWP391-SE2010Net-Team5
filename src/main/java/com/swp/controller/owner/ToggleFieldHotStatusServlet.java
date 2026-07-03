package com.swp.controller.owner;

import com.swp.dao.FieldDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/owner/field/toggle-hot")
public class ToggleFieldHotStatusServlet extends HttpServlet {

    private final FieldDAO fieldDAO = new FieldDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            long fieldId = Long.parseLong(request.getParameter("fieldId"));
            boolean isHot = Boolean.parseBoolean(request.getParameter("isHot"));

            fieldDAO.updateFieldHotStatus(fieldId, isHot);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("success");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("error");
        }
    }
}

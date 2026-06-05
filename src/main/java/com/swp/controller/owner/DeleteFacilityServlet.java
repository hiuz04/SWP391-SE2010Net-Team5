/**
 * Module: Facility Management
 * File: DeleteFacilityServlet.java
 * Description: Xử lý loại bỏ cơ sở.
 *
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 01/06/2026
 */
package com.swp.controller.owner;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/facility/delete")
public class DeleteFacilityServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = Long.parseLong(req.getParameter("id"));
        Constant.fieldDAO.deleteFieldWithFacilityID(id);
        Constant.facilityDAO.deleteFacility(id);
    }
}

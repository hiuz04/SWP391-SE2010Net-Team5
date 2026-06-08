package com.swp.controller.customer;

import com.google.gson.Gson;
import com.swp.dao.FacilityDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/cities")
public class GetCitiesList extends HttpServlet {
    private final FacilityDAO facilityDAO = new FacilityDAO();
    Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> cities = facilityDAO.getAllCities();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write(gson.toJson(cities));
    }
}

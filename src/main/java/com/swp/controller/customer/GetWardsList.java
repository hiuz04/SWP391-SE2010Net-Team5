package com.swp.controller.customer;

import com.google.gson.Gson;
import com.swp.dao.FootballComplexDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/wards")
public class GetWardsList extends HttpServlet {
    private final static FootballComplexDAO FOOTBALL_COMPLEX_DAO = new FootballComplexDAO();
    private static Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> wards = FOOTBALL_COMPLEX_DAO.getAllWards();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write(gson.toJson(wards));
    }
}

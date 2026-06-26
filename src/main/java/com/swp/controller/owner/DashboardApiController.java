package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.dto.OwnerDashboardDTO;
import com.swp.service.owner.OwnerDashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/api/dashboard")
public class DashboardApiController extends HttpServlet {

    private static final OwnerDashboardService dashboardService = new OwnerDashboardService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        OwnerDashboardDTO dashboard = dashboardService.getDashboard();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalDate.class,
                        (JsonSerializer<LocalDate>)
                                (src, type, context)
                                        -> new JsonPrimitive(src.toString())
                )
                .create();

        String json = gson.toJson(dashboard);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write(json);
    }
}

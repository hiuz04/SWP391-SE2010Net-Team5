package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.Facility;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@WebServlet("/facility/edit")
public class EditFacilityServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = Long.parseLong(req.getParameter("id"));
        Facility fac = Constant.facilityDAO.getFacilityDataByID(id);
        resp.setContentType("application/json;charset=UTF-8");

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, t, c)
                                -> new JsonPrimitive(src.toString())
                )
                .registerTypeAdapter(
                        LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, t, c)
                                -> new JsonPrimitive(src.toString())
                )
                .create();
        resp.getWriter().write(gson.toJson(fac));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long facilityID = Long.parseLong(req.getParameter("facilityID"));
        String facilityName = req.getParameter("facilityName");
        String description = req.getParameter("description");
        String address = req.getParameter("address");
        String ward = req.getParameter("ward");
        String district = req.getParameter("district");
        String city = req.getParameter("city");

        String latStr = req.getParameter("latitude");
        String lngStr = req.getParameter("longitude");
        BigDecimal latitude = (latStr == null || latStr.trim().isEmpty())
                ? null
                : new BigDecimal(latStr);

        BigDecimal longitude = (lngStr == null || lngStr.trim().isEmpty())
                ? null
                : new BigDecimal(lngStr);

        String hotline = req.getParameter("hotline");
        String openStr = req.getParameter("openingTime");
        String closeStr = req.getParameter("closingTime");

        LocalTime openingTime = (openStr == null || openStr.trim().isEmpty())
                ? null
                : LocalTime.parse(openStr);

        LocalTime closingTime = (closeStr == null || closeStr.trim().isEmpty())
                ? null
                : LocalTime.parse(closeStr);

        String generalRules = req.getParameter("generalRules");
        String status = req.getParameter("status");
        Boolean featured = Boolean.getBoolean(req.getParameter("featured"));

        Facility fac = new Facility();
        fac.setFacilityId(facilityID);
        fac.setFacilityName(facilityName);
        fac.setDescription(description);
        fac.setAddress(address);
        fac.setWard(ward);
        fac.setDistrict(district);
        fac.setCity(city);
        fac.setLatitude(latitude);
        fac.setLongitude(longitude);
        fac.setHotline(hotline);
        fac.setOpeningTime(openingTime);
        fac.setClosingTime(closingTime);
        fac.setGeneralRules(generalRules);
        fac.setStatus(status);
        fac.setFeatured(featured);

        Constant.facilityDAO.editFacility(fac);
    }
}

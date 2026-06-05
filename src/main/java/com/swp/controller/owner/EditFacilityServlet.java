/**
 * Module: Facility Management
 * File: EditFacilityServlet.java
 * Description: Xử lý chức năng kiểm tra, lấy dữ liệu và thay đổi thông tin cơ sở.
 *
 * Author: Dương Hải Anh
 * Version: 1.1
 * Created date: 01/06/2026
 * Updated date: 06/05/2026
 * Update Notes: Loại bỏ trường latitude và longtitude
 */
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
        fac.setHotline(hotline);
        fac.setOpeningTime(openingTime);
        fac.setClosingTime(closingTime);
        fac.setGeneralRules(generalRules);
        fac.setStatus(status);
        fac.setFeatured(featured);

        Constant.facilityDAO.editFacility(fac);
    }
}

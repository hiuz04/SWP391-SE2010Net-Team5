/**
 * Module: Facility Management
 * File: AddFacilityServlet.java
 * Description: Xử lý chức năng thêm mới cơ sở.
 *
 * Author: Dương Hải Anh
 * Version: 1.1
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 * Update Notes: Loại bỏ 2 trường latitude và longtitude
 */
package com.swp.controller.owner;

import com.swp.model.Facility;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;

@WebServlet("/facility/add")
public class AddFacilityServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        Constant.facilityDAO.addFacility(fac);
    }
}

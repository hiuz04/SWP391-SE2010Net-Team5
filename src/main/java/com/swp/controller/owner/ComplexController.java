package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.FootballComplex;
import com.swp.model.FootballComplexImage;
import com.swp.model.dto.CloudinaryResponse;
import com.swp.model.dto.ComplexDetailDTO;
import com.swp.service.CloudinaryService;
import com.swp.service.FootballComplexService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@WebServlet("/owner/complex")
@MultipartConfig
public class ComplexController extends HttpServlet {

    private static final FootballComplexService complexService = new FootballComplexService();
    private static final CloudinaryService cloudinaryService = new CloudinaryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("get".equals(action)) {
            getInfo(req, resp);
            return;
        }
        req.getRequestDispatcher("/WEB-INF/owner/complex-list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        switch (action == null ? "" : action) {
            case "add":
                add(req, resp);
                break;
            case "edit":
                edit(req, resp);
                break;
            case "delete":
                delete(req, resp);
                break;
            case "status":
                changeStatus(req, resp);
                break;
        }
    }

    private void add(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String complexName = req.getParameter("complexName");
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

        FootballComplex fc = new FootballComplex();
        fc.setComplexName(complexName);
        fc.setDescription(description);
        fc.setAddress(address);
        fc.setWard(ward);
        fc.setDistrict(district);
        fc.setCity(city);
        fc.setLatitude(latitude);
        fc.setLongitude(longitude);
        fc.setHotline(hotline);
        fc.setOpeningTime(openingTime);
        fc.setClosingTime(closingTime);
        fc.setGeneralRules(generalRules);

        long complexId = complexService.addFootballComplex(fc);
        addImage(req, resp, complexId);
    }

    private void edit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("complexId");

        if (id == null || id.trim().isEmpty()) {
            // Sau thêm message trả về
            return;
        }
        long complexId;
        try {
            complexId = Long.parseLong(req.getParameter("complexId"));
        } catch (NumberFormatException e) {
            // Sau thêm message báo lỗi
            return;
        }

        String complexName = req.getParameter("complexName");
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

        FootballComplex fc = new FootballComplex();
        fc.setComplexId(complexId);
        fc.setComplexName(complexName);
        fc.setDescription(description);
        fc.setAddress(address);
        fc.setWard(ward);
        fc.setDistrict(district);
        fc.setCity(city);
        fc.setLatitude(latitude);
        fc.setLongitude(longitude);
        fc.setHotline(hotline);
        fc.setOpeningTime(openingTime);
        fc.setClosingTime(closingTime);
        fc.setGeneralRules(generalRules);

        complexService.updateFootballComplex(fc);
        updateImage(req, resp);
        addImage(req, resp, complexId);
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {

            long id = Long.parseLong(
                    req.getParameter("id")
            );

            complexService.deleteFootballComplex(id);

            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (IllegalStateException e) {

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain;charset=UTF-8");

            resp.getWriter().write(e.getMessage());

        } catch (Exception e) {

            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");

            resp.getWriter().write("Lỗi hệ thống.");
        }
    }

    private void addImage(HttpServletRequest req, HttpServletResponse resp, long complexId) throws ServletException, IOException {
        List<Part> imageParts = req.getParts()
                .stream()
                .filter(part ->
                        "images".equals(part.getName())
                                && part.getSize() > 0)
                .toList();

        if (imageParts.isEmpty()) {
            return;
        }

        String[] thumbnails = req.getParameterValues("thumbnail");

        for (int i = 0; i < imageParts.size(); i++) {

            Part part = imageParts.get(i);

            byte[] data = part.getInputStream().readAllBytes();

            CloudinaryResponse result = cloudinaryService.upload(data);

            FootballComplexImage image = new FootballComplexImage();
            image.setComplexId(complexId);
            image.setImageUrl(result.getImgUrl());
            image.setThumbnail(
                    Boolean.parseBoolean(thumbnails[i]));
            image.setPublicId(result.getPublicId());
            complexService.addImg(image);
        }
    }

    private void getInfo(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = Long.parseLong(req.getParameter("id"));
        FootballComplex fc = complexService.getFootballComplexInfo(id);
        List<FootballComplexImage> imgs = complexService.getFootballComplexImg(id);
        ComplexDetailDTO detail = new ComplexDetailDTO(fc, imgs);

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
        resp.getWriter().write(gson.toJson(detail));
    }

    private void updateImage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] deleted = req.getParameterValues("deletedImg");

        String[] imgOld = req.getParameterValues("imagesOld");
        String[] thumbnailOld = req.getParameterValues("thumbnailOld");

        if (deleted != null) {
            for (String id : deleted) {
                long imgId = Long.parseLong(id);
                FootballComplexImage img = complexService.getImgById(imgId);
                if (img != null) {
                    if (img.getPublicId() != null && !img.getPublicId().trim().isEmpty()) {
                        cloudinaryService.delete(img.getPublicId());
                    }
                    complexService.deleteImg(imgId);
                }
            }
        }

        if (imgOld != null && thumbnailOld != null) {
            for (int i = 0; i < imgOld.length; i++) {
                complexService.updateImg(
                        Long.parseLong(imgOld[i]),
                        Boolean.parseBoolean(thumbnailOld[i])
                );
            }
        }
    }

    private void changeStatus(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            long complexId = Long.parseLong(req.getParameter("complexId"));
            String status = req.getParameter("status");

            complexService.changeStatus(complexId, status);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("SUCCESS");

        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Có lỗi xảy ra khi cập nhật trạng thái.");
        }
    }
}

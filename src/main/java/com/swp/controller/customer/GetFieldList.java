package com.swp.controller.customer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.FieldDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.model.*;
import com.swp.model.dto.ComplexCard;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@WebServlet("/field-list")
public class GetFieldList extends HttpServlet {

    private static final FootballComplexDAO FOOTBALL_COMPLEX_DAO = new FootballComplexDAO();
    private static final FieldDAO fieldDAO = new FieldDAO();
    private static final FieldTypeDAO fieldTypeDao = new FieldTypeDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Bước 1: Nhận các tham số tìm kiếm và bộ lọc từ request (Tỉnh/Thành, Quận/Huyện, Loại sân, Sắp xếp)
        String province = request.getParameter("province");
        String ward = request.getParameter("ward");
        String fieldTypeId = request.getParameter("fieldTypeId");
        String sortOrder = request.getParameter("sortOrder");

        // Bước 2: Nạp toàn bộ danh sách Cơ sở, Sân nhỏ và Loại sân từ CSDL
        List<FootballComplex> complexes = FOOTBALL_COMPLEX_DAO.getAllComplex();
        List<Field> fields = fieldDAO.getAllField();
        List<FieldType> fieldTypes = fieldTypeDao.getAllFieldTypes();

        List<ComplexCard> lists = new ArrayList<>();

        Map<Integer, FieldType> fieldTypeMap = fieldTypes.stream()
                .collect(Collectors.toMap(
                        FieldType::getFieldTypeId,
                        Function.identity()
                ));

        // Bước 3: Xử lý và gom nhóm dữ liệu để hiển thị thành các Thẻ (Card) trên giao diện
        for (FootballComplex fc : complexes) {
            // Lấy ảnh bìa (thumbnail) cho cơ sở
            FootballComplexImage thumbnail = FOOTBALL_COMPLEX_DAO.getThumbnail(fc.getComplexId());

            // Tìm ra cơ sở này đang có những loại sân nào (VD: 5 người, 7 người)
            List<FieldType> typeOfFc = fields.stream()
                    .filter(f -> f.getComplexId() == fc.getComplexId())
                    .map(Field::getFieldTypeId)
                    .distinct()
                    .map(fieldTypeMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            // Bước 4: Thực hiện logic lọc (Filter)
            // Lọc theo Tỉnh/Thành phố
            if (province != null
                    && !province.isBlank()
                    && !province.equalsIgnoreCase(fc.getCity())) {
                continue;
            }

            // Lọc theo Quận/Huyện
            if (ward != null
                    && !ward.isBlank()
                    && !ward.equalsIgnoreCase(fc.getWard())) {
                continue;
            }

            // Lọc theo loại sân (Nếu người dùng chọn Loại sân 5 người, bỏ qua cơ sở không có)
            if (fieldTypeId != null && !fieldTypeId.isBlank()) {

                int typeId = Integer.parseInt(fieldTypeId);

                boolean hasType = typeOfFc.stream()
                        .anyMatch(t -> t.getFieldTypeId() == typeId);

                if (!hasType) {
                    continue;
                }
            }

            // Bước 5: Đóng gói dữ liệu thành đối tượng hiển thị (ComplexCard)
            ComplexCard card = new ComplexCard();

            card.setComplexId(fc.getComplexId());
            card.setComplexName(fc.getComplexName());
            card.setAddress(fc.getAddress());
            card.setCity(fc.getCity());
            card.setWard(fc.getWard());
            card.setFieldTypeList(typeOfFc);
            card.setOpeningTime(fc.getOpeningTime());
            card.setClosingTime(fc.getClosingTime());
            card.setThumbnailUrl(thumbnail != null ? thumbnail.getImageUrl() : null);
            card.setCurrentPrice(FOOTBALL_COMPLEX_DAO.getCurrentPriceForComplex(fc.getComplexId()));

            lists.add(card);
        }

        // Bước 6: Xử lý sắp xếp (VD: Sắp xếp theo giá tăng dần/giảm dần)
        if (sortOrder != null && !sortOrder.isBlank()) {
            if (sortOrder.equals("price_asc")) {
                lists.sort((c1, c2) -> {
                    if (c1.getCurrentPrice() == null && c2.getCurrentPrice() == null) return 0;
                    if (c1.getCurrentPrice() == null) return 1;
                    if (c2.getCurrentPrice() == null) return -1;
                    return c1.getCurrentPrice().compareTo(c2.getCurrentPrice());
                });
            } else if (sortOrder.equals("price_desc")) {
                lists.sort((c1, c2) -> {
                    if (c1.getCurrentPrice() == null && c2.getCurrentPrice() == null) return 0;
                    if (c1.getCurrentPrice() == null) return 1;
                    if (c2.getCurrentPrice() == null) return -1;
                    return c2.getCurrentPrice().compareTo(c1.getCurrentPrice());
                });
            }
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString())
                )
                .create();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write(gson.toJson(lists));
    }
}

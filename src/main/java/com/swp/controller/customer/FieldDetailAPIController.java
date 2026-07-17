package com.swp.controller.customer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.FieldDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.model.Field;
import com.swp.model.FieldType;
import com.swp.model.FootballComplex;
import com.swp.model.PriceRule;
import com.swp.model.dto.FeedbackDTO;
import com.swp.model.dto.FieldDetail;
import com.swp.service.FeedbackService;
import com.swp.service.FieldService;
import com.swp.service.FieldTypeService;
import com.swp.service.FootballComplexService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@WebServlet("/field")
public class FieldDetailAPIController extends HttpServlet {

    private static final FootballComplexService complexService = new FootballComplexService();
    private static final FieldService fieldService = new FieldService();
    private static final FieldTypeService typeService = new FieldTypeService();
    private static final FeedbackService feedbackService = new FeedbackService();
    private static final com.swp.dao.PriceRuleDAO priceRuleDAO = new com.swp.dao.PriceRuleDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long complexId = Long.parseLong(req.getParameter("id"));
        FootballComplex complex = complexService.getFootballComplexInfo(complexId);
        List<Field> fields = fieldService.getFieldOfThisComplex(complexId);
        List<FieldType> fieldTypes = typeService.getAllType();
        List<FeedbackDTO> feedbacks = feedbackService.getAllFeedbackOfThisComplexes(complexId);
        List<PriceRule> priceRules = priceRuleDAO.getByComplexId(complexId);
        FieldDetail detail = new FieldDetail();

        Map<Integer, FieldType> fieldTypeMap = fieldTypes.stream()
                .collect(Collectors.toMap(
                        FieldType::getFieldTypeId,
                        Function.identity()
                ));
        List<FieldType> typeOfFc = fields.stream()
                .map(Field::getFieldTypeId)
                .distinct()
                .map(fieldTypeMap::get)
                .filter(Objects::nonNull)
                .toList();

        detail.setComplexId(complexId);
        detail.setComplexName(complex.getComplexName());
        detail.setComplexAddress(String.join(", ",
                complex.getAddress(),
                complex.getWard(),
                complex.getDistrict(),
                complex.getCity()
        ));
        detail.setDescription(complex.getDescription());
        detail.setFieldTypeList(typeOfFc);
        detail.setOpeningTime(complex.getOpeningTime());
        detail.setClosingTime(complex.getClosingTime());
        detail.setFields(fields);
        detail.setHotline(complex.getHotline());

        java.math.BigDecimal currentPrice = com.swp.util.PriceCalculator.calculateCurrentPrice(priceRules, null, null);
        detail.setCurrentPrice(currentPrice);

        detail.setFeedbacks(feedbacks);
        detail.setPriceRules(priceRules);

        resp.setContentType("application/json;charset=UTF-8");
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString()))
                .create();

        resp.getWriter().write(gson.toJson(detail));
    }
}

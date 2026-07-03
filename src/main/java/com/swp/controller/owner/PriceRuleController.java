package com.swp.controller.owner;

import com.swp.dao.PriceRuleDAO;
import com.swp.dao.FacilityDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.model.PriceRule;
import com.swp.model.Facility;
import com.swp.model.FieldType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@WebServlet("/owner/price-rules")
public class PriceRuleController extends HttpServlet {

    private final PriceRuleDAO priceRuleDAO = new PriceRuleDAO();
    private final FacilityDAO facilityDAO = new FacilityDAO();
    private final FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("get".equals(action)) {
            // For AJAX get if needed
            return;
        }

        List<PriceRule> priceRules = priceRuleDAO.getAllPriceRules();
        List<Facility> facilities = facilityDAO.getAllFacility();
        List<FieldType> fieldTypes = fieldTypeDAO.getAllFieldTypes();
        
        req.setAttribute("priceRules", priceRules);
        req.setAttribute("facilities", facilities);
        req.setAttribute("fieldTypes", fieldTypes);
        
        req.getRequestDispatcher("/WEB-INF/owner/price-rule-list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
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
                default:
                    resp.sendRedirect(req.getContextPath() + "/owner/price-rules");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Error: " + e.getMessage());
            doGet(req, resp);
        }
    }

    private void add(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PriceRule rule = extractPriceRuleFromRequest(req);
        priceRuleDAO.insert(rule);
        resp.sendRedirect(req.getContextPath() + "/owner/price-rules");
    }

    private void edit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = Long.parseLong(req.getParameter("priceRuleId"));
        PriceRule rule = extractPriceRuleFromRequest(req);
        rule.setPriceRuleId(id);
        priceRuleDAO.update(rule);
        resp.sendRedirect(req.getContextPath() + "/owner/price-rules");
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = Long.parseLong(req.getParameter("id"));
        priceRuleDAO.delete(id);
        resp.sendRedirect(req.getContextPath() + "/owner/price-rules");
    }

    private PriceRule extractPriceRuleFromRequest(HttpServletRequest req) {
        PriceRule rule = new PriceRule();
        
        String facilityIdStr = req.getParameter("facilityId");
        if (facilityIdStr != null && !facilityIdStr.isEmpty()) {
            rule.setFacilityId(Long.parseLong(facilityIdStr));
        }

        String fieldTypeIdStr = req.getParameter("fieldTypeId");
        if (fieldTypeIdStr != null && !fieldTypeIdStr.isEmpty()) {
            rule.setFieldTypeId(Integer.parseInt(fieldTypeIdStr));
        }

        rule.setRuleName(req.getParameter("ruleName"));
        rule.setRuleType(req.getParameter("ruleType"));
        
        if ("WEEKDAY".equals(rule.getRuleType()) || "WEEKEND".equals(rule.getRuleType())) {
            rule.setDayOfWeek(req.getParameter("dayOfWeek")); // Can be multiple if handled as comma-separated, else frontend logic handles mapping
        }

        String specificDateStr = req.getParameter("specificDate");
        if (specificDateStr != null && !specificDateStr.isEmpty()) {
            rule.setSpecificDate(LocalDate.parse(specificDateStr));
        }

        String startTimeStr = req.getParameter("startTime");
        if (startTimeStr != null && !startTimeStr.isEmpty()) {
            rule.setStartTime(LocalTime.parse(startTimeStr));
        }

        String endTimeStr = req.getParameter("endTime");
        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            rule.setEndTime(LocalTime.parse(endTimeStr));
        }

        String priceStr = req.getParameter("price");
        if (priceStr != null && !priceStr.isEmpty()) {
            rule.setPrice(new BigDecimal(priceStr));
        }

        String priorityStr = req.getParameter("priority");
        if (priorityStr != null && !priorityStr.isEmpty()) {
            rule.setPriority(Integer.parseInt(priorityStr));
        } else {
            rule.setPriority(0);
        }

        String status = req.getParameter("status");
        rule.setStatus(status != null ? status : "ACTIVE");

        return rule;
    }
}

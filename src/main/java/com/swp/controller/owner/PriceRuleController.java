package com.swp.controller.owner;

import com.swp.dao.PriceRuleDAO;
import com.swp.model.PriceRule;
import com.swp.model.FootballComplex;
import com.swp.service.FootballComplexService;
import com.swp.service.FieldService;
import com.swp.service.FieldTypeService;
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

    private static final PriceRuleDAO priceRuleDAO = new PriceRuleDAO();
    private static final FootballComplexService complexService = new FootballComplexService();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Bước 1: Lấy danh sách cơ sở thể thao mà Owner đang quản lý
        List<FootballComplex> complexes = complexService.getListFootballComplex();
        req.setAttribute("complexes", complexes);
        
        // Bước 2: Nạp danh sách các loại sân (VD: sân 5, sân 7) để hiện trên Form
        FieldTypeService fieldTypeService = new FieldTypeService();
        req.setAttribute("fieldTypes", fieldTypeService.getAllType());

        // Bước 3: Xác định cơ sở đang được chọn (lấy từ param hoặc mặc định là cơ sở đầu tiên)
        long complexId = -1;
        String complexIdParam = req.getParameter("complexId");
        if (complexIdParam != null && !complexIdParam.trim().isEmpty()) {
            complexId = Long.parseLong(complexIdParam);
        } else if (!complexes.isEmpty()) {
            complexId = complexes.get(0).getComplexId();
        }

        // Bước 4: Lấy danh sách luật giá và danh sách sân nhỏ thuộc cơ sở đang chọn
        if (complexId != -1) {
            List<PriceRule> priceRules = priceRuleDAO.getByComplexId(complexId);
            req.setAttribute("priceRules", priceRules);
            req.setAttribute("selectedComplexId", complexId);
            
            FieldService fieldService = new FieldService();
            req.setAttribute("fields", fieldService.getFieldOfThisComplex(complexId));
        }

        // Bước 5: Render trang quản lý bảng giá (JSP)
        req.getRequestDispatcher("/WEB-INF/owner/price-rules.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) action = "";

        long complexId = -1;
        try {
            String complexIdStr = req.getParameter("complexId");
            if (complexIdStr == null || complexIdStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Thiếu tham số complexId.");
            }
            complexId = Long.parseLong(complexIdStr.trim());

            // Bước 2: Dựa vào action, gọi các hàm DAO tương ứng và thiết lập thông báo thành công
            switch (action) {
                case "add":
                    handleAdd(req);
                    req.getSession().setAttribute("successMsg", "Thêm bảng giá thành công!");
                    break;
                case "edit":
                    handleEdit(req);
                    req.getSession().setAttribute("successMsg", "Cập nhật bảng giá thành công!");
                    break;
                case "delete":
                    long priceRuleId = Long.parseLong(req.getParameter("priceRuleId"));
                    priceRuleDAO.delete(priceRuleId, complexId);
                    req.getSession().setAttribute("successMsg", "Xóa bảng giá thành công!");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }

        // Bước 3: Chuyển hướng lại trang danh sách (tránh submit lại form khi F5)
        if (complexId != -1) {
            resp.sendRedirect(req.getContextPath() + "/owner/price-rules?complexId=" + complexId);
        } else {
            resp.sendRedirect(req.getContextPath() + "/owner/price-rules");
        }
    }

    private void handleAdd(HttpServletRequest req) throws Exception {
        PriceRule pr = parseRequest(req);
        priceRuleDAO.insert(pr);
    }

    private void handleEdit(HttpServletRequest req) throws Exception {
        PriceRule pr = parseRequest(req);
        pr.setPriceRuleId(Long.parseLong(req.getParameter("priceRuleId")));
        priceRuleDAO.update(pr);
    }

    private PriceRule parseRequest(HttpServletRequest req) throws Exception {
        PriceRule pr = new PriceRule();
        pr.setComplexId(Long.parseLong(req.getParameter("complexId")));
        
        String fieldTypeIdStr = req.getParameter("fieldTypeId");
        if (fieldTypeIdStr != null && !fieldTypeIdStr.trim().isEmpty()) {
            pr.setFieldTypeId(Integer.parseInt(fieldTypeIdStr));
        }

        String fieldIdStr = req.getParameter("fieldId");
        if (fieldIdStr != null && !fieldIdStr.trim().isEmpty()) {
            pr.setFieldId(Long.parseLong(fieldIdStr));
        }

        pr.setRuleName(req.getParameter("ruleName"));
        
        String dayOfWeek = req.getParameter("dayOfWeek");
        if (dayOfWeek != null && !dayOfWeek.trim().isEmpty()) pr.setDayOfWeek(dayOfWeek);

        String specificDate = req.getParameter("specificDate");
        if (specificDate != null && !specificDate.trim().isEmpty()) {
            pr.setSpecificDate(LocalDate.parse(specificDate));
        }

        String startTime = req.getParameter("startTime");
        if (startTime != null && !startTime.trim().isEmpty()) {
            pr.setStartTime(LocalTime.parse(startTime));
        }

        String endTime = req.getParameter("endTime");
        if (endTime != null && !endTime.trim().isEmpty()) {
            pr.setEndTime(LocalTime.parse(endTime));
        }

        String priceStr = req.getParameter("price");
        if (priceStr == null || priceStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập giá sân.");
        }
        pr.setPrice(new BigDecimal(priceStr.trim()));
        pr.setRuleType(req.getParameter("ruleType"));
        
        String priorityStr = req.getParameter("priority");
        if (priorityStr != null && !priorityStr.trim().isEmpty()) {
            pr.setPriority(Integer.parseInt(priorityStr));
        } else {
            pr.setPriority(0);
        }
        
        String status = req.getParameter("status");
        if (status != null && !status.trim().isEmpty()) {
            pr.setStatus(status);
        } else {
            pr.setStatus("ACTIVE");
        }

        return pr;
    }
}

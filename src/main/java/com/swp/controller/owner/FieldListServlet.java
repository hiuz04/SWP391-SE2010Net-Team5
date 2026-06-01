package com.swp.controller.owner;

import com.swp.model.Facility;
import com.swp.model.Field;
import com.swp.model.FieldType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/owner/field-list")
public class FieldListServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        // Chưa đăng nhập
//        if(session == null || session.getAttribute("user") == null) {
//            resp.sendRedirect(req.getContextPath() + "/login.jsp");
//            return;
//        }

        // Không phải là Owner
//        User user = (User) session.getAttribute("user");
//        if(!Constant.OWNER_ROLE_NAME.equals(user.getRoleName())) {
//            resp.sendRedirect(req.getContextPath() + "/error/error-403.jsp");
//            return;
//        }


        List<Field> fields = Constant.fieldDAO.getAllField();
        List<Facility> facilities = Constant.facilityDAO.getAllFacility();
        List<FieldType> fieldTypes = Constant.fieldTypeDAO.getAllFieldTypes();

        Map<Facility, List<Field>> map = new LinkedHashMap<>();

        for(Facility fac : facilities) {
            map.put(fac, new ArrayList<>());
        }

        for (Field f : fields) {
            for (Facility fac : facilities) {
                if (fac.getFacilityId() == f.getFacilityId()) {
                    map.get(fac).add(f);
                    break;
                }
            }
        }

        req.setAttribute("fieldFacility", map);
        req.setAttribute("facilities", facilities);
        req.setAttribute("fieldTypes", fieldTypes);

        req.getRequestDispatcher("/owner/field-list.jsp").forward(req, resp);
    }
}

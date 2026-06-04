/**
 * Module: Field Management
 * File: FieldListServlet.java
 * Description: Xử lý điều hướng người dùng đến trang hiển thị danh sách Field.
 *
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 04/06/2026
 */
package com.swp.controller.owner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.dto.FacilityWithField;
import com.swp.model.Facility;
import com.swp.model.Field;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/owner/field-list")
public class FieldListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        // Chưa đăng nhập
        if(session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        // Không phải là Owner
        User user = (User) session.getAttribute("user");
        if(!Constant.OWNER_ROLE_NAME.equals(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        req.getRequestDispatcher("/WEB-INF/owner/field-list.jsp").forward(req, resp);
    }
}

/*
 * Author: Tran Bao Long
 * 4/6/2026
 */
package com.swp.controller;

import com.swp.dao.FieldDAO;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.FieldTypeDAO;
import com.swp.model.User;
import com.swp.model.FieldType;
import com.swp.model.dto.TopFieldSummary;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Servlet xử lý trang chủ (homepage).
 * URL: / và /index
 */
@WebServlet(urlPatterns = {"/index"})
public class HomeController extends HttpServlet {

    private final FieldDAO fieldDAO = new FieldDAO();
    private final FootballComplexDAO footballComplexDAO = new FootballComplexDAO();
    private final FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Lấy thông tin user từ session để truyền sang JSP
        HttpSession session = request.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;
        String navRole = (session != null && session.getAttribute("navRole") != null)
                ? (String) session.getAttribute("navRole") : "guest";
        String displayName = sessionUser != null ? sessionUser.getFullName() : "";

        // Lấy danh sách sân được đánh dấu là HOT
        List<TopFieldSummary> topFields;
        try {
            topFields = fieldDAO.getHotFields();
        } catch (Exception e) {
            e.printStackTrace();
            topFields = Collections.emptyList();
        }
        
        List<FieldType> fieldTypes;
        try {
            fieldTypes = fieldTypeDAO.getAllFieldTypes();
        } catch (Exception e) {
            e.printStackTrace();
            fieldTypes = Collections.emptyList();
        }

        request.setAttribute("sessionUser", sessionUser);
        request.setAttribute("navRole", navRole);
        request.setAttribute("displayName", displayName);
        request.setAttribute("topFields", topFields);
        request.setAttribute("fieldTypes", fieldTypes);

        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}

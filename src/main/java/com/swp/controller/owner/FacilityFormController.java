/**
 * Module: Facility Management
 * File: FacilityFormController.java
 * Description: Xử lý điều hướng người dùng đến trang nhập liệu dành riêng cho Facility.
 *
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 04/06/2026
 */
package com.swp.controller.owner;

import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/owner/facility-form")
public class FacilityFormController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/owner/facility-form.jsp").forward(req, resp);
    }
}

/**
 * Module: Field Management
 * File: FieldFormController.java
 * Description: Xử lý điều hướng người dùng đến trang nhập liệu dành riêng cho Field.
 * <p>
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 04/06/2026
 */
package com.swp.controller.owner;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/owner/field-form")
public class FieldFormController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/owner/field-form.jsp").forward(req, resp);
    }
}

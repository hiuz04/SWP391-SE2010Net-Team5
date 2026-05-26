package com.swp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if ("admin".equals(username) && "123".equals(password)) {
            request.setAttribute("message", "Đăng nhập thành công!");
            request.setAttribute("username", username);
        } else {
            request.setAttribute("message", "Sai tài khoản hoặc mật khẩu!");
            request.setAttribute("username", username);
        }

        request.getRequestDispatcher("result.jsp").forward(request, response);
    }
}

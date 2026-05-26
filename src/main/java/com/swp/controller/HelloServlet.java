package com.swp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        response.getWriter().println("<html>");
        response.getWriter().println("<head>");
        response.getWriter().println("<title>Hello Servlet</title>");
        response.getWriter().println("</head>");
        response.getWriter().println("<body>");
        response.getWriter().println("<h1>Hello Servlet!</h1>");
        response.getWriter().println("<p>Đây là servlet chạy bằng phương thức GET.</p>");
        response.getWriter().println("<a href='index.jsp'>Quay lại trang chủ</a>");
        response.getWriter().println("</body>");
        response.getWriter().println("</html>");
    }
}
package com.swp.controller;

import com.swp.util.DBContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@WebServlet("/db-test")
public class DbTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><body><h2>Kiểm tra kết nối SQL Server</h2>");

        try (Connection conn = DBContext.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            out.println("<p style='color:green'>Kết nối thành công.</p>");
            out.println("<p>Database: <b>" + meta.getDatabaseProductName() + "</b></p>");
            out.println("<p>URL: <b>" + meta.getURL() + "</b></p>");
            out.println("<p>User: <b>" + meta.getUserName() + "</b></p>");
        } catch (SQLException e) {
            out.println("<p style='color:red'>Kết nối thất bại: " + e.getMessage() + "</p>");
            out.println("<p>Kiểm tra file db.properties và SQL Server đang chạy.</p>");
        }

        out.println("<br><a href='index.jsp'>Về trang chủ</a>");
        out.println("</body></html>");
    }
}

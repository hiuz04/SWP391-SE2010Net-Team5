package com.swp.controller;

import com.swp.dao.UserDAO;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            Optional<User> user = userDAO.findByEmailAndPassword(email, password);
            if (user.isPresent()) {
                request.setAttribute("message", "Đăng nhập thành công!");
                request.setAttribute("username", user.get().getFullName());
            } else {
                request.setAttribute("message", "Sai email hoặc mật khẩu!");
                request.setAttribute("username", email);
            }
        } catch (RuntimeException e) {
            request.setAttribute("message", "Không kết nối được database. Kiểm tra db.properties và SQL Server.");
            request.setAttribute("username", email);
        }

        request.getRequestDispatcher("result.jsp").forward(request, response);
    }
}

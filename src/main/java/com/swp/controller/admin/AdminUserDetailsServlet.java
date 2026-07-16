package com.swp.controller.admin;

import com.swp.dao.BookingDAO;
import com.swp.dao.UserDAO;
import com.swp.model.dto.BookingView;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/user-details")
public class AdminUserDetailsServlet extends HttpServlet {

    private UserDAO userDAO;
    private BookingDAO bookingDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        bookingDAO = new BookingDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/users");
            return;
        }

        try {
            long userId = Long.parseLong(idParam);
            User userDetail = userDAO.getUserById(userId).orElse(null);

            if (userDetail == null) {
                response.sendRedirect(request.getContextPath() + "/admin/users");
                return;
            }

            List<BookingView> recentBookings = bookingDAO.getBookingHistoryByCustomerId(userId);

            request.setAttribute("userDetail", userDetail);
            request.setAttribute("recentBookings", recentBookings);

            request.getRequestDispatcher("/WEB-INF/admin/user-details.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/users");
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi truy vấn cơ sở dữ liệu");
        }
    }
}

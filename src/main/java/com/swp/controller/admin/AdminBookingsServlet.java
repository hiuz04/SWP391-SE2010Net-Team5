package com.swp.controller.admin;

import com.swp.dao.BookingDAO;
import com.swp.model.dto.BookingView;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/bookings")
public class AdminBookingsServlet extends HttpServlet {

    private BookingDAO bookingDAO;

    @Override
    public void init() throws ServletException {
        bookingDAO = new BookingDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String search = request.getParameter("search");
        String filter = request.getParameter("filter");

        int page = 1;
        int limit = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
            if (request.getParameter("limit") != null) {
                limit = Integer.parseInt(request.getParameter("limit"));
            }
        } catch (NumberFormatException e) {
            // Dùng giá trị mặc định nếu tham số lỗi
        }

        int offset = (page - 1) * limit;

        try {
            List<BookingView> bookingList = bookingDAO.getAdminBookingsPaginated(search, filter, offset, limit);
            int totalBookings = bookingDAO.countAdminBookings(search, filter);
            int totalPages = (int) Math.ceil((double) totalBookings / limit);

            request.setAttribute("bookingList", bookingList);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("limit", limit);
            request.setAttribute("search", search);
            request.setAttribute("filter", filter);

            request.getRequestDispatcher("/WEB-INF/admin/bookings.jsp").forward(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi tải danh sách booking.");
            request.getRequestDispatcher("/WEB-INF/admin/bookings.jsp").forward(request, response);
        }
    }
}

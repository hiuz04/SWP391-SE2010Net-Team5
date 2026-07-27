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

@WebServlet("/admin/booking-detail")
public class AdminBookingDetailServlet extends HttpServlet {

    private BookingDAO bookingDAO;

    @Override
    public void init() throws ServletException {
        bookingDAO = new BookingDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/bookings");
            return;
        }

        try {
            Long bookingId = Long.parseLong(idParam);
            BookingView booking = bookingDAO.getAdminBookingDetailById(bookingId);
            
            if (booking == null) {
                request.setAttribute("errorMessage", "Không tìm thấy thông tin lượt đặt sân.");
                request.getRequestDispatcher("/WEB-INF/admin/bookings.jsp").forward(request, response);
                return;
            }
            
            request.setAttribute("booking", booking);
            request.getRequestDispatcher("/WEB-INF/admin/booking-detail.jsp").forward(request, response);

        } catch (NumberFormatException | SQLException e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/admin/bookings");
        }
    }
}

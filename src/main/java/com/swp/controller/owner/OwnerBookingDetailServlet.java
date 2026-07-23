package com.swp.controller.owner;

import com.swp.dao.BookingDAO;
import com.swp.dao.FootballComplexDAO;
import com.swp.model.FootballComplex;
import com.swp.model.User;
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

@WebServlet("/owner/booking-detail")
public class OwnerBookingDetailServlet extends HttpServlet {

    private BookingDAO bookingDAO;
    private FootballComplexDAO complexDAO;

    @Override
    public void init() throws ServletException {
        bookingDAO = new BookingDAO();
        complexDAO = new FootballComplexDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        
        if (user == null || !"OWNER".equals(user.getRoleName())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/owner/dashboard");
            return;
        }

        try {
            Long bookingId = Long.parseLong(idParam);
            BookingView booking = bookingDAO.getAdminBookingDetailById(bookingId);
            
            if (booking == null) {
                request.setAttribute("errorMessage", "Không tìm thấy thông tin lượt đặt sân.");
                request.getRequestDispatcher("/WEB-INF/owner/dashboard.jsp").forward(request, response);
                return;
            }
            
            request.setAttribute("booking", booking);
            request.getRequestDispatcher("/WEB-INF/owner/booking-detail.jsp").forward(request, response);

        } catch (NumberFormatException | SQLException e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/owner/dashboard");
        }
    }
}

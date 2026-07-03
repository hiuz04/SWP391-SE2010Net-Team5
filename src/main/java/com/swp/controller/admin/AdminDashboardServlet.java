package com.swp.controller.admin;

import com.swp.dao.AdminDashboardDAO;
import com.swp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private AdminDashboardDAO dashboardDAO;

    @Override
    public void init() throws ServletException {
        dashboardDAO = new AdminDashboardDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khong co quyen truy cap.");
            return;
        }

        Map<String, Object> kpis = dashboardDAO.getDashboardKPIs();
        List<Map<String, Object>> revChart = dashboardDAO.getRevenueLast7Days();
        List<Map<String, Object>> typeChart = dashboardDAO.getBookingsByFieldType();
        List<Map<String, Object>> recentBookings = dashboardDAO.getRecentBookings();

        request.setAttribute("kpis", kpis);
        request.setAttribute("revChart", revChart);
        request.setAttribute("typeChart", typeChart);
        request.setAttribute("recentBookings", recentBookings);

        request.getRequestDispatcher("/WEB-INF/admin/dashboard.jsp").forward(request, response);
    }
}

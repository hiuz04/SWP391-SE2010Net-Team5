package com.swp.controller.admin;

import com.swp.dao.AdminDashboardDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;

@WebServlet("/admin/dashboard/export")
public class AdminDashboardExportServlet extends HttpServlet {

    private AdminDashboardDAO dashboardDAO;

    @Override
    public void init() throws ServletException {
        dashboardDAO = new AdminDashboardDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/csv; charset=UTF-8");
        // UTF-8 BOM to make Excel read unicode correctly
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        String filename = "bao_cao_admin_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        Map<String, Object> kpis = dashboardDAO.getDashboardKPIs();
        List<Map<String, Object>> revChart = dashboardDAO.getRevenueLast30Days();
        List<Map<String, Object>> typeChart = dashboardDAO.getBookingsByFieldType();

        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        try (PrintWriter writer = new PrintWriter(response.getOutputStream())) {
            writer.println("BÁO CÁO TỔNG QUAN HỆ THỐNG");
            writer.println("Ngày trích xuất: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            writer.println();

            writer.println("1. CHỈ SỐ KPI");
            writer.println("Doanh thu hôm nay,Doanh thu 30 ngày qua,Lượt đặt sân hôm nay,Khách hàng mới (Hôm nay),Lượt đặt sân chờ xử lý");
            writer.println(
                    (kpis.get("todayRevenue") != null ? kpis.get("todayRevenue").toString() : "0") + "," +
                    (kpis.get("last30DaysRevenue") != null ? kpis.get("last30DaysRevenue").toString() : "0") + "," +
                    (kpis.get("todayBookings") != null ? kpis.get("todayBookings").toString() : "0") + "," +
                    (kpis.get("newCustomers") != null ? kpis.get("newCustomers").toString() : "0") + "," +
                    (kpis.get("pendingBookings") != null ? kpis.get("pendingBookings").toString() : "0")
            );
            writer.println();

            writer.println("2. DOANH THU 30 NGÀY QUA");
            writer.println("Ngày,Doanh thu (VNĐ)");
            for (Map<String, Object> row : revChart) {
                String dateStr = (String) row.get("date");
                if (dateStr != null && dateStr.length() >= 10) {
                    try {
                        String[] parts = dateStr.substring(0, 10).split("-");
                        dateStr = parts[2] + "/" + parts[1] + "/" + parts[0];
                    } catch (Exception e) {}
                }
                writer.println(dateStr + "," + row.get("total"));
            }
            writer.println();

            writer.println("3. TỈ LỆ ĐẶT SÂN THEO LOẠI (30 NGÀY QUA)");
            writer.println("Loại sân,Số lượt đặt");
            for (Map<String, Object> row : typeChart) {
                writer.println(row.get("typeName") + "," + row.get("count"));
            }
        }
    }
}

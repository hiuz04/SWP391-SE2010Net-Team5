package com.swp.service;

import com.swp.dao.OwnerDashboardDAO;
import com.swp.dao.VoucherDAO;
import com.swp.model.dto.OwnerDashboardDTO;

public class OwnerDashboardService {

    private OwnerDashboardDAO dao = new OwnerDashboardDAO();
    private VoucherDAO voucherDAO = new VoucherDAO();

    public OwnerDashboardDTO getDashboard() {

        OwnerDashboardDTO dto = new OwnerDashboardDTO();

        //------------------
        // Booking
        //------------------

        int today = dao.getTodayBooking();

        int yesterday = dao.getYesterdayBooking();

        dto.setTodayBooking(today);

        dto.setBookingDifferent(today - yesterday);

        //------------------
        // Revenue
        //------------------

        long currentRevenue = dao.getCurrentMonthRevenue();
        long previousRevenue = dao.getPreviousMonthRevenue();
        dto.setMonthRevenue(currentRevenue);

        double growth = 0;
        if (previousRevenue > 0) {
            growth = (currentRevenue
                            - previousRevenue)
                            * 100.0
                            / previousRevenue;

        }
        dto.setRevenueGrowthPercent(growth);

        //------------------
        // Fields
        //------------------

        dto.setActiveFields(dao.getActiveFields());
        dto.setTotalFields(dao.getTotalFields());

        //------------------
        // Vouchers
        //------------------

        dto.setTotalVouchers(voucherDAO.countAllStatusVoucher());

        //------------------
        // Chart
        //------------------

        dto.setRevenue7Days(dao.getRevenueLast7Days());

        return dto;
    }
}

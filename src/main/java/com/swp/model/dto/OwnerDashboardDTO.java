package com.swp.model.dto;

import java.util.List;

public class OwnerDashboardDTO {
    private int todayBooking;
    private int bookingDifferent;
    private long monthRevenue;
    private double revenueGrowthPercent;
    private int activeFields;
    private int totalFields;
    private int totalVouchers;

    private List<RevenueDTO> revenue7Days;

    public OwnerDashboardDTO() {
    }

    public OwnerDashboardDTO(int todayBooking, int bookingDifferent, long monthRevenue, double revenueGrowthPercent, int activeFields, int totalFields, int totalVouchers, List<RevenueDTO> revenue7Days) {
        this.todayBooking = todayBooking;
        this.bookingDifferent = bookingDifferent;
        this.monthRevenue = monthRevenue;
        this.revenueGrowthPercent = revenueGrowthPercent;
        this.activeFields = activeFields;
        this.totalFields = totalFields;
        this.totalVouchers = totalVouchers;
        this.revenue7Days = revenue7Days;
    }

    public int getTodayBooking() {
        return todayBooking;
    }

    public void setTodayBooking(int todayBooking) {
        this.todayBooking = todayBooking;
    }

    public int getBookingDifferent() {
        return bookingDifferent;
    }

    public void setBookingDifferent(int bookingDifferent) {
        this.bookingDifferent = bookingDifferent;
    }

    public long getMonthRevenue() {
        return monthRevenue;
    }

    public void setMonthRevenue(long monthRevenue) {
        this.monthRevenue = monthRevenue;
    }

    public double getRevenueGrowthPercent() {
        return revenueGrowthPercent;
    }

    public void setRevenueGrowthPercent(double revenueGrowthPercent) {
        this.revenueGrowthPercent = revenueGrowthPercent;
    }

    public int getActiveFields() {
        return activeFields;
    }

    public void setActiveFields(int activeFields) {
        this.activeFields = activeFields;
    }

    public int getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(int totalFields) {
        this.totalFields = totalFields;
    }

    public int getTotalVouchers() {
        return totalVouchers;
    }

    public void setTotalVouchers(int totalVouchers) {
        this.totalVouchers = totalVouchers;
    }

    public List<RevenueDTO> getRevenue7Days() {
        return revenue7Days;
    }

    public void setRevenue7Days(List<RevenueDTO> revenue7Days) {
        this.revenue7Days = revenue7Days;
    }
}

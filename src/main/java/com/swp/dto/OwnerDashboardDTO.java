package com.swp.dto;

public class OwnerDashboardDTO {

    private double totalRevenue;
    private int totalField;
    private int totalCustomer;
    private int totalBooking;

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalField() {
        return totalField;
    }

    public void setTotalField(int totalField) {
        this.totalField = totalField;
    }

    public int getTotalCustomer() {
        return totalCustomer;
    }

    public void setTotalCustomer(int totalCustomer) {
        this.totalCustomer = totalCustomer;
    }

    public int getTotalBooking() {
        return totalBooking;
    }

    public void setTotalBooking(int totalBooking) {
        this.totalBooking = totalBooking;
    }
}

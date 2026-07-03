package com.swp.model.dto;

import java.time.LocalDate;

public class RevenueDTO {

    private LocalDate date;
    private long revenue;

    public RevenueDTO() {}

    public RevenueDTO(LocalDate date, long revenue) {
        this.date = date;
        this.revenue = revenue;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getRevenue() {
        return revenue;
    }

    public void setRevenue(long revenue) {
        this.revenue = revenue;
    }
}

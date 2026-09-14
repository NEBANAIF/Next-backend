package com.ousman.dto;

/** Revenue/quantity totals for one product within the requested date range. */
public class AnalyticsProductStat {

    private String name;
    private Double revenue;
    private Integer quantity;

    public AnalyticsProductStat() {}

    public AnalyticsProductStat(String name, Double revenue, Integer quantity) {
        this.name = name;
        this.revenue = revenue;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getRevenue() { return revenue; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}

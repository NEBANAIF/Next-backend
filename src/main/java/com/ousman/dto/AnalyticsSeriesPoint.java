package com.ousman.dto;

/**
 * One bucket in the Analytics revenue/quantity time series
 * (one per hour, day, or month depending on the requested granularity).
 */
public class AnalyticsSeriesPoint {

    private String label;   // display label, e.g. "14:00", "Jul 18", "2026-07"
    private String dateKey; // sortable key, e.g. "2026-07-18" or "2026-07-18T14"
    private Double revenue;
    private Integer quantity;
    private Double profit; // no COGS tracked yet — currently equals revenue
    private Double loss;   // no COGS tracked yet — currently always 0

    public AnalyticsSeriesPoint() {}

    public AnalyticsSeriesPoint(String label, String dateKey, Double revenue, Integer quantity, Double profit, Double loss) {
        this.label = label;
        this.dateKey = dateKey;
        this.revenue = revenue;
        this.quantity = quantity;
        this.profit = profit;
        this.loss = loss;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDateKey() { return dateKey; }
    public void setDateKey(String dateKey) { this.dateKey = dateKey; }

    public Double getRevenue() { return revenue; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getProfit() { return profit; }
    public void setProfit(Double profit) { this.profit = profit; }

    public Double getLoss() { return loss; }
    public void setLoss(Double loss) { this.loss = loss; }
}

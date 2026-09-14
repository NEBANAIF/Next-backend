package com.ousman.dto.analytics;

public class TimeSeriesPoint {

    private String label;
    /** ISO local date yyyy-MM-dd when applicable */
    private String dateKey;
    private Integer hour;
    private Double revenue;
    private Long quantity;
    /** For merged P&amp;L charts */
    private Double profit;
    private Double loss;

    public TimeSeriesPoint() {}

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDateKey() { return dateKey; }
    public void setDateKey(String dateKey) { this.dateKey = dateKey; }

    public Integer getHour() { return hour; }
    public void setHour(Integer hour) { this.hour = hour; }

    public Double getRevenue() { return revenue; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }

    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }

    public Double getProfit() { return profit; }
    public void setProfit(Double profit) { this.profit = profit; }

    public Double getLoss() { return loss; }
    public void setLoss(Double loss) { this.loss = loss; }
}

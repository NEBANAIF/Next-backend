package com.ousman.dto;

import java.util.List;

/** Response body for GET /api/analytics/dashboard — shape matches what Analytics.jsx expects. */
public class AnalyticsDashboardResponse {

    private Double totalRevenue;
    private Long saleCount;
    private Long totalQuantity;
    private Double avgOrderValue;
    private Double cogs;
    private Double expenses;
    private Double grossProfit;
    private Double grossMarginPct;
    private Double netProfit;
    private Double netMarginPct;
    private List<AnalyticsProductStat> topProducts;
    private List<AnalyticsSeriesPoint> series;

    public AnalyticsDashboardResponse() {}

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getSaleCount() { return saleCount; }
    public void setSaleCount(Long saleCount) { this.saleCount = saleCount; }

    public Long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Long totalQuantity) { this.totalQuantity = totalQuantity; }

    public Double getAvgOrderValue() { return avgOrderValue; }
    public void setAvgOrderValue(Double avgOrderValue) { this.avgOrderValue = avgOrderValue; }

    public Double getCogs() { return cogs; }
    public void setCogs(Double cogs) { this.cogs = cogs; }

    public Double getExpenses() { return expenses; }
    public void setExpenses(Double expenses) { this.expenses = expenses; }

    public Double getGrossProfit() { return grossProfit; }
    public void setGrossProfit(Double grossProfit) { this.grossProfit = grossProfit; }

    public Double getGrossMarginPct() { return grossMarginPct; }
    public void setGrossMarginPct(Double grossMarginPct) { this.grossMarginPct = grossMarginPct; }

    public Double getNetProfit() { return netProfit; }
    public void setNetProfit(Double netProfit) { this.netProfit = netProfit; }

    public Double getNetMarginPct() { return netMarginPct; }
    public void setNetMarginPct(Double netMarginPct) { this.netMarginPct = netMarginPct; }

    public List<AnalyticsProductStat> getTopProducts() { return topProducts; }
    public void setTopProducts(List<AnalyticsProductStat> topProducts) { this.topProducts = topProducts; }

    public List<AnalyticsSeriesPoint> getSeries() { return series; }
    public void setSeries(List<AnalyticsSeriesPoint> series) { this.series = series; }
}
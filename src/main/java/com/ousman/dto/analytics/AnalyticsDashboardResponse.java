package com.ousman.dto.analytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Database-backed analytics payload shared by dashboard, finance, and analytics UI.
 */
public class AnalyticsDashboardResponse {

    private String from;
    private String to;
    private String granularity;

    private double totalRevenue;
    private long totalQuantity;
    private long saleCount;
    private double cogs;
    private double expenses;
    private double grossProfit;
    private double netProfit;
    private double grossMarginPct;
    private double netMarginPct;
    private double avgOrderValue;

    private double inventoryValue;
    private long productsInStock;
    private long productsLowStock;
    private long productsOutOfStock;
    private long productsHighStock;

    private List<TimeSeriesPoint> series;

    private List<TopProductEntry> topProducts = new ArrayList<>();   // ← NEW

    public AnalyticsDashboardResponse() {}

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(long totalQuantity) { this.totalQuantity = totalQuantity; }

    public long getSaleCount() { return saleCount; }
    public void setSaleCount(long saleCount) { this.saleCount = saleCount; }

    public double getCogs() { return cogs; }
    public void setCogs(double cogs) { this.cogs = cogs; }

    public double getExpenses() { return expenses; }
    public void setExpenses(double expenses) { this.expenses = expenses; }

    public double getGrossProfit() { return grossProfit; }
    public void setGrossProfit(double grossProfit) { this.grossProfit = grossProfit; }

    public double getNetProfit() { return netProfit; }
    public void setNetProfit(double netProfit) { this.netProfit = netProfit; }

    public double getGrossMarginPct() { return grossMarginPct; }
    public void setGrossMarginPct(double grossMarginPct) { this.grossMarginPct = grossMarginPct; }

    public double getNetMarginPct() { return netMarginPct; }
    public void setNetMarginPct(double netMarginPct) { this.netMarginPct = netMarginPct; }

    public double getAvgOrderValue() { return avgOrderValue; }
    public void setAvgOrderValue(double avgOrderValue) { this.avgOrderValue = avgOrderValue; }

    public double getInventoryValue() { return inventoryValue; }
    public void setInventoryValue(double inventoryValue) { this.inventoryValue = inventoryValue; }

    public long getProductsInStock() { return productsInStock; }
    public void setProductsInStock(long productsInStock) { this.productsInStock = productsInStock; }

    public long getProductsLowStock() { return productsLowStock; }
    public void setProductsLowStock(long productsLowStock) { this.productsLowStock = productsLowStock; }

    public long getProductsOutOfStock() { return productsOutOfStock; }
    public void setProductsOutOfStock(long productsOutOfStock) { this.productsOutOfStock = productsOutOfStock; }

    public long getProductsHighStock() { return productsHighStock; }
    public void setProductsHighStock(long productsHighStock) { this.productsHighStock = productsHighStock; }

    public List<TimeSeriesPoint> getSeries() { return series; }
    public void setSeries(List<TimeSeriesPoint> series) { this.series = series; }

    // ── NEW ───────────────────────────────────────────────────────────────────
    public List<TopProductEntry> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProductEntry> topProducts) { this.topProducts = topProducts; }
}
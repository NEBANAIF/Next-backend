package com.ousman.dto.analytics;

public class TopProductEntry {

    private String name;
    private double revenue;
    private long   quantity;

    public TopProductEntry() {}

    public TopProductEntry(String name, double revenue, long quantity) {
        this.name     = name;
        this.revenue  = revenue;
        this.quantity = quantity;
    }

    public String getName()              { return name; }
    public void   setName(String v)      { this.name = v; }

    public double getRevenue()           { return revenue; }
    public void   setRevenue(double v)   { this.revenue = v; }

    public long   getQuantity()          { return quantity; }
    public void   setQuantity(long v)    { this.quantity = v; }
}
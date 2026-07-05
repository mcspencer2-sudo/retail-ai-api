package com.retailai.dto;

public class RetailerStatsDTO {

    private String retailer;
    private long scans;
    private long saves;
    private double conversionRate;

    private long orders;
    private double revenue;
    private double averageOrderValue;

    public RetailerStatsDTO() {
        this.retailer = "N/A";
    }

    public RetailerStatsDTO(
            String retailer,
            long scans,
            long saves,
            double conversionRate
    ) {
        this.retailer = cleanOrDefault(retailer, "N/A");
        this.scans = Math.max(0, scans);
        this.saves = Math.max(0, saves);
        this.conversionRate = normalizeRate(conversionRate);
    }

    public RetailerStatsDTO(
            String retailer,
            long scans,
            long saves,
            double conversionRate,
            long orders,
            double revenue,
            double averageOrderValue
    ) {
        this.retailer = cleanOrDefault(retailer, "N/A");
        this.scans = Math.max(0, scans);
        this.saves = Math.max(0, saves);
        this.conversionRate = normalizeRate(conversionRate);
        this.orders = Math.max(0, orders);
        this.revenue = normalizeMoney(revenue);
        this.averageOrderValue = normalizeMoney(averageOrderValue);
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = cleanOrDefault(retailer, "N/A");
    }

    public long getScans() {
        return scans;
    }

    public void setScans(long scans) {
        this.scans = Math.max(0, scans);
    }

    public long getSaves() {
        return saves;
    }

    public void setSaves(long saves) {
        this.saves = Math.max(0, saves);
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = normalizeRate(conversionRate);
    }

    public long getOrders() {
        return orders;
    }

    public void setOrders(long orders) {
        this.orders = Math.max(0, orders);
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = normalizeMoney(revenue);
    }

    public double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(double averageOrderValue) {
        this.averageOrderValue = normalizeMoney(averageOrderValue);
    }

    private double normalizeRate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }

        double clamped = Math.max(0.0, Math.min(100.0, value));
        return Math.round(clamped * 100.0) / 100.0;
    }

    private double normalizeMoney(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }

        return Math.round(Math.max(0.0, value) * 100.0) / 100.0;
    }

    private String cleanOrDefault(String value, String fallback) {
        if (value == null || value.trim().isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}
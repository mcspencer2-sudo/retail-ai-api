package com.retailai.dto;

public class AnalyticsSummaryDTO {

    private long totalScans;
    private long totalSaves;
    private double conversionRate;
    private String topRetailer;
    private String topScannedItem;
    private String topSavedItem;

    private long totalOrders;
    private double totalRevenue;
    private double averageOrderValue;
    private String topSellingItem;
    private long lowStockPriorityCount;

    public AnalyticsSummaryDTO() {
        this.topRetailer = "N/A";
        this.topScannedItem = "N/A";
        this.topSavedItem = "N/A";
        this.topSellingItem = "N/A";
    }

    public AnalyticsSummaryDTO(
            long totalScans,
            long totalSaves,
            double conversionRate,
            String topRetailer,
            String topScannedItem,
            String topSavedItem
    ) {
        this.totalScans = Math.max(0, totalScans);
        this.totalSaves = Math.max(0, totalSaves);
        this.conversionRate = normalizeRate(conversionRate);
        this.topRetailer = cleanOrDefault(topRetailer, "N/A");
        this.topScannedItem = cleanOrDefault(topScannedItem, "N/A");
        this.topSavedItem = cleanOrDefault(topSavedItem, "N/A");
        this.topSellingItem = "N/A";
    }

    public AnalyticsSummaryDTO(
            long totalScans,
            long totalSaves,
            double conversionRate,
            String topRetailer,
            String topScannedItem,
            String topSavedItem,
            long totalOrders,
            double totalRevenue,
            double averageOrderValue,
            String topSellingItem,
            long lowStockPriorityCount
    ) {
        this.totalScans = Math.max(0, totalScans);
        this.totalSaves = Math.max(0, totalSaves);
        this.conversionRate = normalizeRate(conversionRate);
        this.topRetailer = cleanOrDefault(topRetailer, "N/A");
        this.topScannedItem = cleanOrDefault(topScannedItem, "N/A");
        this.topSavedItem = cleanOrDefault(topSavedItem, "N/A");

        this.totalOrders = Math.max(0, totalOrders);
        this.totalRevenue = normalizeMoney(totalRevenue);
        this.averageOrderValue = normalizeMoney(averageOrderValue);
        this.topSellingItem = cleanOrDefault(topSellingItem, "N/A");
        this.lowStockPriorityCount = Math.max(0, lowStockPriorityCount);
    }

    public long getTotalScans() {
        return totalScans;
    }

    public void setTotalScans(long totalScans) {
        this.totalScans = Math.max(0, totalScans);
    }

    public long getTotalSaves() {
        return totalSaves;
    }

    public void setTotalSaves(long totalSaves) {
        this.totalSaves = Math.max(0, totalSaves);
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = normalizeRate(conversionRate);
    }

    public String getTopRetailer() {
        return topRetailer;
    }

    public void setTopRetailer(String topRetailer) {
        this.topRetailer = cleanOrDefault(topRetailer, "N/A");
    }

    public String getTopScannedItem() {
        return topScannedItem;
    }

    public void setTopScannedItem(String topScannedItem) {
        this.topScannedItem = cleanOrDefault(topScannedItem, "N/A");
    }

    public String getTopSavedItem() {
        return topSavedItem;
    }

    public void setTopSavedItem(String topSavedItem) {
        this.topSavedItem = cleanOrDefault(topSavedItem, "N/A");
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = Math.max(0, totalOrders);
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = normalizeMoney(totalRevenue);
    }

    public double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(double averageOrderValue) {
        this.averageOrderValue = normalizeMoney(averageOrderValue);
    }

    public String getTopSellingItem() {
        return topSellingItem;
    }

    public void setTopSellingItem(String topSellingItem) {
        this.topSellingItem = cleanOrDefault(topSellingItem, "N/A");
    }

    public long getLowStockPriorityCount() {
        return lowStockPriorityCount;
    }

    public void setLowStockPriorityCount(long lowStockPriorityCount) {
        this.lowStockPriorityCount = Math.max(0, lowStockPriorityCount);
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
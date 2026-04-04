package com.retailai.model;

public class AnalyticsSummaryDTO {
    private long totalScans;
    private long totalSaves;
    private double conversionRate;
    private String topScannedItem;
    private String topSavedItem;
    private String topRetailer;

    public AnalyticsSummaryDTO() {
    }

    public AnalyticsSummaryDTO(long totalScans, long totalSaves, double conversionRate,
                               String topScannedItem, String topSavedItem, String topRetailer) {
        this.totalScans = totalScans;
        this.totalSaves = totalSaves;
        this.conversionRate = conversionRate;
        this.topScannedItem = topScannedItem;
        this.topSavedItem = topSavedItem;
        this.topRetailer = topRetailer;
    }

    public long getTotalScans() {
        return totalScans;
    }

    public void setTotalScans(long totalScans) {
        this.totalScans = totalScans;
    }

    public long getTotalSaves() {
        return totalSaves;
    }

    public void setTotalSaves(long totalSaves) {
        this.totalSaves = totalSaves;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public String getTopScannedItem() {
        return topScannedItem;
    }

    public void setTopScannedItem(String topScannedItem) {
        this.topScannedItem = topScannedItem;
    }

    public String getTopSavedItem() {
        return topSavedItem;
    }

    public void setTopSavedItem(String topSavedItem) {
        this.topSavedItem = topSavedItem;
    }

    public String getTopRetailer() {
        return topRetailer;
    }

    public void setTopRetailer(String topRetailer) {
        this.topRetailer = topRetailer;
    }
}
package com.retailai.model;

public class RetailerStatsDTO {

    private String retailer;
    private long scans;
    private long saves;
    private double conversionRate;

    public RetailerStatsDTO() {
    }

    public RetailerStatsDTO(String retailer, long scans, long saves, double conversionRate) {
        this.retailer = retailer;
        this.scans = scans;
        this.saves = saves;
        this.conversionRate = conversionRate;
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = retailer;
    }

    public long getScans() {
        return scans;
    }

    public void setScans(long scans) {
        this.scans = scans;
    }

    public long getSaves() {
        return saves;
    }

    public void setSaves(long saves) {
        this.saves = saves;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }
}
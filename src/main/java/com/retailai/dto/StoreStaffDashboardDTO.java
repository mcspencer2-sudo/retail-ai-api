package com.retailai.dto;

import java.util.ArrayList;
import java.util.List;

public class StoreStaffDashboardDTO {

    private String retailerKey;
    private String storeCode;
    private String storeName;

    private long todaysScans;
    private long todaysSaves;
    private double conversionRate;

    private long totalInventoryItems;
    private long lowStockCount;
    private long outOfStockCount;

    private String topScannedItem;
    private String topSavedItem;

    private List<ActivityDTO> recentActivity = new ArrayList<>();

    public StoreStaffDashboardDTO() {
        this.retailerKey = "";
        this.storeCode = "";
        this.storeName = "";
        this.topScannedItem = "N/A";
        this.topSavedItem = "N/A";
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode);
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public long getTodaysScans() {
        return todaysScans;
    }

    public void setTodaysScans(long todaysScans) {
        this.todaysScans = Math.max(0, todaysScans);
    }

    public long getTodaysSaves() {
        return todaysSaves;
    }

    public void setTodaysSaves(long todaysSaves) {
        this.todaysSaves = Math.max(0, todaysSaves);
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        if (Double.isNaN(conversionRate) || Double.isInfinite(conversionRate)) {
            this.conversionRate = 0.0;
            return;
        }

        this.conversionRate = Math.max(0.0, conversionRate);
    }

    public long getTotalInventoryItems() {
        return totalInventoryItems;
    }

    public void setTotalInventoryItems(long totalInventoryItems) {
        this.totalInventoryItems = Math.max(0, totalInventoryItems);
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = Math.max(0, lowStockCount);
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = Math.max(0, outOfStockCount);
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

    public List<ActivityDTO> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<ActivityDTO> recentActivity) {
        this.recentActivity = recentActivity == null ? new ArrayList<>() : recentActivity;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
package com.retailai.dto;

import java.time.LocalDateTime;

public class ActivityDTO {

    private String eventType;
    private String retailer;
    private String item;
    private String timeAgo;
    private LocalDateTime createdAt;

    private String retailerKey;
    private String storeCode;
    private String storeName;
    private String rfid;
    private String category;

    public ActivityDTO() {
        this.eventType = "UNKNOWN";
        this.retailer = "Retailer";
        this.item = "Product";
        this.timeAgo = "Recently";
    }

    public ActivityDTO(
            String eventType,
            String retailer,
            String item,
            String timeAgo,
            LocalDateTime createdAt
    ) {
        this.eventType = cleanOrDefault(eventType, "UNKNOWN");
        this.retailer = cleanOrDefault(retailer, "Retailer");
        this.item = cleanOrDefault(item, "Product");
        this.timeAgo = cleanOrDefault(timeAgo, "Recently");
        this.createdAt = createdAt;
    }

    public ActivityDTO(
            String eventType,
            String retailer,
            String item,
            String timeAgo,
            LocalDateTime createdAt,
            String retailerKey,
            String storeCode,
            String storeName,
            String rfid,
            String category
    ) {
        this.eventType = cleanOrDefault(eventType, "UNKNOWN");
        this.retailer = cleanOrDefault(retailer, "Retailer");
        this.item = cleanOrDefault(item, "Product");
        this.timeAgo = cleanOrDefault(timeAgo, "Recently");
        this.createdAt = createdAt;
        this.retailerKey = clean(retailerKey);
        this.storeCode = clean(storeCode);
        this.storeName = clean(storeName);
        this.rfid = clean(rfid);
        this.category = clean(category);
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = cleanOrDefault(eventType, "UNKNOWN");
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = cleanOrDefault(retailer, "Retailer");
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = cleanOrDefault(item, "Product");
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = cleanOrDefault(timeAgo, "Recently");
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
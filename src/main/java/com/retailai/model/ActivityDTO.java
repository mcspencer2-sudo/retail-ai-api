package com.retailai.model;

public class ActivityDTO {
    private String retailer;
    private String item;
    private String eventType;
    private String timeAgo;
    private String createdAt;

    public ActivityDTO() {
    }

    public ActivityDTO(String retailer, String item, String eventType, String timeAgo, String createdAt) {
        this.retailer = retailer;
        this.item = item;
        this.eventType = eventType;
        this.timeAgo = timeAgo;
        this.createdAt = createdAt;
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = retailer;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
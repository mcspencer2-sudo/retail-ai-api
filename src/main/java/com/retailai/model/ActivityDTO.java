package com.retailai.model;

import java.time.LocalDateTime;

public class ActivityDTO {

    private String eventType;
    private String retailer;
    private String item;
    private String timeAgo;
    private LocalDateTime createdAt;

    public ActivityDTO() {
    }

    public ActivityDTO(String eventType, String retailer, String item, String timeAgo, LocalDateTime createdAt) {
        this.eventType = eventType;
        this.retailer = retailer;
        this.item = item;
        this.timeAgo = timeAgo;
        this.createdAt = createdAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
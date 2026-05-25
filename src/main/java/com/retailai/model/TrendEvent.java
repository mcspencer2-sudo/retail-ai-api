package com.retailai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trend_events")
public class TrendEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String retailerName;

    private String retailerKey;

    private String storeCode;

    private String itemName;

    private String eventType;

    private LocalDateTime createdAt;

    public TrendEvent() {
    }

    public TrendEvent(
            String retailerName,
            String itemName,
            String eventType,
            LocalDateTime createdAt
    ) {
        this.retailerName = retailerName;
        this.retailerKey = "";
        this.storeCode = "";
        this.itemName = itemName;
        this.eventType = eventType;
        this.createdAt = createdAt;
    }

    public TrendEvent(
            String retailerName,
            String retailerKey,
            String storeCode,
            String itemName,
            String eventType,
            LocalDateTime createdAt
    ) {
        this.retailerName = retailerName;
        this.retailerKey = retailerKey;
        this.storeCode = storeCode;
        this.itemName = itemName;
        this.eventType = eventType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = retailerName;
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = retailerKey;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
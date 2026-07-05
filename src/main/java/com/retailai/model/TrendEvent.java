package com.retailai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trend_events",
        indexes = {
                @Index(name = "idx_trend_event_type_created", columnList = "eventType,createdAt"),
                @Index(name = "idx_trend_retailer_store", columnList = "retailerKey,storeCode"),
                @Index(name = "idx_trend_store_created", columnList = "storeCode,createdAt"),
                @Index(name = "idx_trend_item", columnList = "itemName"),
                @Index(name = "idx_trend_rfid", columnList = "rfid"),
                @Index(name = "idx_trend_user", columnList = "userId"),
                @Index(name = "idx_trend_tenant", columnList = "tenantId")
        }
)
public class TrendEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String retailerName;

    private String retailerKey;

    private String storeCode;

    private String storeName;

    private String rfid;

    private String itemName;

    private String eventType;

    private String userId;

    private String tenantId;

    @Column(length = 1000)
    private String metadata;

    private LocalDateTime createdAt;

    public TrendEvent() {
    }

    public TrendEvent(
            String retailerName,
            String itemName,
            String eventType,
            LocalDateTime createdAt
    ) {
        this.retailerName = clean(retailerName);
        this.retailerKey = "";
        this.storeCode = "";
        this.storeName = "";
        this.rfid = "";
        this.itemName = clean(itemName);
        this.eventType = normalizeEventType(eventType);
        this.userId = "";
        this.tenantId = "";
        this.metadata = "";
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
        this.retailerName = clean(retailerName);
        this.retailerKey = cleanUpper(retailerKey);
        this.storeCode = cleanUpper(storeCode);
        this.storeName = "";
        this.rfid = "";
        this.itemName = clean(itemName);
        this.eventType = normalizeEventType(eventType);
        this.userId = "";
        this.tenantId = "";
        this.metadata = "";
        this.createdAt = createdAt;
    }

    public TrendEvent(
            String retailerName,
            String retailerKey,
            String storeCode,
            String storeName,
            String rfid,
            String itemName,
            String eventType,
            String userId,
            String tenantId,
            String metadata,
            LocalDateTime createdAt
    ) {
        this.retailerName = clean(retailerName);
        this.retailerKey = cleanUpper(retailerKey);
        this.storeCode = cleanUpper(storeCode);
        this.storeName = clean(storeName);
        this.rfid = clean(rfid);
        this.itemName = clean(itemName);
        this.eventType = normalizeEventType(eventType);
        this.userId = clean(userId);
        this.tenantId = clean(tenantId);
        this.metadata = clean(metadata);
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onCreate() {
        normalizeFields();

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        normalizeFields();

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getRetailerName() {
        return clean(retailerName);
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
    }

    public String getRetailerKey() {
        return clean(retailerKey);
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = cleanUpper(retailerKey);
    }

    public String getStoreCode() {
        return clean(storeCode);
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = cleanUpper(storeCode);
    }

    public String getStoreName() {
        return clean(storeName);
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public String getRfid() {
        return clean(rfid);
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public String getItemName() {
        return clean(itemName);
    }

    public void setItemName(String itemName) {
        this.itemName = clean(itemName);
    }

    public String getEventType() {
        return normalizeEventType(eventType);
    }

    public void setEventType(String eventType) {
        this.eventType = normalizeEventType(eventType);
    }

    public String getUserId() {
        return clean(userId);
    }

    public void setUserId(String userId) {
        this.userId = clean(userId);
    }

    public String getTenantId() {
        return clean(tenantId);
    }

    public void setTenantId(String tenantId) {
        this.tenantId = clean(tenantId);
    }

    public String getMetadata() {
        return clean(metadata);
    }

    public void setMetadata(String metadata) {
        this.metadata = clean(metadata);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private void normalizeFields() {
        retailerName = clean(retailerName);
        retailerKey = cleanUpper(retailerKey);
        storeCode = cleanUpper(storeCode);
        storeName = clean(storeName);
        rfid = clean(rfid);
        itemName = clean(itemName);
        eventType = normalizeEventType(eventType);
        userId = clean(userId);
        tenantId = clean(tenantId);
        metadata = clean(metadata);
    }

    private String normalizeEventType(String value) {
        String cleaned = clean(value).toUpperCase();

        if (cleaned.isBlank()) {
            return "UNKNOWN";
        }

        return cleaned;
    }

    private String cleanUpper(String value) {
        return clean(value).toUpperCase();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
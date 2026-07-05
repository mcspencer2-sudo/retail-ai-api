package com.retailai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "scan_history",
        indexes = {
                @Index(name = "idx_scan_history_user_id", columnList = "userId"),
                @Index(name = "idx_scan_history_tenant_id", columnList = "tenantId"),
                @Index(name = "idx_scan_history_retailer_store", columnList = "retailerKey,storeCode"),
                @Index(name = "idx_scan_history_created_at", columnList = "createdAt"),
                @Index(name = "idx_scan_history_user_store_created", columnList = "userId,retailerKey,storeCode,createdAt"),
                @Index(name = "idx_scan_history_tenant_store_created", columnList = "tenantId,retailerKey,storeCode,createdAt"),
                @Index(name = "idx_scan_history_rfid", columnList = "rfid")
        }
)
public class ScanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String userId = "";

    @Column(nullable = false, length = 120)
    private String tenantId = "";

    @Column(length = 120)
    private String storeId = "";

    @Column(length = 180)
    private String userEmail = "";

    @Column(nullable = false, length = 80)
    private String retailerKey = "";

    @Column(length = 180)
    private String retailerName = "";

    @Column(nullable = false, length = 80)
    private String storeCode = "";

    @Column(length = 180)
    private String storeName = "";

    @Column(nullable = false, length = 160)
    private String rfid = "";

    @Column(length = 255)
    private String itemName = "";

    @Column(length = 180)
    private String brand = "";

    @Column(length = 80)
    private String category = "";

    @Column(length = 80)
    private String color = "";

    @Column(nullable = false)
    private Double price = 0.0;

    @Column(length = 1200)
    private String imageUrl = "";

    @Column(length = 80)
    private String vibe = "";

    @Column(nullable = false)
    private Integer matchScore = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ScanHistory() {
    }

    @PrePersist
    public void prePersist() {
        userId = clean(userId);
        tenantId = clean(tenantId);
        storeId = clean(storeId);
        userEmail = clean(userEmail);

        retailerKey = clean(retailerKey).toUpperCase();
        retailerName = clean(retailerName);
        storeCode = clean(storeCode).toUpperCase();
        storeName = clean(storeName);

        rfid = clean(rfid);
        itemName = clean(itemName);
        brand = clean(brand);
        category = clean(category);
        color = clean(color);
        imageUrl = clean(imageUrl);
        vibe = clean(vibe);

        price = price == null ? 0.0 : Math.max(0.0, price);
        matchScore = clampMatchScore(matchScore);

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean hasRequiredScope() {
        return !getUserId().isBlank()
                && !getTenantId().isBlank()
                && !getRetailerKey().isBlank()
                && !getStoreCode().isBlank()
                && !getRfid().isBlank();
    }

    public String getDisplayName() {
        if (!getItemName().isBlank()) {
            return getItemName();
        }

        return getRfid();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStoreId() {
        return clean(storeId);
    }

    public void setStoreId(String storeId) {
        this.storeId = clean(storeId);
    }

    public String getUserEmail() {
        return clean(userEmail);
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = clean(userEmail);
    }

    public String getRetailerKey() {
        return clean(retailerKey);
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey).toUpperCase();
    }

    public String getRetailerName() {
        return clean(retailerName);
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
    }

    public String getStoreCode() {
        return clean(storeCode);
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode).toUpperCase();
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

    public String getName() {
        return getItemName();
    }

    public void setName(String name) {
        this.itemName = clean(name);
    }

    public String getBrand() {
        return clean(brand);
    }

    public void setBrand(String brand) {
        this.brand = clean(brand);
    }

    public String getCategory() {
        return clean(category);
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getColor() {
        return clean(color);
    }

    public void setColor(String color) {
        this.color = clean(color);
    }

    public Double getPrice() {
        return price == null ? 0.0 : price;
    }

    public void setPrice(Double price) {
        this.price = price == null ? 0.0 : Math.max(0.0, price);
    }

    public String getImageUrl() {
        return clean(imageUrl);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public String getVibe() {
        return clean(vibe);
    }

    public void setVibe(String vibe) {
        this.vibe = clean(vibe);
    }

    public Integer getMatchScore() {
        return matchScore == null ? 0 : matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = clampMatchScore(matchScore);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getScannedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.createdAt = scannedAt;
    }

    private Integer clampMatchScore(Integer value) {
        if (value == null) {
            return 0;
        }

        return Math.max(0, Math.min(100, value));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
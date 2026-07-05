package com.retailai.dto;

import com.retailai.model.ScanHistory;

import java.time.LocalDateTime;

public class ScanHistoryDTO {

    private Long id;

    private String userId = "";
    private String tenantId = "";
    private String storeId = "";
    private String userEmail = "";

    private String retailerKey = "";
    private String retailerName = "";
    private String storeCode = "";
    private String storeName = "";

    private String rfid = "";
    private String itemName = "";
    private String name = "";
    private String brand = "";
    private String category = "";
    private String color = "";
    private Double price = 0.0;
    private String imageUrl = "";
    private String vibe = "";
    private Integer matchScore = 0;

    private LocalDateTime createdAt;
    private LocalDateTime scannedAt;

    public ScanHistoryDTO() {
    }

    public static ScanHistoryDTO fromEntity(ScanHistory history) {
        ScanHistoryDTO dto = new ScanHistoryDTO();

        if (history == null) {
            return dto;
        }

        dto.setId(history.getId());

        dto.setUserId(history.getUserId());
        dto.setTenantId(history.getTenantId());
        dto.setStoreId(history.getStoreId());
        dto.setUserEmail(history.getUserEmail());

        dto.setRetailerKey(history.getRetailerKey());
        dto.setRetailerName(history.getRetailerName());
        dto.setStoreCode(history.getStoreCode());
        dto.setStoreName(history.getStoreName());

        dto.setRfid(history.getRfid());
        dto.setItemName(history.getItemName());
        dto.setBrand(history.getBrand());
        dto.setCategory(history.getCategory());
        dto.setColor(history.getColor());
        dto.setPrice(history.getPrice());
        dto.setImageUrl(history.getImageUrl());
        dto.setVibe(history.getVibe());
        dto.setMatchScore(history.getMatchScore());

        dto.setCreatedAt(history.getCreatedAt());
        dto.setScannedAt(history.getScannedAt());

        return dto;
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
        this.name = clean(itemName);
    }

    public String getName() {
        return clean(name).isBlank() ? getItemName() : clean(name);
    }

    public void setName(String name) {
        this.name = clean(name);
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
        int safeScore = matchScore == null ? 0 : matchScore;
        this.matchScore = Math.max(0, Math.min(100, safeScore));
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;

        if (this.scannedAt == null) {
            this.scannedAt = createdAt;
        }
    }

    public LocalDateTime getScannedAt() {
        return scannedAt == null ? createdAt : scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;

        if (this.createdAt == null) {
            this.createdAt = scannedAt;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
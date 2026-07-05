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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "bag_items",
        indexes = {
                @Index(name = "idx_bag_item_user_id", columnList = "userId"),
                @Index(name = "idx_bag_item_tenant_id", columnList = "tenantId"),
                @Index(name = "idx_bag_item_store_id", columnList = "storeId"),
                @Index(name = "idx_bag_item_email", columnList = "email"),
                @Index(name = "idx_bag_item_retailer_store", columnList = "retailerKey,storeCode"),
                @Index(name = "idx_bag_item_user_store", columnList = "userId,retailerKey,storeCode"),
                @Index(name = "idx_bag_item_tenant_store", columnList = "tenantId,retailerKey,storeCode"),
                @Index(name = "idx_bag_item_user_rfid", columnList = "userId,rfid"),
                @Index(name = "idx_bag_item_scope_rfid", columnList = "userId,tenantId,retailerKey,storeCode,rfid"),
                @Index(name = "idx_bag_item_created_at", columnList = "createdAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bag_item_user_store_rfid",
                        columnNames = {"userId", "retailerKey", "storeCode", "rfid"}
                )
        }
)
public class BagItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80)
    private String userId;

    @Column(length = 80)
    private String tenantId;

    @Column(length = 80)
    private String storeId;

    @Column(length = 180)
    private String email;

    @Column(nullable = false, length = 80)
    private String retailerKey;

    @Column(length = 160)
    private String retailerName;

    @Column(nullable = false, length = 80)
    private String storeCode;

    @Column(length = 160)
    private String storeName;

    @Column(nullable = false, length = 120)
    private String rfid;

    @Column(length = 220)
    private String itemName;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private Double price = 0.0;

    @Column(length = 80)
    private String category;

    @Column(length = 80)
    private String vibe = "";

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false, length = 40)
    private String source = "SCAN";

    @Column(length = 100)
    private String sourceOrderNumber = "";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public BagItem() {
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        normalizeFields();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeFields();
    }

    public Long getId() {
        return id;
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

    public String getEmail() {
        return clean(email);
    }

    public void setEmail(String email) {
        this.email = clean(email);
    }

    public String getUserEmail() {
        return clean(email);
    }

    public void setUserEmail(String userEmail) {
        this.email = clean(userEmail);
    }

    public String getRetailerKey() {
        return clean(retailerKey);
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = upper(retailerKey);
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
        this.storeCode = upper(storeCode);
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
        this.rfid = upper(rfid);
    }

    public String getItemName() {
        return clean(itemName);
    }

    public void setItemName(String itemName) {
        this.itemName = clean(itemName);
    }

    public String getImageUrl() {
        return clean(imageUrl);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public Double getPrice() {
        return money(price);
    }

    public void setPrice(Double price) {
        this.price = money(price);
    }

    public void setPrice(double price) {
        this.price = money(price);
    }

    public String getCategory() {
        return clean(category);
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getVibe() {
        return clean(vibe);
    }

    public void setVibe(String vibe) {
        this.vibe = clean(vibe);
    }

    public Integer getQuantity() {
        return quantity == null || quantity < 1 ? 1 : quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity == null || quantity < 1 ? 1 : quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity < 1 ? 1 : quantity;
    }

    public String getSource() {
        return clean(source).isBlank() ? "SCAN" : clean(source);
    }

    public void setSource(String source) {
        String cleaned = clean(source);
        this.source = cleaned.isBlank() ? "SCAN" : cleaned;
    }

    public String getSourceOrderNumber() {
        return clean(sourceOrderNumber);
    }

    public void setSourceOrderNumber(String sourceOrderNumber) {
        this.sourceOrderNumber = clean(sourceOrderNumber);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    private void normalizeFields() {
        userId = clean(userId);
        tenantId = clean(tenantId);
        storeId = clean(storeId);
        email = clean(email);

        retailerKey = upper(retailerKey);
        retailerName = clean(retailerName);
        storeCode = upper(storeCode);
        storeName = clean(storeName);

        rfid = upper(rfid);
        itemName = clean(itemName);
        imageUrl = clean(imageUrl);
        category = clean(category);
        vibe = clean(vibe);

        price = money(price);
        quantity = quantity == null || quantity < 1 ? 1 : quantity;

        source = clean(source).isBlank() ? "SCAN" : clean(source);
        sourceOrderNumber = clean(sourceOrderNumber);

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String upper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private Double money(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, Math.round(value * 100.0) / 100.0);
    }

    private Double money(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, Math.round(value * 100.0) / 100.0);
    }
}
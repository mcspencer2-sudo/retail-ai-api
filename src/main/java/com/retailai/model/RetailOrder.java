package com.retailai.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "retail_orders",
        indexes = {
                @Index(name = "idx_retail_order_order_number", columnList = "orderNumber"),
                @Index(name = "idx_retail_order_user_id", columnList = "userId"),
                @Index(name = "idx_retail_order_tenant_store", columnList = "tenantId,storeCode"),
                @Index(name = "idx_retail_order_retailer_store", columnList = "retailerKey,storeCode"),
                @Index(name = "idx_retail_order_created_at", columnList = "createdAt")
        }
)
public class RetailOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String orderNumber;

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

    @Column(nullable = false)
    private Integer itemCount = 0;

    @Column(nullable = false)
    private Double subtotal = 0.0;

    @Column(nullable = false)
    private Double tax = 0.0;

    @Column(nullable = false)
    private Double total = 0.0;

    @Column(nullable = false, length = 40)
    private String status = "COMPLETED";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<RetailOrderItem> items = new ArrayList<>();

    public RetailOrder() {
    }

    @PrePersist
    public void beforeCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "COMPLETED";
        }

        if (items == null) {
            items = new ArrayList<>();
        }

        itemCount = Math.max(0, itemCount == null ? items.size() : itemCount);
        subtotal = money(subtotal);
        tax = money(tax);
        total = money(total);
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = clean(orderNumber);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = clean(userId);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = clean(tenantId);
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = clean(storeId);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = clean(email);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey);
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
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

    public Integer getItemCount() {
        if (itemCount != null && itemCount >= 0) {
            return itemCount;
        }

        return items == null ? 0 : items.size();
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount == null ? 0 : Math.max(0, itemCount);
    }

    public Double getSubtotal() {
        return money(subtotal);
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = money(subtotal);
    }

    public Double getTax() {
        return money(tax);
    }

    public void setTax(Double tax) {
        this.tax = money(tax);
    }

    public Double getTotal() {
        return money(total);
    }

    public void setTotal(Double total) {
        this.total = money(total);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        String cleaned = clean(status);
        this.status = cleaned.isBlank() ? "COMPLETED" : cleaned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public List<RetailOrderItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }

        return items;
    }

    public void setItems(List<RetailOrderItem> items) {
        this.items = new ArrayList<>();

        if (items == null) {
            this.itemCount = 0;
            return;
        }

        for (RetailOrderItem item : items) {
            addItem(item);
        }

        this.itemCount = this.items.size();
    }

    public void addItem(RetailOrderItem item) {
        if (item == null) {
            return;
        }

        item.setOrder(this);
        getItems().add(item);
        this.itemCount = getItems().size();
    }

    public void removeItem(RetailOrderItem item) {
        if (item == null || items == null) {
            return;
        }

        if (items.remove(item)) {
            item.setOrder(null);
        }

        this.itemCount = items.size();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Double money(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, value);
    }
}
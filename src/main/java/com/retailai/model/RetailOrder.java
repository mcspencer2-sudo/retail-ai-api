package com.retailai.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
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
                @Index(name = "idx_retail_order_retailer_store", columnList = "retailerKey,storeCode")
        }
)
public class RetailOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;

    private String userId;
    private String tenantId;
    private String storeId;
    private String email;

    private String retailerKey;
    private String retailerName;
    private String storeCode;
    private String storeName;

    private Integer itemCount;

    private Double subtotal;
    private Double tax;
    private Double total;

    private String status;

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
        return itemCount == null ? 0 : itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount == null ? 0 : Math.max(0, itemCount);
    }

    public Double getSubtotal() {
        return subtotal == null ? 0.0 : subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal == null ? 0.0 : subtotal;
    }

    public Double getTax() {
        return tax == null ? 0.0 : tax;
    }

    public void setTax(Double tax) {
        this.tax = tax == null ? 0.0 : tax;
    }

    public Double getTotal() {
        return total == null ? 0.0 : total;
    }

    public void setTotal(Double total) {
        this.total = total == null ? 0.0 : total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = clean(status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<RetailOrderItem> getItems() {
        return items;
    }

    public void setItems(List<RetailOrderItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public void addItem(RetailOrderItem item) {
        if (item == null) {
            return;
        }

        item.setOrder(this);
        this.items.add(item);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
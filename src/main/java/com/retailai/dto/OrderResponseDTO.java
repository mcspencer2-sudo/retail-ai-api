package com.retailai.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderResponseDTO {

    private Long id;
    private String orderNumber;

    private String status;

    private String retailerKey;
    private String retailerName;
    private String storeCode;
    private String storeName;

    private Integer itemCount;

    private Double subtotal;
    private Double tax;
    private Double total;

    private LocalDateTime createdAt;

    private List<OrderItemDTO> items = new ArrayList<>();

    public OrderResponseDTO() {
        this.orderNumber = "";
        this.status = "COMPLETED";
        this.retailerKey = "";
        this.retailerName = "";
        this.storeCode = "";
        this.storeName = "";
        this.itemCount = 0;
        this.subtotal = 0.0;
        this.tax = 0.0;
        this.total = 0.0;
        this.createdAt = LocalDateTime.now();
        this.items = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = clean(orderNumber);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        String cleaned = clean(status);
        this.status = cleaned.isBlank() ? "COMPLETED" : cleaned;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public List<OrderItemDTO> getItems() {
        return items == null ? new ArrayList<>() : items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items == null ? new ArrayList<>() : items;
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
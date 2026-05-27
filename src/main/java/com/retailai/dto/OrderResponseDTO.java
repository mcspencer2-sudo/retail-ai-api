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
        this.status = clean(status);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
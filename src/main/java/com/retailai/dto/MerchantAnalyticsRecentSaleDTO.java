package com.retailai.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MerchantAnalyticsRecentSaleDTO {

    private String orderNumber = "";
    private String receiptNumber = "";
    private String customerName = "";
    private LocalDateTime createdAt;
    private String status = "";
    private Integer itemCount = 0;
    private Double subtotal = 0.0;
    private Double tax = 0.0;
    private Double total = 0.0;
    private List<String> itemNames = new ArrayList<>();

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = clean(orderNumber);
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = clean(receiptNumber);
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = clean(customerName);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = clean(status);
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount == null ? 0 : Math.max(0, itemCount);
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal == null ? 0.0 : Math.max(0.0, subtotal);
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax == null ? 0.0 : Math.max(0.0, tax);
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total == null ? 0.0 : Math.max(0.0, total);
    }

    public List<String> getItemNames() {
        return itemNames;
    }

    public void setItemNames(List<String> itemNames) {
        this.itemNames = itemNames == null ? new ArrayList<>() : itemNames;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
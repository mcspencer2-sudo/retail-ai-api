package com.retailai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "retail_order_items",
        indexes = {
                @Index(name = "idx_retail_order_item_rfid", columnList = "rfid"),
                @Index(name = "idx_retail_order_item_retailer_store", columnList = "retailerKey,storeCode")
        }
)
public class RetailOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rfid;

    private String itemName;
    private String category;
    private String imageUrl;

    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;

    private String retailerKey;
    private String retailerName;
    private String storeCode;
    private String storeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private RetailOrder order;

    public RetailOrderItem() {
    }

    public Long getId() {
        return id;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = clean(itemName);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public Double getUnitPrice() {
        return unitPrice == null ? 0.0 : unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice == null ? 0.0 : unitPrice;
    }

    public Integer getQuantity() {
        return quantity == null ? 1 : quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity == null ? 1 : Math.max(1, quantity);
    }

    public Double getLineTotal() {
        return lineTotal == null ? 0.0 : lineTotal;
    }

    public void setLineTotal(Double lineTotal) {
        this.lineTotal = lineTotal == null ? 0.0 : lineTotal;
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

    public RetailOrder getOrder() {
        return order;
    }

    public void setOrder(RetailOrder order) {
        this.order = order;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
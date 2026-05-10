package com.retailai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Product {

    @Id
    private String rfid;

    private String retailerKey;
    private String retailerName;
    private String storeCode;
    private String storeName;

    private String itemName;
    private String brand;
    private String category;
    private String color;
    private String imageUrl;

    private Double price;
    private Integer stockQuantity;
    private Boolean active;
    private Boolean inStoreOnly;
    private Boolean available;

    public Product() {
    }

    public Product(
            String rfid,
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName,
            String itemName,
            String brand,
            String category,
            String color,
            String imageUrl,
            Double price,
            Integer stockQuantity,
            Boolean active,
            Boolean inStoreOnly,
            Boolean available
    ) {
        this.rfid = rfid;
        this.retailerKey = retailerKey;
        this.retailerName = retailerName;
        this.storeCode = storeCode;
        this.storeName = storeName;
        this.itemName = itemName;
        this.brand = brand;
        this.category = category;
        this.color = color;
        this.imageUrl = imageUrl;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = active;
        this.inStoreOnly = inStoreOnly;
        this.available = available;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = retailerKey;
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = retailerName;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getInStoreOnly() {
        return inStoreOnly;
    }

    public void setInStoreOnly(Boolean inStoreOnly) {
        this.inStoreOnly = inStoreOnly;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public boolean isAvailableForStyling() {
        return Boolean.TRUE.equals(active)
                && Boolean.TRUE.equals(available)
                && stockQuantity != null
                && stockQuantity > 0;
    }
}
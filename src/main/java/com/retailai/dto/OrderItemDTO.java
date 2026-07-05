package com.retailai.dto;

public class OrderItemDTO {

    private String rfid;
    private String itemName;
    private String category;
    private String imageUrl;
    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;
    private String retailerName;
    private String retailerKey;
    private String storeName;
    private String storeCode;

    public OrderItemDTO() {
        this.rfid = "";
        this.itemName = "";
        this.category = "";
        this.imageUrl = "";
        this.unitPrice = 0.0;
        this.quantity = 1;
        this.lineTotal = null;
        this.retailerName = "";
        this.retailerKey = "";
        this.storeName = "";
        this.storeCode = "";
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
        return money(unitPrice);
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = money(unitPrice);
    }

    public Integer getQuantity() {
        return quantity == null ? 1 : Math.max(1, quantity);
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity == null ? 1 : Math.max(1, quantity);
    }

    public Double getLineTotal() {
        if (lineTotal != null) {
            return money(lineTotal);
        }

        return money(getUnitPrice() * getQuantity());
    }

    public void setLineTotal(Double lineTotal) {
        this.lineTotal = lineTotal == null ? null : money(lineTotal);
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey);
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode);
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
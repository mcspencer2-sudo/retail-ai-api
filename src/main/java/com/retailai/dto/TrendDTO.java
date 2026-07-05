package com.retailai.dto;

public class TrendDTO {

    private String store;
    private String item;
    private int count;

    private String retailerKey;
    private String storeCode;
    private String category;
    private String rfid;
    private String trendType;

    public TrendDTO() {
        this.store = "Retailer";
        this.item = "Product";
        this.trendType = "SAVE";
    }

    public TrendDTO(String store, String item, int count) {
        this.store = cleanOrDefault(store, "Retailer");
        this.item = cleanOrDefault(item, "Product");
        this.count = Math.max(0, count);
        this.trendType = "SAVE";
    }

    public TrendDTO(
            String store,
            String item,
            int count,
            String retailerKey,
            String storeCode,
            String category,
            String rfid,
            String trendType
    ) {
        this.store = cleanOrDefault(store, "Retailer");
        this.item = cleanOrDefault(item, "Product");
        this.count = Math.max(0, count);
        this.retailerKey = clean(retailerKey);
        this.storeCode = clean(storeCode);
        this.category = clean(category);
        this.rfid = clean(rfid);
        this.trendType = cleanOrDefault(trendType, "SAVE");
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = cleanOrDefault(store, "Retailer");
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = cleanOrDefault(item, "Product");
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public String getTrendType() {
        return trendType;
    }

    public void setTrendType(String trendType) {
        this.trendType = cleanOrDefault(trendType, "SAVE");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
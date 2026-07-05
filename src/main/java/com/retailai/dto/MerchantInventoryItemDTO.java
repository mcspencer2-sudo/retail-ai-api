package com.retailai.dto;

public class MerchantInventoryItemDTO {

    private String rfid = "";
    private String itemName = "";
    private String brand = "";
    private String category = "";
    private String color = "";
    private Double price = 0.0;
    private String imageUrl = "";
    private Integer stockQuantity = 0;

    private String retailerName = "";
    private String retailerKey = "";
    private String storeName = "";
    private String storeCode = "";

    private Boolean available = false;
    private Boolean active = true;
    private Boolean synced = true;

    private Boolean lowStock = false;
    private Boolean outOfStock = false;
    private Integer reorderThreshold = 3;
    private Integer idealStockLevel = 12;
    private Integer suggestedReorderQuantity = 0;
    private String inventoryAlert = "Stock level is healthy.";

    private Double inventoryValue = 0.0;

    private String size = "";
    private String fit = "";
    private String material = "";
    private String gender = "";
    private String season = "";
    private String occasion = "";
    private String styleTags = "";
    private String pattern = "";

    public MerchantInventoryItemDTO() {
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

    public String getName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = clean(itemName);
    }

    public void setName(String name) {
        this.itemName = clean(name);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = clean(brand);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = clean(color);
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price == null ? 0.0 : Math.max(0.0, price);
        recomputeInventoryValue();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public Integer getStock() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity == null ? 0 : Math.max(0, stockQuantity);
        recomputeInventoryStatus();
        recomputeInventoryValue();
    }

    public void setStock(Integer stock) {
        setStockQuantity(stock);
    }

    public String getRetailerName() {
        return retailerName;
    }

    public String getRetailer() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
    }

    public void setRetailer(String retailer) {
        this.retailerName = clean(retailer);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey).toUpperCase();
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
        this.storeCode = clean(storeCode).toUpperCase();
    }

    public Boolean getAvailable() {
        return available;
    }

    public Boolean isAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available != null && available;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active == null || active;
    }

    public Boolean getSynced() {
        return synced;
    }

    public Boolean isSynced() {
        return synced;
    }

    public void setSynced(Boolean synced) {
        this.synced = synced == null || synced;
    }

    public Boolean getLowStock() {
        return lowStock;
    }

    public Boolean isLowStock() {
        return lowStock;
    }

    public void setLowStock(Boolean lowStock) {
        this.lowStock = lowStock != null && lowStock;
    }

    public Boolean getOutOfStock() {
        return outOfStock;
    }

    public Boolean isOutOfStock() {
        return outOfStock;
    }

    public void setOutOfStock(Boolean outOfStock) {
        this.outOfStock = outOfStock != null && outOfStock;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold == null ? 3 : Math.max(0, reorderThreshold);
        recomputeInventoryStatus();
    }

    public Integer getIdealStockLevel() {
        return idealStockLevel;
    }

    public void setIdealStockLevel(Integer idealStockLevel) {
        this.idealStockLevel = idealStockLevel == null ? 12 : Math.max(0, idealStockLevel);
        recomputeInventoryStatus();
    }

    public Integer getSuggestedReorderQuantity() {
        return suggestedReorderQuantity;
    }

    public void setSuggestedReorderQuantity(Integer suggestedReorderQuantity) {
        this.suggestedReorderQuantity = suggestedReorderQuantity == null
                ? 0
                : Math.max(0, suggestedReorderQuantity);
    }

    public String getInventoryAlert() {
        return inventoryAlert;
    }

    public void setInventoryAlert(String inventoryAlert) {
        String cleaned = clean(inventoryAlert);
        this.inventoryAlert = cleaned.isBlank() ? "Stock level is healthy." : cleaned;
    }

    public Double getInventoryValue() {
        return inventoryValue;
    }

    public void setInventoryValue(Double inventoryValue) {
        this.inventoryValue = inventoryValue == null ? 0.0 : Math.max(0.0, inventoryValue);
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = clean(size);
    }

    public String getFit() {
        return fit;
    }

    public void setFit(String fit) {
        this.fit = clean(fit);
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = clean(material);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = clean(gender);
    }

    public String getGenderStyle() {
        return gender;
    }

    public void setGenderStyle(String genderStyle) {
        this.gender = clean(genderStyle);
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = clean(season);
    }

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = clean(occasion);
    }

    public String getStyleTags() {
        return styleTags;
    }

    public void setStyleTags(String styleTags) {
        this.styleTags = clean(styleTags);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = clean(pattern);
    }

    public void recomputeInventoryStatus() {
        int stock = stockQuantity == null ? 0 : stockQuantity;
        int threshold = reorderThreshold == null ? 3 : reorderThreshold;
        int ideal = idealStockLevel == null ? 12 : idealStockLevel;

        this.outOfStock = stock <= 0;
        this.lowStock = stock > 0 && stock <= threshold;
        this.suggestedReorderQuantity = Math.max(0, ideal - stock);

        if (Boolean.TRUE.equals(outOfStock)) {
            this.inventoryAlert = "Out of stock — reorder immediately.";
        } else if (Boolean.TRUE.equals(lowStock)) {
            this.inventoryAlert = "Low stock — suggested reorder: "
                    + this.suggestedReorderQuantity
                    + " units.";
        } else {
            this.inventoryAlert = "Stock level is healthy.";
        }
    }

    private void recomputeInventoryValue() {
        int stock = stockQuantity == null ? 0 : stockQuantity;
        double safePrice = price == null ? 0.0 : price;

        this.inventoryValue = Math.max(0.0, stock * safePrice);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
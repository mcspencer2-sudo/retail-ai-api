package com.retailai.dto;

public class MerchantLowStockItemDTO {

    private String rfid = "";
    private String itemName = "";
    private String brand = "";
    private String category = "";
    private String color = "";
    private Integer stockQuantity = 0;
    private Integer reorderThreshold = 3;
    private Integer suggestedReorderQuantity = 0;
    private Double price = 0.0;
    private Double inventoryValueAtRisk = 0.0;

    public MerchantLowStockItemDTO() {
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid).toUpperCase();
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

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity == null ? 0 : Math.max(0, stockQuantity);
        recomputeInventoryValueAtRisk();
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold == null ? 3 : Math.max(0, reorderThreshold);
    }

    public Integer getSuggestedReorderQuantity() {
        return suggestedReorderQuantity;
    }

    public void setSuggestedReorderQuantity(Integer suggestedReorderQuantity) {
        this.suggestedReorderQuantity = suggestedReorderQuantity == null
                ? 0
                : Math.max(0, suggestedReorderQuantity);
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price == null ? 0.0 : Math.max(0.0, price);
        recomputeInventoryValueAtRisk();
    }

    public Double getInventoryValueAtRisk() {
        return inventoryValueAtRisk;
    }

    public void setInventoryValueAtRisk(Double inventoryValueAtRisk) {
        this.inventoryValueAtRisk = inventoryValueAtRisk == null
                ? 0.0
                : Math.max(0.0, inventoryValueAtRisk);
    }

    private void recomputeInventoryValueAtRisk() {
        int stock = stockQuantity == null ? 0 : stockQuantity;
        double safePrice = price == null ? 0.0 : price;

        this.inventoryValueAtRisk = Math.max(0.0, stock * safePrice);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
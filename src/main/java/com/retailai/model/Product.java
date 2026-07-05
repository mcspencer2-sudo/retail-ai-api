package com.retailai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_retailer_key", columnList = "retailerKey"),
                @Index(name = "idx_product_store_code", columnList = "storeCode"),
                @Index(name = "idx_product_retailer_store", columnList = "retailerKey,storeCode"),
                @Index(name = "idx_product_category", columnList = "category"),
                @Index(name = "idx_product_brand", columnList = "brand"),
                @Index(name = "idx_product_active_available_stock", columnList = "active,available,stockQuantity"),
                @Index(name = "idx_product_retailer_store_category", columnList = "retailerKey,storeCode,category")
        }
)
public class Product {

    @Id
    @Column(nullable = false, length = 120)
    private String rfid;

    @Column(length = 80)
    private String retailerKey;

    @Column(length = 160)
    private String retailerName;

    @Column(length = 120)
    private String storeCode;

    @Column(length = 180)
    private String storeName;

    @Column(length = 220)
    private String itemName;

    @Column(length = 160)
    private String brand;

    @Column(length = 80)
    private String category;

    @Column(length = 80)
    private String color;

    @Column(length = 1000)
    private String imageUrl;

    private Double price;
    private Integer stockQuantity;
    private Boolean active;
    private Boolean inStoreOnly;
    private Boolean available;

    @Column(length = 80)
    private String size;

    @Column(length = 120)
    private String fit;

    @Column(length = 180)
    private String material;

    @Column(length = 80)
    private String gender;

    @Column(length = 120)
    private String season;

    @Column(length = 220)
    private String occasion;

    @Column(length = 1000)
    private String styleTags;

    @Column(length = 120)
    private String pattern;

    public Product() {
        this.price = 0.0;
        this.stockQuantity = 0;
        this.active = true;
        this.inStoreOnly = false;
        this.available = false;
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
            Boolean available,
            String size,
            String fit,
            String material,
            String gender,
            String season,
            String occasion,
            String styleTags,
            String pattern
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
        this.size = size;
        this.fit = fit;
        this.material = material;
        this.gender = gender;
        this.season = season;
        this.occasion = occasion;
        this.styleTags = styleTags;
        this.pattern = pattern;
    }

    @PrePersist
    public void onCreate() {
        normalizeForSave();
    }

    @PreUpdate
    public void onUpdate() {
        normalizeForSave();
    }

    private void normalizeForSave() {
        rfid = cleanUpper(rfid);
        retailerKey = cleanUpper(retailerKey);
        storeCode = cleanUpper(storeCode);

        retailerName = clean(retailerName);
        storeName = clean(storeName);
        itemName = clean(itemName);
        brand = clean(brand);
        category = normalizeCategoryForDisplay(category);
        color = clean(color);
        imageUrl = clean(imageUrl);

        size = clean(size);
        fit = clean(fit);
        material = clean(material);
        gender = clean(gender);
        season = clean(season);
        occasion = clean(occasion);
        styleTags = clean(styleTags);
        pattern = clean(pattern);

        if (price == null || price < 0) {
            price = 0.0;
        }

        if (stockQuantity == null || stockQuantity < 0) {
            stockQuantity = 0;
        }

        if (active == null) {
            active = true;
        }

        if (inStoreOnly == null) {
            inStoreOnly = false;
        }

        available = Boolean.TRUE.equals(active) && stockQuantity > 0;
    }

    public String getRfid() {
        return clean(rfid);
    }

    public void setRfid(String rfid) {
        this.rfid = cleanUpper(rfid);
    }

    public String getRetailerKey() {
        return clean(retailerKey);
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = cleanUpper(retailerKey);
    }

    public String getRetailerName() {
        return clean(retailerName);
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
    }

    public String getStoreCode() {
        return clean(storeCode);
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = cleanUpper(storeCode);
    }

    public String getStoreName() {
        return clean(storeName);
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public String getItemName() {
        return clean(itemName);
    }

    public void setItemName(String itemName) {
        this.itemName = clean(itemName);
    }

    public String getBrand() {
        return clean(brand);
    }

    public void setBrand(String brand) {
        this.brand = clean(brand);
    }

    public String getCategory() {
        return clean(category);
    }

    public void setCategory(String category) {
        this.category = normalizeCategoryForDisplay(category);
    }

    public String getColor() {
        return clean(color);
    }

    public void setColor(String color) {
        this.color = clean(color);
    }

    public String getImageUrl() {
        return clean(imageUrl);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public Double getPrice() {
        return price == null ? 0.0 : price;
    }

    public void setPrice(Double price) {
        this.price = price == null ? 0.0 : Math.max(0.0, price);
    }

    public Integer getStockQuantity() {
        return stockQuantity == null ? 0 : stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity == null ? 0 : Math.max(0, stockQuantity);
        this.available = Boolean.TRUE.equals(getActive()) && this.stockQuantity > 0;
    }

    public Boolean getActive() {
        return active == null ? Boolean.TRUE : active;
    }

    public void setActive(Boolean active) {
        this.active = active == null ? Boolean.TRUE : active;
        this.available = Boolean.TRUE.equals(this.active) && getStockQuantity() > 0;
    }

    public Boolean getInStoreOnly() {
        return inStoreOnly == null ? Boolean.FALSE : inStoreOnly;
    }

    public void setInStoreOnly(Boolean inStoreOnly) {
        this.inStoreOnly = inStoreOnly == null ? Boolean.FALSE : inStoreOnly;
    }

    public Boolean getAvailable() {
        return available == null ? Boolean.FALSE : available;
    }

    public void setAvailable(Boolean available) {
        boolean requestedAvailable = Boolean.TRUE.equals(available);
        this.available = requestedAvailable
                && Boolean.TRUE.equals(getActive())
                && getStockQuantity() > 0;
    }

    public String getSize() {
        return clean(size);
    }

    public void setSize(String size) {
        this.size = clean(size);
    }

    public String getFit() {
        return clean(fit);
    }

    public void setFit(String fit) {
        this.fit = clean(fit);
    }

    public String getMaterial() {
        return clean(material);
    }

    public void setMaterial(String material) {
        this.material = clean(material);
    }

    public String getGender() {
        return clean(gender);
    }

    public void setGender(String gender) {
        this.gender = clean(gender);
    }

    public String getSeason() {
        return clean(season);
    }

    public void setSeason(String season) {
        this.season = clean(season);
    }

    public String getOccasion() {
        return clean(occasion);
    }

    public void setOccasion(String occasion) {
        this.occasion = clean(occasion);
    }

    public String getStyleTags() {
        return clean(styleTags);
    }

    public void setStyleTags(String styleTags) {
        this.styleTags = clean(styleTags);
    }

    public String getPattern() {
        return clean(pattern);
    }

    public void setPattern(String pattern) {
        this.pattern = clean(pattern);
    }

    public boolean isAvailableForStyling() {
        return Boolean.TRUE.equals(getActive())
                && Boolean.TRUE.equals(getAvailable())
                && getStockQuantity() > 0;
    }

    public boolean isLowStock(int threshold) {
        int safeThreshold = Math.max(0, threshold);
        int stock = getStockQuantity();

        return stock > 0 && stock <= safeThreshold;
    }

    public boolean isOutOfStock() {
        return getStockQuantity() <= 0;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanUpper(String value) {
        return clean(value).toUpperCase();
    }

    private String normalizeCategoryForDisplay(String value) {
        String normalized = clean(value).toLowerCase();

        return switch (normalized) {
            case "top", "tops", "shirt", "shirts", "tee", "t-shirt", "hoodie", "sweater", "knit", "blouse" -> "Tops";
            case "bottom", "bottoms", "pants", "trousers", "jeans", "cargo", "shorts", "skirt" -> "Bottoms";
            case "shoe", "shoes", "sneaker", "sneakers", "boot", "boots", "loafer", "loafers", "heel", "heels", "sandal", "sandals" -> "Shoes";
            case "outerwear", "coat", "jacket", "blazer", "parka", "cardigan", "trench" -> "Outerwear";
            default -> clean(value);
        };
    }
}
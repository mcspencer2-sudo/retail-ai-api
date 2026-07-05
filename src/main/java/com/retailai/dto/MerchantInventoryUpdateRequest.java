package com.retailai.dto;

public class MerchantInventoryUpdateRequest {

    private String itemName;
    private String brand;
    private String category;
    private String color;
    private Double price;
    private String imageUrl;

    private String size;
    private String fit;
    private String material;
    private String gender;
    private String season;
    private String occasion;
    private String styleTags;
    private String pattern;

    private Integer stockQuantity;
    private Boolean active;
    private Boolean available;

    public MerchantInventoryUpdateRequest() {
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = clean(itemName);
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
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
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

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
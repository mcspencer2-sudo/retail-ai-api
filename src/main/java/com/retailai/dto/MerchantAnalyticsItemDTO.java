package com.retailai.dto;

public class MerchantAnalyticsItemDTO {

    private String rfid = "";
    private String name = "";
    private String brand = "";
    private String category = "";
    private String color = "";
    private String imageUrl = "";
    private Double price = 0.0;
    private Integer stockQuantity = 0;
    private String metricLabel = "";
    private Integer metricValue = 0;
    private Double revenue = 0.0;

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = clean(name);
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price == null ? 0.0 : Math.max(0.0, price);
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity == null ? 0 : Math.max(0, stockQuantity);
    }

    public String getMetricLabel() {
        return metricLabel;
    }

    public void setMetricLabel(String metricLabel) {
        this.metricLabel = clean(metricLabel);
    }

    public Integer getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Integer metricValue) {
        this.metricValue = metricValue == null ? 0 : Math.max(0, metricValue);
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue == null ? 0.0 : Math.max(0.0, revenue);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
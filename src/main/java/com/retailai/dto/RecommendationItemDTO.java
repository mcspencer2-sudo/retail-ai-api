package com.retailai.dto;

public class RecommendationItemDTO {

    private String rfid;
    private String name;
    private String brand;
    private String category;
    private String color;
    private String retailer;
    private Double price;
    private Integer matchScore;
    private Integer styleMatch;
    private Integer colorMatch;
    private Integer occasionMatch;
    private String reason;
    private String imageUrl;

    public RecommendationItemDTO() {
    }

    public RecommendationItemDTO(
            String rfid,
            String name,
            String brand,
            String category,
            String color,
            String retailer,
            Double price,
            Integer matchScore,
            Integer styleMatch,
            Integer colorMatch,
            Integer occasionMatch,
            String reason,
            String imageUrl
    ) {
        this.rfid = rfid;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.color = color;
        this.retailer = retailer;
        this.price = price;
        this.matchScore = matchScore;
        this.styleMatch = styleMatch;
        this.colorMatch = colorMatch;
        this.occasionMatch = occasionMatch;
        this.reason = reason;
        this.imageUrl = imageUrl;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = retailer;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public Integer getStyleMatch() {
        return styleMatch;
    }

    public void setStyleMatch(Integer styleMatch) {
        this.styleMatch = styleMatch;
    }

    public Integer getColorMatch() {
        return colorMatch;
    }

    public void setColorMatch(Integer colorMatch) {
        this.colorMatch = colorMatch;
    }

    public Integer getOccasionMatch() {
        return occasionMatch;
    }

    public void setOccasionMatch(Integer occasionMatch) {
        this.occasionMatch = occasionMatch;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
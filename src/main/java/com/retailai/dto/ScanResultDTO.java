package com.retailai.dto;

import java.util.List;

public class ScanResultDTO {

    private String rfid;
    private String name;
    private String brand;
    private String category;
    private String color;
    private String retailer;
    private String retailerKey;
    private String storeCode;
    private String storeName;
    private Double price;
    private Integer matchScore;
    private String imageUrl;
    private String stylingAdvice;
    private String whyItWorks;
    private List<RecommendationItemDTO> suggestions;
    private FullOutfitDTO fullOutfit;

    public ScanResultDTO() {
    }

    public ScanResultDTO(
            String rfid,
            String name,
            String brand,
            String category,
            String color,
            String retailer,
            String retailerKey,
            String storeCode,
            String storeName,
            Double price,
            Integer matchScore,
            String imageUrl,
            String stylingAdvice,
            String whyItWorks,
            List<RecommendationItemDTO> suggestions,
            FullOutfitDTO fullOutfit
    ) {
        this.rfid = rfid;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.color = color;
        this.retailer = retailer;
        this.retailerKey = retailerKey;
        this.storeCode = storeCode;
        this.storeName = storeName;
        this.price = price;
        this.matchScore = matchScore;
        this.imageUrl = imageUrl;
        this.stylingAdvice = stylingAdvice;
        this.whyItWorks = whyItWorks;
        this.suggestions = suggestions;
        this.fullOutfit = fullOutfit;
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

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = retailerKey;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStylingAdvice() {
        return stylingAdvice;
    }

    public void setStylingAdvice(String stylingAdvice) {
        this.stylingAdvice = stylingAdvice;
    }

    public String getWhyItWorks() {
        return whyItWorks;
    }

    public void setWhyItWorks(String whyItWorks) {
        this.whyItWorks = whyItWorks;
    }

    public List<RecommendationItemDTO> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<RecommendationItemDTO> suggestions) {
        this.suggestions = suggestions;
    }

    public FullOutfitDTO getFullOutfit() {
        return fullOutfit;
    }

    public void setFullOutfit(FullOutfitDTO fullOutfit) {
        this.fullOutfit = fullOutfit;
    }
}
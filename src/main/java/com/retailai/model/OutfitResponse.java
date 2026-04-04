package com.retailai.model;

import java.util.List;

public class OutfitResponse {

    private String rfid;
    private String retailerName;
    private String itemName;
    private String stylingAdvice;
    private String imageUrl;
    private double price;
    private List<Product> suggestions;

    public OutfitResponse() {
    }

    public OutfitResponse(String rfid,
                          String retailerName,
                          String itemName,
                          String stylingAdvice,
                          String imageUrl,
                          double price,
                          List<Product> suggestions) {
        this.rfid = rfid;
        this.retailerName = retailerName;
        this.itemName = itemName;
        this.stylingAdvice = stylingAdvice;
        this.imageUrl = imageUrl;
        this.price = price;
        this.suggestions = suggestions;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = retailerName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getStylingAdvice() {
        return stylingAdvice;
    }

    public void setStylingAdvice(String stylingAdvice) {
        this.stylingAdvice = stylingAdvice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<Product> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<Product> suggestions) {
        this.suggestions = suggestions;
    }
}
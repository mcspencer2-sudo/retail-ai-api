package com.retailai.model;

import jakarta.persistence.*;

@Entity
public class BagItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rfid;
    private String retailerName;
    private String itemName;
    private String imageUrl;
    private double price;

    // ✅ ADD THIS
    private String category;

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
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

    // ✅ ADD THESE
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
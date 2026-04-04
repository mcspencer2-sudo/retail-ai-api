package com.retailai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bag_items")
public class BagItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rfid;
    private String retailerName;
    private String itemName;
    private String imageUrl;
    private double price;
    private LocalDateTime savedAt;

    public BagItem() {
    }

    public BagItem(String rfid, String retailerName, String itemName, String imageUrl, double price, LocalDateTime savedAt) {
        this.rfid = rfid;
        this.retailerName = retailerName;
        this.itemName = itemName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.savedAt = savedAt;
    }

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

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }
}
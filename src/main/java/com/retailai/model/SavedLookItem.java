package com.retailai.model;

import jakarta.persistence.*;

@Entity
@Table(name = "saved_look_item")
public class SavedLookItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_look_id", nullable = false)
    private SavedLook savedLook;

    @Column(nullable = false)
    private String roleName;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private String rfid;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String retailerName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private Double price = 0.0;

    @Column(length = 2000)
    private String imageUrl;

    @Column(nullable = false)
    private Integer matchScore = 0;

    @Column(nullable = false)
    private Integer styleMatch = 0;

    @Column(nullable = false)
    private Integer colorMatch = 0;

    @Column(nullable = false)
    private Integer occasionMatch = 0;

    @Column(length = 4000)
    private String reason;

    public Long getId() {
        return id;
    }

    public SavedLook getSavedLook() {
        return savedLook;
    }

    public void setSavedLook(SavedLook savedLook) {
        this.savedLook = savedLook;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = retailerName;
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
        this.imageUrl = imageUrl;
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
}
package com.retailai.model;

import jakarta.persistence.*;

import java.util.Locale;

@Entity
@Table(name = "saved_look_item")
public class SavedLookItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_look_id", nullable = false)
    private SavedLook savedLook;

    @Column(nullable = false, length = 40)
    private String roleName = "";

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false, length = 120)
    private String rfid = "";

    @Column(nullable = false)
    private String itemName = "";

    @Column(nullable = false)
    private String brand = "";

    @Column(nullable = false)
    private String retailerName = "";

    @Column(nullable = false)
    private String category = "";

    @Column(nullable = false)
    private String color = "";

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
        this.roleName = safe(roleName).toUpperCase(Locale.ROOT);
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder == null ? 0 : Math.max(0, displayOrder);
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = safe(rfid).toUpperCase(Locale.ROOT);
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = safe(itemName);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = safe(brand);
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = safe(retailerName);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = safe(category);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = safe(color);
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price == null ? 0.0 : Math.max(0.0, price);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = safeNullable(imageUrl);
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = normalizeScore(matchScore);
    }

    public Integer getStyleMatch() {
        return styleMatch;
    }

    public void setStyleMatch(Integer styleMatch) {
        this.styleMatch = normalizeScore(styleMatch);
    }

    public Integer getColorMatch() {
        return colorMatch;
    }

    public void setColorMatch(Integer colorMatch) {
        this.colorMatch = normalizeScore(colorMatch);
    }

    public Integer getOccasionMatch() {
        return occasionMatch;
    }

    public void setOccasionMatch(Integer occasionMatch) {
        this.occasionMatch = normalizeScore(occasionMatch);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = safeNullable(reason);
    }

    private Integer normalizeScore(Integer value) {
        if (value == null) {
            return 0;
        }

        return Math.max(0, Math.min(100, value));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
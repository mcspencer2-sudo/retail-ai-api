package com.retailai.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "customer_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_preferences_user_store",
                        columnNames = {"user_id", "store_code"}
                )
        }
)
public class CustomerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "tenant_id", length = 80)
    private String tenantId;

    @Column(name = "retailer_key", length = 80)
    private String retailerKey;

    @Column(name = "store_code", nullable = false, length = 100)
    private String storeCode;

    @Column(name = "store_name", length = 180)
    private String storeName;

    @Column(name = "size_top", length = 40)
    private String sizeTop;

    @Column(name = "size_bottom", length = 40)
    private String sizeBottom;

    @Column(name = "shoe_size", length = 40)
    private String shoeSize;

    @Column(name = "budget_min")
    private Double budgetMin;

    @Column(name = "budget_max")
    private Double budgetMax;

    @Column(name = "favorite_colors", length = 500)
    private String favoriteColors;

    @Column(name = "avoided_colors", length = 500)
    private String avoidedColors;

    @Column(name = "fit_preference", length = 60)
    private String fitPreference;

    @Column(name = "gender_style", length = 80)
    private String genderStyle;

    @Column(name = "preferred_materials", length = 500)
    private String preferredMaterials;

    @Column(name = "style_keywords", length = 700)
    private String styleKeywords;

    @Column(name = "disliked_styles", length = 700)
    private String dislikedStyles;

    @Column(name = "occasion_priority", length = 80)
    private String occasionPriority;

    @Column(name = "notes", length = 1500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CustomerPreference() {
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = clean(userId);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = clean(email);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = clean(tenantId);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode);
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public String getSizeTop() {
        return sizeTop;
    }

    public void setSizeTop(String sizeTop) {
        this.sizeTop = clean(sizeTop);
    }

    public String getSizeBottom() {
        return sizeBottom;
    }

    public void setSizeBottom(String sizeBottom) {
        this.sizeBottom = clean(sizeBottom);
    }

    public String getShoeSize() {
        return shoeSize;
    }

    public void setShoeSize(String shoeSize) {
        this.shoeSize = clean(shoeSize);
    }

    public Double getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }

    public Double getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    public String getFavoriteColors() {
        return favoriteColors;
    }

    public void setFavoriteColors(String favoriteColors) {
        this.favoriteColors = clean(favoriteColors);
    }

    public String getAvoidedColors() {
        return avoidedColors;
    }

    public void setAvoidedColors(String avoidedColors) {
        this.avoidedColors = clean(avoidedColors);
    }

    public String getFitPreference() {
        return fitPreference;
    }

    public void setFitPreference(String fitPreference) {
        this.fitPreference = clean(fitPreference);
    }

    public String getGenderStyle() {
        return genderStyle;
    }

    public void setGenderStyle(String genderStyle) {
        this.genderStyle = clean(genderStyle);
    }

    public String getPreferredMaterials() {
        return preferredMaterials;
    }

    public void setPreferredMaterials(String preferredMaterials) {
        this.preferredMaterials = clean(preferredMaterials);
    }

    public String getStyleKeywords() {
        return styleKeywords;
    }

    public void setStyleKeywords(String styleKeywords) {
        this.styleKeywords = clean(styleKeywords);
    }

    public String getDislikedStyles() {
        return dislikedStyles;
    }

    public void setDislikedStyles(String dislikedStyles) {
        this.dislikedStyles = clean(dislikedStyles);
    }

    public String getOccasionPriority() {
        return occasionPriority;
    }

    public void setOccasionPriority(String occasionPriority) {
        this.occasionPriority = clean(occasionPriority);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = clean(notes);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
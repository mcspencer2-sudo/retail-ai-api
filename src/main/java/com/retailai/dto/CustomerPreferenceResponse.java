package com.retailai.dto;

import com.retailai.customer.CustomerPreference;

import java.time.LocalDateTime;

public class CustomerPreferenceResponse {

    private Long id;

    private String userId;
    private String email;
    private String tenantId;
    private String retailerKey;
    private String storeCode;
    private String storeName;

    private String sizeTop;
    private String sizeBottom;
    private String shoeSize;

    private Double budgetMin;
    private Double budgetMax;

    private String favoriteColors;
    private String avoidedColors;

    private String fitPreference;
    private String genderStyle;
    private String preferredMaterials;
    private String occasionPriority;
    private String styleKeywords;
    private String dislikedStyles;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerPreferenceResponse() {
    }

    public CustomerPreferenceResponse(CustomerPreference preference) {
        if (preference == null) {
            return;
        }

        this.id = preference.getId();

        this.userId = preference.getUserId();
        this.email = preference.getEmail();
        this.tenantId = preference.getTenantId();
        this.retailerKey = preference.getRetailerKey();
        this.storeCode = preference.getStoreCode();
        this.storeName = preference.getStoreName();

        this.sizeTop = preference.getSizeTop();
        this.sizeBottom = preference.getSizeBottom();
        this.shoeSize = preference.getShoeSize();

        this.budgetMin = preference.getBudgetMin();
        this.budgetMax = preference.getBudgetMax();

        this.favoriteColors = preference.getFavoriteColors();
        this.avoidedColors = preference.getAvoidedColors();

        this.fitPreference = preference.getFitPreference();
        this.genderStyle = preference.getGenderStyle();
        this.preferredMaterials = preference.getPreferredMaterials();
        this.occasionPriority = preference.getOccasionPriority();
        this.styleKeywords = preference.getStyleKeywords();
        this.dislikedStyles = preference.getDislikedStyles();
        this.notes = preference.getNotes();

        this.createdAt = preference.getCreatedAt();
        this.updatedAt = preference.getUpdatedAt();
    }

    public static CustomerPreferenceResponse from(CustomerPreference preference) {
        return new CustomerPreferenceResponse(preference);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getGenderPreference() {
        return genderStyle;
    }

    public void setGenderPreference(String genderPreference) {
        this.genderStyle = clean(genderPreference);
    }

    public String getPreferredMaterials() {
        return preferredMaterials;
    }

    public void setPreferredMaterials(String preferredMaterials) {
        this.preferredMaterials = clean(preferredMaterials);
    }

    public String getOccasionPriority() {
        return occasionPriority;
    }

    public void setOccasionPriority(String occasionPriority) {
        this.occasionPriority = clean(occasionPriority);
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
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
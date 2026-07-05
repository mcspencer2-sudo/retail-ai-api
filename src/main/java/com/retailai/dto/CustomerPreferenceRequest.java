package com.retailai.dto;

public class CustomerPreferenceRequest {

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
    private String dislikedMaterials;
    private String occasionPriority;
    private String styleKeywords;
    private String dislikedStyles;
    private String notes;

    public CustomerPreferenceRequest() {
    }

    public String getSizeTop() {
        return clean(sizeTop);
    }

    public void setSizeTop(String sizeTop) {
        this.sizeTop = clean(sizeTop);
    }

    public String getSizeBottom() {
        return clean(sizeBottom);
    }

    public void setSizeBottom(String sizeBottom) {
        this.sizeBottom = clean(sizeBottom);
    }

    public String getShoeSize() {
        return clean(shoeSize);
    }

    public void setShoeSize(String shoeSize) {
        this.shoeSize = clean(shoeSize);
    }

    public Double getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = normalizeBudgetValue(budgetMin);
        normalizeBudgetRange();
    }

    public Double getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = normalizeBudgetValue(budgetMax);
        normalizeBudgetRange();
    }

    public String getFavoriteColors() {
        return clean(favoriteColors);
    }

    public void setFavoriteColors(String favoriteColors) {
        this.favoriteColors = clean(favoriteColors);
    }

    public String getAvoidedColors() {
        return clean(avoidedColors);
    }

    public void setAvoidedColors(String avoidedColors) {
        this.avoidedColors = clean(avoidedColors);
    }

    public String getFitPreference() {
        return clean(fitPreference);
    }

    public void setFitPreference(String fitPreference) {
        this.fitPreference = clean(fitPreference);
    }

    public String getGenderStyle() {
        return clean(genderStyle);
    }

    public void setGenderStyle(String genderStyle) {
        this.genderStyle = clean(genderStyle);
    }

    public String getGenderPreference() {
        return getGenderStyle();
    }

    public void setGenderPreference(String genderPreference) {
        this.genderStyle = clean(genderPreference);
    }

    public String getPreferredMaterials() {
        return clean(preferredMaterials);
    }

    public void setPreferredMaterials(String preferredMaterials) {
        this.preferredMaterials = clean(preferredMaterials);
    }

    public String getDislikedMaterials() {
        return clean(dislikedMaterials);
    }

    public void setDislikedMaterials(String dislikedMaterials) {
        this.dislikedMaterials = clean(dislikedMaterials);
    }

    public String getOccasionPriority() {
        return clean(occasionPriority);
    }

    public void setOccasionPriority(String occasionPriority) {
        this.occasionPriority = clean(occasionPriority);
    }

    public String getStyleKeywords() {
        return clean(styleKeywords);
    }

    public void setStyleKeywords(String styleKeywords) {
        this.styleKeywords = clean(styleKeywords);
    }

    public String getDislikedStyles() {
        return clean(dislikedStyles);
    }

    public void setDislikedStyles(String dislikedStyles) {
        this.dislikedStyles = clean(dislikedStyles);
    }

    public String getNotes() {
        return clean(notes);
    }

    public void setNotes(String notes) {
        this.notes = clean(notes);
    }

    public String getPreferredSize() {
        if (!getSizeTop().isBlank()) {
            return getSizeTop();
        }

        if (!getSizeBottom().isBlank()) {
            return getSizeBottom();
        }

        return getShoeSize();
    }

    public void setPreferredSize(String preferredSize) {
        String cleaned = clean(preferredSize);

        this.sizeTop = cleaned;
        this.sizeBottom = cleaned;
        this.shoeSize = cleaned;
    }

    public String getPreferredFit() {
        return getFitPreference();
    }

    public void setPreferredFit(String preferredFit) {
        this.fitPreference = clean(preferredFit);
    }

    public boolean hasBudgetPreference() {
        return budgetMin != null || budgetMax != null;
    }

    public boolean hasSizePreference() {
        return !getSizeTop().isBlank()
                || !getSizeBottom().isBlank()
                || !getShoeSize().isBlank();
    }

    public boolean hasColorPreference() {
        return !getFavoriteColors().isBlank()
                || !getAvoidedColors().isBlank();
    }

    public boolean hasFitPreference() {
        return !getFitPreference().isBlank();
    }

    public boolean hasGenderPreference() {
        String gender = getGenderPreference();
        return !gender.isBlank() && !"Any".equalsIgnoreCase(gender);
    }

    public boolean hasMaterialPreference() {
        return !getPreferredMaterials().isBlank()
                || !getDislikedMaterials().isBlank();
    }

    public boolean hasStyleKeywordPreference() {
        return !getStyleKeywords().isBlank()
                || !getDislikedStyles().isBlank();
    }

    public boolean hasOccasionPreference() {
        return !getOccasionPriority().isBlank();
    }

    public boolean hasStylePreference() {
        return hasFitPreference()
                || hasGenderPreference()
                || hasMaterialPreference()
                || hasOccasionPreference()
                || hasStyleKeywordPreference()
                || !getNotes().isBlank();
    }

    public boolean hasAnyPreference() {
        return hasBudgetPreference()
                || hasSizePreference()
                || hasColorPreference()
                || hasStylePreference();
    }

    private Double normalizeBudgetValue(Double value) {
        if (value == null) {
            return null;
        }

        if (value.isNaN() || value.isInfinite()) {
            return null;
        }

        return Math.max(0.0, value);
    }

    private void normalizeBudgetRange() {
        if (budgetMin == null || budgetMax == null) {
            return;
        }

        if (budgetMin <= budgetMax) {
            return;
        }

        Double oldMin = budgetMin;
        budgetMin = budgetMax;
        budgetMax = oldMin;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
package com.retailai.dto;

public class RecommendationItemDTO {

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
    private Integer styleMatch;
    private Integer colorMatch;
    private Integer occasionMatch;

    private Integer preferenceMatch;
    private Integer budgetMatch;
    private Integer sizeMatch;
    private Integer fitMatch;
    private Integer materialMatch;

    private String reason;
    private String preferenceNote;
    private String imageUrl;

    private String size;
    private String fit;
    private String material;
    private String gender;
    private String season;
    private String occasion;
    private String styleTags;
    private String pattern;

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

    public RecommendationItemDTO(
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
            Integer styleMatch,
            Integer colorMatch,
            Integer occasionMatch,
            Integer preferenceMatch,
            Integer budgetMatch,
            Integer sizeMatch,
            Integer fitMatch,
            Integer materialMatch,
            String reason,
            String preferenceNote,
            String imageUrl,
            String size,
            String fit,
            String material,
            String gender,
            String season,
            String occasion,
            String styleTags,
            String pattern
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
        this.styleMatch = styleMatch;
        this.colorMatch = colorMatch;
        this.occasionMatch = occasionMatch;
        this.preferenceMatch = preferenceMatch;
        this.budgetMatch = budgetMatch;
        this.sizeMatch = sizeMatch;
        this.fitMatch = fitMatch;
        this.materialMatch = materialMatch;
        this.reason = reason;
        this.preferenceNote = preferenceNote;
        this.imageUrl = imageUrl;
        this.size = size;
        this.fit = fit;
        this.material = material;
        this.gender = gender;
        this.season = season;
        this.occasion = occasion;
        this.styleTags = styleTags;
        this.pattern = pattern;
    }

    public String getRfid() {
        return rfid;
    }

    public String getItemRfid() {
        return rfid;
    }

    public String getProductRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public void setItemRfid(String itemRfid) {
        this.rfid = clean(itemRfid);
    }

    public void setProductRfid(String productRfid) {
        this.rfid = clean(productRfid);
    }

    public String getName() {
        return name;
    }

    public String getItemName() {
        return name;
    }

    public String getProductName() {
        return name;
    }

    public void setName(String name) {
        this.name = clean(name);
    }

    public void setItemName(String itemName) {
        this.name = clean(itemName);
    }

    public void setProductName(String productName) {
        this.name = clean(productName);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = clean(brand);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = clean(color);
    }

    public String getRetailer() {
        return retailer;
    }

    public String getRetailerName() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = clean(retailer);
    }

    public void setRetailerName(String retailerName) {
        this.retailer = clean(retailerName);
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

    public Double getPrice() {
        return price == null ? 0.0 : price;
    }

    public void setPrice(Double price) {
        this.price = price == null ? 0.0 : price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Integer getMatchScore() {
        return matchScore == null ? 0 : matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = clampNullableScore(matchScore);
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = clampScore(matchScore);
    }

    public Integer getStyleMatch() {
        return styleMatch == null ? 0 : styleMatch;
    }

    public void setStyleMatch(Integer styleMatch) {
        this.styleMatch = clampNullableScore(styleMatch);
    }

    public void setStyleMatch(int styleMatch) {
        this.styleMatch = clampScore(styleMatch);
    }

    public Integer getColorMatch() {
        return colorMatch == null ? 0 : colorMatch;
    }

    public void setColorMatch(Integer colorMatch) {
        this.colorMatch = clampNullableScore(colorMatch);
    }

    public void setColorMatch(int colorMatch) {
        this.colorMatch = clampScore(colorMatch);
    }

    public Integer getOccasionMatch() {
        return occasionMatch == null ? 0 : occasionMatch;
    }

    public void setOccasionMatch(Integer occasionMatch) {
        this.occasionMatch = clampNullableScore(occasionMatch);
    }

    public void setOccasionMatch(int occasionMatch) {
        this.occasionMatch = clampScore(occasionMatch);
    }

    public Integer getPreferenceMatch() {
        return preferenceMatch == null ? 0 : preferenceMatch;
    }

    public void setPreferenceMatch(Integer preferenceMatch) {
        this.preferenceMatch = clampNullableScore(preferenceMatch);
    }

    public void setPreferenceMatch(int preferenceMatch) {
        this.preferenceMatch = clampScore(preferenceMatch);
    }

    /*
     * Alias for newer backend/frontend naming.
     * Same value as preferenceMatch.
     */
    public Integer getPreferenceMatchScore() {
        return getPreferenceMatch();
    }

    public void setPreferenceMatchScore(Integer preferenceMatchScore) {
        this.preferenceMatch = clampNullableScore(preferenceMatchScore);
    }

    public void setPreferenceMatchScore(int preferenceMatchScore) {
        this.preferenceMatch = clampScore(preferenceMatchScore);
    }

    public Integer getBudgetMatch() {
        return budgetMatch == null ? 0 : budgetMatch;
    }

    public void setBudgetMatch(Integer budgetMatch) {
        this.budgetMatch = clampNullableScore(budgetMatch);
    }

    public void setBudgetMatch(int budgetMatch) {
        this.budgetMatch = clampScore(budgetMatch);
    }

    public Integer getSizeMatch() {
        return sizeMatch == null ? 0 : sizeMatch;
    }

    public void setSizeMatch(Integer sizeMatch) {
        this.sizeMatch = clampNullableScore(sizeMatch);
    }

    public void setSizeMatch(int sizeMatch) {
        this.sizeMatch = clampScore(sizeMatch);
    }

    public Integer getFitMatch() {
        return fitMatch == null ? 0 : fitMatch;
    }

    public void setFitMatch(Integer fitMatch) {
        this.fitMatch = clampNullableScore(fitMatch);
    }

    public void setFitMatch(int fitMatch) {
        this.fitMatch = clampScore(fitMatch);
    }

    public Integer getMaterialMatch() {
        return materialMatch == null ? 0 : materialMatch;
    }

    public void setMaterialMatch(Integer materialMatch) {
        this.materialMatch = clampNullableScore(materialMatch);
    }

    public void setMaterialMatch(int materialMatch) {
        this.materialMatch = clampScore(materialMatch);
    }

    public String getReason() {
        return reason;
    }

    public String getWhyItWorks() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = clean(reason);
    }

    public void setWhyItWorks(String whyItWorks) {
        this.reason = clean(whyItWorks);
    }

    public String getPreferenceNote() {
        return preferenceNote;
    }

    public void setPreferenceNote(String preferenceNote) {
        this.preferenceNote = clean(preferenceNote);
    }

    /*
     * Alias for newer backend/frontend naming.
     * Same value as preferenceNote.
     */
    public String getPreferenceReason() {
        return preferenceNote;
    }

    public void setPreferenceReason(String preferenceReason) {
        this.preferenceNote = clean(preferenceReason);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImage_url() {
        return imageUrl;
    }

    public String getProductImageUrl() {
        return imageUrl;
    }

    public String getPhotoUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public void setImage_url(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public void setProductImageUrl(String productImageUrl) {
        this.imageUrl = clean(productImageUrl);
    }

    public void setPhotoUrl(String photoUrl) {
        this.imageUrl = clean(photoUrl);
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = clean(size);
    }

    public String getFit() {
        return fit;
    }

    public void setFit(String fit) {
        this.fit = clean(fit);
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = clean(material);
    }

    public String getGender() {
        return gender;
    }

    public String getGenderStyle() {
        return gender;
    }

    public String getGenderPreference() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = clean(gender);
    }

    public void setGenderStyle(String genderStyle) {
        this.gender = clean(genderStyle);
    }

    public void setGenderPreference(String genderPreference) {
        this.gender = clean(genderPreference);
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = clean(season);
    }

    public String getOccasion() {
        return occasion;
    }

    public String getOccasionPriority() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = clean(occasion);
    }

    public void setOccasionPriority(String occasionPriority) {
        this.occasion = clean(occasionPriority);
    }

    public String getStyleTags() {
        return styleTags;
    }

    public String getStyleKeywords() {
        return styleTags;
    }

    public void setStyleTags(String styleTags) {
        this.styleTags = clean(styleTags);
    }

    public void setStyleKeywords(String styleKeywords) {
        this.styleTags = clean(styleKeywords);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = clean(pattern);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer clampNullableScore(Integer value) {
        return value == null ? 0 : clampScore(value);
    }

    private int clampScore(int value) {
        if (value < 0) {
            return 0;
        }

        if (value > 100) {
            return 100;
        }

        return value;
    }
}
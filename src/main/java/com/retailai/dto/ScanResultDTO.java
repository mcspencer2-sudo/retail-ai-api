package com.retailai.dto;

import java.util.ArrayList;
import java.util.List;

public class ScanResultDTO {

    private String rfid = "";
    private String name = "";
    private String brand = "";
    private String category = "";
    private String color = "";

    private String retailer = "";
    private String retailerKey = "";
    private String storeCode = "";
    private String storeName = "";

    private Double price = 0.0;
    private Integer matchScore = 0;

    private String imageUrl = "";
    private String stylingAdvice = "";
    private String whyItWorks = "";

    private List<RecommendationItemDTO> suggestions = new ArrayList<>();
    private FullOutfitDTO fullOutfit;

    private Integer preferenceScore = 0;
    private Integer budgetScore = 0;
    private Integer sizeScore = 0;
    private Integer fitScore = 0;
    private Integer materialScore = 0;

    private String stylingNote = "";
    private String occasionNote = "";
    private String seasonNote = "";
    private String colorNote = "";
    private String fitNote = "";
    private String materialNote = "";
    private String preferenceNote = "";

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
        setRfid(rfid);
        setName(name);
        setBrand(brand);
        setCategory(category);
        setColor(color);
        setRetailer(retailer);
        setRetailerKey(retailerKey);
        setStoreCode(storeCode);
        setStoreName(storeName);
        setPrice(price);
        setMatchScore(matchScore);
        setImageUrl(imageUrl);
        setStylingAdvice(stylingAdvice);
        setWhyItWorks(whyItWorks);
        setSuggestions(suggestions);
        setFullOutfit(fullOutfit);
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public String getName() {
        return name;
    }

    public String getItemName() {
        return name;
    }

    public void setName(String name) {
        this.name = clean(name);
    }

    public void setItemName(String itemName) {
        this.name = clean(itemName);
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
        this.retailerKey = clean(retailerKey).toUpperCase();
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode).toUpperCase();
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
        this.price = price == null ? 0.0 : Math.max(0.0, price);
    }

    public Integer getMatchScore() {
        return matchScore == null ? 0 : matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = clampScoreOrZero(matchScore);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public String getStylingAdvice() {
        return stylingAdvice;
    }

    public void setStylingAdvice(String stylingAdvice) {
        this.stylingAdvice = clean(stylingAdvice);
    }

    public String getWhyItWorks() {
        return whyItWorks;
    }

    public void setWhyItWorks(String whyItWorks) {
        this.whyItWorks = clean(whyItWorks);
    }

    public List<RecommendationItemDTO> getSuggestions() {
        return suggestions == null ? new ArrayList<>() : suggestions;
    }

    public void setSuggestions(List<RecommendationItemDTO> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }

    public FullOutfitDTO getFullOutfit() {
        return fullOutfit;
    }

    public void setFullOutfit(FullOutfitDTO fullOutfit) {
        this.fullOutfit = fullOutfit;
    }

    public Integer getPreferenceScore() {
        return preferenceScore == null ? 0 : preferenceScore;
    }

    public Integer getPreferenceMatchScore() {
        return getPreferenceScore();
    }

    public void setPreferenceScore(Integer preferenceScore) {
        this.preferenceScore = clampScoreOrZero(preferenceScore);
    }

    public void setPreferenceMatchScore(Integer preferenceMatchScore) {
        this.preferenceScore = clampScoreOrZero(preferenceMatchScore);
    }

    public Integer getBudgetScore() {
        return budgetScore == null ? 0 : budgetScore;
    }

    public Integer getBudgetMatch() {
        return getBudgetScore();
    }

    public void setBudgetScore(Integer budgetScore) {
        this.budgetScore = clampScoreOrZero(budgetScore);
    }

    public void setBudgetMatch(Integer budgetMatch) {
        this.budgetScore = clampScoreOrZero(budgetMatch);
    }

    public Integer getSizeScore() {
        return sizeScore == null ? 0 : sizeScore;
    }

    public Integer getSizeMatch() {
        return getSizeScore();
    }

    public void setSizeScore(Integer sizeScore) {
        this.sizeScore = clampScoreOrZero(sizeScore);
    }

    public void setSizeMatch(Integer sizeMatch) {
        this.sizeScore = clampScoreOrZero(sizeMatch);
    }

    public Integer getFitScore() {
        return fitScore == null ? 0 : fitScore;
    }

    public Integer getFitMatch() {
        return getFitScore();
    }

    public void setFitScore(Integer fitScore) {
        this.fitScore = clampScoreOrZero(fitScore);
    }

    public void setFitMatch(Integer fitMatch) {
        this.fitScore = clampScoreOrZero(fitMatch);
    }

    public Integer getMaterialScore() {
        return materialScore == null ? 0 : materialScore;
    }

    public Integer getMaterialMatch() {
        return getMaterialScore();
    }

    public void setMaterialScore(Integer materialScore) {
        this.materialScore = clampScoreOrZero(materialScore);
    }

    public void setMaterialMatch(Integer materialMatch) {
        this.materialScore = clampScoreOrZero(materialMatch);
    }

    public String getStylingNote() {
        return stylingNote;
    }

    public void setStylingNote(String stylingNote) {
        this.stylingNote = clean(stylingNote);
    }

    public String getOccasionNote() {
        return occasionNote;
    }

    public void setOccasionNote(String occasionNote) {
        this.occasionNote = clean(occasionNote);
    }

    public String getSeasonNote() {
        return seasonNote;
    }

    public void setSeasonNote(String seasonNote) {
        this.seasonNote = clean(seasonNote);
    }

    public String getColorNote() {
        return colorNote;
    }

    public String getColorPairingNote() {
        return colorNote;
    }

    public void setColorNote(String colorNote) {
        this.colorNote = clean(colorNote);
    }

    public void setColorPairingNote(String colorPairingNote) {
        this.colorNote = clean(colorPairingNote);
    }

    public String getFitNote() {
        return fitNote;
    }

    public void setFitNote(String fitNote) {
        this.fitNote = clean(fitNote);
    }

    public String getMaterialNote() {
        return materialNote;
    }

    public String getTextureNote() {
        return materialNote;
    }

    public void setMaterialNote(String materialNote) {
        this.materialNote = clean(materialNote);
    }

    public void setTextureNote(String textureNote) {
        this.materialNote = clean(textureNote);
    }

    public String getPreferenceNote() {
        return preferenceNote;
    }

    public String getMatchedPreferenceReason() {
        return preferenceNote;
    }

    public void setPreferenceNote(String preferenceNote) {
        this.preferenceNote = clean(preferenceNote);
    }

    public void setMatchedPreferenceReason(String matchedPreferenceReason) {
        this.preferenceNote = clean(matchedPreferenceReason);
    }

    private Integer clampScoreOrZero(Integer score) {
        if (score == null) {
            return 0;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
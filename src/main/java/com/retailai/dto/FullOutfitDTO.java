package com.retailai.dto;

public class FullOutfitDTO {

    private RecommendationItemDTO top;
    private RecommendationItemDTO bottom;
    private RecommendationItemDTO shoes;
    private RecommendationItemDTO outerwear;

    private int overallScore;
    private String explanation;

    private int styleScore;
    private int colorScore;
    private int occasionScore;

    private int preferenceScore;
    private int budgetScore;
    private int sizeScore;
    private int fitScore;
    private int materialScore;

    private String stylingNote;
    private String occasionNote;
    private String seasonNote;
    private String colorNote;
    private String fitNote;
    private String materialNote;
    private String preferenceNote;

    public FullOutfitDTO() {
    }

    public RecommendationItemDTO getTop() {
        return top;
    }

    public void setTop(RecommendationItemDTO top) {
        this.top = top;
    }

    public RecommendationItemDTO getBottom() {
        return bottom;
    }

    public void setBottom(RecommendationItemDTO bottom) {
        this.bottom = bottom;
    }

    public RecommendationItemDTO getShoes() {
        return shoes;
    }

    public void setShoes(RecommendationItemDTO shoes) {
        this.shoes = shoes;
    }

    public RecommendationItemDTO getOuterwear() {
        return outerwear;
    }

    public void setOuterwear(RecommendationItemDTO outerwear) {
        this.outerwear = outerwear;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = clampScore(overallScore);
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getStyleScore() {
        return styleScore;
    }

    public void setStyleScore(int styleScore) {
        this.styleScore = clampScore(styleScore);
    }

    public int getColorScore() {
        return colorScore;
    }

    public void setColorScore(int colorScore) {
        this.colorScore = clampScore(colorScore);
    }

    public int getOccasionScore() {
        return occasionScore;
    }

    public void setOccasionScore(int occasionScore) {
        this.occasionScore = clampScore(occasionScore);
    }

    public int getPreferenceScore() {
        return preferenceScore;
    }

    public void setPreferenceScore(int preferenceScore) {
        this.preferenceScore = clampScore(preferenceScore);
    }

    public int getBudgetScore() {
        return budgetScore;
    }

    public void setBudgetScore(int budgetScore) {
        this.budgetScore = clampScore(budgetScore);
    }

    public int getSizeScore() {
        return sizeScore;
    }

    public void setSizeScore(int sizeScore) {
        this.sizeScore = clampScore(sizeScore);
    }

    public int getFitScore() {
        return fitScore;
    }

    public void setFitScore(int fitScore) {
        this.fitScore = clampScore(fitScore);
    }

    public int getMaterialScore() {
        return materialScore;
    }

    public void setMaterialScore(int materialScore) {
        this.materialScore = clampScore(materialScore);
    }

    public String getStylingNote() {
        return stylingNote;
    }

    public void setStylingNote(String stylingNote) {
        this.stylingNote = stylingNote;
    }

    public String getOccasionNote() {
        return occasionNote;
    }

    public void setOccasionNote(String occasionNote) {
        this.occasionNote = occasionNote;
    }

    public String getSeasonNote() {
        return seasonNote;
    }

    public void setSeasonNote(String seasonNote) {
        this.seasonNote = seasonNote;
    }

    public String getColorNote() {
        return colorNote;
    }

    public String getColorPairingNote() {
        return colorNote;
    }

    public void setColorNote(String colorNote) {
        this.colorNote = colorNote;
    }

    public void setColorPairingNote(String colorPairingNote) {
        this.colorNote = colorPairingNote;
    }

    public String getFitNote() {
        return fitNote;
    }

    public void setFitNote(String fitNote) {
        this.fitNote = fitNote;
    }

    public String getMaterialNote() {
        return materialNote;
    }

    public String getTextureNote() {
        return materialNote;
    }

    public void setMaterialNote(String materialNote) {
        this.materialNote = materialNote;
    }

    public void setTextureNote(String textureNote) {
        this.materialNote = textureNote;
    }

    public String getPreferenceNote() {
        return preferenceNote;
    }

    public void setPreferenceNote(String preferenceNote) {
        this.preferenceNote = preferenceNote;
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
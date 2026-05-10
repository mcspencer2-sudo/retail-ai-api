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
        this.overallScore = overallScore;
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
        this.styleScore = styleScore;
    }

    public int getColorScore() {
        return colorScore;
    }

    public void setColorScore(int colorScore) {
        this.colorScore = colorScore;
    }

    public int getOccasionScore() {
        return occasionScore;
    }

    public void setOccasionScore(int occasionScore) {
        this.occasionScore = occasionScore;
    }
}
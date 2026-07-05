package com.retailai.dto;

import java.util.ArrayList;
import java.util.List;

public class LookResponseDTO {

    private FullOutfitDTO fullOutfit;
    private List<RecommendationItemDTO> suggestions = new ArrayList<>();
    private Integer variation = 0;

    private Integer preferenceScore;
    private Integer budgetScore;
    private Integer sizeScore;
    private Integer fitScore;
    private Integer materialScore;

    private String stylingNote = "";
    private String occasionNote = "";
    private String seasonNote = "";
    private String colorNote = "";
    private String fitNote = "";
    private String materialNote = "";
    private String preferenceNote = "";

    public LookResponseDTO() {
    }

    public LookResponseDTO(
            FullOutfitDTO fullOutfit,
            List<RecommendationItemDTO> suggestions,
            Integer variation
    ) {
        this.fullOutfit = fullOutfit;
        setSuggestions(suggestions);
        setVariation(variation);
    }

    public LookResponseDTO(
            FullOutfitDTO fullOutfit,
            List<RecommendationItemDTO> suggestions,
            Integer variation,
            String stylingNote,
            String occasionNote,
            String seasonNote,
            String colorNote,
            String fitNote,
            String materialNote,
            String preferenceNote
    ) {
        this.fullOutfit = fullOutfit;
        setSuggestions(suggestions);
        setVariation(variation);
        setStylingNote(stylingNote);
        setOccasionNote(occasionNote);
        setSeasonNote(seasonNote);
        setColorNote(colorNote);
        setFitNote(fitNote);
        setMaterialNote(materialNote);
        setPreferenceNote(preferenceNote);
        syncNotesToFullOutfit();
    }

    public FullOutfitDTO getFullOutfit() {
        return fullOutfit;
    }

    public void setFullOutfit(FullOutfitDTO fullOutfit) {
        this.fullOutfit = fullOutfit;
        syncNotesToFullOutfit();
    }

    public List<RecommendationItemDTO> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<RecommendationItemDTO> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }

    public Integer getVariation() {
        return variation;
    }

    public void setVariation(Integer variation) {
        this.variation = variation == null ? 0 : Math.max(0, variation);
    }

    public Integer getPreferenceScore() {
        return preferenceScore;
    }

    public void setPreferenceScore(Integer preferenceScore) {
        this.preferenceScore = clampScore(preferenceScore);
    }

    public Integer getBudgetScore() {
        return budgetScore;
    }

    public void setBudgetScore(Integer budgetScore) {
        this.budgetScore = clampScore(budgetScore);
    }

    public Integer getSizeScore() {
        return sizeScore;
    }

    public void setSizeScore(Integer sizeScore) {
        this.sizeScore = clampScore(sizeScore);
    }

    public Integer getFitScore() {
        return fitScore;
    }

    public void setFitScore(Integer fitScore) {
        this.fitScore = clampScore(fitScore);
    }

    public Integer getMaterialScore() {
        return materialScore;
    }

    public void setMaterialScore(Integer materialScore) {
        this.materialScore = clampScore(materialScore);
    }

    public String getStylingNote() {
        return stylingNote;
    }

    public void setStylingNote(String stylingNote) {
        this.stylingNote = clean(stylingNote);
        syncNotesToFullOutfit();
    }

    public String getOccasionNote() {
        return occasionNote;
    }

    public void setOccasionNote(String occasionNote) {
        this.occasionNote = clean(occasionNote);
        syncNotesToFullOutfit();
    }

    public String getSeasonNote() {
        return seasonNote;
    }

    public void setSeasonNote(String seasonNote) {
        this.seasonNote = clean(seasonNote);
        syncNotesToFullOutfit();
    }

    public String getColorNote() {
        return colorNote;
    }

    public String getColorPairingNote() {
        return colorNote;
    }

    public void setColorNote(String colorNote) {
        this.colorNote = clean(colorNote);
        syncNotesToFullOutfit();
    }

    public void setColorPairingNote(String colorPairingNote) {
        this.colorNote = clean(colorPairingNote);
        syncNotesToFullOutfit();
    }

    public String getFitNote() {
        return fitNote;
    }

    public void setFitNote(String fitNote) {
        this.fitNote = clean(fitNote);
        syncNotesToFullOutfit();
    }

    public String getMaterialNote() {
        return materialNote;
    }

    public String getTextureNote() {
        return materialNote;
    }

    public void setMaterialNote(String materialNote) {
        this.materialNote = clean(materialNote);
        syncNotesToFullOutfit();
    }

    public void setTextureNote(String textureNote) {
        this.materialNote = clean(textureNote);
        syncNotesToFullOutfit();
    }

    public String getPreferenceNote() {
        return preferenceNote;
    }

    public void setPreferenceNote(String preferenceNote) {
        this.preferenceNote = clean(preferenceNote);
        syncNotesToFullOutfit();
    }

    public void syncNotesToFullOutfit() {
        if (fullOutfit == null) {
            return;
        }

        if (!stylingNote.isBlank()) {
            fullOutfit.setStylingNote(stylingNote);
        }

        if (!occasionNote.isBlank()) {
            fullOutfit.setOccasionNote(occasionNote);
        }

        if (!seasonNote.isBlank()) {
            fullOutfit.setSeasonNote(seasonNote);
        }

        if (!colorNote.isBlank()) {
            fullOutfit.setColorNote(colorNote);
        }

        if (!fitNote.isBlank()) {
            fullOutfit.setFitNote(fitNote);
        }

        if (!materialNote.isBlank()) {
            fullOutfit.setMaterialNote(materialNote);
        }

        if (!preferenceNote.isBlank()) {
            fullOutfit.setPreferenceNote(preferenceNote);
        }
    }

    private Integer clampScore(Integer score) {
        if (score == null) {
            return null;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
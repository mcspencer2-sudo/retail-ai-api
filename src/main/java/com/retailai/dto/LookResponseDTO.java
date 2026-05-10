package com.retailai.dto;

import java.util.List;

public class LookResponseDTO {

    private FullOutfitDTO fullOutfit;
    private List<RecommendationItemDTO> suggestions;
    private Integer variation;

    public LookResponseDTO() {
    }

    public LookResponseDTO(FullOutfitDTO fullOutfit, List<RecommendationItemDTO> suggestions, Integer variation) {
        this.fullOutfit = fullOutfit;
        this.suggestions = suggestions;
        this.variation = variation;
    }

    public FullOutfitDTO getFullOutfit() {
        return fullOutfit;
    }

    public void setFullOutfit(FullOutfitDTO fullOutfit) {
        this.fullOutfit = fullOutfit;
    }

    public List<RecommendationItemDTO> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<RecommendationItemDTO> suggestions) {
        this.suggestions = suggestions;
    }

    public Integer getVariation() {
        return variation;
    }

    public void setVariation(Integer variation) {
        this.variation = variation;
    }
}
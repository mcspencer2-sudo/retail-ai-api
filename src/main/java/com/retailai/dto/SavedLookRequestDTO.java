package com.retailai.dto;

import java.util.List;

public class SavedLookRequestDTO {

    private String vibe;
    private String retailerKey;
    private String storeCode;
    private String storeName;
    private ScanResultDTO anchor;
    private FullOutfitDTO look;
    private List<RecommendationItemDTO> suggestions;

    public String getVibe() {
        return vibe;
    }

    public void setVibe(String vibe) {
        this.vibe = vibe;
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = retailerKey;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public ScanResultDTO getAnchor() {
        return anchor;
    }

    public void setAnchor(ScanResultDTO anchor) {
        this.anchor = anchor;
    }

    public FullOutfitDTO getLook() {
        return look;
    }

    public void setLook(FullOutfitDTO look) {
        this.look = look;
    }

    public List<RecommendationItemDTO> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<RecommendationItemDTO> suggestions) {
        this.suggestions = suggestions;
    }
}
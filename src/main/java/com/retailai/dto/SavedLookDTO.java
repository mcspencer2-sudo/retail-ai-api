package com.retailai.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavedLookDTO {

    private Long id;

    private String title;
    private String notes;
    private List<String> tags = new ArrayList<>();

    private String vibe;
    private String retailerKey;
    private String storeCode;
    private String storeName;

    private ScanResultDTO anchor;
    private FullOutfitDTO look;
    private List<RecommendationItemDTO> suggestions = new ArrayList<>();

    private Double score;
    private LocalDateTime savedAt;
    private boolean active;

    private String shareToken;
    private Boolean publicShareEnabled = false;
    private LocalDateTime shareCreatedAt;
    private String shareUrl;

    public SavedLookDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = safeNullable(title);
    }

    public String getName() {
        return getTitle();
    }

    public void setName(String name) {
        setTitle(name);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = safeNullable(notes);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = normalizeTags(tags);
    }

    public List<String> getTagList() {
        return getTags();
    }

    public void setTagList(List<String> tagList) {
        setTags(tagList);
    }

    public String getTagsCsv() {
        return String.join(",", tags);
    }

    public void setTagsCsv(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            this.tags = new ArrayList<>();
            return;
        }

        String[] parts = tagsCsv.split(",");
        List<String> parsedTags = new ArrayList<>();

        for (String part : parts) {
            String normalized = normalizeTag(part);

            if (!normalized.isBlank() && !parsedTags.contains(normalized)) {
                parsedTags.add(normalized);
            }
        }

        this.tags = parsedTags;
    }

    public String getVibe() {
        return vibe;
    }

    public void setVibe(String vibe) {
        this.vibe = safeNullable(vibe);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = safeNullable(retailerKey);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = safeNullable(storeCode);
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = safeNullable(storeName);
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
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score == null ? 0.0 : score;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = safeNullable(shareToken);
    }

    public Boolean getPublicShareEnabled() {
        return publicShareEnabled;
    }

    public boolean isPublicShareEnabled() {
        return Boolean.TRUE.equals(publicShareEnabled);
    }

    public void setPublicShareEnabled(Boolean publicShareEnabled) {
        this.publicShareEnabled = publicShareEnabled != null && publicShareEnabled;
    }

    public LocalDateTime getShareCreatedAt() {
        return shareCreatedAt;
    }

    public void setShareCreatedAt(LocalDateTime shareCreatedAt) {
        this.shareCreatedAt = shareCreatedAt;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = safeNullable(shareUrl);
    }

    private List<String> normalizeTags(List<String> values) {
        List<String> normalizedTags = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return normalizedTags;
        }

        for (String value : values) {
            String tag = normalizeTag(value);

            if (!tag.isBlank() && !normalizedTags.contains(tag)) {
                normalizedTags.add(tag);
            }
        }

        return normalizedTags;
    }

    private String normalizeTag(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
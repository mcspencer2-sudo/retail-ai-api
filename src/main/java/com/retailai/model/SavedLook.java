package com.retailai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "saved_look")
public class SavedLook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String vibe = "Casual";

    @Column(nullable = false)
    private Integer score = 0;

    @Column(nullable = false)
    private LocalDateTime savedAt = LocalDateTime.now();

    @Column(nullable = false, length = 255)
    private String title = "Saved Look";

    @Column(length = 4000)
    private String notes;

    @Column(length = 1000)
    private String tagsCsv = "";

    @Column(length = 120, unique = true)
    private String shareToken;

    @Column(nullable = false)
    private Boolean publicShareEnabled = false;

    private LocalDateTime shareCreatedAt;

    @Column(nullable = false)
    private String anchorRfid = "";

    @Column(nullable = false)
    private String anchorName = "";

    @Column(nullable = false)
    private String anchorBrand = "";

    @Column(nullable = false)
    private String anchorRetailer = "";

    @Column(nullable = false)
    private String anchorRetailerKey = "";

    @Column(nullable = false)
    private String anchorStoreCode = "";

    @Column(nullable = false)
    private String anchorStoreName = "";

    @Column(nullable = false)
    private String anchorCategory = "";

    @Column(nullable = false)
    private String anchorColor = "";

    @Column(nullable = false)
    private Double anchorPrice = 0.0;

    @Column(length = 2000)
    private String anchorImageUrl;

    @Column(length = 4000)
    private String anchorStylingAdvice;

    @Column(length = 4000)
    private String explanation;

    @Column(nullable = false)
    private Integer overallScore = 0;

    @Column(nullable = false)
    private Integer styleScore = 0;

    @Column(nullable = false)
    private Integer colorScore = 0;

    @Column(nullable = false)
    private Integer occasionScore = 0;

    @OneToMany(mappedBy = "savedLook", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<SavedLookItem> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = safe(userEmail);
    }

    public String getVibe() {
        return vibe;
    }

    public void setVibe(String vibe) {
        String cleaned = safe(vibe);
        this.vibe = cleaned.isBlank() ? "Casual" : cleaned;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score == null ? 0 : score;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt == null ? LocalDateTime.now() : savedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String cleaned = safe(title);
        this.title = cleaned.isBlank() ? "Saved Look" : cleaned;
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

    public String getTagsCsv() {
        return tagsCsv;
    }

    public void setTagsCsv(String tagsCsv) {
        this.tagsCsv = normalizeTagsCsv(tagsCsv);
    }

    public List<String> getTags() {
        return csvToTags(tagsCsv);
    }

    public void setTags(List<String> tags) {
        this.tagsCsv = tagsToCsv(tags);
    }

    public List<String> getTagList() {
        return getTags();
    }

    public void setTagList(List<String> tags) {
        setTags(tags);
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

    public void setPublicShareEnabled(Boolean publicShareEnabled) {
        this.publicShareEnabled = publicShareEnabled != null && publicShareEnabled;
    }

    public LocalDateTime getShareCreatedAt() {
        return shareCreatedAt;
    }

    public void setShareCreatedAt(LocalDateTime shareCreatedAt) {
        this.shareCreatedAt = shareCreatedAt;
    }

    public String getAnchorRfid() {
        return anchorRfid;
    }

    public void setAnchorRfid(String anchorRfid) {
        this.anchorRfid = safe(anchorRfid);
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = safe(anchorName);
    }

    public String getAnchorBrand() {
        return anchorBrand;
    }

    public void setAnchorBrand(String anchorBrand) {
        this.anchorBrand = safe(anchorBrand);
    }

    public String getAnchorRetailer() {
        return anchorRetailer;
    }

    public void setAnchorRetailer(String anchorRetailer) {
        this.anchorRetailer = safe(anchorRetailer);
    }

    public String getAnchorRetailerKey() {
        return anchorRetailerKey;
    }

    public void setAnchorRetailerKey(String anchorRetailerKey) {
        this.anchorRetailerKey = safe(anchorRetailerKey);
    }

    public String getAnchorStoreCode() {
        return anchorStoreCode;
    }

    public void setAnchorStoreCode(String anchorStoreCode) {
        this.anchorStoreCode = safe(anchorStoreCode);
    }

    public String getAnchorStoreName() {
        return anchorStoreName;
    }

    public void setAnchorStoreName(String anchorStoreName) {
        this.anchorStoreName = safe(anchorStoreName);
    }

    public String getAnchorCategory() {
        return anchorCategory;
    }

    public void setAnchorCategory(String anchorCategory) {
        this.anchorCategory = safe(anchorCategory);
    }

    public String getAnchorColor() {
        return anchorColor;
    }

    public void setAnchorColor(String anchorColor) {
        this.anchorColor = safe(anchorColor);
    }

    public Double getAnchorPrice() {
        return anchorPrice;
    }

    public void setAnchorPrice(Double anchorPrice) {
        this.anchorPrice = anchorPrice == null ? 0.0 : anchorPrice;
    }

    public String getAnchorImageUrl() {
        return anchorImageUrl;
    }

    public void setAnchorImageUrl(String anchorImageUrl) {
        this.anchorImageUrl = safeNullable(anchorImageUrl);
    }

    public String getAnchorStylingAdvice() {
        return anchorStylingAdvice;
    }

    public void setAnchorStylingAdvice(String anchorStylingAdvice) {
        this.anchorStylingAdvice = safeNullable(anchorStylingAdvice);
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = safeNullable(explanation);
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore == null ? 0 : overallScore;
    }

    public Integer getStyleScore() {
        return styleScore;
    }

    public void setStyleScore(Integer styleScore) {
        this.styleScore = styleScore == null ? 0 : styleScore;
    }

    public Integer getColorScore() {
        return colorScore;
    }

    public void setColorScore(Integer colorScore) {
        this.colorScore = colorScore == null ? 0 : colorScore;
    }

    public Integer getOccasionScore() {
        return occasionScore;
    }

    public void setOccasionScore(Integer occasionScore) {
        this.occasionScore = occasionScore == null ? 0 : occasionScore;
    }

    public List<SavedLookItem> getItems() {
        return items;
    }

    public void setItems(List<SavedLookItem> items) {
        this.items.clear();

        if (items == null) {
            return;
        }

        for (SavedLookItem item : items) {
            addItem(item);
        }
    }

    public void addItem(SavedLookItem item) {
        if (item == null) {
            return;
        }

        item.setSavedLook(this);
        this.items.add(item);
    }

    public void clearItems() {
        this.items.clear();
    }

    public boolean isPubliclyShareable() {
        return Boolean.TRUE.equals(publicShareEnabled)
                && shareToken != null
                && !shareToken.isBlank();
    }

    public void enablePublicShare(String token) {
        String cleanedToken = safe(token);

        if (cleanedToken.isBlank()) {
            throw new IllegalArgumentException("Share token is required.");
        }

        this.shareToken = cleanedToken;
        this.publicShareEnabled = true;
        this.shareCreatedAt = LocalDateTime.now();
    }

    public void disablePublicShare() {
        this.publicShareEnabled = false;
    }

    private String normalizeTagsCsv(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return tagsToCsv(Arrays.asList(value.split(",")));
    }

    private String tagsToCsv(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }

        List<String> cleanedTags = new ArrayList<>();

        for (String tag : tags) {
            String cleaned = normalizeTag(tag);

            if (!cleaned.isBlank() && !cleanedTags.contains(cleaned)) {
                cleanedTags.add(cleaned);
            }
        }

        return String.join(",", cleanedTags);
    }

    private List<String> csvToTags(String csv) {
        List<String> tags = new ArrayList<>();

        if (csv == null || csv.isBlank()) {
            return tags;
        }

        String[] parts = csv.split(",");

        for (String part : parts) {
            String cleaned = normalizeTag(part);

            if (!cleaned.isBlank() && !tags.contains(cleaned)) {
                tags.add(cleaned);
            }
        }

        return tags;
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
package com.retailai.service;

import com.retailai.dto.FullOutfitDTO;
import com.retailai.dto.RecommendationItemDTO;
import com.retailai.dto.SavedLookDTO;
import com.retailai.dto.SavedLookRequestDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.model.AppUser;
import com.retailai.model.SavedLook;
import com.retailai.model.SavedLookItem;
import com.retailai.repository.SavedLookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SavedLookService {

    private final SavedLookRepository savedLookRepository;
    private final CurrentUserService currentUserService;

    public SavedLookService(SavedLookRepository savedLookRepository,
                            CurrentUserService currentUserService) {
        this.savedLookRepository = savedLookRepository;
        this.currentUserService = currentUserService;
    }

    public SavedLookDTO saveLook(SavedLookRequestDTO payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Saved look payload is required.");
        }

        AppUser user = currentUserService.getCurrentUser();

        SavedLook savedLook = new SavedLook();
        savedLook.setTenantId(user.getTenantId());
        savedLook.setUserEmail(user.getEmail());
        savedLook.setVibe(isBlank(payload.getVibe()) ? "Casual" : payload.getVibe().trim());
        savedLook.setSavedAt(LocalDateTime.now());

        ScanResultDTO anchor = payload.getAnchor();
        FullOutfitDTO look = payload.getLook();

        savedLook.setAnchorRfid(anchor == null ? "" : safe(anchor.getRfid()));
        savedLook.setAnchorName(anchor == null ? "" : safe(anchor.getName()));
        savedLook.setAnchorBrand(anchor == null ? "" : safe(anchor.getBrand()));
        savedLook.setAnchorRetailer(anchor == null ? "" : safe(anchor.getRetailer()));
        savedLook.setAnchorRetailerKey(anchor == null ? safe(payload.getRetailerKey()) : safe(anchor.getRetailerKey()));
        savedLook.setAnchorStoreCode(anchor == null ? safe(payload.getStoreCode()) : safe(anchor.getStoreCode()));
        savedLook.setAnchorStoreName(anchor == null ? safe(payload.getStoreName()) : safe(anchor.getStoreName()));
        savedLook.setAnchorCategory(anchor == null ? "" : safe(anchor.getCategory()));
        savedLook.setAnchorColor(anchor == null ? "" : safe(anchor.getColor()));
        savedLook.setAnchorPrice(anchor == null ? 0.0 : safeDouble(anchor.getPrice()));
        savedLook.setAnchorImageUrl(anchor == null ? null : nullable(anchor.getImageUrl()));
        savedLook.setAnchorStylingAdvice(anchor == null ? null : nullable(anchor.getStylingAdvice()));

        savedLook.setExplanation(look == null ? null : nullable(look.getExplanation()));
        savedLook.setOverallScore(look == null ? 0 : safeInt(look.getOverallScore()));
        savedLook.setStyleScore(look == null ? 0 : safeInt(look.getStyleScore()));
        savedLook.setColorScore(look == null ? 0 : safeInt(look.getColorScore()));
        savedLook.setOccasionScore(look == null ? 0 : safeInt(look.getOccasionScore()));
        savedLook.setScore(savedLook.getOverallScore());

        savedLook.clearItems();

        if (look != null) {
            addRecommendationItem(savedLook, "TOP", look.getTop(), 1);
            addRecommendationItem(savedLook, "BOTTOM", look.getBottom(), 2);
            addRecommendationItem(savedLook, "SHOES", look.getShoes(), 3);
            addRecommendationItem(savedLook, "OUTERWEAR", look.getOuterwear(), 4);
        }

        List<RecommendationItemDTO> suggestions = payload.getSuggestions();
        if (suggestions != null && !suggestions.isEmpty()) {
            int displayOrder = 100;
            for (RecommendationItemDTO suggestion : suggestions) {
                addRecommendationItem(savedLook, "SUGGESTION", suggestion, displayOrder++);
            }
        }

        SavedLook persisted = savedLookRepository.save(savedLook);
        return toDto(persisted);
    }

    public List<SavedLookDTO> getSavedLooks() {
        AppUser user = currentUserService.getCurrentUser();

        return savedLookRepository.findByTenantIdAndUserEmailOrderBySavedAtDesc(
                        user.getTenantId(),
                        user.getEmail()
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    public SavedLookDTO getSavedLookById(Long lookId) {
        return toDto(getOwnedLook(lookId));
    }

    public void deleteSavedLook(Long lookId) {
        SavedLook savedLook = getOwnedLook(lookId);
        savedLookRepository.delete(savedLook);
    }

    public void clearSavedLooks() {
        AppUser user = currentUserService.getCurrentUser();

        List<SavedLook> looks = savedLookRepository.findByTenantIdAndUserEmailOrderBySavedAtDesc(
                user.getTenantId(),
                user.getEmail()
        );

        if (!looks.isEmpty()) {
            savedLookRepository.deleteAll(looks);
        }
    }

    private SavedLook getOwnedLook(Long lookId) {
        if (lookId == null) {
            throw new IllegalArgumentException("Saved look id is required.");
        }

        AppUser user = currentUserService.getCurrentUser();

        return savedLookRepository.findByIdAndTenantIdAndUserEmail(
                        lookId,
                        user.getTenantId(),
                        user.getEmail()
                )
                .orElseThrow(() -> new IllegalArgumentException("Saved look not found."));
    }

    private void addRecommendationItem(SavedLook savedLook,
                                       String role,
                                       RecommendationItemDTO dto,
                                       int displayOrder) {
        if (savedLook == null || dto == null) {
            return;
        }

        SavedLookItem item = new SavedLookItem();
        item.setSavedLook(savedLook);
        item.setRoleName(safe(role));
        item.setDisplayOrder(displayOrder);
        item.setRfid(safe(dto.getRfid()));
        item.setItemName(safe(dto.getName()));
        item.setBrand(safe(dto.getBrand()));
        item.setRetailerName(safe(dto.getRetailer()));
        item.setCategory(safe(dto.getCategory()));
        item.setColor(safe(dto.getColor()));
        item.setPrice(safeDouble(dto.getPrice()));
        item.setImageUrl(nullable(dto.getImageUrl()));
        item.setMatchScore(safeInt(dto.getMatchScore()));
        item.setStyleMatch(safeInt(dto.getStyleMatch()));
        item.setColorMatch(safeInt(dto.getColorMatch()));
        item.setOccasionMatch(safeInt(dto.getOccasionMatch()));
        item.setReason(nullable(dto.getReason()));

        savedLook.addItem(item);
    }

    private SavedLookDTO toDto(SavedLook savedLook) {
        SavedLookDTO dto = new SavedLookDTO();
        dto.setId(savedLook.getId());
        dto.setVibe(savedLook.getVibe());
        dto.setRetailerKey(savedLook.getAnchorRetailerKey());
        dto.setStoreCode(savedLook.getAnchorStoreCode());
        dto.setStoreName(savedLook.getAnchorStoreName());
        dto.setScore(savedLook.getScore() == null ? 0.0 : savedLook.getScore().doubleValue());
        dto.setSavedAt(savedLook.getSavedAt());
        dto.setActive(false);

        ScanResultDTO anchor = new ScanResultDTO();
        anchor.setRfid(savedLook.getAnchorRfid());
        anchor.setName(savedLook.getAnchorName());
        anchor.setBrand(savedLook.getAnchorBrand());
        anchor.setRetailer(savedLook.getAnchorRetailer());
        anchor.setRetailerKey(savedLook.getAnchorRetailerKey());
        anchor.setStoreCode(savedLook.getAnchorStoreCode());
        anchor.setStoreName(savedLook.getAnchorStoreName());
        anchor.setCategory(savedLook.getAnchorCategory());
        anchor.setColor(savedLook.getAnchorColor());
        anchor.setPrice(savedLook.getAnchorPrice());
        anchor.setImageUrl(savedLook.getAnchorImageUrl());
        anchor.setStylingAdvice(savedLook.getAnchorStylingAdvice());
        dto.setAnchor(anchor);

        FullOutfitDTO look = new FullOutfitDTO();
        look.setExplanation(savedLook.getExplanation());
        look.setOverallScore(safeInt(savedLook.getOverallScore()));
        look.setStyleScore(safeInt(savedLook.getStyleScore()));
        look.setColorScore(safeInt(savedLook.getColorScore()));
        look.setOccasionScore(safeInt(savedLook.getOccasionScore()));

        List<RecommendationItemDTO> suggestions = new ArrayList<>();

        if (savedLook.getItems() != null) {
            for (SavedLookItem item : savedLook.getItems()) {
                if (item == null) {
                    continue;
                }

                RecommendationItemDTO recommendation = new RecommendationItemDTO();
                recommendation.setRfid(item.getRfid());
                recommendation.setName(item.getItemName());
                recommendation.setBrand(item.getBrand());
                recommendation.setRetailer(item.getRetailerName());
                recommendation.setCategory(item.getCategory());
                recommendation.setColor(item.getColor());
                recommendation.setPrice(item.getPrice());
                recommendation.setImageUrl(item.getImageUrl());
                recommendation.setMatchScore(safeInt(item.getMatchScore()));
                recommendation.setStyleMatch(safeInt(item.getStyleMatch()));
                recommendation.setColorMatch(safeInt(item.getColorMatch()));
                recommendation.setOccasionMatch(safeInt(item.getOccasionMatch()));
                recommendation.setReason(item.getReason());

                String role = safe(item.getRoleName()).toUpperCase();

                switch (role) {
                    case "TOP" -> look.setTop(recommendation);
                    case "BOTTOM" -> look.setBottom(recommendation);
                    case "SHOES" -> look.setShoes(recommendation);
                    case "OUTERWEAR" -> look.setOuterwear(recommendation);
                    case "SUGGESTION" -> suggestions.add(recommendation);
                    default -> {
                    }
                }
            }
        }

        dto.setLook(look);
        dto.setSuggestions(suggestions);
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}
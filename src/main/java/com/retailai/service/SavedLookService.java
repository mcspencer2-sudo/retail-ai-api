package com.retailai.service;

import com.retailai.dto.FullOutfitDTO;
import com.retailai.dto.RecommendationItemDTO;
import com.retailai.dto.SavedLookDTO;
import com.retailai.dto.SavedLookRequestDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.model.AppUser;
import com.retailai.model.Product;
import com.retailai.model.SavedLook;
import com.retailai.model.SavedLookItem;
import com.retailai.repository.AppUserRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.SavedLookRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SavedLookService {

    private static final Long FALLBACK_TENANT_ID = 0L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SavedLookRepository savedLookRepository;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final HttpServletRequest request;
    private final ProductRepository productRepository;

    public SavedLookService(
            SavedLookRepository savedLookRepository,
            JwtService jwtService,
            AppUserRepository appUserRepository,
            HttpServletRequest request,
            ProductRepository productRepository
    ) {
        this.savedLookRepository = savedLookRepository;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.request = request;
        this.productRepository = productRepository;
    }

    @Transactional
    public SavedLookDTO saveLook(SavedLookRequestDTO payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Saved look payload is required.");
        }

        CurrentSavedLookOwner owner = resolveOwner();

        SavedLook savedLook = new SavedLook();
        savedLook.setTenantId(owner.tenantId());
        savedLook.setUserEmail(owner.email());
        savedLook.setVibe(isBlank(payload.getVibe()) ? "Casual" : payload.getVibe().trim());
        savedLook.setSavedAt(LocalDateTime.now());

        applySavedLookMetadata(savedLook, payload);

        ScanResultDTO anchor = payload.getAnchor();
        FullOutfitDTO look = payload.getLook();

        savedLook.setAnchorRfid(anchor == null ? "" : safe(anchor.getRfid()));
        savedLook.setAnchorName(anchor == null ? "" : safe(anchor.getName()));
        savedLook.setAnchorBrand(anchor == null ? "" : safe(anchor.getBrand()));
        savedLook.setAnchorRetailer(anchor == null ? "" : safe(anchor.getRetailer()));

        savedLook.setAnchorRetailerKey(
                anchor == null || isBlank(anchor.getRetailerKey())
                        ? safe(payload.getRetailerKey())
                        : safe(anchor.getRetailerKey())
        );

        savedLook.setAnchorStoreCode(
                anchor == null || isBlank(anchor.getStoreCode())
                        ? safe(payload.getStoreCode())
                        : safe(anchor.getStoreCode())
        );

        savedLook.setAnchorStoreName(
                anchor == null || isBlank(anchor.getStoreName())
                        ? safe(payload.getStoreName())
                        : safe(anchor.getStoreName())
        );

        savedLook.setAnchorCategory(anchor == null ? "" : safe(anchor.getCategory()));
        savedLook.setAnchorColor(anchor == null ? "" : safe(anchor.getColor()));
        savedLook.setAnchorPrice(anchor == null ? 0.0 : safeDouble(anchor.getPrice()));
        savedLook.setAnchorImageUrl(anchor == null ? null : nullable(anchor.getImageUrl()));
        savedLook.setAnchorStylingAdvice(anchor == null ? null : nullable(anchor.getStylingAdvice()));

        applyFullOutfit(savedLook, look);

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

    @Transactional(readOnly = true)
    public List<SavedLookDTO> getSavedLooks() {
        CurrentSavedLookOwner owner = resolveOwner();

        return savedLookRepository.findByTenantIdAndUserEmailOrderBySavedAtDesc(
                        owner.tenantId(),
                        owner.email()
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavedLookDTO getSavedLookById(Long lookId) {
        return toDto(getOwnedLook(lookId));
    }

    @Transactional
    public SavedLookDTO updateSavedLook(Long lookId, SavedLookRequestDTO payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Saved look update payload is required.");
        }

        SavedLook savedLook = getOwnedLook(lookId);

        applySavedLookMetadata(savedLook, payload);

        if (!isBlank(payload.getVibe())) {
            savedLook.setVibe(payload.getVibe().trim());
        }

        if (!isBlank(payload.getRetailerKey())) {
            savedLook.setAnchorRetailerKey(payload.getRetailerKey());
        }

        if (!isBlank(payload.getStoreCode())) {
            savedLook.setAnchorStoreCode(payload.getStoreCode());
        }

        if (!isBlank(payload.getStoreName())) {
            savedLook.setAnchorStoreName(payload.getStoreName());
        }

        SavedLook persisted = savedLookRepository.save(savedLook);

        return toDto(persisted);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkSavedLookAvailability(Long lookId) {
        SavedLook savedLook = getOwnedLook(lookId);

        List<Map<String, Object>> availableItems = new ArrayList<>();
        List<Map<String, Object>> unavailableItems = new ArrayList<>();
        Set<String> seenRfids = new LinkedHashSet<>();

        addAvailabilityCandidate(
                savedLook,
                seenRfids,
                availableItems,
                unavailableItems,
                "ANCHOR",
                savedLook.getAnchorRfid(),
                savedLook.getAnchorName()
        );

        if (savedLook.getItems() != null) {
            for (SavedLookItem item : savedLook.getItems()) {
                if (item == null) {
                    continue;
                }

                addAvailabilityCandidate(
                        savedLook,
                        seenRfids,
                        availableItems,
                        unavailableItems,
                        item.getRoleName(),
                        item.getRfid(),
                        item.getItemName()
                );
            }
        }

        boolean allAvailable = unavailableItems.isEmpty();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("savedLookId", savedLook.getId());
        response.put("totalCount", availableItems.size() + unavailableItems.size());
        response.put("availableCount", availableItems.size());
        response.put("unavailableCount", unavailableItems.size());
        response.put("availableItems", availableItems);
        response.put("unavailableItems", unavailableItems);
        response.put("allAvailable", allAvailable);
        response.put(
                "message",
                allAvailable
                        ? "All saved look items are currently available."
                        : unavailableItems.size() + " saved look item(s) are unavailable."
        );

        return response;
    }

    @Transactional(readOnly = true)
    public SavedLookDTO regenerateSavedLook(Long lookId) {
        SavedLook savedLook = getOwnedLook(lookId);

        /*
         * Temporary backend-safe regenerate behavior:
         * This endpoint currently rehydrates the saved look as a fresh workspace.
         * True AI regeneration should later call the outfit generation service using
         * the saved anchor RFID, vibe, tenant, retailer, and store context.
         */
        SavedLookDTO dto = toDto(savedLook);

        if (dto != null) {
            dto.setActive(false);
        }

        return dto;
    }

    @Transactional
    public Map<String, Object> createPublicShareToken(Long lookId) {
        SavedLook savedLook = getOwnedLook(lookId);

        String token = savedLook.getShareToken();

        if (isBlank(token)) {
            token = generateShareToken();
            savedLook.setShareToken(token);
        }

        savedLook.setPublicShareEnabled(true);
        savedLook.setShareCreatedAt(LocalDateTime.now());

        SavedLook persisted = savedLookRepository.save(savedLook);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("savedLookId", persisted.getId());
        response.put("shareToken", persisted.getShareToken());
        response.put("publicShareEnabled", Boolean.TRUE.equals(persisted.getPublicShareEnabled()));
        response.put("shareCreatedAt", persisted.getShareCreatedAt());
        response.put("message", "Public share link created.");

        return response;
    }

    @Transactional
    public Map<String, Object> disablePublicShare(Long lookId) {
        SavedLook savedLook = getOwnedLook(lookId);

        savedLook.setPublicShareEnabled(false);

        SavedLook persisted = savedLookRepository.save(savedLook);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("savedLookId", persisted.getId());
        response.put("shareToken", persisted.getShareToken());
        response.put("publicShareEnabled", Boolean.TRUE.equals(persisted.getPublicShareEnabled()));
        response.put("message", "Public share link disabled.");

        return response;
    }

    @Transactional(readOnly = true)
    public SavedLookDTO getPublicSharedLook(String shareToken) {
        String safeToken = safe(shareToken);

        if (safeToken.isBlank()) {
            throw new IllegalArgumentException("Share token is required.");
        }

        SavedLook savedLook = savedLookRepository.findByShareTokenAndPublicShareEnabledTrue(safeToken)
                .orElseThrow(() -> new IllegalArgumentException("Shared look not found."));

        SavedLookDTO dto = toDto(savedLook);

        if (dto != null) {
            dto.setActive(false);
        }

        return dto;
    }

    @Transactional
    public void deleteSavedLook(Long lookId) {
        SavedLook savedLook = getOwnedLook(lookId);
        savedLookRepository.delete(savedLook);
    }

    @Transactional
    public void clearSavedLooks() {
        CurrentSavedLookOwner owner = resolveOwner();

        List<SavedLook> looks = savedLookRepository.findByTenantIdAndUserEmailOrderBySavedAtDesc(
                owner.tenantId(),
                owner.email()
        );

        if (!looks.isEmpty()) {
            savedLookRepository.deleteAll(looks);
        }
    }

    private void applySavedLookMetadata(SavedLook savedLook, SavedLookRequestDTO payload) {
        if (savedLook == null || payload == null) {
            return;
        }

        String title = firstNonBlank(
                payload.getTitle(),
                payload.getName(),
                savedLook.getAnchorName(),
                "Saved Look"
        );

        String notes = safe(payload.getNotes());

        savedLook.setTitle(title);
        savedLook.setNotes(notes);
        savedLook.setTagsCsv(tagsToCsv(payload.getTags()));
    }

    private void applyFullOutfit(SavedLook savedLook, FullOutfitDTO look) {
        savedLook.setExplanation(look == null ? null : nullable(look.getExplanation()));
        savedLook.setOverallScore(look == null ? 0 : safeInt(look.getOverallScore()));
        savedLook.setStyleScore(look == null ? 0 : safeInt(look.getStyleScore()));
        savedLook.setColorScore(look == null ? 0 : safeInt(look.getColorScore()));
        savedLook.setOccasionScore(look == null ? 0 : safeInt(look.getOccasionScore()));
        savedLook.setScore(savedLook.getOverallScore());

        savedLook.clearItems();

        if (look == null) {
            return;
        }

        addRecommendationItem(savedLook, "TOP", look.getTop(), 1);
        addRecommendationItem(savedLook, "BOTTOM", look.getBottom(), 2);
        addRecommendationItem(savedLook, "SHOES", look.getShoes(), 3);
        addRecommendationItem(savedLook, "OUTERWEAR", look.getOuterwear(), 4);
    }

    private void addAvailabilityCandidate(
            SavedLook savedLook,
            Set<String> seenRfids,
            List<Map<String, Object>> availableItems,
            List<Map<String, Object>> unavailableItems,
            String role,
            String rfid,
            String fallbackName
    ) {
        String safeRfid = safe(rfid);

        if (safeRfid.isBlank()) {
            return;
        }

        String seenKey = safeRfid.toUpperCase(Locale.ROOT);

        if (seenRfids.contains(seenKey)) {
            return;
        }

        seenRfids.add(seenKey);

        String retailerKey = safe(savedLook.getAnchorRetailerKey()).toUpperCase(Locale.ROOT);
        String storeCode = safe(savedLook.getAnchorStoreCode()).toUpperCase(Locale.ROOT);

        Optional<Product> productOptional = productRepository.findByRfidAndRetailerKeyAndStoreCode(
                seenKey,
                retailerKey,
                storeCode
        );

        if (productOptional.isEmpty()) {
            unavailableItems.add(buildAvailabilityItem(
                    role,
                    safeRfid,
                    fallbackName,
                    "Missing from inventory",
                    false,
                    false,
                    0
            ));
            return;
        }

        Product product = productOptional.get();

        boolean active = Boolean.TRUE.equals(product.getActive());
        boolean available = Boolean.TRUE.equals(product.getAvailable());
        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        if (!active) {
            unavailableItems.add(buildAvailabilityItem(
                    role,
                    product.getRfid(),
                    product.getItemName(),
                    "Inactive",
                    false,
                    available,
                    stockQuantity
            ));
            return;
        }

        if (!available) {
            unavailableItems.add(buildAvailabilityItem(
                    role,
                    product.getRfid(),
                    product.getItemName(),
                    "Unavailable",
                    active,
                    false,
                    stockQuantity
            ));
            return;
        }

        if (stockQuantity <= 0) {
            unavailableItems.add(buildAvailabilityItem(
                    role,
                    product.getRfid(),
                    product.getItemName(),
                    "Out of stock",
                    active,
                    available,
                    stockQuantity
            ));
            return;
        }

        availableItems.add(buildAvailabilityItem(
                role,
                product.getRfid(),
                product.getItemName(),
                "Available",
                active,
                available,
                stockQuantity
        ));
    }

    private Map<String, Object> buildAvailabilityItem(
            String role,
            String rfid,
            String name,
            String status,
            boolean active,
            boolean available,
            int stockQuantity
    ) {
        Map<String, Object> item = new LinkedHashMap<>();

        item.put("role", safe(role));
        item.put("rfid", safe(rfid));
        item.put("name", firstNonBlank(name, rfid, "Item"));
        item.put("status", safe(status));
        item.put("reason", safe(status));
        item.put("active", active);
        item.put("available", available);
        item.put("stockQuantity", Math.max(0, stockQuantity));

        return item;
    }

    private SavedLook getOwnedLook(Long lookId) {
        if (lookId == null) {
            throw new IllegalArgumentException("Saved look id is required.");
        }

        CurrentSavedLookOwner owner = resolveOwner();

        return savedLookRepository.findByIdAndTenantIdAndUserEmail(
                        lookId,
                        owner.tenantId(),
                        owner.email()
                )
                .orElseThrow(() -> new IllegalArgumentException("Saved look not found."));
    }

    private CurrentSavedLookOwner resolveOwner() {
        String token = extractBearerToken();

        if (token.isBlank()) {
            throw new SecurityException("Missing authentication token.");
        }

        if (!jwtService.isTokenValid(token)) {
            throw new SecurityException("Invalid authentication token.");
        }

        String email = safe(jwtService.extractEmail(token)).toLowerCase(Locale.ROOT);

        if (email.isBlank()) {
            throw new SecurityException("Token does not contain a valid email.");
        }

        Optional<AppUser> userOptional = appUserRepository.findByEmailIgnoreCase(email);

        if (userOptional.isPresent()) {
            AppUser user = userOptional.get();

            Long tenantId = user.getTenantId() == null
                    ? FALLBACK_TENANT_ID
                    : user.getTenantId();

            String userEmail = safe(user.getEmail()).toLowerCase(Locale.ROOT);

            return new CurrentSavedLookOwner(
                    tenantId,
                    userEmail.isBlank() ? email : userEmail
            );
        }

        return new CurrentSavedLookOwner(FALLBACK_TENANT_ID, email);
    }

    private String extractBearerToken() {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            return "";
        }

        if (!authHeader.startsWith("Bearer ")) {
            return "";
        }

        return authHeader.substring(7).trim();
    }

    private void addRecommendationItem(
            SavedLook savedLook,
            String role,
            RecommendationItemDTO dto,
            int displayOrder
    ) {
        if (savedLook == null || dto == null) {
            return;
        }

        String rfid = safe(dto.getRfid());

        if (rfid.isBlank()) {
            return;
        }

        SavedLookItem item = new SavedLookItem();

        item.setSavedLook(savedLook);
        item.setRoleName(safe(role));
        item.setDisplayOrder(displayOrder);
        item.setRfid(rfid);
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
        if (savedLook == null) {
            return null;
        }

        SavedLookDTO dto = new SavedLookDTO();

        dto.setId(savedLook.getId());
        dto.setTitle(firstNonBlank(savedLook.getTitle(), savedLook.getAnchorName(), "Saved Look"));
        dto.setName(firstNonBlank(savedLook.getTitle(), savedLook.getAnchorName(), "Saved Look"));
        dto.setNotes(savedLook.getNotes());
        dto.setTags(csvToTags(savedLook.getTagsCsv()));

        dto.setVibe(savedLook.getVibe());
        dto.setRetailerKey(savedLook.getAnchorRetailerKey());
        dto.setStoreCode(savedLook.getAnchorStoreCode());
        dto.setStoreName(savedLook.getAnchorStoreName());
        dto.setScore(savedLook.getScore() == null ? 0.0 : savedLook.getScore().doubleValue());
        dto.setSavedAt(savedLook.getSavedAt());
        dto.setActive(true);

        dto.setShareToken(savedLook.getShareToken());
        dto.setPublicShareEnabled(savedLook.getPublicShareEnabled());
        dto.setShareCreatedAt(savedLook.getShareCreatedAt());

        if (
                Boolean.TRUE.equals(savedLook.getPublicShareEnabled()) &&
                        savedLook.getShareToken() != null &&
                        !savedLook.getShareToken().isBlank()
        ) {
            dto.setShareUrl("/shared-look/" + savedLook.getShareToken());
        } else {
            dto.setShareUrl(null);
        }

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

                RecommendationItemDTO recommendation = toRecommendationDto(item);
                String role = safe(item.getRoleName()).toUpperCase(Locale.ROOT);

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

    private RecommendationItemDTO toRecommendationDto(SavedLookItem item) {
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

        return recommendation;
    }

    private String tagsToCsv(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }

        return String.join(
                ",",
                tags.stream()
                        .map(this::normalizeTag)
                        .filter(tag -> !tag.isBlank())
                        .distinct()
                        .toList()
        );
    }

    private List<String> csvToTags(String value) {
        List<String> tags = new ArrayList<>();

        if (value == null || value.isBlank()) {
            return tags;
        }

        String[] parts = value.split(",");

        for (String part : parts) {
            String tag = normalizeTag(part);

            if (!tag.isBlank()) {
                tags.add(tag);
            }
        }

        return tags;
    }

    private String normalizeTag(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            String safeValue = safe(value);

            if (!safeValue.isBlank()) {
                return safeValue;
            }
        }

        return "";
    }

    private String generateShareToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            byte[] bytes = new byte[24];
            SECURE_RANDOM.nextBytes(bytes);

            String token = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes);

            if (!savedLookRepository.existsByShareToken(token)) {
                return token;
            }
        }

        throw new IllegalStateException("Unable to generate a unique share token.");
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

    private record CurrentSavedLookOwner(
            Long tenantId,
            String email
    ) {
    }
}
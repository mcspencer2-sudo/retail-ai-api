package com.retailai.service;

import com.retailai.dto.FullOutfitDTO;
import com.retailai.dto.RecommendationItemDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.model.Product;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AIStylistService {

    private static final String PLACEHOLDER_IMAGE =
            "https://placehold.co/500x620?text=No+Image";

    private static final List<String> OUTFIT_CATEGORY_ORDER = List.of(
            "tops",
            "bottoms",
            "shoes",
            "outerwear"
    );

    public String generateAdvice(Product product, String vibe) {
        String category = normalizeCategory(product != null ? product.getCategory() : null);
        String item = displayItemName(product != null ? product.getItemName() : null);

        return switch (safeLower(vibe)) {
            case "casual" -> casualAdvice(category, item);
            case "formal" -> formalAdvice(category, item);
            case "date night" -> dateNightAdvice(category, item);
            case "streetwear" -> streetwearAdvice(category, item);
            case "luxury" -> luxuryAdvice(category, item);
            default -> defaultAdvice(item);
        };
    }

    public FullOutfitDTO buildFullOutfit(ScanResultDTO scanResultDTO) {
        if (scanResultDTO == null) {
            return null;
        }

        List<RecommendationItemDTO> suggestions = scanResultDTO.getSuggestions() == null
                ? List.of()
                : scanResultDTO.getSuggestions();

        RecommendationItemDTO anchorItem = toAnchorRecommendation(scanResultDTO);
        String anchorCategory = normalizeCategory(scanResultDTO.getCategory());

        Set<String> usedRfids = new LinkedHashSet<>();
        addUsedRfid(usedRfids, anchorItem);

        RecommendationItemDTO top = null;
        RecommendationItemDTO bottom = null;
        RecommendationItemDTO shoes = null;
        RecommendationItemDTO outerwear = null;

        if ("tops".equals(anchorCategory)) {
            top = anchorItem;
        }

        if ("bottoms".equals(anchorCategory)) {
            bottom = anchorItem;
        }

        if ("shoes".equals(anchorCategory)) {
            shoes = anchorItem;
        }

        if ("outerwear".equals(anchorCategory)) {
            outerwear = anchorItem;
        }

        if (top == null) {
            top = findBestByCategory(suggestions, "tops", usedRfids, anchorItem);
            addUsedRfid(usedRfids, top);
        }

        if (bottom == null) {
            bottom = findBestByCategory(suggestions, "bottoms", usedRfids, anchorItem);
            addUsedRfid(usedRfids, bottom);
        }

        if (shoes == null) {
            shoes = findBestByCategory(suggestions, "shoes", usedRfids, anchorItem);
            addUsedRfid(usedRfids, shoes);
        }

        if (outerwear == null) {
            outerwear = findBestByCategory(suggestions, "outerwear", usedRfids, anchorItem);
            addUsedRfid(usedRfids, outerwear);
        }

        if (top == null && bottom == null && shoes == null && outerwear == null) {
            return null;
        }

        FullOutfitDTO outfit = new FullOutfitDTO();
        outfit.setTop(top);
        outfit.setBottom(bottom);
        outfit.setShoes(shoes);
        outfit.setOuterwear(outerwear);

        applyScoresAndExplanation(
                outfit,
                generateOutfitExplanation(scanResultDTO, top, bottom, shoes, outerwear)
        );

        return outfit;
    }

    public FullOutfitDTO buildFullOutfitFromProducts(
            Product scannedProduct,
            Product topProduct,
            Product bottomProduct,
            Product shoesProduct,
            Product outerwearProduct,
            String vibe
    ) {
        if (scannedProduct == null) {
            return null;
        }

        String scannedCategory = normalizeCategory(scannedProduct.getCategory());

        RecommendationItemDTO scannedAnchor = toOutfitItemDto(scannedProduct, scannedProduct, vibe);

        Set<String> usedRfids = new LinkedHashSet<>();
        addUsedRfid(usedRfids, scannedAnchor);

        RecommendationItemDTO top = null;
        RecommendationItemDTO bottom = null;
        RecommendationItemDTO shoes = null;
        RecommendationItemDTO outerwear = null;

        if ("tops".equals(scannedCategory)) {
            top = scannedAnchor;
        } else if (isProductAvailable(topProduct)) {
            top = toOutfitItemDto(scannedProduct, topProduct, vibe);

            if (isUsedRfid(usedRfids, top)) {
                top = null;
            }

            addUsedRfid(usedRfids, top);
        }

        if ("bottoms".equals(scannedCategory)) {
            bottom = scannedAnchor;
        } else if (isProductAvailable(bottomProduct)) {
            bottom = toOutfitItemDto(scannedProduct, bottomProduct, vibe);

            if (isUsedRfid(usedRfids, bottom)) {
                bottom = null;
            }

            addUsedRfid(usedRfids, bottom);
        }

        if ("shoes".equals(scannedCategory)) {
            shoes = scannedAnchor;
        } else if (isProductAvailable(shoesProduct)) {
            shoes = toOutfitItemDto(scannedProduct, shoesProduct, vibe);

            if (isUsedRfid(usedRfids, shoes)) {
                shoes = null;
            }

            addUsedRfid(usedRfids, shoes);
        }

        if ("outerwear".equals(scannedCategory)) {
            outerwear = scannedAnchor;
        } else if (isProductAvailable(outerwearProduct)) {
            outerwear = toOutfitItemDto(scannedProduct, outerwearProduct, vibe);

            if (isUsedRfid(usedRfids, outerwear)) {
                outerwear = null;
            }

            addUsedRfid(usedRfids, outerwear);
        }

        if (top == null && bottom == null && shoes == null && outerwear == null) {
            return null;
        }

        FullOutfitDTO outfit = new FullOutfitDTO();
        outfit.setTop(top);
        outfit.setBottom(bottom);
        outfit.setShoes(shoes);
        outfit.setOuterwear(outerwear);

        applyScoresAndExplanation(
                outfit,
                generateOutfitExplanationFromProducts(
                        scannedProduct,
                        topProduct,
                        bottomProduct,
                        shoesProduct,
                        outerwearProduct
                )
        );

        return outfit;
    }

    private void applyScoresAndExplanation(
            FullOutfitDTO outfit,
            String explanation
    ) {
        int styleScore = averageScores(
                scoreOrNull(outfit.getTop() != null ? outfit.getTop().getStyleMatch() : null),
                scoreOrNull(outfit.getBottom() != null ? outfit.getBottom().getStyleMatch() : null),
                scoreOrNull(outfit.getShoes() != null ? outfit.getShoes().getStyleMatch() : null),
                scoreOrNull(outfit.getOuterwear() != null ? outfit.getOuterwear().getStyleMatch() : null)
        );

        int colorScore = averageScores(
                scoreOrNull(outfit.getTop() != null ? outfit.getTop().getColorMatch() : null),
                scoreOrNull(outfit.getBottom() != null ? outfit.getBottom().getColorMatch() : null),
                scoreOrNull(outfit.getShoes() != null ? outfit.getShoes().getColorMatch() : null),
                scoreOrNull(outfit.getOuterwear() != null ? outfit.getOuterwear().getColorMatch() : null)
        );

        int occasionScore = averageScores(
                scoreOrNull(outfit.getTop() != null ? outfit.getTop().getOccasionMatch() : null),
                scoreOrNull(outfit.getBottom() != null ? outfit.getBottom().getOccasionMatch() : null),
                scoreOrNull(outfit.getShoes() != null ? outfit.getShoes().getOccasionMatch() : null),
                scoreOrNull(outfit.getOuterwear() != null ? outfit.getOuterwear().getOccasionMatch() : null)
        );

        int overallScore = clampScore(Math.round(
                (styleScore * 0.5f)
                        + (colorScore * 0.3f)
                        + (occasionScore * 0.2f)
        ));

        outfit.setStyleScore(styleScore);
        outfit.setColorScore(colorScore);
        outfit.setOccasionScore(occasionScore);
        outfit.setOverallScore(overallScore);
        outfit.setExplanation(cleanSentence(explanation));
    }

    private RecommendationItemDTO toAnchorRecommendation(ScanResultDTO scanResultDTO) {
        int baseScore = defaultedScore(scanResultDTO.getMatchScore(), 88);

        RecommendationItemDTO item = new RecommendationItemDTO();
        item.setRfid(safe(scanResultDTO.getRfid()));
        item.setName(safe(scanResultDTO.getName()));
        item.setBrand(safe(scanResultDTO.getBrand()));
        item.setCategory(safe(scanResultDTO.getCategory()));
        item.setColor(safe(scanResultDTO.getColor()));
        item.setRetailer(safe(scanResultDTO.getRetailer()));
        item.setPrice(safePrice(scanResultDTO.getPrice()));
        item.setImageUrl(safeImage(scanResultDTO.getImageUrl()));
        item.setMatchScore(clampScore(baseScore));
        item.setStyleMatch(clampScore(baseScore));
        item.setColorMatch(clampScore(baseScore));
        item.setOccasionMatch(clampScore(baseScore));
        item.setReason(safe(scanResultDTO.getWhyItWorks()));
        return item;
    }

    private RecommendationItemDTO toOutfitItemDto(Product scannedProduct, Product candidate, String vibe) {
        if (candidate == null) {
            return null;
        }

        int styleMatch = calculateStyleMatch(scannedProduct, candidate, vibe);
        int colorMatch = calculateColorMatch(scannedProduct, candidate);
        int occasionMatch = calculateOccasionMatch(candidate, vibe);
        int materialSeasonMatch = calculateMaterialSeasonMatch(candidate, vibe);
        int priceCompatibility = calculateProductPriceCompatibility(scannedProduct, candidate);

        int overallMatch = clampScore(Math.round(
                (styleMatch * 0.32f)
                        + (colorMatch * 0.24f)
                        + (occasionMatch * 0.22f)
                        + (materialSeasonMatch * 0.12f)
                        + (priceCompatibility * 0.10f)
        ));

        RecommendationItemDTO dto = new RecommendationItemDTO();
        dto.setRfid(safe(candidate.getRfid()));
        dto.setName(safe(candidate.getItemName()));
        dto.setBrand(safeBrand(candidate));
        dto.setCategory(safe(candidate.getCategory()));
        dto.setColor(safeColor(candidate));
        dto.setRetailer(safe(candidate.getRetailerName()));
        dto.setPrice(safePrice(candidate.getPrice()));
        dto.setMatchScore(overallMatch);
        dto.setStyleMatch(styleMatch);
        dto.setColorMatch(colorMatch);
        dto.setOccasionMatch(occasionMatch);
        dto.setReason(generateRecommendationReason(scannedProduct, candidate, vibe));
        dto.setImageUrl(safeImage(candidate.getImageUrl()));
        return dto;
    }

    private RecommendationItemDTO findBestByCategory(
            List<RecommendationItemDTO> suggestions,
            String normalizedCategory,
            Set<String> usedRfids,
            RecommendationItemDTO anchorItem
    ) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }

        return suggestions.stream()
                .filter(Objects::nonNull)
                .filter(this::isRecommendationAvailable)
                .filter(item -> normalizeCategory(item.getCategory()).equals(normalizedCategory))
                .filter(item -> !isUsedRfid(usedRfids, item))
                .filter(item -> !sameRfid(item, anchorItem) || normalizeCategory(anchorItem.getCategory()).equals(normalizedCategory))
                .max(Comparator.comparingInt(item -> combinedItemScore(item, anchorItem, normalizedCategory)))
                .orElse(null);
    }

    private void addUsedRfid(Set<String> usedRfids, RecommendationItemDTO item) {
        if (usedRfids == null || item == null) {
            return;
        }

        String rfid = safe(item.getRfid()).toUpperCase();

        if (!rfid.isBlank()) {
            usedRfids.add(rfid);
        }
    }

    private boolean isUsedRfid(Set<String> usedRfids, RecommendationItemDTO item) {
        if (usedRfids == null || item == null) {
            return false;
        }

        String rfid = safe(item.getRfid()).toUpperCase();

        return !rfid.isBlank() && usedRfids.contains(rfid);
    }

    private boolean isTopCategory(String category) {
        return "tops".equals(normalizeCategory(category));
    }

    private boolean isBottomCategory(String category) {
        return "bottoms".equals(normalizeCategory(category));
    }

    private boolean isShoeCategory(String category) {
        return "shoes".equals(normalizeCategory(category));
    }

    private boolean isOuterwearCategory(String category) {
        return "outerwear".equals(normalizeCategory(category));
    }

    private int combinedItemScore(
            RecommendationItemDTO item,
            RecommendationItemDTO anchorItem,
            String targetCategory
    ) {
        if (item == null) {
            return 0;
        }

        int style = defaultedScore(item.getStyleMatch(), 76);
        int color = defaultedScore(item.getColorMatch(), 76);
        int occasion = defaultedScore(item.getOccasionMatch(), 76);
        int match = defaultedScore(item.getMatchScore(), 76);

        int score = Math.round(
                (style * 0.34f)
                        + (color * 0.24f)
                        + (occasion * 0.22f)
                        + (match * 0.20f)
        );

        score += categoryPriorityBoost(item, targetCategory);
        score += colorHarmonyBoost(anchorItem, item);
        score += priceCompatibilityBoost(anchorItem, item);

        if (!isRecommendationAvailable(item)) {
            score -= 35;
        }

        return clampScore(score);
    }

    private int categoryPriorityBoost(RecommendationItemDTO item, String targetCategory) {
        String category = normalizeCategory(item != null ? item.getCategory() : null);

        if (category.equals(targetCategory)) {
            return 8;
        }

        if (OUTFIT_CATEGORY_ORDER.contains(category)) {
            return 3;
        }

        return 0;
    }

    private int colorHarmonyBoost(RecommendationItemDTO anchorItem, RecommendationItemDTO candidate) {
        if (anchorItem == null || candidate == null) {
            return 0;
        }

        String anchorColor = safeLower(anchorItem.getColor());
        String candidateColor = safeLower(candidate.getColor());

        if (anchorColor.isBlank() || candidateColor.isBlank()) {
            return 0;
        }

        if (anchorColor.equals(candidateColor)) {
            return 4;
        }

        if (isNeutral(anchorColor) || isNeutral(candidateColor)) {
            return 7;
        }

        if (sameKnownColorFamily(anchorColor, candidateColor)) {
            return 5;
        }

        if (isComplementaryColorFamily(anchorColor, candidateColor)) {
            return 4;
        }

        return 0;
    }

    private int priceCompatibilityBoost(RecommendationItemDTO anchorItem, RecommendationItemDTO candidate) {
        if (anchorItem == null || candidate == null) {
            return 0;
        }

        double anchorPrice = anchorItem.getPrice() == null ? 0.0 : anchorItem.getPrice();
        double candidatePrice = candidate.getPrice() == null ? 0.0 : candidate.getPrice();

        if (anchorPrice <= 0 || candidatePrice <= 0) {
            return 0;
        }

        String anchorTier = priceTier(anchorPrice);
        String candidateTier = priceTier(candidatePrice);

        if (anchorTier.equals(candidateTier)) {
            return 5;
        }

        if (isAdjacentPriceTier(anchorTier, candidateTier)) {
            return 3;
        }

        return -3;
    }

    private String priceTier(double price) {
        if (price < 50) {
            return "budget";
        }

        if (price < 150) {
            return "mid";
        }

        if (price < 350) {
            return "premium";
        }

        return "luxury";
    }

    private boolean isAdjacentPriceTier(String a, String b) {
        List<String> tiers = List.of("budget", "mid", "premium", "luxury");

        int first = tiers.indexOf(a);
        int second = tiers.indexOf(b);

        if (first < 0 || second < 0) {
            return false;
        }

        return Math.abs(first - second) == 1;
    }

    private boolean sameRfid(RecommendationItemDTO first, RecommendationItemDTO second) {
        String firstRfid = safe(first != null ? first.getRfid() : "").trim();
        String secondRfid = safe(second != null ? second.getRfid() : "").trim();

        return !firstRfid.isBlank() && firstRfid.equalsIgnoreCase(secondRfid);
    }

    private boolean isRecommendationAvailable(RecommendationItemDTO item) {
        if (item == null) {
            return false;
        }

        Boolean available = readBooleanFlag(item, "getAvailable", "isAvailable");
        Boolean active = readBooleanFlag(item, "getActive", "isActive");
        Boolean enabled = readBooleanFlag(item, "getEnabled", "isEnabled");
        Boolean outOfStock = readBooleanFlag(item, "getOutOfStock", "isOutOfStock");
        Boolean discontinued = readBooleanFlag(item, "getDiscontinued", "isDiscontinued");
        Number stock = readNumberField(item, "getStockQuantity", "getStock", "getQuantity", "getInventoryCount");

        if (available != null && !available) {
            return false;
        }

        if (active != null && !active) {
            return false;
        }

        if (enabled != null && !enabled) {
            return false;
        }

        if (outOfStock != null && outOfStock) {
            return false;
        }

        if (discontinued != null && discontinued) {
            return false;
        }

        return stock == null || stock.doubleValue() > 0;
    }

    private boolean isProductAvailable(Product product) {
        if (product == null) {
            return false;
        }

        Boolean available = readBooleanFlag(product, "getAvailable", "isAvailable");
        Boolean active = readBooleanFlag(product, "getActive", "isActive");
        Boolean enabled = readBooleanFlag(product, "getEnabled", "isEnabled");
        Boolean outOfStock = readBooleanFlag(product, "getOutOfStock", "isOutOfStock");
        Boolean discontinued = readBooleanFlag(product, "getDiscontinued", "isDiscontinued");
        Number stock = readNumberField(product, "getStockQuantity", "getStock", "getQuantity", "getInventoryCount");

        if (available != null && !available) {
            return false;
        }

        if (active != null && !active) {
            return false;
        }

        if (enabled != null && !enabled) {
            return false;
        }

        if (outOfStock != null && outOfStock) {
            return false;
        }

        if (discontinued != null && discontinued) {
            return false;
        }

        return stock == null || stock.doubleValue() > 0;
    }

    private Boolean readBooleanFlag(Object source, String... methodNames) {
        Object value = readReflectiveValue(source, methodNames);

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return null;
    }

    private Number readNumberField(Object source, String... methodNames) {
        Object value = readReflectiveValue(source, methodNames);

        if (value instanceof Number numberValue) {
            return numberValue;
        }

        return null;
    }

    private Object readReflectiveValue(Object source, String... methodNames) {
        if (source == null || methodNames == null) {
            return null;
        }

        for (String methodName : methodNames) {
            try {
                Method method = source.getClass().getMethod(methodName);
                return method.invoke(source);
            } catch (ReflectiveOperationException ignored) {
                // Optional getter not present on this DTO/model version.
            }
        }

        return null;
    }

    private Integer scoreOrNull(Integer score) {
        return score == null ? null : clampScore(score);
    }

    private int averageScores(Integer... scores) {
        int sum = 0;
        int count = 0;

        for (Integer score : scores) {
            if (score != null) {
                sum += score;
                count++;
            }
        }

        if (count == 0) {
            return 0;
        }

        return Math.round((float) sum / count);
    }

    private String generateOutfitExplanation(
            ScanResultDTO scanResultDTO,
            RecommendationItemDTO top,
            RecommendationItemDTO bottom,
            RecommendationItemDTO shoes,
            RecommendationItemDTO outerwear
    ) {
        StringBuilder sb = new StringBuilder();
        String anchorName = displayItemName(scanResultDTO.getName());
        String anchorColorFamily = getColorFamily(scanResultDTO.getColor());

        if ("neutral".equals(anchorColorFamily)) {
            sb.append("The scanned anchor keeps the palette grounded and gives the look a clean starting point");
        } else {
            sb.append(anchorName).append(" sets the tone of the outfit and gives it a clear focal point");
        }

        appendRecommendationExplanation(sb, top, scanResultDTO.getRfid(), "sharpens the line through the top half");
        appendRecommendationExplanation(sb, bottom, scanResultDTO.getRfid(), bottomPhrase(bottom));
        appendRecommendationExplanation(sb, outerwear, scanResultDTO.getRfid(), "adds depth and structure without overpowering the look");
        appendRecommendationExplanation(sb, shoes, scanResultDTO.getRfid(), shoePhrase(shoes));

        appendMissingCategoryExplanation(sb, top, bottom, shoes, outerwear);

        sb.append(".");
        return sb.toString();
    }

    private String generateOutfitExplanationFromProducts(
            Product scannedProduct,
            Product top,
            Product bottom,
            Product shoes,
            Product outerwear
    ) {
        StringBuilder sb = new StringBuilder();
        String scannedCategory = normalizeCategory(scannedProduct.getCategory());
        String anchorName = displayItemName(scannedProduct.getItemName());
        String anchorColorFamily = getColorFamily(scannedProduct.getColor());

        if ("neutral".equals(anchorColorFamily)) {
            sb.append("The scanned anchor keeps the palette grounded and gives the look a clean starting point");
        } else {
            sb.append(anchorName).append(" gives the outfit a strong visual starting point");
        }

        if (top != null && !"tops".equals(scannedCategory)) {
            sb.append(", while ")
                    .append(displayItemName(top.getItemName()))
                    .append(" adds shape and clarity up top");
        }

        if (bottom != null && !"bottoms".equals(scannedCategory)) {
            sb.append(", with ")
                    .append(displayItemName(bottom.getItemName()))
                    .append(" ")
                    .append(productBottomPhrase(bottom));
        }

        if (outerwear != null && !"outerwear".equals(scannedCategory)) {
            sb.append(", while ")
                    .append(displayItemName(outerwear.getItemName()))
                    .append(" adds depth and structure to the outfit");
        }

        if (shoes != null && !"shoes".equals(scannedCategory)) {
            sb.append(", and ")
                    .append(displayItemName(shoes.getItemName()))
                    .append(" ")
                    .append(productShoePhrase(shoes));
        }

        sb.append(".");
        return sb.toString();
    }

    private void appendRecommendationExplanation(
            StringBuilder sb,
            RecommendationItemDTO item,
            String anchorRfid,
            String phrase
    ) {
        if (sb == null || item == null) {
            return;
        }

        if (safe(item.getRfid()).equalsIgnoreCase(safe(anchorRfid))) {
            return;
        }

        sb.append(", while ")
                .append(displayItemName(item.getName()))
                .append(" ")
                .append(phrase);
    }

    private void appendMissingCategoryExplanation(
            StringBuilder sb,
            RecommendationItemDTO top,
            RecommendationItemDTO bottom,
            RecommendationItemDTO shoes,
            RecommendationItemDTO outerwear
    ) {
        if (sb == null) {
            return;
        }

        int missing = 0;

        if (top == null) {
            missing++;
        }

        if (bottom == null) {
            missing++;
        }

        if (shoes == null) {
            missing++;
        }

        if (outerwear == null) {
            missing++;
        }

        if (missing == 0) {
            sb.append(", creating a complete head-to-toe outfit");
        } else if (missing == 1) {
            sb.append(", with one category left open because no stronger available match was found");
        } else if (missing > 1) {
            sb.append(", with a leaner outfit built from the strongest available categories");
        }
    }

    private String bottomPhrase(RecommendationItemDTO bottom) {
        if (bottom != null && safeLower(bottom.getName()).contains("slim")) {
            return "keeps the silhouette lean and streamlined";
        }

        return "grounds the proportions";
    }

    private String shoePhrase(RecommendationItemDTO shoes) {
        if (shoes == null) {
            return "finishes the outfit cleanly";
        }

        boolean sneakerLike =
                safeLower(shoes.getCategory()).contains("sneaker")
                        || safeLower(shoes.getName()).contains("sneaker");

        return sneakerLike
                ? "keeps the finish modern and easy"
                : "finishes the outfit with a more polished edge";
    }

    private String productBottomPhrase(Product bottom) {
        if (bottom != null && safeLower(bottom.getItemName()).contains("slim")) {
            return "keeps the line clean and elongated";
        }

        return "grounds the proportions";
    }

    private String productShoePhrase(Product shoes) {
        if (shoes == null) {
            return "finishes the outfit cleanly";
        }

        boolean sneakerLike =
                safeLower(shoes.getCategory()).contains("sneaker")
                        || safeLower(shoes.getItemName()).contains("sneaker");

        return sneakerLike
                ? "keeps the finish relaxed but intentional"
                : "gives the outfit a more refined finish";
    }

    private int calculateStyleMatch(Product scannedProduct, Product candidate, String vibe) {
        int score = 74;

        String scannedCategory = normalizeCategory(scannedProduct != null ? scannedProduct.getCategory() : null);
        String candidateCategory = normalizeCategory(candidate != null ? candidate.getCategory() : null);
        String candidateName = safeLower(candidate != null ? candidate.getItemName() : null);
        String candidateCategoryRaw = safeLower(candidate != null ? candidate.getCategory() : null);
        String vibeLower = safeLower(vibe);

        if (!scannedCategory.isBlank()
                && !candidateCategory.isBlank()
                && !scannedCategory.equals(candidateCategory)
                && OUTFIT_CATEGORY_ORDER.contains(candidateCategory)) {
            score += 10;
        }

        if ("casual".equals(vibeLower) && containsAny(candidateName, "shirt", "jeans", "sneaker", "hoodie", "coat", "trouser", "tee", "cardigan")) {
            score += 8;
        }

        if ("formal".equals(vibeLower) && containsAny(candidateName, "blazer", "shirt", "trouser", "loafer", "coat", "dress")) {
            score += 10;
        }

        if ("date night".equals(vibeLower) && containsAny(candidateName, "boot", "jacket", "coat", "heel", "dress", "satin", "moto")) {
            score += 10;
        }

        if ("streetwear".equals(vibeLower) && containsAny(candidateName, "hoodie", "cargo", "sneaker", "oversized", "jacket", "runner", "puffer")) {
            score += 10;
        }

        if ("luxury".equals(vibeLower) && containsAny(candidateName, "tailored", "leather", "premium", "coat", "loafer", "cashmere", "trench")) {
            score += 10;
        }

        if ("formal".equals(vibeLower) && containsAny(candidateCategoryRaw, "hoodie", "cargo", "runner")) {
            score -= 8;
        }

        if ("streetwear".equals(vibeLower) && containsAny(candidateName, "oxford", "loafer", "tailored")) {
            score += 4;
        }

        return clampScore(score);
    }

    private int calculateColorMatch(Product scannedProduct, Product candidate) {
        String scannedColor = safeLower(safeColor(scannedProduct));
        String candidateColor = safeLower(safeColor(candidate));

        if (scannedColor.isBlank() || candidateColor.isBlank()) {
            return 80;
        }

        if (scannedColor.equals(candidateColor)) {
            return 96;
        }

        if (isNeutral(scannedColor) || isNeutral(candidateColor)) {
            return 92;
        }

        if (sameKnownColorFamily(scannedColor, candidateColor)) {
            return 88;
        }

        if (isComplementaryColorFamily(scannedColor, candidateColor)) {
            return 86;
        }

        return 80;
    }

    private int calculateOccasionMatch(Product candidate, String vibe) {
        int score = 76;
        String name = safeLower(candidate != null ? candidate.getItemName() : null);
        String category = safeLower(candidate != null ? candidate.getCategory() : null);
        String vibeLower = safeLower(vibe);

        String searchable = name + " " + category;

        if ("casual".equals(vibeLower) && containsAny(searchable, "shirt", "jeans", "sneaker", "hoodie", "coat", "trouser", "tee", "cardigan")) {
            score += 12;
        }

        if ("formal".equals(vibeLower) && containsAny(searchable, "blazer", "shirt", "trouser", "loafer", "dress", "coat", "oxford", "tailored")) {
            score += 12;
        }

        if ("date night".equals(vibeLower) && containsAny(searchable, "boot", "jacket", "coat", "heel", "dress", "satin", "moto", "leather")) {
            score += 12;
        }

        if ("streetwear".equals(vibeLower) && containsAny(searchable, "hoodie", "cargo", "sneaker", "oversized", "runner", "puffer", "graphic")) {
            score += 12;
        }

        if ("luxury".equals(vibeLower) && containsAny(searchable, "leather", "tailored", "premium", "loafer", "coat", "cashmere", "trench", "wool")) {
            score += 12;
        }

        if ("formal".equals(vibeLower) && containsAny(searchable, "flip flop", "slides", "distressed", "graphic hoodie")) {
            score -= 12;
        }

        if ("luxury".equals(vibeLower) && containsAny(searchable, "cheap", "basic", "flip flop")) {
            score -= 8;
        }

        return clampScore(score);
    }

    private int calculateMaterialSeasonMatch(Product candidate, String vibe) {
        int score = 78;

        String name = safeLower(candidate != null ? candidate.getItemName() : null);
        String material = safeLower(readStringField(candidate, "getMaterial"));
        String season = safeLower(readStringField(candidate, "getSeason"));
        String vibeLower = safeLower(vibe);

        String searchable = name + " " + material + " " + season;

        if (containsAny(searchable, "cotton", "denim", "canvas", "linen")) {
            score += 5;
        }

        if (containsAny(searchable, "wool", "cashmere", "leather", "suede")) {
            score += 7;
        }

        if ("luxury".equals(vibeLower) && containsAny(searchable, "cashmere", "wool", "leather", "suede", "silk")) {
            score += 9;
        }

        if ("streetwear".equals(vibeLower) && containsAny(searchable, "fleece", "nylon", "denim", "canvas")) {
            score += 7;
        }

        if ("formal".equals(vibeLower) && containsAny(searchable, "wool", "cotton", "silk", "leather")) {
            score += 7;
        }

        if (containsAny(searchable, "winter", "fall", "coat", "wool", "cashmere", "puffer")) {
            score += 4;
        }

        if (containsAny(searchable, "summer", "linen", "lightweight", "cotton")) {
            score += 4;
        }

        return clampScore(score);
    }

    private int calculateProductPriceCompatibility(Product scannedProduct, Product candidate) {
        if (scannedProduct == null || candidate == null) {
            return 80;
        }

        double scannedPrice = scannedProduct.getPrice() == null ? 0.0 : scannedProduct.getPrice();
        double candidatePrice = candidate.getPrice() == null ? 0.0 : candidate.getPrice();

        if (scannedPrice <= 0 || candidatePrice <= 0) {
            return 80;
        }

        String scannedTier = priceTier(scannedPrice);
        String candidateTier = priceTier(candidatePrice);

        if (scannedTier.equals(candidateTier)) {
            return 92;
        }

        if (isAdjacentPriceTier(scannedTier, candidateTier)) {
            return 86;
        }

        return 76;
    }

    private String generateRecommendationReason(Product scannedProduct, Product candidate, String vibe) {
        String scannedCategory = normalizeCategory(scannedProduct != null ? scannedProduct.getCategory() : null);
        String candidateCategory = normalizeCategory(candidate != null ? candidate.getCategory() : null);
        String scannedColor = safeLower(safeColor(scannedProduct));
        String candidateColor = safeLower(safeColor(candidate));
        String vibeLower = safeLower(vibe);
        String candidateName = displayItemName(candidate != null ? candidate.getItemName() : null);

        if ("outerwear".equals(scannedCategory) && "tops".equals(candidateCategory)) {
            return candidateName + " softens the outer layer and keeps the look balanced through the upper half.";
        }

        if ("outerwear".equals(scannedCategory) && "bottoms".equals(candidateCategory)) {
            return candidateName + " grounds the statement layer and makes the outfit feel more wearable and intentional.";
        }

        if ("tops".equals(candidateCategory) && "casual".equals(vibeLower)) {
            return candidateName + " keeps the outfit clean and easy, while still giving the look enough structure.";
        }

        if ("outerwear".equals(candidateCategory) && "casual".equals(vibeLower)) {
            return candidateName + " adds shape and depth without taking away from the relaxed direction of the look.";
        }

        if ("bottoms".equals(candidateCategory) && "casual".equals(vibeLower)) {
            return candidateName + " keeps the silhouette relaxed and supports the casual tone without feeling flat.";
        }

        if ("shoes".equals(candidateCategory)) {
            return candidateName + " finishes the outfit cleanly and helps lock in the overall direction.";
        }

        if (isNeutral(scannedColor) || isNeutral(candidateColor)) {
            return candidateName + " works especially well here because the neutral tone makes the full look easier to balance.";
        }

        if (sameKnownColorFamily(scannedColor, candidateColor)) {
            return candidateName + " connects naturally with the scanned piece, keeping the palette cohesive without feeling too matched.";
        }

        if (isComplementaryColorFamily(scannedColor, candidateColor)) {
            return candidateName + " adds contrast while still keeping the color story intentional.";
        }

        return candidateName + " supports the look by balancing the silhouette, the color story, and the overall vibe.";
    }

    private String getColorFamily(String color) {
        String c = safeLower(color);

        if (containsAny(c, "beige", "white", "cream", "gray", "grey", "black", "tan", "brown", "charcoal", "navy", "neutral", "khaki", "camel", "stone")) {
            return "neutral";
        }

        if (containsAny(c, "blue", "navy", "grey", "gray", "silver")) {
            return "cool";
        }

        if (containsAny(c, "red", "orange", "yellow", "pink", "burgundy", "maroon")) {
            return "warm";
        }

        if (containsAny(c, "green", "olive", "khaki", "brown", "tan", "camel")) {
            return "earth";
        }

        return "unknown";
    }

    private boolean sameKnownColorFamily(String a, String b) {
        String familyA = getColorFamily(a);
        String familyB = getColorFamily(b);

        if ("unknown".equals(familyA) || "unknown".equals(familyB)) {
            return false;
        }

        return familyA.equals(familyB);
    }

    private boolean isComplementaryColorFamily(String a, String b) {
        String familyA = getColorFamily(a);
        String familyB = getColorFamily(b);

        if ("unknown".equals(familyA) || "unknown".equals(familyB)) {
            return false;
        }

        return ("warm".equals(familyA) && "cool".equals(familyB))
                || ("cool".equals(familyA) && "warm".equals(familyB))
                || ("earth".equals(familyA) && "neutral".equals(familyB))
                || ("neutral".equals(familyA) && "earth".equals(familyB));
    }

    private String casualAdvice(String category, String item) {
        return switch (category) {
            case "tops" ->
                    item + " is an easy everyday anchor. Pair it with relaxed trousers or denim and clean sneakers for a look that feels effortless but still considered.";
            case "bottoms" ->
                    item + " keeps the silhouette sharp without feeling overworked. Style it with a clean tee, knit, or hoodie and let the footwear keep the look relaxed.";
            case "shoes" ->
                    item + " works best as the finishing piece in a laid-back outfit. Add straight-leg bottoms and a clean top to keep the overall look balanced.";
            case "outerwear" ->
                    item + " adds structure to a casual outfit without making it feel too dressed. Layer it over simple essentials so the silhouette stays easy and refined.";
            default ->
                    defaultAdvice(item);
        };
    }

    private String formalAdvice(String category, String item) {
        return switch (category) {
            case "tops" ->
                    item + " gives the look a polished foundation. Pair it with tailored trousers and refined footwear to keep the outfit sharp and composed.";
            case "bottoms" ->
                    item + " brings structure and polish to the outfit. Balance it with a crisp top and cleaner footwear for a more elevated formal line.";
            case "shoes" ->
                    item + " finishes a formal outfit with precision. Keep the surrounding pieces clean and tailored so the entire look feels intentional.";
            case "outerwear" ->
                    item + " adds authority and shape to the look. Layer it over a disciplined base to keep the outfit refined from top to bottom.";
            default ->
                    defaultAdvice(item);
        };
    }

    private String dateNightAdvice(String category, String item) {
        return switch (category) {
            case "tops" ->
                    item + " adds just enough presence for a night-out look. Pair it with sharper bottoms and cleaner footwear to keep the outfit confident and pulled together.";
            case "bottoms" ->
                    item + " helps create a cleaner, more flattering line. Balance it with a fitted top and a stronger shoe to make the outfit feel intentional.";
            case "shoes" ->
                    item + " gives the outfit personality and finish. Let the rest of the look stay streamlined so the footwear feels deliberate rather than loud.";
            case "outerwear" ->
                    item + " adds mood and structure to the outfit. Use it as the final layer over a cleaner base so the look feels elevated without trying too hard.";
            default ->
                    defaultAdvice(item);
        };
    }

    private String streetwearAdvice(String category, String item) {
        return switch (category) {
            case "tops" ->
                    item + " works best when it has room to lead the outfit. Pair it with looser bottoms and stronger sneakers so the proportions feel intentional.";
            case "bottoms" ->
                    item + " gives the look a strong streetwear foundation. Balance it with a clean graphic or oversized top and let the footwear add the final statement.";
            case "shoes" ->
                    item + " should be treated like the visual anchor. Keep the rest of the outfit clean enough to support the footwear without competing with it.";
            case "outerwear" ->
                    item + " adds depth and attitude to the outfit. Layer it over a hoodie or tee so the overall look feels built rather than simply styled.";
            default ->
                    defaultAdvice(item);
        };
    }

    private String luxuryAdvice(String category, String item) {
        return switch (category) {
            case "tops" ->
                    item + " works best when the rest of the outfit stays clean and elevated. Pair it with refined tailoring and premium footwear for a sharper luxury feel.";
            case "bottoms" ->
                    item + " brings polish and shape to the outfit. Keep the top clean and the footwear deliberate so the look feels expensive rather than overdone.";
            case "shoes" ->
                    item + " grounds the outfit with a more elevated finish. Let the surrounding pieces stay streamlined so the look reads premium and intentional.";
            case "outerwear" ->
                    item + " introduces presence and refinement at once. Layer it over minimal, well-cut essentials so the whole outfit feels elevated.";
            default ->
                    defaultAdvice(item);
        };
    }

    private String defaultAdvice(String item) {
        return item + " is a strong foundation piece. Build around it with complementary proportions, a clean color story, and one or two intentional finishing elements.";
    }

    private int clampScore(int rawScore) {
        if (rawScore < 70) {
            return 70;
        }

        if (rawScore > 98) {
            return 98;
        }

        return rawScore;
    }

    private boolean containsAny(String text, String... keywords) {
        String safeText = safeLower(text);

        if (safeText.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (safeText.contains(safeLower(keyword))) {
                return true;
            }
        }

        return false;
    }

    private boolean isNeutral(String color) {
        String normalized = safeLower(color);

        return "black".equals(normalized)
                || "white".equals(normalized)
                || "grey".equals(normalized)
                || "gray".equals(normalized)
                || "charcoal".equals(normalized)
                || "beige".equals(normalized)
                || "cream".equals(normalized)
                || "brown".equals(normalized)
                || "navy".equals(normalized)
                || "neutral".equals(normalized)
                || "camel".equals(normalized)
                || "khaki".equals(normalized)
                || "tan".equals(normalized)
                || "stone".equals(normalized)
                || "olive".equals(normalized);
    }

    private String readStringField(Object source, String... methodNames) {
        Object value = readReflectiveValue(source, methodNames);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String safeColor(Product product) {
        if (product == null || product.getColor() == null || product.getColor().isBlank()) {
            return "Neutral";
        }

        return product.getColor().trim();
    }

    private String safeBrand(Product product) {
        if (product == null) {
            return "";
        }

        if (product.getBrand() != null && !product.getBrand().isBlank()) {
            return product.getBrand().trim();
        }

        return safe(product.getRetailerName());
    }

    private Double safePrice(Double value) {
        return value == null ? 0.0 : value;
    }

    private String safeImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return PLACEHOLDER_IMAGE;
        }

        return imageUrl.trim();
    }

    private int defaultedScore(Integer value, int fallback) {
        return clampScore(value == null ? fallback : value);
    }

    private String normalizeCategory(String category) {
        String normalized = safeLower(category);

        return switch (normalized) {
            case "top", "tops", "shirt", "shirts", "tee", "t-shirt", "hoodie", "blouse", "sweater", "knit", "cardigan" -> "tops";
            case "bottom", "bottoms", "pants", "trousers", "jeans", "skirt", "shorts", "short", "cargo", "jogger", "joggers" -> "bottoms";
            case "shoe", "shoes", "sneaker", "sneakers", "boot", "boots", "loafer", "loafers", "heel", "heels", "sandal", "sandals", "runner", "runners" -> "shoes";
            case "outerwear", "coat", "jacket", "blazer", "parka", "trench", "puffer", "overshirt", "moto" -> "outerwear";
            default -> normalized;
        };
    }

    private String displayItemName(String value) {
        String cleaned = safe(value);
        return cleaned.isBlank() ? "This piece" : cleaned;
    }

    private String cleanSentence(String value) {
        String cleaned = safe(value);

        if (cleaned.isBlank()) {
            return "This outfit balances the scanned anchor with complementary pieces for a complete, wearable look.";
        }

        if (!cleaned.endsWith(".")) {
            return cleaned + ".";
        }

        return cleaned;
    }
}
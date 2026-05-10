package com.retailai.service;

import com.retailai.dto.FullOutfitDTO;
import com.retailai.dto.RecommendationItemDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.model.Product;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AIStylistService {

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

        List<RecommendationItemDTO> suggestions = scanResultDTO.getSuggestions();
        if (suggestions == null) {
            suggestions = List.of();
        }

        RecommendationItemDTO anchorItem = toAnchorRecommendation(scanResultDTO);
        String anchorCategory = normalizeCategory(scanResultDTO.getCategory());

        RecommendationItemDTO top = isTopCategory(anchorCategory) ? anchorItem : findBestTop(suggestions);
        RecommendationItemDTO bottom = isBottomCategory(anchorCategory) ? anchorItem : findBestBottom(suggestions);
        RecommendationItemDTO shoes = isShoeCategory(anchorCategory) ? anchorItem : findBestShoes(suggestions);
        RecommendationItemDTO outerwear = isOuterwearCategory(anchorCategory) ? anchorItem : findBestOuterwear(suggestions);

        if (top == null && bottom == null && shoes == null && outerwear == null) {
            return null;
        }

        FullOutfitDTO outfit = new FullOutfitDTO();
        outfit.setTop(top);
        outfit.setBottom(bottom);
        outfit.setShoes(shoes);
        outfit.setOuterwear(outerwear);

        int styleScore = averageScores(
                scoreOrNull(top != null ? top.getStyleMatch() : null),
                scoreOrNull(bottom != null ? bottom.getStyleMatch() : null),
                scoreOrNull(shoes != null ? shoes.getStyleMatch() : null),
                scoreOrNull(outerwear != null ? outerwear.getStyleMatch() : null)
        );

        int colorScore = averageScores(
                scoreOrNull(top != null ? top.getColorMatch() : null),
                scoreOrNull(bottom != null ? bottom.getColorMatch() : null),
                scoreOrNull(shoes != null ? shoes.getColorMatch() : null),
                scoreOrNull(outerwear != null ? outerwear.getColorMatch() : null)
        );

        int occasionScore = averageScores(
                scoreOrNull(top != null ? top.getOccasionMatch() : null),
                scoreOrNull(bottom != null ? bottom.getOccasionMatch() : null),
                scoreOrNull(shoes != null ? shoes.getOccasionMatch() : null),
                scoreOrNull(outerwear != null ? outerwear.getOccasionMatch() : null)
        );

        int overallScore = clampScore(Math.round(
                (styleScore * 0.5f) +
                        (colorScore * 0.3f) +
                        (occasionScore * 0.2f)
        ));

        outfit.setStyleScore(styleScore);
        outfit.setColorScore(colorScore);
        outfit.setOccasionScore(occasionScore);
        outfit.setOverallScore(overallScore);
        outfit.setExplanation(generateOutfitExplanation(scanResultDTO, top, bottom, shoes, outerwear));

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

        RecommendationItemDTO top = "tops".equals(scannedCategory)
                ? scannedAnchor
                : (topProduct != null ? toOutfitItemDto(scannedProduct, topProduct, vibe) : null);

        RecommendationItemDTO bottom = "bottoms".equals(scannedCategory)
                ? scannedAnchor
                : (bottomProduct != null ? toOutfitItemDto(scannedProduct, bottomProduct, vibe) : null);

        RecommendationItemDTO shoes = "shoes".equals(scannedCategory)
                ? scannedAnchor
                : (shoesProduct != null ? toOutfitItemDto(scannedProduct, shoesProduct, vibe) : null);

        RecommendationItemDTO outerwear = "outerwear".equals(scannedCategory)
                ? scannedAnchor
                : (outerwearProduct != null ? toOutfitItemDto(scannedProduct, outerwearProduct, vibe) : null);

        if (top == null && bottom == null && shoes == null && outerwear == null) {
            return null;
        }

        FullOutfitDTO outfit = new FullOutfitDTO();
        outfit.setTop(top);
        outfit.setBottom(bottom);
        outfit.setShoes(shoes);
        outfit.setOuterwear(outerwear);

        int styleScore = averageScores(
                scoreOrNull(top != null ? top.getStyleMatch() : null),
                scoreOrNull(bottom != null ? bottom.getStyleMatch() : null),
                scoreOrNull(shoes != null ? shoes.getStyleMatch() : null),
                scoreOrNull(outerwear != null ? outerwear.getStyleMatch() : null)
        );

        int colorScore = averageScores(
                scoreOrNull(top != null ? top.getColorMatch() : null),
                scoreOrNull(bottom != null ? bottom.getColorMatch() : null),
                scoreOrNull(shoes != null ? shoes.getColorMatch() : null),
                scoreOrNull(outerwear != null ? outerwear.getColorMatch() : null)
        );

        int occasionScore = averageScores(
                scoreOrNull(top != null ? top.getOccasionMatch() : null),
                scoreOrNull(bottom != null ? bottom.getOccasionMatch() : null),
                scoreOrNull(shoes != null ? shoes.getOccasionMatch() : null),
                scoreOrNull(outerwear != null ? outerwear.getOccasionMatch() : null)
        );

        int overallScore = clampScore(Math.round(
                (styleScore * 0.5f) +
                        (colorScore * 0.3f) +
                        (occasionScore * 0.2f)
        ));

        outfit.setStyleScore(styleScore);
        outfit.setColorScore(colorScore);
        outfit.setOccasionScore(occasionScore);
        outfit.setOverallScore(overallScore);
        outfit.setExplanation(generateOutfitExplanationFromProducts(
                scannedProduct,
                topProduct,
                bottomProduct,
                shoesProduct,
                outerwearProduct
        ));

        return outfit;
    }

    private RecommendationItemDTO toAnchorRecommendation(ScanResultDTO scanResultDTO) {
        RecommendationItemDTO item = new RecommendationItemDTO();
        item.setRfid(scanResultDTO.getRfid());
        item.setName(scanResultDTO.getName());
        item.setBrand(scanResultDTO.getBrand());
        item.setCategory(scanResultDTO.getCategory());
        item.setColor(scanResultDTO.getColor());
        item.setRetailer(scanResultDTO.getRetailer());
        item.setPrice(scanResultDTO.getPrice());
        item.setImageUrl(scanResultDTO.getImageUrl());
        item.setMatchScore(defaultedScore(scanResultDTO.getMatchScore(), 88));
        item.setStyleMatch(defaultedScore(scanResultDTO.getMatchScore(), 88));
        item.setColorMatch(defaultedScore(scanResultDTO.getMatchScore(), 88));
        item.setOccasionMatch(defaultedScore(scanResultDTO.getMatchScore(), 88));
        item.setReason(scanResultDTO.getWhyItWorks());
        return item;
    }

    private RecommendationItemDTO toOutfitItemDto(Product scannedProduct, Product candidate, String vibe) {
        if (candidate == null) {
            return null;
        }

        int styleMatch = calculateStyleMatch(scannedProduct, candidate, vibe);
        int colorMatch = calculateColorMatch(scannedProduct, candidate);
        int occasionMatch = calculateOccasionMatch(candidate, vibe);
        int overallMatch = clampScore(Math.round((styleMatch + colorMatch + occasionMatch) / 3.0f));

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

    private RecommendationItemDTO findBestTop(List<RecommendationItemDTO> suggestions) {
        return suggestions.stream()
                .filter(Objects::nonNull)
                .filter(item -> isTopCategory(item.getCategory()))
                .max(Comparator.comparingInt(this::combinedItemScore))
                .orElse(null);
    }

    private RecommendationItemDTO findBestBottom(List<RecommendationItemDTO> suggestions) {
        return suggestions.stream()
                .filter(Objects::nonNull)
                .filter(item -> isBottomCategory(item.getCategory()))
                .max(Comparator.comparingInt(this::combinedItemScore))
                .orElse(null);
    }

    private RecommendationItemDTO findBestShoes(List<RecommendationItemDTO> suggestions) {
        return suggestions.stream()
                .filter(Objects::nonNull)
                .filter(item -> isShoeCategory(item.getCategory()))
                .max(Comparator.comparingInt(this::combinedItemScore))
                .orElse(null);
    }

    private RecommendationItemDTO findBestOuterwear(List<RecommendationItemDTO> suggestions) {
        return suggestions.stream()
                .filter(Objects::nonNull)
                .filter(item -> isOuterwearCategory(item.getCategory()))
                .max(Comparator.comparingInt(this::combinedItemScore))
                .orElse(null);
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

    private int combinedItemScore(RecommendationItemDTO item) {
        if (item == null) {
            return 0;
        }

        int style = defaultedScore(item.getStyleMatch(), 0);
        int color = defaultedScore(item.getColorMatch(), 0);
        int occasion = defaultedScore(item.getOccasionMatch(), 0);
        int match = defaultedScore(item.getMatchScore(), 0);

        return Math.round((style * 0.4f) + (color * 0.2f) + (occasion * 0.2f) + (match * 0.2f));
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

        if (top != null && !safe(top.getName()).equalsIgnoreCase(safe(scanResultDTO.getName()))) {
            sb.append(", while ")
                    .append(displayItemName(top.getName()))
                    .append(" sharpens the line through the top half");
        }

        if (bottom != null && !safe(bottom.getName()).equalsIgnoreCase(safe(scanResultDTO.getName()))) {
            if (safeLower(bottom.getName()).contains("slim")) {
                sb.append(", and ")
                        .append(displayItemName(bottom.getName()))
                        .append(" keeps the silhouette lean and streamlined");
            } else {
                sb.append(", with ")
                        .append(displayItemName(bottom.getName()))
                        .append(" grounding the proportions");
            }
        }

        if (outerwear != null && !safe(outerwear.getName()).equalsIgnoreCase(safe(scanResultDTO.getName()))) {
            sb.append(", while ")
                    .append(displayItemName(outerwear.getName()))
                    .append(" adds depth and structure without overpowering the look");
        }

        if (shoes != null && !safe(shoes.getName()).equalsIgnoreCase(safe(scanResultDTO.getName()))) {
            boolean sneakerLike =
                    safeLower(shoes.getCategory()).contains("sneaker") ||
                            safeLower(shoes.getName()).contains("sneaker");

            if (sneakerLike) {
                sb.append(", and ")
                        .append(displayItemName(shoes.getName()))
                        .append(" keeps the finish modern and easy");
            } else {
                sb.append(", and ")
                        .append(displayItemName(shoes.getName()))
                        .append(" finishes the outfit with a more polished edge");
            }
        }

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
            if (safeLower(bottom.getItemName()).contains("slim")) {
                sb.append(", and ")
                        .append(displayItemName(bottom.getItemName()))
                        .append(" keeps the line clean and elongated");
            } else {
                sb.append(", with ")
                        .append(displayItemName(bottom.getItemName()))
                        .append(" grounding the proportions");
            }
        }

        if (outerwear != null && !"outerwear".equals(scannedCategory)) {
            sb.append(", while ")
                    .append(displayItemName(outerwear.getItemName()))
                    .append(" adds depth and structure to the outfit");
        }

        if (shoes != null && !"shoes".equals(scannedCategory)) {
            boolean sneakerLike =
                    safeLower(shoes.getCategory()).contains("sneaker") ||
                            safeLower(shoes.getItemName()).contains("sneaker");

            if (sneakerLike) {
                sb.append(", and ")
                        .append(displayItemName(shoes.getItemName()))
                        .append(" keeps the finish relaxed but intentional");
            } else {
                sb.append(", and ")
                        .append(displayItemName(shoes.getItemName()))
                        .append(" gives the outfit a more refined finish");
            }
        }

        sb.append(".");
        return sb.toString();
    }

    private int calculateStyleMatch(Product scannedProduct, Product candidate, String vibe) {
        int score = 74;

        String scannedCategory = normalizeCategory(scannedProduct != null ? scannedProduct.getCategory() : null);
        String candidateCategory = normalizeCategory(candidate != null ? candidate.getCategory() : null);
        String candidateName = safeLower(candidate != null ? candidate.getItemName() : null);
        String vibeLower = safeLower(vibe);

        if ("tops".equals(scannedCategory)
                && ("bottoms".equals(candidateCategory) || "shoes".equals(candidateCategory) || "outerwear".equals(candidateCategory))) {
            score += 10;
        }

        if ("bottoms".equals(scannedCategory)
                && ("tops".equals(candidateCategory) || "shoes".equals(candidateCategory) || "outerwear".equals(candidateCategory))) {
            score += 10;
        }

        if ("shoes".equals(scannedCategory)
                && ("tops".equals(candidateCategory) || "bottoms".equals(candidateCategory) || "outerwear".equals(candidateCategory))) {
            score += 10;
        }

        if ("outerwear".equals(scannedCategory)
                && ("tops".equals(candidateCategory) || "bottoms".equals(candidateCategory) || "shoes".equals(candidateCategory))) {
            score += 10;
        }

        if ("casual".equals(vibeLower) && containsAny(candidateName, "shirt", "jeans", "sneaker", "hoodie", "coat", "trouser")) {
            score += 8;
        }

        if ("formal".equals(vibeLower) && containsAny(candidateName, "blazer", "shirt", "trouser", "loafer", "coat")) {
            score += 10;
        }

        if ("streetwear".equals(vibeLower) && containsAny(candidateName, "hoodie", "cargo", "sneaker", "oversized", "jacket")) {
            score += 10;
        }

        if ("luxury".equals(vibeLower) && containsAny(candidateName, "tailored", "leather", "premium", "coat", "loafer")) {
            score += 10;
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

        if (sameColorFamily(scannedColor, candidateColor)) {
            return 88;
        }

        return 80;
    }

    private int calculateOccasionMatch(Product candidate, String vibe) {
        int score = 76;
        String name = safeLower(candidate != null ? candidate.getItemName() : null);
        String vibeLower = safeLower(vibe);

        if ("casual".equals(vibeLower) && containsAny(name, "shirt", "jeans", "sneaker", "hoodie", "coat", "trouser")) {
            score += 12;
        }

        if ("formal".equals(vibeLower) && containsAny(name, "blazer", "shirt", "trouser", "loafer", "dress")) {
            score += 12;
        }

        if ("date night".equals(vibeLower) && containsAny(name, "boot", "jacket", "coat", "heel", "dress")) {
            score += 12;
        }

        if ("streetwear".equals(vibeLower) && containsAny(name, "hoodie", "cargo", "sneaker", "oversized")) {
            score += 12;
        }

        if ("luxury".equals(vibeLower) && containsAny(name, "leather", "tailored", "premium", "loafer", "coat")) {
            score += 12;
        }

        return clampScore(score);
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

        if (sameColorFamily(scannedColor, candidateColor)) {
            return candidateName + " connects naturally with the scanned piece, keeping the palette cohesive without feeling too matched.";
        }

        return candidateName + " supports the look by balancing the silhouette, the color story, and the overall vibe.";
    }

    private String getColorFamily(String color) {
        String c = safeLower(color);

        if (containsAny(c, "beige", "white", "cream", "gray", "grey", "black", "tan", "brown", "charcoal", "navy", "neutral")) {
            return "neutral";
        }
        if (containsAny(c, "blue")) {
            return "cool";
        }
        if (containsAny(c, "red", "orange", "yellow")) {
            return "warm";
        }
        if (containsAny(c, "green", "olive")) {
            return "earth";
        }

        return "unknown";
    }

    private boolean sameColorFamily(String a, String b) {
        return getColorFamily(a).equals(getColorFamily(b));
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
        if (text == null || text.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
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
                || "neutral".equals(normalized);
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
            return "https://placehold.co/500x620?text=No+Image";
        }
        return imageUrl.trim();
    }

    private int defaultedScore(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private String normalizeCategory(String category) {
        String normalized = safeLower(category);

        return switch (normalized) {
            case "top", "tops", "shirt", "shirts", "tee", "t-shirt", "hoodie", "blouse", "sweater" -> "tops";
            case "bottom", "bottoms", "pants", "trousers", "jeans", "skirt", "shorts", "short", "cargo" -> "bottoms";
            case "shoe", "shoes", "sneaker", "sneakers", "boot", "boots", "loafer", "loafers", "heel", "heels", "sandal", "sandals" -> "shoes";
            case "outerwear", "coat", "jacket", "blazer" -> "outerwear";
            default -> normalized;
        };
    }

    private String displayItemName(String value) {
        String cleaned = safe(value);
        return cleaned.isBlank() ? "This piece" : cleaned;
    }
}
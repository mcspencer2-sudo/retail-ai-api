package com.retailai.service;

import com.retailai.model.ActivityDTO;
import com.retailai.model.AnalyticsSummaryDTO;
import com.retailai.model.BagItem;
import com.retailai.model.BagSummaryResponse;
import com.retailai.model.OutfitResponse;
import com.retailai.model.Product;
import com.retailai.model.RetailerStatsDTO;
import com.retailai.model.TrendDTO;
import com.retailai.model.TrendEvent;
import com.retailai.repository.BagItemRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.TrendEventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final BagItemRepository bagItemRepository;
    private final TrendEventRepository trendEventRepository;
    private final AIStylistService aiStylistService;

    public InventoryService(ProductRepository productRepository,
                            BagItemRepository bagItemRepository,
                            TrendEventRepository trendEventRepository,
                            AIStylistService aiStylistService) {
        this.productRepository = productRepository;
        this.bagItemRepository = bagItemRepository;
        this.trendEventRepository = trendEventRepository;
        this.aiStylistService = aiStylistService;
    }

    public OutfitResponse scanItem(String retailerKey, String rfid, String vibe) {
        String retailerName = mapRetailerKeyToName(retailerKey);

        Product product = productRepository.findById(rfid)
                .orElseThrow(() -> new RuntimeException("RFID not found: " + rfid));

        if (!product.getRetailerName().equalsIgnoreCase(retailerName)) {
            throw new IllegalArgumentException("RFID does not belong to selected retailer");
        }

        saveTrendEvent("SCAN", product);

        String stylingAdvice = aiStylistService.generateAdvice(product, vibe);
        List<Product> suggestions = generateSmartSuggestions(product, vibe);

        return new OutfitResponse(
                product.getRfid(),
                product.getRetailerName(),
                product.getItemName(),
                stylingAdvice,
                product.getImageUrl(),
                product.getPrice(),
                suggestions
        );
    }

    public String saveToBag(String rfid) {
        Product product = productRepository.findById(rfid)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BagItem item = new BagItem();
        item.setRfid(product.getRfid());
        item.setRetailerName(product.getRetailerName());
        item.setItemName(product.getItemName());
        item.setImageUrl(product.getImageUrl());
        item.setPrice(product.getPrice());
        item.setCategory(product.getCategory());

        bagItemRepository.save(item);
        saveTrendEvent("SAVE", product);

        return product.getItemName() + " added to your style bag.";
    }

    public BagSummaryResponse getBagSummary() {
        List<BagItem> items = bagItemRepository.findAll();
        double subtotal = items.stream().mapToDouble(BagItem::getPrice).sum();
        double tax = subtotal * 0.0825;
        double total = subtotal + tax;

        return new BagSummaryResponse(items, subtotal, tax, total);
    }

    public String removeBagItem(Long id) {
        if (!bagItemRepository.existsById(id)) {
            throw new RuntimeException("Bag item not found: " + id);
        }

        bagItemRepository.deleteById(id);
        return "Item removed from bag.";
    }

    public String clearBag() {
        bagItemRepository.deleteAll();
        return "Bag cleared.";
    }

    public List<TrendDTO> getTrends() {
        Map<String, Long> grouped = trendEventRepository.findAll().stream()
                .filter(event -> "SAVE".equalsIgnoreCase(event.getEventType()))
                .collect(Collectors.groupingBy(
                        e -> e.getRetailerName() + "||" + e.getItemName(),
                        Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|\\|");
                    return new TrendDTO(parts[0], parts[1], entry.getValue().intValue());
                })
                .sorted(Comparator.comparingInt(TrendDTO::getCount).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    public AnalyticsSummaryDTO getAnalyticsSummary() {
        List<TrendEvent> events = trendEventRepository.findAll();

        long totalScans = events.stream()
                .filter(e -> "SCAN".equalsIgnoreCase(e.getEventType()))
                .count();

        long totalSaves = events.stream()
                .filter(e -> "SAVE".equalsIgnoreCase(e.getEventType()))
                .count();

        double conversionRate = totalScans == 0 ? 0.0 : ((double) totalSaves / totalScans) * 100.0;

        String topRetailer = events.stream()
                .collect(Collectors.groupingBy(TrendEvent::getRetailerName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topScannedItem = events.stream()
                .filter(e -> "SCAN".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(TrendEvent::getItemName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topSavedItem = events.stream()
                .filter(e -> "SAVE".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(TrendEvent::getItemName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return new AnalyticsSummaryDTO(
                totalScans,
                totalSaves,
                conversionRate,
                topRetailer,
                topScannedItem,
                topSavedItem
        );
    }

    public List<ActivityDTO> getRecentActivity(String eventType, String retailer) {
        return trendEventRepository.findAll().stream()
                .sorted(Comparator.comparing(TrendEvent::getCreatedAt).reversed())
                .filter(e -> "ALL".equalsIgnoreCase(eventType) || e.getEventType().equalsIgnoreCase(eventType))
                .filter(e -> "ALL".equalsIgnoreCase(retailer) || e.getRetailerName().equalsIgnoreCase(retailer))
                .limit(20)
                .map(event -> new ActivityDTO(
                        event.getEventType(),
                        event.getRetailerName(),
                        event.getItemName(),
                        timeAgo(event.getCreatedAt()),
                        event.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<RetailerStatsDTO> getRetailerStats() {
        List<TrendEvent> events = trendEventRepository.findAll();

        Map<String, Long> scansByRetailer = events.stream()
                .filter(e -> "SCAN".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(TrendEvent::getRetailerName, Collectors.counting()));

        Map<String, Long> savesByRetailer = events.stream()
                .filter(e -> "SAVE".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(TrendEvent::getRetailerName, Collectors.counting()));

        Set<String> retailers = new HashSet<>();
        retailers.addAll(scansByRetailer.keySet());
        retailers.addAll(savesByRetailer.keySet());

        return retailers.stream()
                .map(retailer -> {
                    long scans = scansByRetailer.getOrDefault(retailer, 0L);
                    long saves = savesByRetailer.getOrDefault(retailer, 0L);
                    double conversion = scans == 0 ? 0.0 : ((double) saves / scans) * 100.0;
                    return new RetailerStatsDTO(retailer, scans, saves, conversion);
                })
                .sorted(Comparator.comparingLong(RetailerStatsDTO::getScans).reversed())
                .collect(Collectors.toList());
    }

    private List<Product> generateSmartSuggestions(Product scannedProduct, String vibe) {
        List<Product> allProducts = productRepository.findAll();
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);

        return allProducts.stream()
                .filter(p -> !p.getRfid().equalsIgnoreCase(scannedProduct.getRfid()))
                .filter(p -> targetCategories.contains(normalizeCategory(p.getCategory())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe))
                        .reversed()
                        .thenComparing(Product::getRetailerName)
                        .thenComparing(Product::getItemName))
                .collect(Collectors.toMap(
                        p -> normalizeCategory(p.getCategory()),
                        p -> p,
                        (first, second) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    private int scoreSuggestion(Product scannedProduct, Product candidate, String vibe) {
        int score = 0;

        String scannedCategory = normalizeCategory(scannedProduct.getCategory());
        String candidateCategory = normalizeCategory(candidate.getCategory());
        String scannedRetailer = safeLower(scannedProduct.getRetailerName());
        String candidateRetailer = safeLower(candidate.getRetailerName());
        String scannedName = safeLower(scannedProduct.getItemName());
        String candidateName = safeLower(candidate.getItemName());
        String vibeLower = safeLower(vibe);

        if (!candidateCategory.isBlank()) {
            score += 40;
        }

        if (!candidateRetailer.equals(scannedRetailer)) {
            score += 20;
        } else {
            score += 5;
        }

        double scannedPrice = scannedProduct.getPrice();
        double candidatePrice = candidate.getPrice();

        if (scannedPrice > 0 && candidatePrice > 0) {
            double ratio = candidatePrice / scannedPrice;

            if (ratio >= 0.6 && ratio <= 1.4) {
                score += 20;
            } else if (ratio >= 0.4 && ratio <= 1.8) {
                score += 10;
            }
        }

        if ("casual".equals(vibeLower)) {
            if (containsAny(candidateName, "tee", "t-shirt", "shirt", "jeans", "sneakers", "hoodie", "coat")) {
                score += 12;
            }
        }

        if ("formal".equals(vibeLower)) {
            if (containsAny(candidateName, "shirt", "blazer", "trouser", "oxford", "loafer", "coat", "dress")) {
                score += 12;
            }
        }

        if ("date night".equals(vibeLower)) {
            if (containsAny(candidateName, "boots", "jacket", "coat", "fitted", "heel", "sleek", "dress")) {
                score += 12;
            }
        }

        if ("tops".equals(scannedCategory) && ("bottoms".equals(candidateCategory) || "shoes".equals(candidateCategory))) {
            score += 10;
        }
        if ("bottoms".equals(scannedCategory) && ("tops".equals(candidateCategory) || "shoes".equals(candidateCategory))) {
            score += 10;
        }
        if ("shoes".equals(scannedCategory) && ("tops".equals(candidateCategory) || "bottoms".equals(candidateCategory))) {
            score += 10;
        }
        if ("outerwear".equals(scannedCategory) && ("tops".equals(candidateCategory) || "bottoms".equals(candidateCategory))) {
            score += 10;
        }

        if (containsAny(scannedName, "coat", "jacket") && "outerwear".equals(candidateCategory)) {
            score -= 10;
        }

        return score;
    }

    private Set<String> getTargetCategories(String scannedCategory, String vibe) {
        String category = normalizeCategory(scannedCategory);
        Set<String> targets = new LinkedHashSet<>();

        switch (category) {
            case "tops" -> {
                targets.add("bottoms");
                targets.add("shoes");
                targets.add("outerwear");
            }
            case "bottoms" -> {
                targets.add("tops");
                targets.add("shoes");
                if (!"Date Night".equalsIgnoreCase(vibe)) {
                    targets.add("outerwear");
                }
            }
            case "shoes" -> {
                targets.add("tops");
                targets.add("bottoms");
                targets.add("outerwear");
            }
            case "outerwear" -> {
                targets.add("tops");
                targets.add("bottoms");
                targets.add("shoes");
            }
            default -> {
                targets.add("tops");
                targets.add("bottoms");
                targets.add("shoes");
            }
        }

        return targets;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "";
        }
        return category.trim().toLowerCase();
    }

    private String mapRetailerKeyToName(String retailerKey) {
        return switch (retailerKey.toUpperCase()) {
            case "MACY001" -> "Macy's";
            case "ZARA001" -> "Zara";
            case "NORD001" -> "Nordstrom";
            case "NIKE001" -> "Nike";
            default -> throw new IllegalArgumentException("Unknown retailer key: " + retailerKey);
        };
    }

    private void saveTrendEvent(String eventType, Product product) {
        TrendEvent event = new TrendEvent();
        event.setEventType(eventType);
        event.setRetailerName(product.getRetailerName());
        event.setItemName(product.getItemName());
        event.setCreatedAt(LocalDateTime.now());
        trendEventRepository.save(event);
    }

    private String timeAgo(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "N/A";
        }

        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";

        long hours = minutes / 60;
        if (hours < 24) return hours + " hr ago";

        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }
}
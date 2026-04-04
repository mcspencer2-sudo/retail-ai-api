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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private static final double TAX_RATE = 0.07;

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
        Product product = productRepository.findById(rfid)
                .orElseThrow(() -> new RuntimeException("Item not found for RFID: " + rfid));

        String expectedRetailerName = resolveRetailerName(retailerKey);
        String actualRetailerName = product.getRetailerName();

        if (!normalize(expectedRetailerName).equals(normalize(actualRetailerName))) {
            throw new RuntimeException(
                    "RFID " + rfid + " does not belong to retailer " + expectedRetailerName
            );
        }

        trendEventRepository.save(
                new TrendEvent(
                        product.getRetailerName(),
                        product.getItemName(),
                        "SCAN",
                        LocalDateTime.now()
                )
        );

        String advice = aiStylistService.generateAdvice(product, vibe);
        List<Product> suggestions = getOutfitSuggestions(product);

        return new OutfitResponse(
                product.getRfid(),
                product.getRetailerName(),
                product.getItemName(),
                advice,
                product.getImageUrl(),
                product.getPrice(),
                suggestions
        );
    }

    public List<Product> getOutfitSuggestions(Product baseItem) {
        List<Product> all = new ArrayList<>(productRepository.findAll());
        Collections.shuffle(all);

        Product top = null;
        Product bottom = null;
        Product shoes = null;
        Product outerwear = null;

        String baseCategory = normalize(baseItem.getCategory());

        for (Product p : all) {
            if (p.getRfid().equalsIgnoreCase(baseItem.getRfid())) {
                continue;
            }

            String cat = normalize(p.getCategory());

            if (cat.equals("tops") && top == null && !baseCategory.equals("tops")) {
                top = p;
            } else if (cat.equals("bottoms") && bottom == null && !baseCategory.equals("bottoms")) {
                bottom = p;
            } else if (cat.equals("shoes") && shoes == null && !baseCategory.equals("shoes")) {
                shoes = p;
            } else if (cat.equals("outerwear") && outerwear == null && !baseCategory.equals("outerwear")) {
                outerwear = p;
            }
        }

        List<Product> outfit = new ArrayList<>();

        if (top != null) {
            outfit.add(top);
        }
        if (bottom != null) {
            outfit.add(bottom);
        }
        if (shoes != null) {
            outfit.add(shoes);
        }
        if (outerwear != null) {
            outfit.add(outerwear);
        }

        return outfit;
    }

    public String saveToBag(String rfid) {
        Product product = productRepository.findById(rfid)
                .orElseThrow(() -> new RuntimeException("Cannot save. RFID not found: " + rfid));

        BagItem bagItem = new BagItem(
                product.getRfid(),
                product.getRetailerName(),
                product.getItemName(),
                product.getImageUrl(),
                product.getPrice(),
                LocalDateTime.now()
        );

        bagItemRepository.save(bagItem);

        trendEventRepository.save(
                new TrendEvent(
                        product.getRetailerName(),
                        product.getItemName(),
                        "SAVE",
                        LocalDateTime.now()
                )
        );

        return product.getItemName() + " added to your style bag.";
    }

    public BagSummaryResponse getBagSummary() {
        List<BagItem> items = bagItemRepository.findAll();

        double subtotal = items.stream()
                .mapToDouble(BagItem::getPrice)
                .sum();

        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        return new BagSummaryResponse(items, subtotal, tax, total);
    }

    public String removeBagItem(Long id) {
        BagItem item = bagItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bag item not found with id: " + id));

        bagItemRepository.delete(item);
        return item.getItemName() + " removed from your style bag.";
    }

    public String clearBag() {
        bagItemRepository.deleteAll();
        return "Style bag cleared.";
    }

    public List<TrendDTO> getTrends() {
        List<TrendEvent> saveEvents = trendEventRepository.findByEventTypeIgnoreCase("SAVE");

        Map<String, Integer> counts = new HashMap<>();

        for (TrendEvent event : saveEvents) {
            String key = event.getRetailerName() + "||" + event.getItemName();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        List<TrendDTO> trends = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String[] parts = entry.getKey().split("\\|\\|", 2);
            String store = parts[0];
            String item = parts[1];
            int count = entry.getValue();

            trends.add(new TrendDTO(store, item, count));
        }

        trends.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        return trends;
    }

    public AnalyticsSummaryDTO getAnalyticsSummary() {
        List<TrendEvent> allEvents = trendEventRepository.findAll();

        long totalScans = 0;
        long totalSaves = 0;

        Map<String, Integer> scannedItemCounts = new HashMap<>();
        Map<String, Integer> savedItemCounts = new HashMap<>();
        Map<String, Integer> retailerCounts = new HashMap<>();

        for (TrendEvent event : allEvents) {
            String itemName = event.getItemName();
            String retailerName = event.getRetailerName();

            retailerCounts.put(retailerName, retailerCounts.getOrDefault(retailerName, 0) + 1);

            if ("SCAN".equalsIgnoreCase(event.getEventType())) {
                totalScans++;
                scannedItemCounts.put(itemName, scannedItemCounts.getOrDefault(itemName, 0) + 1);
            } else if ("SAVE".equalsIgnoreCase(event.getEventType())) {
                totalSaves++;
                savedItemCounts.put(itemName, savedItemCounts.getOrDefault(itemName, 0) + 1);
            }
        }

        double conversionRate = totalScans == 0 ? 0.0 : ((double) totalSaves / totalScans) * 100.0;

        String topScannedItem = findTopKey(scannedItemCounts);
        String topSavedItem = findTopKey(savedItemCounts);
        String topRetailer = findTopKey(retailerCounts);

        return new AnalyticsSummaryDTO(
                totalScans,
                totalSaves,
                Math.round(conversionRate * 100.0) / 100.0,
                topScannedItem,
                topSavedItem,
                topRetailer
        );
    }

    public List<ActivityDTO> getRecentActivity(String eventType, String retailer) {
        List<TrendEvent> events;

        boolean allEvents = eventType == null || eventType.isBlank() || eventType.equalsIgnoreCase("ALL");
        boolean allRetailers = retailer == null || retailer.isBlank() || retailer.equalsIgnoreCase("ALL");

        if (allEvents && allRetailers) {
            events = trendEventRepository.findAll();
        } else if (!allEvents && allRetailers) {
            events = trendEventRepository.findByEventTypeIgnoreCase(eventType);
        } else if (allEvents) {
            events = trendEventRepository.findByRetailerNameIgnoreCase(retailer);
        } else {
            events = trendEventRepository.findByEventTypeIgnoreCaseAndRetailerNameIgnoreCase(eventType, retailer);
        }

        events.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        List<ActivityDTO> activityList = new ArrayList<>();
        int limit = Math.min(events.size(), 12);

        for (int i = 0; i < limit; i++) {
            TrendEvent event = events.get(i);

            activityList.add(new ActivityDTO(
                    event.getRetailerName(),
                    event.getItemName(),
                    event.getEventType(),
                    getTimeAgo(event.getCreatedAt()),
                    event.getCreatedAt().toString()
            ));
        }

        return activityList;
    }

    public List<RetailerStatsDTO> getRetailerStats() {
        List<TrendEvent> events = trendEventRepository.findAll();

        Map<String, Long> scanCounts = new HashMap<>();
        Map<String, Long> saveCounts = new HashMap<>();

        for (TrendEvent event : events) {
            String retailer = event.getRetailerName();

            if ("SCAN".equalsIgnoreCase(event.getEventType())) {
                scanCounts.put(retailer, scanCounts.getOrDefault(retailer, 0L) + 1);
            } else if ("SAVE".equalsIgnoreCase(event.getEventType())) {
                saveCounts.put(retailer, saveCounts.getOrDefault(retailer, 0L) + 1);
            }
        }

        List<RetailerStatsDTO> stats = new ArrayList<>();

        for (String retailer : scanCounts.keySet()) {
            long scans = scanCounts.getOrDefault(retailer, 0L);
            long saves = saveCounts.getOrDefault(retailer, 0L);
            double conversionRate = scans == 0 ? 0.0 : ((double) saves / scans) * 100.0;

            stats.add(new RetailerStatsDTO(
                    retailer,
                    scans,
                    saves,
                    Math.round(conversionRate * 100.0) / 100.0
            ));
        }

        for (String retailer : saveCounts.keySet()) {
            if (!scanCounts.containsKey(retailer)) {
                stats.add(new RetailerStatsDTO(
                        retailer,
                        0,
                        saveCounts.get(retailer),
                        0.0
                ));
            }
        }

        stats.sort((a, b) -> Long.compare((b.getScans() + b.getSaves()), (a.getScans() + a.getSaves())));
        return stats;
    }

    private String resolveRetailerName(String retailerKey) {
        String normalized = normalize(retailerKey);

        return switch (normalized) {
            case "macy001", "macys", "macy's" -> "Macy's";
            case "zara001", "zara" -> "Zara";
            case "nike001", "nike" -> "Nike";
            case "nord001", "nordstrom" -> "Nordstrom";
            default -> throw new IllegalArgumentException("Unknown retailer: " + retailerKey);
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replace("’", "'")
                .toLowerCase();
    }

    private String findTopKey(Map<String, Integer> counts) {
        return counts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    private String getTimeAgo(LocalDateTime time) {
        long seconds = Duration.between(time, LocalDateTime.now()).getSeconds();

        if (seconds < 60) {
            return seconds + "s ago";
        }
        if (seconds < 3600) {
            return (seconds / 60) + "m ago";
        }
        if (seconds < 86400) {
            return (seconds / 3600) + "h ago";
        }
        return (seconds / 86400) + "d ago";
    }
}
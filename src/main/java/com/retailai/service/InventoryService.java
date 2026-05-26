package com.retailai.service;

import com.retailai.dto.ActivityDTO;
import com.retailai.dto.AnalyticsSummaryDTO;
import com.retailai.dto.FullOutfitDTO;
import com.retailai.dto.LookResponseDTO;
import com.retailai.dto.MerchantInventoryItemDTO;
import com.retailai.dto.MerchantInventoryPageDTO;
import com.retailai.dto.StoreStaffDashboardDTO;
import com.retailai.dto.RecommendationItemDTO;
import com.retailai.dto.RetailerStatsDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.dto.TrendDTO;
import com.retailai.model.BagItem;
import com.retailai.model.BagSummaryResponse;
import com.retailai.model.Product;
import com.retailai.model.TrendEvent;
import com.retailai.repository.BagItemRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.TrendEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final int DEFAULT_REORDER_THRESHOLD = 3;
    private static final int DEFAULT_IDEAL_STOCK_LEVEL = 12;

    private final ProductRepository productRepository;
    private final BagItemRepository bagItemRepository;
    private final TrendEventRepository trendEventRepository;
    private final AIStylistService aiStylistService;

    @Value("${retailai.demo-scan-mode:true}")
    private boolean demoScanMode;

    public InventoryService(
            ProductRepository productRepository,
            BagItemRepository bagItemRepository,
            TrendEventRepository trendEventRepository,
            AIStylistService aiStylistService
    ) {
        this.productRepository = productRepository;
        this.bagItemRepository = bagItemRepository;
        this.trendEventRepository = trendEventRepository;
        this.aiStylistService = aiStylistService;
    }

    public ScanResultDTO scanItem(String retailerKey, String rfid, String vibe) {
        return scanItem(retailerKey, null, rfid, vibe);
    }

    public ScanResultDTO scanItem(String retailerKey, String storeCode, String rfid, String vibe) {
        Product product = loadScannedProductForContext(retailerKey, storeCode, rfid);

        saveTrendEvent("SCAN", product);

        String stylingAdvice;
        try {
            stylingAdvice = aiStylistService.generateAdvice(product, vibe);
        } catch (RuntimeException e) {
            stylingAdvice = "This item is a strong styling anchor and can be paired with complementary pieces for a polished look.";
        }

        String whyItWorks = generateWhyItWorks(product, vibe);

        List<RecommendationItemDTO> suggestions;
        try {
            suggestions = generateSmartSuggestions(product, vibe);
        } catch (RuntimeException e) {
            suggestions = List.of();
        }

        ScanResultDTO scanResult = new ScanResultDTO();
        scanResult.setRfid(safe(product.getRfid()));
        scanResult.setName(safe(product.getItemName()));
        scanResult.setBrand(safeBrand(product));
        scanResult.setCategory(safe(product.getCategory()));
        scanResult.setColor(safeColor(product));
        scanResult.setRetailer(safe(product.getRetailerName()));
        scanResult.setRetailerKey(safe(product.getRetailerKey()));
        scanResult.setStoreCode(safe(product.getStoreCode()));
        scanResult.setStoreName(safe(product.getStoreName()));
        scanResult.setPrice(safePrice(product.getPrice()));
        scanResult.setMatchScore(calculateMainMatchScore(product, vibe));
        scanResult.setImageUrl(safeImage(product.getImageUrl()));
        scanResult.setStylingAdvice(stylingAdvice);
        scanResult.setWhyItWorks(whyItWorks);
        scanResult.setSuggestions(suggestions);

        FullOutfitDTO fullOutfit = null;

        try {
            fullOutfit = aiStylistService.buildFullOutfit(scanResult);
            scanResult.setFullOutfit(fullOutfit);
        } catch (RuntimeException e) {
            scanResult.setFullOutfit(null);
        }

        List<RecommendationItemDTO> filteredSuggestions;

        try {
            filteredSuggestions = generateAlternativeSuggestions(product, vibe, fullOutfit, 0);

            if (filteredSuggestions.isEmpty()) {
                filteredSuggestions = removeItemsAlreadyInFullOutfit(suggestions, fullOutfit);
            }
        } catch (RuntimeException e) {
            filteredSuggestions = suggestions;
        }

        scanResult.setSuggestions(filteredSuggestions);

        return scanResult;
    }

    public String saveToBag(String rfid) {
        Product product = productRepository.findById(normalizeRequired(rfid, "RFID is required."))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return saveProductToBag(
                product,
                "",
                "",
                "",
                "",
                safe(product.getRetailerKey()),
                safe(product.getStoreCode())
        );
    }

    public String saveToBag(String retailerKey, String storeCode, String rfid) {
        Product product = loadScannedProductForContext(retailerKey, storeCode, rfid);

        return saveProductToBag(
                product,
                "",
                "",
                "",
                "",
                retailerKey,
                storeCode
        );
    }

    public String saveToBag(
            String userId,
            String tenantId,
            String storeId,
            String userEmail,
            String retailerKey,
            String storeCode,
            String rfid
    ) {
        Product product = loadScannedProductForContext(retailerKey, storeCode, rfid);

        return saveProductToBag(
                product,
                userId,
                tenantId,
                storeId,
                userEmail,
                retailerKey,
                storeCode
        );
    }

    private String saveProductToBag(
            Product product,
            String userId,
            String tenantId,
            String storeId,
            String userEmail,
            String retailerKey,
            String storeCode
    ) {
        if (!isProductAvailableForStyling(product)) {
            throw new IllegalStateException("This item is currently unavailable and cannot be saved.");
        }

        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);
        String safeStoreId = safe(storeId);
        String safeUserEmail = safe(userEmail);
        String safeRetailerKey = safe(retailerKey);
        String safeStoreCode = safe(storeCode);
        String productRfid = safe(product.getRfid());

        boolean alreadySaved = bagItemRepository.findAll().stream()
                .filter(item -> matchesBagScope(item, safeUserId, safeTenantId, safeStoreCode))
                .anyMatch(item -> safe(item.getRfid()).equalsIgnoreCase(productRfid));

        if (alreadySaved) {
            return safe(product.getItemName()) + " is already in your style bag.";
        }

        BagItem item = new BagItem();
        item.setRfid(product.getRfid());
        item.setRetailerName(product.getRetailerName());
        item.setItemName(product.getItemName());
        item.setImageUrl(product.getImageUrl());
        item.setPrice(product.getPrice() == null ? 0.0 : product.getPrice());
        item.setCategory(product.getCategory());

        item.setUserId(safeUserId);
        item.setTenantId(safeTenantId);
        item.setStoreId(safeStoreId);
        item.setUserEmail(safeUserEmail);
        item.setRetailerKey(safeRetailerKey.isBlank() ? safe(product.getRetailerKey()) : safeRetailerKey);
        item.setStoreCode(safeStoreCode.isBlank() ? safe(product.getStoreCode()) : safeStoreCode);
        item.setStoreName(safe(product.getStoreName()));

        bagItemRepository.save(item);
        saveTrendEvent("SAVE", product);

        return safe(product.getItemName()) + " added to your style bag.";
    }

    public LookResponseDTO createFullLook(String rfid, String vibe) {
        return createFullLook(null, null, rfid, vibe);
    }

    public LookResponseDTO createFullLook(String retailerKey, String storeCode, String rfid, String vibe) {
        Product scannedProduct = loadScannedProductForContext(retailerKey, storeCode, rfid);

        List<RecommendationItemDTO> suggestions = generateSmartSuggestions(scannedProduct, vibe);

        if (suggestions.isEmpty()) {
            throw new RuntimeException("No full look recommendations found for RFID: " + rfid);
        }

        ScanResultDTO scanResult = buildScanResultForLook(scannedProduct, vibe, suggestions);
        FullOutfitDTO fullOutfit = aiStylistService.buildFullOutfit(scanResult);

        if (fullOutfit == null) {
            throw new RuntimeException("Unable to build full outfit for RFID: " + rfid);
        }

        List<RecommendationItemDTO> filteredSuggestions = generateAlternativeSuggestions(
                scannedProduct,
                vibe,
                fullOutfit,
                0
        );

        if (filteredSuggestions.isEmpty()) {
            filteredSuggestions = removeItemsAlreadyInFullOutfit(suggestions, fullOutfit);
        }

        LookResponseDTO response = new LookResponseDTO();
        response.setSuggestions(filteredSuggestions);
        response.setFullOutfit(fullOutfit);
        response.setVariation(0);

        return response;
    }

    public LookResponseDTO generateAgain(String rfid, String vibe, Integer variation) {
        return generateAgain(null, null, rfid, vibe, variation);
    }

    public LookResponseDTO generateAgain(String retailerKey, String storeCode, String rfid, String vibe, Integer variation) {
        Product scannedProduct = loadScannedProductForContext(retailerKey, storeCode, rfid);

        int safeVariation = variation == null ? 1 : Math.max(1, variation);

        List<RecommendationItemDTO> suggestions = generateSmartSuggestionsForVariation(
                scannedProduct,
                vibe,
                safeVariation
        );

        if (suggestions.isEmpty()) {
            throw new RuntimeException("No alternate look recommendations found for RFID: " + rfid);
        }

        ScanResultDTO scanResult = buildScanResultForLook(scannedProduct, vibe, suggestions);
        FullOutfitDTO fullOutfit = aiStylistService.buildFullOutfit(scanResult);

        if (fullOutfit == null) {
            throw new RuntimeException("Unable to build alternate outfit for RFID: " + rfid);
        }

        List<RecommendationItemDTO> filteredSuggestions = generateAlternativeSuggestions(
                scannedProduct,
                vibe,
                fullOutfit,
                safeVariation
        );

        if (filteredSuggestions.isEmpty()) {
            filteredSuggestions = removeItemsAlreadyInFullOutfit(suggestions, fullOutfit);
        }

        LookResponseDTO response = new LookResponseDTO();
        response.setSuggestions(filteredSuggestions);
        response.setFullOutfit(fullOutfit);
        response.setVariation(safeVariation);

        return response;
    }

    public LookResponseDTO swapLookItem(
            String rfid,
            String vibe,
            String swapCategory,
            String currentTopRfid,
            String currentBottomRfid,
            String currentShoesRfid,
            String currentOuterwearRfid
    ) {
        return swapLookItem(
                null,
                null,
                rfid,
                vibe,
                swapCategory,
                currentTopRfid,
                currentBottomRfid,
                currentShoesRfid,
                currentOuterwearRfid
        );
    }

    public LookResponseDTO swapLookItem(
            String retailerKey,
            String storeCode,
            String rfid,
            String vibe,
            String swapCategory,
            String currentTopRfid,
            String currentBottomRfid,
            String currentShoesRfid,
            String currentOuterwearRfid
    ) {
        Product scannedProduct = loadScannedProductForContext(retailerKey, storeCode, rfid);

        String normalizedSwapCategory = normalizeSwapCategory(swapCategory);
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);

        if (!targetCategories.contains(normalizedSwapCategory)) {
            throw new IllegalArgumentException("Swap category is not valid for this scanned item: " + swapCategory);
        }

        Map<String, Product> currentLook = new LinkedHashMap<>();

        Product currentTop = findProductIfValidForContext(currentTopRfid, "tops", rfid, scannedProduct);
        Product currentBottom = findProductIfValidForContext(currentBottomRfid, "bottoms", rfid, scannedProduct);
        Product currentShoes = findProductIfValidForContext(currentShoesRfid, "shoes", rfid, scannedProduct);
        Product currentOuterwear = findProductIfValidForContext(currentOuterwearRfid, "outerwear", rfid, scannedProduct);

        if (currentTop != null) currentLook.put("tops", currentTop);
        if (currentBottom != null) currentLook.put("bottoms", currentBottom);
        if (currentShoes != null) currentLook.put("shoes", currentShoes);
        if (currentOuterwear != null) currentLook.put("outerwear", currentOuterwear);

        List<Product> baseSuggestions = generateSmartSuggestionProducts(scannedProduct, vibe);

        for (Product product : baseSuggestions) {
            String category = normalizeCategory(product.getCategory());

            if (targetCategories.contains(category) && !currentLook.containsKey(category)) {
                currentLook.put(category, product);
            }
        }

        Set<String> excludedRfids = new LinkedHashSet<>();
        excludedRfids.add(safe(scannedProduct.getRfid()));

        for (Product product : currentLook.values()) {
            excludedRfids.add(safe(product.getRfid()));
        }

        Product replacement = findBestCandidateForCategory(
                scannedProduct,
                vibe,
                normalizedSwapCategory,
                excludedRfids
        );

        if (replacement == null) {
            throw new RuntimeException("No alternate " + normalizedSwapCategory + " recommendation found.");
        }

        currentLook.put(normalizedSwapCategory, replacement);

        excludedRfids.clear();
        excludedRfids.add(safe(scannedProduct.getRfid()));

        for (Product product : currentLook.values()) {
            excludedRfids.add(safe(product.getRfid()));
        }

        for (String category : orderedCategories()) {
            if (!targetCategories.contains(category)) {
                continue;
            }

            if (!currentLook.containsKey(category)) {
                Product fallback = findBestCandidateForCategory(scannedProduct, vibe, category, excludedRfids);

                if (fallback != null) {
                    currentLook.put(category, fallback);
                    excludedRfids.add(safe(fallback.getRfid()));
                }
            }
        }

        List<RecommendationItemDTO> currentLookItems = new ArrayList<>();

        for (String category : orderedCategories()) {
            Product product = currentLook.get(category);

            if (product != null && targetCategories.contains(category)) {
                currentLookItems.add(toRecommendationDto(scannedProduct, product, vibe));
            }
        }

        if (currentLookItems.isEmpty()) {
            throw new RuntimeException("No outfit recommendations available after swap.");
        }

        FullOutfitDTO fullOutfit = aiStylistService.buildFullOutfitFromProducts(
                scannedProduct,
                currentLook.get("tops"),
                currentLook.get("bottoms"),
                currentLook.get("shoes"),
                currentLook.get("outerwear"),
                vibe
        );

        if (fullOutfit == null) {
            throw new RuntimeException("Unable to build swapped outfit for RFID: " + rfid);
        }

        List<RecommendationItemDTO> filteredSuggestions = generateSmartSwapSuggestions(
                scannedProduct,
                vibe,
                fullOutfit,
                normalizedSwapCategory
        );

        if (filteredSuggestions.isEmpty()) {
            filteredSuggestions = removeItemsAlreadyInFullOutfit(currentLookItems, fullOutfit);
        }

        LookResponseDTO response = new LookResponseDTO();
        response.setSuggestions(filteredSuggestions);
        response.setFullOutfit(fullOutfit);
        response.setVariation(0);

        return response;
    }

    public BagSummaryResponse getBagSummary() {
        return buildBagSummary(bagItemRepository.findAll());
    }

    public BagSummaryResponse getBagSummary(String userId, String tenantId, String storeCode) {
        List<BagItem> scopedItems = bagItemRepository.findAll().stream()
                .filter(item -> matchesBagScope(item, userId, tenantId, storeCode))
                .collect(Collectors.toList());

        return buildBagSummary(scopedItems);
    }

    private BagSummaryResponse buildBagSummary(List<BagItem> items) {
        double subtotal = items.stream()
                .mapToDouble(BagItem::getPrice)
                .sum();

        double tax = subtotal * 0.0825;
        double total = subtotal + tax;

        return new BagSummaryResponse(items, subtotal, tax, total);
    }

    public String removeBagItem(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Bag item id is required.");
        }

        if (!bagItemRepository.existsById(id)) {
            throw new RuntimeException("Bag item not found: " + id);
        }

        bagItemRepository.deleteById(id);
        return "Item removed from bag.";
    }

    public String removeBagItem(Long id, String userId, String tenantId, String storeCode) {
        if (id == null) {
            throw new IllegalArgumentException("Bag item id is required.");
        }

        BagItem item = bagItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bag item not found: " + id));

        if (!matchesBagScope(item, userId, tenantId, storeCode)) {
            throw new SecurityException("You do not have permission to remove this bag item.");
        }

        bagItemRepository.deleteById(id);
        return "Item removed from bag.";
    }

    public String clearBag() {
        bagItemRepository.deleteAll();
        return "Bag cleared.";
    }

    public String clearBag(String userId, String tenantId, String storeCode) {
        List<BagItem> scopedItems = bagItemRepository.findAll().stream()
                .filter(item -> matchesBagScope(item, userId, tenantId, storeCode))
                .collect(Collectors.toList());

        bagItemRepository.deleteAll(scopedItems);
        return "Bag cleared.";
    }

    public List<TrendDTO> getTrends() {
        return getTrends(null, null);
    }

    public List<TrendDTO> getTrends(String retailerKey, String storeCode) {
        Map<String, Long> grouped = trendEventRepository.findAll().stream()
                .filter(event -> matchesTrendRetailer(event, retailerKey))
                .filter(event -> matchesTrendStore(event, storeCode))
                .filter(event -> "SAVE".equalsIgnoreCase(event.getEventType()))
                .collect(Collectors.groupingBy(
                        e -> safe(e.getRetailerName()) + "||" + safe(e.getItemName()),
                        Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|\\|", 2);
                    String store = parts.length > 0 ? parts[0] : "Retailer";
                    String item = parts.length > 1 ? parts[1] : "Product";

                    return new TrendDTO(store, item, entry.getValue().intValue());
                })
                .sorted(Comparator.comparingInt(TrendDTO::getCount).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    public StoreStaffDashboardDTO getStoreStaffDashboard(String retailerKey, String storeCode) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.");
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.");

        AnalyticsSummaryDTO summary = getAnalyticsSummary(safeRetailerKey, safeStoreCode);

        List<ActivityDTO> recentActivity;
        try {
            recentActivity = getRecentActivity("ALL", safeRetailerKey, safeStoreCode);
        } catch (RuntimeException e) {
            recentActivity = new ArrayList<>();
        }

        MerchantInventoryPageDTO inventoryPage = getMerchantInventory(
                safeRetailerKey,
                safeStoreCode,
                null,
                null,
                0,
                50
        );

        List<MerchantInventoryItemDTO> inventoryItems =
                inventoryPage == null || inventoryPage.getItems() == null
                        ? new ArrayList<>()
                        : inventoryPage.getItems();

        long totalInventoryItems =
                inventoryPage == null
                        ? inventoryItems.size()
                        : Math.max(inventoryPage.getTotalItems(), inventoryItems.size());

        long lowStockCount = inventoryItems.stream()
                .filter(item -> {
                    Integer stock = item.getStockQuantity();
                    int safeStock = stock == null ? 0 : stock;
                    return safeStock > 0 && safeStock <= 3;
                })
                .count();

        long outOfStockCount = inventoryItems.stream()
                .filter(item -> {
                    Integer stock = item.getStockQuantity();
                    int safeStock = stock == null ? 0 : stock;
                    return safeStock <= 0;
                })
                .count();

        StoreStaffDashboardDTO dashboard = new StoreStaffDashboardDTO();

        dashboard.setRetailerKey(safeRetailerKey);
        dashboard.setStoreCode(safeStoreCode);
        dashboard.setStoreName(resolveStoreNameFromInventory(inventoryItems, safeStoreCode));

        dashboard.setTodaysScans(summary == null ? 0 : summary.getTotalScans());
        dashboard.setTodaysSaves(summary == null ? 0 : summary.getTotalSaves());
        dashboard.setConversionRate(summary == null ? 0.0 : summary.getConversionRate());

        dashboard.setTotalInventoryItems(totalInventoryItems);
        dashboard.setLowStockCount(lowStockCount);
        dashboard.setOutOfStockCount(outOfStockCount);

        dashboard.setTopScannedItem(summary == null ? "N/A" : summary.getTopScannedItem());
        dashboard.setTopSavedItem(summary == null ? "N/A" : summary.getTopSavedItem());

        dashboard.setRecentActivity(
                recentActivity == null
                        ? new ArrayList<>()
                        : recentActivity.stream().limit(6).toList()
        );

        return dashboard;
    }

    private String resolveStoreNameFromInventory(
            List<MerchantInventoryItemDTO> inventoryItems,
            String fallbackStoreCode
    ) {
        if (inventoryItems != null) {
            for (MerchantInventoryItemDTO item : inventoryItems) {
                if (item == null) {
                    continue;
                }

                String storeName = safe(item.getStoreName());

                if (!storeName.isBlank()) {
                    return storeName;
                }
            }
        }

        return safe(fallbackStoreCode);
    }
    public AnalyticsSummaryDTO getAnalyticsSummary() {
        return getAnalyticsSummary(null, null);
    }

    public AnalyticsSummaryDTO getAnalyticsSummary(String retailerKey, String storeCode) {
        List<TrendEvent> events = trendEventRepository.findAll().stream()
                .filter(event -> matchesTrendRetailer(event, retailerKey))
                .filter(event -> matchesTrendStore(event, storeCode))
                .collect(Collectors.toList());

        long totalScans = events.stream()
                .filter(e -> "SCAN".equalsIgnoreCase(e.getEventType()))
                .count();

        long totalSaves = events.stream()
                .filter(e -> "SAVE".equalsIgnoreCase(e.getEventType()))
                .count();

        double conversionRate = totalScans == 0
                ? 0.0
                : ((double) totalSaves / totalScans) * 100.0;

        String topRetailer = events.stream()
                .collect(Collectors.groupingBy(e -> safe(e.getRetailerName()), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> !entry.getKey().isBlank())
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topScannedItem = events.stream()
                .filter(e -> "SCAN".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(e -> safe(e.getItemName()), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> !entry.getKey().isBlank())
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topSavedItem = events.stream()
                .filter(e -> "SAVE".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(e -> safe(e.getItemName()), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> !entry.getKey().isBlank())
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

    public List<ActivityDTO> getRecentActivity(String eventType, String retailerKey, String storeCode) {
        String safeEventType = safe(eventType);

        return trendEventRepository.findAll().stream()
                .filter(event -> matchesTrendRetailer(event, retailerKey))
                .filter(event -> matchesTrendStore(event, storeCode))
                .sorted(Comparator.comparing(
                        TrendEvent::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .filter(e -> "ALL".equalsIgnoreCase(safeEventType)
                        || safe(e.getEventType()).equalsIgnoreCase(safeEventType))
                .limit(20)
                .map(event -> new ActivityDTO(
                        safe(event.getEventType()),
                        safe(event.getRetailerName()),
                        safe(event.getItemName()),
                        timeAgo(event.getCreatedAt()),
                        event.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<RetailerStatsDTO> getRetailerStats() {
        return getRetailerStats(null, null);
    }

    public List<RetailerStatsDTO> getRetailerStats(String retailerKey, String storeCode) {
        List<TrendEvent> events = trendEventRepository.findAll().stream()
                .filter(event -> matchesTrendRetailer(event, retailerKey))
                .filter(event -> matchesTrendStore(event, storeCode))
                .collect(Collectors.toList());

        Map<String, Long> scansByRetailer = events.stream()
                .filter(e -> "SCAN".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(e -> safe(e.getRetailerName()), Collectors.counting()));

        Map<String, Long> savesByRetailer = events.stream()
                .filter(e -> "SAVE".equalsIgnoreCase(e.getEventType()))
                .collect(Collectors.groupingBy(e -> safe(e.getRetailerName()), Collectors.counting()));

        Set<String> retailers = new HashSet<>();
        retailers.addAll(scansByRetailer.keySet());
        retailers.addAll(savesByRetailer.keySet());

        return retailers.stream()
                .filter(retailerName -> !safe(retailerName).isBlank())
                .map(retailerName -> {
                    long scans = scansByRetailer.getOrDefault(retailerName, 0L);
                    long saves = savesByRetailer.getOrDefault(retailerName, 0L);

                    double conversion = scans == 0
                            ? 0.0
                            : ((double) saves / scans) * 100.0;

                    return new RetailerStatsDTO(retailerName, scans, saves, conversion);
                })
                .sorted(Comparator.comparingLong(RetailerStatsDTO::getScans).reversed())
                .collect(Collectors.toList());
    }

    public MerchantInventoryItemDTO updateMerchantInventoryStock(
            String rfid,
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    ) {
        if (rfid == null || rfid.isBlank()) {
            throw new IllegalArgumentException("RFID is required.");
        }

        if (stockQuantity == null) {
            throw new IllegalArgumentException("Stock quantity is required.");
        }

        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }

        Product product = findMerchantInventoryProductForUpdate(rfid, retailerKey, storeCode);

        product.setStockQuantity(stockQuantity);

        boolean active = Boolean.TRUE.equals(product.getActive());
        product.setAvailable(active && stockQuantity > 0);

        Product saved = productRepository.save(product);
        return toMerchantInventoryItemDto(saved);
    }

    public MerchantInventoryItemDTO updateMerchantInventoryActive(
            String rfid,
            String retailerKey,
            String storeCode,
            Boolean active
    ) {
        if (rfid == null || rfid.isBlank()) {
            throw new IllegalArgumentException("RFID is required.");
        }

        if (active == null) {
            throw new IllegalArgumentException("Active flag is required.");
        }

        Product product = findMerchantInventoryProductForUpdate(rfid, retailerKey, storeCode);

        product.setActive(active);

        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setAvailable(Boolean.TRUE.equals(active) && stockQuantity > 0);

        Product saved = productRepository.save(product);
        return toMerchantInventoryItemDto(saved);
    }

    public MerchantInventoryItemDTO resyncMerchantInventoryItem(
            String rfid,
            String retailerKey,
            String storeCode
    ) {
        if (rfid == null || rfid.isBlank()) {
            throw new IllegalArgumentException("RFID is required.");
        }

        Product product = findMerchantInventoryProductForUpdate(rfid, retailerKey, storeCode);

        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        boolean active = Boolean.TRUE.equals(product.getActive());

        product.setAvailable(active && stockQuantity > 0);

        Product saved = productRepository.save(product);
        return toMerchantInventoryItemDto(saved);
    }

    public MerchantInventoryPageDTO getMerchantInventory(
            String retailerKey,
            String storeCode,
            String query,
            String category,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));

        List<Product> filtered = findMerchantInventoryProducts(retailerKey, storeCode, query, category);

        int totalItems = filtered.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalItems);
        int toIndex = Math.min(fromIndex + safeSize, totalItems);

        List<MerchantInventoryItemDTO> pageItems = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toMerchantInventoryItemDto)
                .collect(Collectors.toList());

        MerchantInventoryPageDTO response = new MerchantInventoryPageDTO();
        response.setItems(pageItems);
        response.setTotalItems(totalItems);
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalPages(totalPages);

        return response;
    }

    public String cleanupUploadedTestProducts(String retailerKey, String storeCode) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.");
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.");

        List<Product> products = productRepository.findByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );

        List<Product> productsToDelete = products.stream()
                .filter(product -> {
                    String rfid = safe(product.getRfid()).toUpperCase();

                    return rfid.startsWith("RFID-A-")
                            || rfid.startsWith("RFID-B-");
                })
                .collect(Collectors.toList());

        if (productsToDelete.isEmpty()) {
            return "No uploaded test products found for store " + safeStoreCode + ".";
        }

        productRepository.deleteAll(productsToDelete);

        return "Deleted " + productsToDelete.size()
                + " uploaded test product(s) for store " + safeStoreCode + ".";
    }

    public String exportMerchantInventoryCsv(
            String retailerKey,
            String storeCode,
            String query,
            String category
    ) {
        List<Product> products = findMerchantInventoryProducts(retailerKey, storeCode, query, category);

        StringBuilder csv = new StringBuilder();
        csv.append("rfid,item_name,brand,category,color,price,image_url,stock_quantity,retailer_key,retailer_name,store_code,store_name,active,available,low_stock,out_of_stock,reorder_threshold,suggested_reorder_quantity,inventory_alert\n");

        for (Product product : products) {
            MerchantInventoryItemDTO item = toMerchantInventoryItemDto(product);
            appendMerchantInventoryCsvRow(csv, item, true);
        }

        return csv.toString();
    }

    public String exportLowStockInventoryCsv(
            String retailerKey,
            String storeCode,
            String query,
            String category,
            Integer threshold
    ) {
        int safeThreshold = threshold == null ? DEFAULT_REORDER_THRESHOLD : Math.max(0, threshold);

        List<Product> products = findMerchantInventoryProducts(retailerKey, storeCode, query, category).stream()
                .filter(product -> {
                    int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                    return stockQuantity <= safeThreshold;
                })
                .sorted(Comparator
                        .comparingInt((Product product) -> product.getStockQuantity() == null ? 0 : product.getStockQuantity())
                        .thenComparing(product -> safe(product.getItemName()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(product -> safe(product.getRfid())))
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("rfid,item_name,brand,category,color,price,image_url,stock_quantity,retailer_key,retailer_name,store_code,store_name,active,available,low_stock_threshold\n");

        for (Product product : products) {
            MerchantInventoryItemDTO item = toMerchantInventoryItemDto(product);

            csv.append(csvValue(item.getRfid())).append(",");
            csv.append(csvValue(item.getItemName())).append(",");
            csv.append(csvValue(item.getBrand())).append(",");
            csv.append(csvValue(item.getCategory())).append(",");
            csv.append(csvValue(item.getColor())).append(",");
            csv.append(item.getPrice() == null ? "0.0" : item.getPrice()).append(",");
            csv.append(csvValue(item.getImageUrl())).append(",");
            csv.append(item.getStockQuantity() == null ? "0" : item.getStockQuantity()).append(",");
            csv.append(csvValue(item.getRetailerKey())).append(",");
            csv.append(csvValue(item.getRetailerName())).append(",");
            csv.append(csvValue(item.getStoreCode())).append(",");
            csv.append(csvValue(item.getStoreName())).append(",");
            csv.append(Boolean.TRUE.equals(item.getActive())).append(",");
            csv.append(Boolean.TRUE.equals(item.getAvailable())).append(",");
            csv.append(safeThreshold).append("\n");
        }

        return csv.toString();
    }

    public String exportMerchantReorderReportCsv(
            String retailerKey,
            String storeCode,
            String query,
            String category
    ) {
        List<Product> products = findMerchantInventoryProducts(retailerKey, storeCode, query, category);

        StringBuilder csv = new StringBuilder();
        csv.append("rfid,item_name,brand,category,color,price,image_url,stock_quantity,retailer_key,retailer_name,store_code,store_name,active,available,reorder_threshold,suggested_reorder_quantity,inventory_alert\n");

        for (Product product : products) {
            MerchantInventoryItemDTO item = toMerchantInventoryItemDto(product);

            boolean needsReorder = Boolean.TRUE.equals(item.getOutOfStock())
                    || Boolean.TRUE.equals(item.getLowStock());

            if (!needsReorder) {
                continue;
            }

            csv.append(csvValue(item.getRfid())).append(",");
            csv.append(csvValue(item.getItemName())).append(",");
            csv.append(csvValue(item.getBrand())).append(",");
            csv.append(csvValue(item.getCategory())).append(",");
            csv.append(csvValue(item.getColor())).append(",");
            csv.append(item.getPrice() == null ? "0.0" : item.getPrice()).append(",");
            csv.append(csvValue(item.getImageUrl())).append(",");
            csv.append(item.getStockQuantity() == null ? "0" : item.getStockQuantity()).append(",");
            csv.append(csvValue(item.getRetailerKey())).append(",");
            csv.append(csvValue(item.getRetailerName())).append(",");
            csv.append(csvValue(item.getStoreCode())).append(",");
            csv.append(csvValue(item.getStoreName())).append(",");
            csv.append(Boolean.TRUE.equals(item.getActive())).append(",");
            csv.append(Boolean.TRUE.equals(item.getAvailable())).append(",");
            csv.append(item.getReorderThreshold() == null ? DEFAULT_REORDER_THRESHOLD : item.getReorderThreshold()).append(",");
            csv.append(item.getSuggestedReorderQuantity() == null ? "0" : item.getSuggestedReorderQuantity()).append(",");
            csv.append(csvValue(item.getInventoryAlert())).append("\n");
        }

        return csv.toString();
    }

    private List<Product> findMerchantInventoryProducts(
            String retailerKey,
            String storeCode,
            String query,
            String category
    ) {
        String safeRetailerKey = safe(retailerKey);
        String safeStoreCode = safe(storeCode);
        String safeQuery = safeLower(query);
        String safeCategory = safeLower(category);

        List<Product> baseProducts;

        if (!safeRetailerKey.isBlank() && !safeStoreCode.isBlank()) {
            baseProducts = productRepository.findByRetailerKeyAndStoreCode(safeRetailerKey, safeStoreCode);
        } else if (!safeRetailerKey.isBlank()) {
            baseProducts = productRepository.findByRetailerKey(safeRetailerKey);
        } else if (!safeStoreCode.isBlank()) {
            baseProducts = productRepository.findByStoreCode(safeStoreCode);
        } else {
            baseProducts = productRepository.findAll();
        }

        return baseProducts.stream()
                .filter(product -> safeCategory.isBlank()
                        || normalizeCategory(product.getCategory()).equals(safeCategory)
                        || safe(product.getCategory()).equalsIgnoreCase(safeCategory))
                .filter(product -> {
                    if (safeQuery.isBlank()) {
                        return true;
                    }

                    return safe(product.getRfid()).toLowerCase().contains(safeQuery)
                            || safe(product.getItemName()).toLowerCase().contains(safeQuery)
                            || safe(product.getBrand()).toLowerCase().contains(safeQuery)
                            || safe(product.getCategory()).toLowerCase().contains(safeQuery)
                            || safe(product.getColor()).toLowerCase().contains(safeQuery);
                })
                .sorted(Comparator
                        .comparing((Product p) -> safe(p.getItemName()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p -> safe(p.getRfid())))
                .collect(Collectors.toList());
    }

    private Product findMerchantInventoryProductForUpdate(
            String rfid,
            String retailerKey,
            String storeCode
    ) {
        Product product = productRepository.findById(normalizeRequired(rfid, "RFID is required."))
                .orElseThrow(() -> new RuntimeException("Inventory item not found for RFID: " + rfid));

        if (!matchesRetailerSelection(product, retailerKey)) {
            throw new IllegalArgumentException("RFID does not belong to the selected retailer.");
        }

        if (!matchesStoreSelection(product, storeCode)) {
            throw new IllegalArgumentException("RFID does not belong to the selected store.");
        }

        return product;
    }

    private MerchantInventoryItemDTO toMerchantInventoryItemDto(Product product) {
        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        int reorderThreshold = DEFAULT_REORDER_THRESHOLD;
        int idealStockLevel = DEFAULT_IDEAL_STOCK_LEVEL;

        boolean outOfStock = stockQuantity <= 0;
        boolean lowStock = stockQuantity > 0 && stockQuantity <= reorderThreshold;
        int suggestedReorderQuantity = Math.max(0, idealStockLevel - stockQuantity);

        String inventoryAlert;

        if (outOfStock) {
            inventoryAlert = "Out of stock — reorder immediately.";
        } else if (lowStock) {
            inventoryAlert = "Low stock — suggested reorder: " + suggestedReorderQuantity + " units.";
        } else {
            inventoryAlert = "Stock level is healthy.";
        }

        MerchantInventoryItemDTO dto = new MerchantInventoryItemDTO();
        dto.setRfid(safe(product.getRfid()));
        dto.setItemName(safe(product.getItemName()));
        dto.setBrand(safe(product.getBrand()));
        dto.setCategory(safe(product.getCategory()));
        dto.setColor(safeColor(product));
        dto.setPrice(safePrice(product.getPrice()));
        dto.setImageUrl(safeImage(product.getImageUrl()));
        dto.setStockQuantity(stockQuantity);
        dto.setRetailerName(safe(product.getRetailerName()));
        dto.setRetailerKey(safe(product.getRetailerKey()));
        dto.setStoreName(safe(product.getStoreName()));
        dto.setStoreCode(safe(product.getStoreCode()));
        dto.setAvailable(Boolean.TRUE.equals(product.getAvailable()));
        dto.setActive(Boolean.TRUE.equals(product.getActive()));
        dto.setLowStock(lowStock);
        dto.setOutOfStock(outOfStock);
        dto.setReorderThreshold(reorderThreshold);
        dto.setSuggestedReorderQuantity(suggestedReorderQuantity);
        dto.setInventoryAlert(inventoryAlert);

        return dto;
    }

    private void appendMerchantInventoryCsvRow(
            StringBuilder csv,
            MerchantInventoryItemDTO item,
            boolean includeInventoryAlertColumns
    ) {
        csv.append(csvValue(item.getRfid())).append(",");
        csv.append(csvValue(item.getItemName())).append(",");
        csv.append(csvValue(item.getBrand())).append(",");
        csv.append(csvValue(item.getCategory())).append(",");
        csv.append(csvValue(item.getColor())).append(",");
        csv.append(item.getPrice() == null ? "0.0" : item.getPrice()).append(",");
        csv.append(csvValue(item.getImageUrl())).append(",");
        csv.append(item.getStockQuantity() == null ? "0" : item.getStockQuantity()).append(",");
        csv.append(csvValue(item.getRetailerKey())).append(",");
        csv.append(csvValue(item.getRetailerName())).append(",");
        csv.append(csvValue(item.getStoreCode())).append(",");
        csv.append(csvValue(item.getStoreName())).append(",");
        csv.append(Boolean.TRUE.equals(item.getActive())).append(",");
        csv.append(Boolean.TRUE.equals(item.getAvailable()));

        if (includeInventoryAlertColumns) {
            csv.append(",");
            csv.append(Boolean.TRUE.equals(item.getLowStock())).append(",");
            csv.append(Boolean.TRUE.equals(item.getOutOfStock())).append(",");
            csv.append(item.getReorderThreshold() == null ? DEFAULT_REORDER_THRESHOLD : item.getReorderThreshold()).append(",");
            csv.append(item.getSuggestedReorderQuantity() == null ? "0" : item.getSuggestedReorderQuantity()).append(",");
            csv.append(csvValue(item.getInventoryAlert()));
        }

        csv.append("\n");
    }

    private Product loadScannedProductForContext(String retailerKey, String storeCode, String rfid) {
        String safeRfid = normalizeRequired(rfid, "RFID is required");

        Product product = productRepository.findById(safeRfid)
                .orElseThrow(() -> new RuntimeException("RFID not found: " + safeRfid));

        if (!matchesRetailerSelection(product, retailerKey)) {
            throw new IllegalArgumentException("RFID does not belong to selected retailer");
        }

        if (!matchesStoreSelection(product, storeCode)) {
            throw new IllegalArgumentException("RFID does not belong to selected store");
        }

        if (!isProductAvailableForStyling(product)) {
            throw new IllegalStateException("This item is not currently available in inventory");
        }

        return product;
    }

    private ScanResultDTO buildScanResultForLook(
            Product scannedProduct,
            String vibe,
            List<RecommendationItemDTO> suggestions
    ) {
        ScanResultDTO scanResult = new ScanResultDTO();
        scanResult.setRfid(safe(scannedProduct.getRfid()));
        scanResult.setName(safe(scannedProduct.getItemName()));
        scanResult.setBrand(safeBrand(scannedProduct));
        scanResult.setCategory(safe(scannedProduct.getCategory()));
        scanResult.setColor(safeColor(scannedProduct));
        scanResult.setRetailer(safe(scannedProduct.getRetailerName()));
        scanResult.setRetailerKey(safe(scannedProduct.getRetailerKey()));
        scanResult.setStoreCode(safe(scannedProduct.getStoreCode()));
        scanResult.setStoreName(safe(scannedProduct.getStoreName()));
        scanResult.setPrice(safePrice(scannedProduct.getPrice()));
        scanResult.setMatchScore(calculateMainMatchScore(scannedProduct, vibe));
        scanResult.setImageUrl(safeImage(scannedProduct.getImageUrl()));

        try {
            scanResult.setStylingAdvice(aiStylistService.generateAdvice(scannedProduct, vibe));
        } catch (RuntimeException e) {
            scanResult.setStylingAdvice("This item is a strong styling anchor for the selected vibe.");
        }

        scanResult.setWhyItWorks(generateWhyItWorks(scannedProduct, vibe));
        scanResult.setSuggestions(suggestions);

        return scanResult;
    }

    private List<Product> findAvailableProductsForCategory(Product scannedProduct, String normalizedCategory) {
        String retailerKey = safe(scannedProduct.getRetailerKey());
        String storeCode = safe(scannedProduct.getStoreCode());
        String categoryValue = toStoredCategoryValue(normalizedCategory);

        List<Product> rawResults;

        if (!retailerKey.isBlank() && !storeCode.isBlank()) {
            rawResults = productRepository.findByRetailerKeyAndStoreCodeAndCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
                    retailerKey,
                    storeCode,
                    categoryValue,
                    0
            );
        } else if (!retailerKey.isBlank()) {
            rawResults = productRepository.findByRetailerKeyAndCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
                    retailerKey,
                    categoryValue,
                    0
            );
        } else if (!storeCode.isBlank()) {
            rawResults = productRepository.findByStoreCodeAndCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
                    storeCode,
                    categoryValue,
                    0
            );
        } else {
            rawResults = productRepository.findByCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
                    categoryValue,
                    0
            );
        }

        return rawResults.stream()
                .filter(this::isProductAvailableForStyling)
                .filter(product -> matchesRetailerSelection(product, retailerKey))
                .filter(product -> matchesStoreSelection(product, storeCode))
                .collect(Collectors.toList());
    }

    private List<Product> findAvailableProductsForTargetCategories(
            Product scannedProduct,
            Set<String> targetCategories
    ) {
        Map<String, Product> deduped = new LinkedHashMap<>();

        for (String category : targetCategories) {
            List<Product> products = findAvailableProductsForCategory(scannedProduct, category);

            for (Product product : products) {
                deduped.putIfAbsent(safe(product.getRfid()), product);
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private String toStoredCategoryValue(String normalizedCategory) {
        return switch (safeLower(normalizedCategory)) {
            case "tops" -> "Tops";
            case "bottoms" -> "Bottoms";
            case "shoes" -> "Shoes";
            case "outerwear" -> "Outerwear";
            default -> safe(normalizedCategory);
        };
    }

    private List<RecommendationItemDTO> generateSmartSuggestions(Product scannedProduct, String vibe) {
        return generateSmartSuggestionProducts(scannedProduct, vibe).stream()
                .map(candidate -> toRecommendationDto(scannedProduct, candidate, vibe))
                .collect(Collectors.toList());
    }

    private List<RecommendationItemDTO> generateSmartSuggestionsForVariation(
            Product scannedProduct,
            String vibe,
            int variation
    ) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);

        Map<String, List<Product>> groupedByCategory = findAvailableProductsForTargetCategories(scannedProduct, targetCategories).stream()
                .filter(p -> !safe(p.getRfid()).equalsIgnoreCase(safe(scannedProduct.getRfid())))
                .filter(p -> targetCategories.contains(normalizeCategory(p.getCategory())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe))
                        .reversed()
                        .thenComparing(p -> safe(p.getRetailerName()))
                        .thenComparing(p -> safe(p.getStoreName()))
                        .thenComparing(p -> safe(p.getItemName())))
                .collect(Collectors.groupingBy(
                        p -> normalizeCategory(p.getCategory()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Product> selected = new ArrayList<>();
        int offset = Math.max(0, variation - 1);

        for (String category : orderedCategories()) {
            if (!targetCategories.contains(category)) {
                continue;
            }

            List<Product> candidates = groupedByCategory.getOrDefault(category, List.of());

            if (candidates.isEmpty()) {
                continue;
            }

            Product chosen = candidates.get(offset % candidates.size());
            selected.add(chosen);
        }

        return selected.stream()
                .map(candidate -> toRecommendationDto(scannedProduct, candidate, vibe))
                .collect(Collectors.toList());
    }

    private List<Product> generateSmartSuggestionProducts(Product scannedProduct, String vibe) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);

        return findAvailableProductsForTargetCategories(scannedProduct, targetCategories).stream()
                .filter(p -> !safe(p.getRfid()).equalsIgnoreCase(safe(scannedProduct.getRfid())))
                .filter(p -> targetCategories.contains(normalizeCategory(p.getCategory())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe))
                        .reversed()
                        .thenComparing(p -> safe(p.getRetailerName()))
                        .thenComparing(p -> safe(p.getStoreName()))
                        .thenComparing(p -> safe(p.getItemName())))
                .collect(Collectors.toMap(
                        p -> normalizeCategory(p.getCategory()),
                        p -> p,
                        (first, second) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(4)
                .collect(Collectors.toList());
    }

    private List<RecommendationItemDTO> generateAlternativeSuggestions(
            Product scannedProduct,
            String vibe,
            FullOutfitDTO fullOutfit,
            int variationOffset
    ) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);
        Set<String> excludedRfids = collectUsedRfids(scannedProduct, fullOutfit);

        Map<String, List<Product>> groupedByCategory = findAvailableProductsForTargetCategories(scannedProduct, targetCategories).stream()
                .filter(p -> !excludedRfids.contains(safe(p.getRfid())))
                .filter(p -> targetCategories.contains(normalizeCategory(p.getCategory())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe))
                        .reversed()
                        .thenComparing(p -> safe(p.getRetailerName()))
                        .thenComparing(p -> safe(p.getStoreName()))
                        .thenComparing(p -> safe(p.getItemName())))
                .collect(Collectors.groupingBy(
                        p -> normalizeCategory(p.getCategory()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RecommendationItemDTO> alternatives = new ArrayList<>();
        int safeOffset = Math.max(0, variationOffset);

        for (String category : orderedCategories()) {
            if (!targetCategories.contains(category)) {
                continue;
            }

            List<Product> candidates = groupedByCategory.getOrDefault(category, List.of());

            if (candidates.isEmpty()) {
                continue;
            }

            Product chosen = candidates.get(safeOffset % candidates.size());
            alternatives.add(toRecommendationDto(scannedProduct, chosen, vibe));
        }

        return removeItemsAlreadyInFullOutfit(alternatives, fullOutfit);
    }

    private List<RecommendationItemDTO> generateSmartSwapSuggestions(
            Product scannedProduct,
            String vibe,
            FullOutfitDTO fullOutfit,
            String prioritizedCategory
    ) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);
        Set<String> excludedRfids = collectUsedRfids(scannedProduct, fullOutfit);

        if (prioritizedCategory == null || !targetCategories.contains(prioritizedCategory)) {
            return List.of();
        }

        return findAvailableProductsForCategory(scannedProduct, prioritizedCategory).stream()
                .filter(p -> !excludedRfids.contains(safe(p.getRfid())))
                .filter(p -> normalizeCategory(p.getCategory()).equals(prioritizedCategory))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSwapSuggestion(scannedProduct, p, vibe, prioritizedCategory))
                        .reversed()
                        .thenComparing(p -> safe(p.getRetailerName()))
                        .thenComparing(p -> safe(p.getStoreName()))
                        .thenComparing(p -> safe(p.getItemName())))
                .limit(3)
                .map(candidate -> toRecommendationDto(scannedProduct, candidate, vibe))
                .collect(Collectors.toList());
    }

    private int scoreSwapSuggestion(
            Product scannedProduct,
            Product candidate,
            String vibe,
            String prioritizedCategory
    ) {
        int score = scoreSuggestion(scannedProduct, candidate, vibe);
        String candidateCategory = normalizeCategory(candidate.getCategory());

        if (candidateCategory.equals(prioritizedCategory)) {
            score += 40;
        }

        if (!safe(candidate.getRetailerName()).equalsIgnoreCase(safe(scannedProduct.getRetailerName()))) {
            score += demoScanMode ? 10 : 0;
        }

        if (safe(candidate.getStoreCode()).equalsIgnoreCase(safe(scannedProduct.getStoreCode()))
                && !safe(candidate.getStoreCode()).isBlank()) {
            score += 8;
        }

        String scannedColor = safeLower(safeColor(scannedProduct));
        String candidateColor = safeLower(safeColor(candidate));

        if (scannedColor.equals(candidateColor)) {
            score += 10;
        } else if (isNeutral(scannedColor) || isNeutral(candidateColor)) {
            score += 6;
        }

        double scannedPrice = safePrice(scannedProduct.getPrice());
        double candidatePrice = safePrice(candidate.getPrice());

        if (scannedPrice > 0 && candidatePrice > 0) {
            double ratio = candidatePrice / scannedPrice;

            if (ratio >= 0.65 && ratio <= 1.35) {
                score += 8;
            } else if (ratio >= 0.45 && ratio <= 1.75) {
                score += 4;
            }
        }

        return score;
    }

    private List<RecommendationItemDTO> removeItemsAlreadyInFullOutfit(
            List<RecommendationItemDTO> suggestions,
            FullOutfitDTO fullOutfit
    ) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }

        if (fullOutfit == null) {
            return suggestions;
        }

        Set<String> usedRfids = new LinkedHashSet<>();
        addOutfitItemRfid(usedRfids, fullOutfit.getTop());
        addOutfitItemRfid(usedRfids, fullOutfit.getBottom());
        addOutfitItemRfid(usedRfids, fullOutfit.getShoes());
        addOutfitItemRfid(usedRfids, fullOutfit.getOuterwear());

        return suggestions.stream()
                .filter(item -> item != null)
                .filter(item -> !usedRfids.contains(safe(item.getRfid())))
                .collect(Collectors.toList());
    }

    private void addOutfitItemRfid(Set<String> usedRfids, RecommendationItemDTO item) {
        if (item == null) {
            return;
        }

        String rfid = safe(item.getRfid());

        if (!rfid.isBlank()) {
            usedRfids.add(rfid);
        }
    }

    private Set<String> collectUsedRfids(Product scannedProduct, FullOutfitDTO fullOutfit) {
        Set<String> usedRfids = new LinkedHashSet<>();
        usedRfids.add(safe(scannedProduct.getRfid()));

        if (fullOutfit != null) {
            addOutfitItemRfid(usedRfids, fullOutfit.getTop());
            addOutfitItemRfid(usedRfids, fullOutfit.getBottom());
            addOutfitItemRfid(usedRfids, fullOutfit.getShoes());
            addOutfitItemRfid(usedRfids, fullOutfit.getOuterwear());
        }

        return usedRfids;
    }

    private Product findBestCandidateForCategory(
            Product scannedProduct,
            String vibe,
            String targetCategory,
            Set<String> excludedRfids
    ) {
        return findAvailableProductsForCategory(scannedProduct, targetCategory).stream()
                .filter(p -> !safe(p.getRfid()).equalsIgnoreCase(safe(scannedProduct.getRfid())))
                .filter(p -> normalizeCategory(p.getCategory()).equals(targetCategory))
                .filter(p -> !excludedRfids.contains(safe(p.getRfid())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe))
                        .reversed()
                        .thenComparing(p -> safe(p.getRetailerName()))
                        .thenComparing(p -> safe(p.getStoreName()))
                        .thenComparing(p -> safe(p.getItemName())))
                .findFirst()
                .orElse(null);
    }

    private Product findProductIfValidForContext(
            String rfid,
            String expectedCategory,
            String scannedRfid,
            Product scannedProduct
    ) {
        if (rfid == null || rfid.isBlank()) {
            return null;
        }

        Product product = productRepository.findById(rfid.trim()).orElse(null);

        if (product == null) {
            return null;
        }

        if (safe(product.getRfid()).equalsIgnoreCase(safe(scannedRfid))) {
            return null;
        }

        if (!isProductAvailableForStyling(product)) {
            return null;
        }

        if (!normalizeCategory(product.getCategory()).equals(expectedCategory)) {
            return null;
        }

        if (!matchesRetailerSelection(product, scannedProduct.getRetailerKey())) {
            return null;
        }

        if (!matchesStoreSelection(product, scannedProduct.getStoreCode())) {
            return null;
        }

        return product;
    }

    private RecommendationItemDTO toRecommendationDto(Product scannedProduct, Product candidate, String vibe) {
        int styleMatch = calculateStyleMatch(scannedProduct, candidate, vibe);
        int colorMatch = calculateColorMatch(scannedProduct, candidate);
        int occasionMatch = calculateOccasionMatch(candidate, vibe);
        int overallMatch = clampScore((styleMatch + colorMatch + occasionMatch) / 3);

        String reason = generateRecommendationReason(scannedProduct, candidate, vibe);

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
        dto.setReason(reason);
        dto.setImageUrl(safeImage(candidate.getImageUrl()));

        return dto;
    }

    private int calculateStyleMatch(Product scannedProduct, Product candidate, String vibe) {
        int score = 80;

        String scannedCategory = normalizeCategory(scannedProduct.getCategory());
        String candidateCategory = normalizeCategory(candidate.getCategory());
        String candidateName = safeLower(candidate.getItemName());
        String vibeLower = safeLower(vibe);

        if ("tops".equals(scannedCategory)
                && ("bottoms".equals(candidateCategory) || "shoes".equals(candidateCategory) || "outerwear".equals(candidateCategory))) {
            score += 8;
        }

        if ("bottoms".equals(scannedCategory)
                && ("tops".equals(candidateCategory) || "shoes".equals(candidateCategory) || "outerwear".equals(candidateCategory))) {
            score += 8;
        }

        if ("shoes".equals(scannedCategory)
                && ("tops".equals(candidateCategory) || "bottoms".equals(candidateCategory) || "outerwear".equals(candidateCategory))) {
            score += 8;
        }

        if ("outerwear".equals(scannedCategory)
                && ("tops".equals(candidateCategory) || "bottoms".equals(candidateCategory) || "shoes".equals(candidateCategory))) {
            score += 8;
        }

        if ("casual".equals(vibeLower) && containsAny(candidateName, "shirt", "jeans", "sneaker", "hoodie", "coat", "trouser", "tee")) {
            score += 8;
        }

        if ("formal".equals(vibeLower) && containsAny(candidateName, "blazer", "shirt", "trouser", "loafer", "coat")) {
            score += 8;
        }

        if ("streetwear".equals(vibeLower) && containsAny(candidateName, "hoodie", "cargo", "sneaker", "oversized", "jacket")) {
            score += 8;
        }

        if ("luxury".equals(vibeLower) && containsAny(candidateName, "tailored", "leather", "premium", "coat", "loafer", "cashmere")) {
            score += 8;
        }

        if (safe(candidate.getStoreCode()).equalsIgnoreCase(safe(scannedProduct.getStoreCode()))
                && !safe(candidate.getStoreCode()).isBlank()) {
            score += 4;
        }

        return clampScore(score);
    }

    private int calculateColorMatch(Product scannedProduct, Product candidate) {
        String scannedColor = safeLower(safeColor(scannedProduct));
        String candidateColor = safeLower(safeColor(candidate));

        if (scannedColor.isBlank() || candidateColor.isBlank()) {
            return 82;
        }

        if (scannedColor.equals(candidateColor)) {
            return 96;
        }

        if (isNeutral(scannedColor) || isNeutral(candidateColor)) {
            return 92;
        }

        return 86;
    }

    private int calculateOccasionMatch(Product candidate, String vibe) {
        int score = 82;
        String name = safeLower(candidate.getItemName());
        String vibeLower = safeLower(vibe);

        if ("casual".equals(vibeLower) && containsAny(name, "shirt", "jeans", "sneaker", "hoodie", "coat", "trouser", "tee")) {
            score += 10;
        }

        if ("formal".equals(vibeLower) && containsAny(name, "blazer", "shirt", "trouser", "loafer", "dress", "coat")) {
            score += 10;
        }

        if ("date night".equals(vibeLower) && containsAny(name, "boot", "jacket", "coat", "heel", "dress", "satin")) {
            score += 10;
        }

        if ("streetwear".equals(vibeLower) && containsAny(name, "hoodie", "cargo", "sneaker", "oversized", "jacket")) {
            score += 10;
        }

        if ("luxury".equals(vibeLower) && containsAny(name, "leather", "tailored", "premium", "loafer", "coat", "cashmere")) {
            score += 10;
        }

        return clampScore(score);
    }

    private String generateRecommendationReason(Product scannedProduct, Product candidate, String vibe) {
        String scannedCategory = normalizeCategory(scannedProduct.getCategory());
        String candidateCategory = normalizeCategory(candidate.getCategory());
        String scannedColor = safeLower(safeColor(scannedProduct));
        String candidateColor = safeLower(safeColor(candidate));
        String vibeLower = safeLower(vibe);

        if (safe(candidate.getStoreCode()).equalsIgnoreCase(safe(scannedProduct.getStoreCode()))
                && !safe(candidate.getStoreCode()).isBlank()) {
            return "This piece is available in the same store context and fits the overall styling direction.";
        }

        if ("outerwear".equals(scannedCategory) && "tops".equals(candidateCategory)) {
            return "This top softens the outer layer and helps balance the overall silhouette.";
        }

        if ("outerwear".equals(scannedCategory) && "bottoms".equals(candidateCategory)) {
            return "These bottoms ground the statement outerwear and keep the look wearable.";
        }

        if ("tops".equals(candidateCategory) && "casual".equals(vibeLower)) {
            return "This top keeps the outfit clean and easy to wear while supporting the casual vibe.";
        }

        if ("outerwear".equals(candidateCategory) && "casual".equals(vibeLower)) {
            return "This outerwear layer adds structure and helps complete the outfit without losing the relaxed feel.";
        }

        if ("bottoms".equals(candidateCategory) && "casual".equals(vibeLower)) {
            return "These bottoms keep the outfit relaxed and work naturally with the selected casual vibe.";
        }

        if ("shoes".equals(candidateCategory)) {
            return "These shoes reinforce the outfit direction and help complete the full look.";
        }

        if (isNeutral(scannedColor) || isNeutral(candidateColor)) {
            return "Neutral tones make this piece easy to pair with the rest of the outfit.";
        }

        return "This piece supports the outfit by matching the vibe, category balance, and overall styling direction.";
    }

    private List<String> orderedCategories() {
        return List.of("tops", "bottoms", "shoes", "outerwear");
    }

    private int calculateMainMatchScore(Product product, String vibe) {
        int score = 84;

        String category = normalizeCategory(product.getCategory());
        String name = safeLower(product.getItemName());
        String vibeLower = safeLower(vibe);

        if (!category.isBlank()) {
            score += 4;
        }

        if (!safeColor(product).isBlank()) {
            score += 2;
        }

        if (isProductAvailableForStyling(product)) {
            score += 4;
        }

        if ("casual".equals(vibeLower) && containsAny(name, "hoodie", "jean", "sneaker", "tee", "coat", "trouser", "shirt")) {
            score += 4;
        }

        if ("formal".equals(vibeLower) && containsAny(name, "blazer", "trouser", "loafer", "shirt", "coat")) {
            score += 4;
        }

        if ("streetwear".equals(vibeLower) && containsAny(name, "hoodie", "cargo", "sneaker", "oversized", "jacket")) {
            score += 4;
        }

        if ("luxury".equals(vibeLower) && containsAny(name, "coat", "tailored", "premium", "leather", "cashmere")) {
            score += 4;
        }

        if ("outerwear".equals(category) || "tops".equals(category) || "shoes".equals(category) || "bottoms".equals(category)) {
            score += 2;
        }

        return clampScore(score);
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

        if (isProductAvailableForStyling(candidate)) {
            score += 18;
        }

        if (demoScanMode && !candidateRetailer.equals(scannedRetailer)) {
            score += 20;
        } else {
            score += 8;
        }

        if (safe(candidate.getStoreCode()).equalsIgnoreCase(safe(scannedProduct.getStoreCode()))
                && !safe(candidate.getStoreCode()).isBlank()) {
            score += 12;
        }

        double scannedPrice = safePrice(scannedProduct.getPrice());
        double candidatePrice = safePrice(candidate.getPrice());

        if (scannedPrice > 0 && candidatePrice > 0) {
            double ratio = candidatePrice / scannedPrice;

            if (ratio >= 0.6 && ratio <= 1.4) {
                score += 20;
            } else if (ratio >= 0.4 && ratio <= 1.8) {
                score += 10;
            }
        }

        if ("casual".equals(vibeLower) && containsAny(candidateName, "tee", "t-shirt", "shirt", "jeans", "sneakers", "hoodie", "coat", "trouser")) {
            score += 12;
        }

        if ("formal".equals(vibeLower) && containsAny(candidateName, "shirt", "blazer", "trouser", "oxford", "loafer", "coat", "dress")) {
            score += 12;
        }

        if ("date night".equals(vibeLower) && containsAny(candidateName, "boots", "jacket", "coat", "fitted", "heel", "sleek", "dress", "satin")) {
            score += 12;
        }

        if ("streetwear".equals(vibeLower) && containsAny(candidateName, "cargo", "hoodie", "sneaker", "jacket", "oversized")) {
            score += 12;
        }

        if ("luxury".equals(vibeLower) && containsAny(candidateName, "coat", "leather", "tailored", "premium", "heel", "loafer", "cashmere")) {
            score += 12;
        }

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
                targets.add("outerwear");
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
                targets.add("outerwear");
            }
        }

        return targets;
    }

    private String normalizeSwapCategory(String swapCategory) {
        String normalized = safeLower(swapCategory);

        return switch (normalized) {
            case "top", "tops", "shirt", "shirts", "tee", "t-shirt", "hoodie", "sweater", "knit", "blouse" -> "tops";
            case "bottom", "bottoms", "pants", "trousers", "jeans", "cargo", "shorts", "skirt" -> "bottoms";
            case "shoe", "shoes", "sneaker", "sneakers", "boot", "boots", "loafer", "loafers", "heel", "heels", "sandal", "sandals" -> "shoes";
            case "outerwear", "coat", "jacket", "blazer", "parka", "cardigan", "trench" -> "outerwear";
            default -> throw new IllegalArgumentException("Unsupported swap category: " + swapCategory);
        };
    }

    private String generateWhyItWorks(Product product, String vibe) {
        String color = safeColor(product);
        String category = safe(product.getCategory());
        String vibeName = safe(vibe);

        return "The " + (color.isBlank() ? "tone" : color.toLowerCase())
                + ", " + (category.isBlank() ? "silhouette" : category.toLowerCase())
                + ", and " + (vibeName.isBlank() ? "overall styling direction" : vibeName.toLowerCase() + " styling direction")
                + " make this piece easy to build around across multiple outfit combinations.";
    }

    private boolean matchesBagScope(
            BagItem item,
            String userId,
            String tenantId,
            String storeCode
    ) {
        if (item == null) {
            return false;
        }

        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);
        String safeStoreCode = safe(storeCode);

        boolean userMatches = safeUserId.isBlank()
                || safe(item.getUserId()).equalsIgnoreCase(safeUserId);

        boolean tenantMatches = safeTenantId.isBlank()
                || safe(item.getTenantId()).equalsIgnoreCase(safeTenantId);

        boolean storeMatches = safeStoreCode.isBlank()
                || safe(item.getStoreCode()).equalsIgnoreCase(safeStoreCode);

        return userMatches && tenantMatches && storeMatches;
    }

    private boolean matchesTrendRetailer(TrendEvent event, String retailerKey) {
        if (retailerKey == null || retailerKey.isBlank() || "ALL".equalsIgnoreCase(retailerKey)) {
            return true;
        }

        return safe(event.getRetailerKey()).equalsIgnoreCase(safe(retailerKey));
    }

    private boolean matchesTrendStore(TrendEvent event, String storeCode) {
        if (storeCode == null || storeCode.isBlank() || "ALL".equalsIgnoreCase(storeCode)) {
            return true;
        }

        return safe(event.getStoreCode()).equalsIgnoreCase(safe(storeCode));
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("\n")) {
            return "\"" + cleaned.replace("\"", "\"\"") + "\"";
        }

        return cleaned;
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
        return Set.of(
                "black",
                "white",
                "grey",
                "gray",
                "charcoal",
                "beige",
                "cream",
                "brown",
                "navy",
                "neutral",
                "camel",
                "khaki",
                "tan",
                "stone",
                "olive"
        ).contains(safeLower(color));
    }

    private boolean isProductAvailableForStyling(Product product) {
        if (product == null) {
            return false;
        }

        return Boolean.TRUE.equals(product.getActive())
                && Boolean.TRUE.equals(product.getAvailable())
                && product.getStockQuantity() != null
                && product.getStockQuantity() > 0;
    }

    private boolean matchesRetailerSelection(Product product, String retailerKey) {
        if (retailerKey == null || retailerKey.isBlank()) {
            return true;
        }

        String safeRetailerKey = safe(retailerKey);
        String safeProductRetailerKey = safe(product.getRetailerKey());

        if (!safeProductRetailerKey.isBlank()) {
            return safeProductRetailerKey.equalsIgnoreCase(safeRetailerKey);
        }

        String expectedRetailerName = mapRetailerKeyToName(safeRetailerKey);
        return safe(product.getRetailerName()).equalsIgnoreCase(expectedRetailerName);
    }

    private boolean matchesStoreSelection(Product product, String storeCode) {
        if (storeCode == null || storeCode.isBlank()) {
            return true;
        }

        return safe(product.getStoreCode()).equalsIgnoreCase(safe(storeCode));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = safe(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeCategory(String category) {
        String normalized = safeLower(category);

        return switch (normalized) {
            case "top", "tops", "shirt", "shirts", "tee", "t-shirt", "hoodie", "sweater", "knit", "blouse" -> "tops";
            case "bottom", "bottoms", "pants", "trousers", "jeans", "cargo", "shorts", "skirt" -> "bottoms";
            case "shoe", "shoes", "sneaker", "sneakers", "boot", "boots", "loafer", "loafers", "heel", "heels", "sandal", "sandals" -> "shoes";
            case "outerwear", "coat", "jacket", "blazer", "parka", "cardigan", "trench" -> "outerwear";
            default -> normalized;
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "https://placehold.co/500x620?text=Scanned+Item";
        }

        return imageUrl.trim();
    }

    private Double safePrice(Double value) {
        return value == null ? 0.0 : value;
    }

    private String safeBrand(Product product) {
        String brand = product.getBrand();

        if (brand != null && !brand.isBlank()) {
            return brand.trim();
        }

        return safe(product.getRetailerName());
    }

    private String safeColor(Product product) {
        String color = product.getColor();

        if (color != null && !color.isBlank()) {
            return color.trim();
        }

        return "Neutral";
    }

    private String mapRetailerKeyToName(String retailerKey) {
        if (retailerKey == null || retailerKey.isBlank()) {
            return "";
        }

        return switch (retailerKey.trim().toUpperCase()) {
            case "MACY001" -> "Macy's";
            case "ZARA001" -> "Zara";
            case "NORD001" -> "Nordstrom";
            case "NIKE001" -> "Nike";
            case "WALMART001" -> "Walmart";
            case "TARGET001" -> "Target";
            case "MCS001" -> "Universal Stylist App";
            default -> retailerKey.trim();
        };
    }

    private void saveTrendEvent(String eventType, Product product) {
        TrendEvent event = new TrendEvent();
        event.setEventType(eventType);
        event.setRetailerName(product.getRetailerName());
        event.setItemName(product.getItemName());
        event.setRetailerKey(product.getRetailerKey());
        event.setStoreCode(product.getStoreCode());
        event.setCreatedAt(LocalDateTime.now());

        trendEventRepository.save(event);
    }

    private String timeAgo(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "N/A";
        }

        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();

        if (minutes < 1) {
            return "Just now";
        }

        if (minutes < 60) {
            return minutes + " min ago";
        }

        long hours = minutes / 60;

        if (hours < 24) {
            return hours + " hr ago";
        }

        long days = hours / 24;

        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }
}
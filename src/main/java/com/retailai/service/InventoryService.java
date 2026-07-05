package com.retailai.service;

import com.retailai.dto.ActivityDTO;
import com.retailai.dto.AnalyticsSummaryDTO;
import com.retailai.dto.CustomerPreferenceRequest;
import com.retailai.dto.FullOutfitDTO;
import com.retailai.dto.LookResponseDTO;
import com.retailai.dto.MerchantInventoryItemDTO;
import com.retailai.dto.MerchantInventoryPageDTO;
import com.retailai.dto.MerchantInventoryUpdateRequest;
import com.retailai.dto.StoreStaffDashboardDTO;
import com.retailai.dto.RecommendationItemDTO;
import com.retailai.dto.RetailerStatsDTO;
import com.retailai.dto.ScanHistoryDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.model.ScanHistory;
import com.retailai.repository.ScanHistoryRepository;
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
    private final ScanHistoryRepository scanHistoryRepository;
    private final AIStylistService aiStylistService;

    @Value("${retailai.demo-scan-mode:true}")
    private boolean demoScanMode;

    public InventoryService(
            ProductRepository productRepository,
            BagItemRepository bagItemRepository,
            TrendEventRepository trendEventRepository,
            ScanHistoryRepository scanHistoryRepository,
            AIStylistService aiStylistService
    ) {
        this.productRepository = productRepository;
        this.bagItemRepository = bagItemRepository;
        this.trendEventRepository = trendEventRepository;
        this.scanHistoryRepository = scanHistoryRepository;
        this.aiStylistService = aiStylistService;
    }

    public ScanResultDTO scanItem(String retailerKey, String rfid, String vibe) {
        return scanItem(retailerKey, null, rfid, vibe, null);
    }

    public ScanResultDTO scanItem(String retailerKey, String storeCode, String rfid, String vibe) {
        return scanItem(retailerKey, storeCode, rfid, vibe, null);
    }

    public ScanResultDTO scanItem(
            String retailerKey,
            String storeCode,
            String rfid,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        Product product = loadScannedProductForContext(retailerKey, storeCode, rfid);

        saveTrendEvent("SCAN", product);

        saveScanHistory(
                product,
                vibe,
                "",
                "",
                "",
                "",
                retailerKey,
                storeCode
        );

        return buildScanResultFromProduct(product, vibe, preferences);
    }

    public ScanResultDTO scanItem(
            String userId,
            String tenantId,
            String storeId,
            String userEmail,
            String retailerKey,
            String storeCode,
            String rfid,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        Product product = loadScannedProductForContext(retailerKey, storeCode, rfid);

        saveTrendEvent("SCAN", product);

        saveScanHistory(
                product,
                vibe,
                userId,
                tenantId,
                storeId,
                userEmail,
                retailerKey,
                storeCode
        );

        return buildScanResultFromProduct(product, vibe, preferences);
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

        String safeRetailerKey = safe(retailerKey).isBlank()
                ? safe(product.getRetailerKey()).toUpperCase()
                : safe(retailerKey).toUpperCase();

        String safeStoreCode = safe(storeCode).isBlank()
                ? safe(product.getStoreCode()).toUpperCase()
                : safe(storeCode).toUpperCase();

        String productRfid = normalizeRequired(product.getRfid(), "Product RFID is required.");

        boolean alreadySaved;

        if (!safeUserId.isBlank()) {
            alreadySaved = bagItemRepository.findByUserIdAndRetailerKeyAndStoreCodeAndRfidIgnoreCase(
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode,
                    productRfid
            ).isPresent();
        } else if (!safeTenantId.isBlank()) {
            alreadySaved = bagItemRepository.findByTenantIdAndRetailerKeyAndStoreCodeAndRfidIgnoreCase(
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode,
                    productRfid
            ).isPresent();
        } else {
            alreadySaved = bagItemRepository.findByRetailerKeyAndStoreCodeAndRfidIgnoreCase(
                    safeRetailerKey,
                    safeStoreCode,
                    productRfid
            ).isPresent();
        }

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
        item.setRetailerKey(safeRetailerKey);
        item.setStoreCode(safeStoreCode);
        item.setStoreName(safe(product.getStoreName()));
        item.setQuantity(1);
        item.setSource("SCAN");

        bagItemRepository.save(item);
        saveTrendEvent("SAVE", product);

        return safe(product.getItemName()) + " added to your style bag.";
    }

    public LookResponseDTO createFullLook(String rfid, String vibe) {
        return createFullLook(null, null, rfid, vibe, null);
    }

    public LookResponseDTO createFullLook(String retailerKey, String storeCode, String rfid, String vibe) {
        return createFullLook(retailerKey, storeCode, rfid, vibe, null);
    }

    public LookResponseDTO createFullLook(
            String retailerKey,
            String storeCode,
            String rfid,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        Product scannedProduct = loadScannedProductForContext(retailerKey, storeCode, rfid);

        List<RecommendationItemDTO> suggestions = generateSmartSuggestions(scannedProduct, vibe, preferences);

        if (suggestions.isEmpty() && preferences != null) {
            suggestions = generateSmartSuggestions(scannedProduct, vibe, null);
        }

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
                0,
                preferences
        );

        if (filteredSuggestions.isEmpty()) {
            filteredSuggestions = removeItemsAlreadyInFullOutfit(suggestions, fullOutfit);
        }

        LookResponseDTO response = new LookResponseDTO();
        response.setSuggestions(filteredSuggestions);
        response.setFullOutfit(fullOutfit);
        response.setVariation(0);

        enrichLookResponseWithStylingNotes(
                response,
                scannedProduct,
                vibe,
                fullOutfit,
                preferences
        );

        return response;
    }

    public LookResponseDTO generateAgain(String rfid, String vibe, Integer variation) {
        return generateAgain(null, null, rfid, vibe, variation, null);
    }

    public LookResponseDTO generateAgain(
            String retailerKey,
            String storeCode,
            String rfid,
            String vibe,
            Integer variation
    ) {
        return generateAgain(retailerKey, storeCode, rfid, vibe, variation, null);
    }

    public LookResponseDTO generateAgain(
            String retailerKey,
            String storeCode,
            String rfid,
            String vibe,
            Integer variation,
            CustomerPreferenceRequest preferences
    ) {
        Product scannedProduct = loadScannedProductForContext(retailerKey, storeCode, rfid);

        int safeVariation = variation == null ? 1 : Math.max(1, variation);

        List<RecommendationItemDTO> suggestions = generateSmartSuggestionsForVariation(
                scannedProduct,
                vibe,
                safeVariation,
                preferences
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
                safeVariation,
                preferences
        );

        if (filteredSuggestions.isEmpty()) {
            filteredSuggestions = removeItemsAlreadyInFullOutfit(suggestions, fullOutfit);
        }

        LookResponseDTO response = new LookResponseDTO();
        response.setSuggestions(filteredSuggestions);
        response.setFullOutfit(fullOutfit);
        response.setVariation(safeVariation);

        enrichLookResponseWithStylingNotes(
                response,
                scannedProduct,
                vibe,
                fullOutfit,
                preferences
        );

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
                currentOuterwearRfid,
                null
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
        return swapLookItem(
                retailerKey,
                storeCode,
                rfid,
                vibe,
                swapCategory,
                currentTopRfid,
                currentBottomRfid,
                currentShoesRfid,
                currentOuterwearRfid,
                null
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
            String currentOuterwearRfid,
            CustomerPreferenceRequest preferences
    ) {
        Product scannedProduct = loadScannedProductForContext(retailerKey, storeCode, rfid);

        String normalizedSwapCategory = normalizeSwapCategory(swapCategory);
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);

        if (!targetCategories.contains(normalizedSwapCategory)) {
            throw new IllegalArgumentException("Swap category is not valid for this scanned item: " + swapCategory);
        }

        Map<String, Product> currentLook = new LinkedHashMap<>();

        Product currentTop = findProductIfValidForContext(currentTopRfid, "tops", rfid, scannedProduct, preferences);
        Product currentBottom = findProductIfValidForContext(currentBottomRfid, "bottoms", rfid, scannedProduct, preferences);
        Product currentShoes = findProductIfValidForContext(currentShoesRfid, "shoes", rfid, scannedProduct, preferences);
        Product currentOuterwear = findProductIfValidForContext(currentOuterwearRfid, "outerwear", rfid, scannedProduct, preferences);

        if (currentTop != null) {
            currentLook.put("tops", currentTop);
        }

        if (currentBottom != null) {
            currentLook.put("bottoms", currentBottom);
        }

        if (currentShoes != null) {
            currentLook.put("shoes", currentShoes);
        }

        if (currentOuterwear != null) {
            currentLook.put("outerwear", currentOuterwear);
        }

        List<Product> baseSuggestions = generateSmartSuggestionProducts(scannedProduct, vibe, preferences);

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
                excludedRfids,
                preferences
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
                Product fallback = findBestCandidateForCategory(scannedProduct, vibe, category, excludedRfids, preferences);

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
                currentLookItems.add(toRecommendationDto(scannedProduct, product, vibe, preferences));
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
                normalizedSwapCategory,
                preferences
        );

        if (filteredSuggestions.isEmpty()) {
            filteredSuggestions = removeItemsAlreadyInFullOutfit(currentLookItems, fullOutfit);
        }

        LookResponseDTO response = new LookResponseDTO();
        response.setSuggestions(filteredSuggestions);
        response.setFullOutfit(fullOutfit);
        response.setVariation(0);

        enrichLookResponseWithStylingNotes(
                response,
                scannedProduct,
                vibe,
                fullOutfit,
                preferences
        );

        return response;
    }

    public BagSummaryResponse getBagSummary() {
        return buildBagSummary(bagItemRepository.findAll());
    }

    public BagSummaryResponse getBagSummary(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        List<BagItem> scopedItems = loadScopedBagItems(
                userId,
                tenantId,
                retailerKey,
                storeCode
        );

        return buildBagSummary(scopedItems);
    }

    public BagSummaryResponse updateBagItemQuantity(
            Long bagItemId,
            Integer quantity,
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        if (bagItemId == null) {
            throw new IllegalArgumentException("Bag item id is required.");
        }

        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required.");
        }

        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase();
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase();
        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);

        BagItem item;

        if (!safeUserId.isBlank()) {
            item = bagItemRepository.findByIdAndUserIdAndRetailerKeyAndStoreCode(
                    bagItemId,
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            ).orElseThrow(() -> new SecurityException("You do not have permission to update this bag item."));
        } else if (!safeTenantId.isBlank()) {
            item = bagItemRepository.findByIdAndTenantIdAndRetailerKeyAndStoreCode(
                    bagItemId,
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            ).orElseThrow(() -> new SecurityException("You do not have permission to update this bag item."));
        } else {
            item = bagItemRepository.findByIdAndRetailerKeyAndStoreCode(
                    bagItemId,
                    safeRetailerKey,
                    safeStoreCode
            ).orElseThrow(() -> new SecurityException("You do not have permission to update this bag item."));
        }

        item.setQuantity(quantity);
        bagItemRepository.save(item);

        return getBagSummary(
                safeUserId,
                safeTenantId,
                safeRetailerKey,
                safeStoreCode
        );
    }

    public int removeUnavailableBagItems(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase();
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase();
        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);

        List<BagItem> scopedItems = loadScopedBagItems(
                safeUserId,
                safeTenantId,
                safeRetailerKey,
                safeStoreCode
        );

        if (scopedItems.isEmpty()) {
            return 0;
        }

        List<BagItem> unavailableItems = scopedItems.stream()
                .filter(bagItem -> {
                    String rfid = safe(bagItem.getRfid());

                    if (rfid.isBlank()) {
                        return true;
                    }

                    Product product = productRepository.findById(rfid).orElse(null);

                    if (product == null) {
                        return true;
                    }

                    if (!matchesRetailerSelection(product, safeRetailerKey)) {
                        return true;
                    }

                    if (!matchesStoreSelection(product, safeStoreCode)) {
                        return true;
                    }

                    if (!isProductAvailableForStyling(product)) {
                        return true;
                    }

                    int requestedQuantity = bagItem.getQuantity() == null
                            ? 1
                            : Math.max(1, bagItem.getQuantity());

                    int stockQuantity = product.getStockQuantity() == null
                            ? 0
                            : product.getStockQuantity();

                    return stockQuantity < requestedQuantity;
                })
                .toList();

        if (unavailableItems.isEmpty()) {
            return 0;
        }

        bagItemRepository.deleteAll(unavailableItems);

        return unavailableItems.size();
    }

    private BagSummaryResponse buildBagSummary(List<BagItem> items) {

        List<BagItem> safeItems = items == null ? List.of() : items;

        double subtotal = safeItems.stream()
                .mapToDouble(item -> {
                    double price = item.getPrice() == null ? 0.0 : item.getPrice();
                    int quantity = item.getQuantity() == null ? 1 : Math.max(1, item.getQuantity());
                    return price * quantity;
                })
                .sum();

        double tax = subtotal * 0.0825;
        double total = subtotal + tax;

        return new BagSummaryResponse(safeItems, subtotal, tax, total);
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

    public String removeBagItem(
            Long id,
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        if (id == null) {
            throw new IllegalArgumentException("Bag item id is required.");
        }

        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase();
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase();
        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);

        BagItem item;

        if (!safeUserId.isBlank()) {
            item = bagItemRepository.findByIdAndUserIdAndRetailerKeyAndStoreCode(
                    id,
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            ).orElseThrow(() -> new SecurityException("You do not have permission to remove this bag item."));
        } else if (!safeTenantId.isBlank()) {
            item = bagItemRepository.findByIdAndTenantIdAndRetailerKeyAndStoreCode(
                    id,
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            ).orElseThrow(() -> new SecurityException("You do not have permission to remove this bag item."));
        } else {
            item = bagItemRepository.findByIdAndRetailerKeyAndStoreCode(
                    id,
                    safeRetailerKey,
                    safeStoreCode
            ).orElseThrow(() -> new SecurityException("You do not have permission to remove this bag item."));
        }

        bagItemRepository.delete(item);
        return "Item removed from bag.";
    }

    public String clearBag() {
        bagItemRepository.deleteAll();
        return "Bag cleared.";
    }

    public String clearBag(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase();
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase();
        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);

        if (!safeUserId.isBlank()) {
            bagItemRepository.deleteByUserIdAndRetailerKeyAndStoreCode(
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Bag cleared.";
        }

        if (!safeTenantId.isBlank()) {
            bagItemRepository.deleteByTenantIdAndRetailerKeyAndStoreCode(
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Bag cleared.";
        }

        bagItemRepository.deleteByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );

        return "Bag cleared.";
    }

    public List<ScanHistoryDTO> getScanHistory(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase();
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase();
        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);

        List<ScanHistory> history;

        if (!safeUserId.isBlank()) {
            history = scanHistoryRepository.findTop30ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            );
        } else if (!safeTenantId.isBlank()) {
            history = scanHistoryRepository.findTop30ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            );
        } else {
            history = scanHistoryRepository.findTop30ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeRetailerKey,
                    safeStoreCode
            );
        }

        return history.stream()
                .map(ScanHistoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public String clearScanHistory(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase();
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase();
        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);

        if (!safeUserId.isBlank()) {
            scanHistoryRepository.deleteByUserIdAndRetailerKeyAndStoreCode(
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Scan history cleared.";
        }

        if (!safeTenantId.isBlank()) {
            scanHistoryRepository.deleteByTenantIdAndRetailerKeyAndStoreCode(
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Scan history cleared.";
        }

        scanHistoryRepository.deleteByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );

        return "Scan history cleared.";
    }

    private List<BagItem> loadScopedBagItems(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase();
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase();
        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);

        if (!safeUserId.isBlank()) {
            return bagItemRepository.findByUserIdAndRetailerKeyAndStoreCode(
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            );
        }

        if (!safeTenantId.isBlank()) {
            return bagItemRepository.findByTenantIdAndRetailerKeyAndStoreCode(
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            );
        }

        return bagItemRepository.findByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );
    }

    public List<TrendDTO> getTrends() {
        return getTrends(null, null);
    }

    public List<TrendDTO> getTrends(String retailerKey, String storeCode) {
        List<TrendEvent> events = loadTrendEventsForStore(retailerKey, storeCode, "SAVE");

        Map<String, Long> grouped = events.stream()
                .filter(event -> "SAVE".equalsIgnoreCase(event.getEventType()))
                .collect(Collectors.groupingBy(
                        event -> safe(event.getRetailerName()) + "||" + safe(event.getItemName()),
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
        List<TrendEvent> events = loadTrendEventsForStore(retailerKey, storeCode, "ALL");

        long totalScans = events.stream()
                .filter(event -> "SCAN".equalsIgnoreCase(event.getEventType()))
                .count();

        long totalSaves = events.stream()
                .filter(event -> "SAVE".equalsIgnoreCase(event.getEventType()))
                .count();

        double conversionRate = totalScans == 0
                ? 0.0
                : ((double) totalSaves / totalScans) * 100.0;

        String topRetailer = events.stream()
                .collect(Collectors.groupingBy(
                        event -> safe(event.getRetailerName()),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().isBlank())
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topScannedItem = events.stream()
                .filter(event -> "SCAN".equalsIgnoreCase(event.getEventType()))
                .collect(Collectors.groupingBy(
                        event -> safe(event.getItemName()),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().isBlank())
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topSavedItem = events.stream()
                .filter(event -> "SAVE".equalsIgnoreCase(event.getEventType()))
                .collect(Collectors.groupingBy(
                        event -> safe(event.getItemName()),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
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
        List<TrendEvent> events;

        boolean hasStoreScope = !safe(retailerKey).isBlank()
                && !safe(storeCode).isBlank()
                && !"ALL".equalsIgnoreCase(retailerKey)
                && !"ALL".equalsIgnoreCase(storeCode);

        if (hasStoreScope && !"ALL".equalsIgnoreCase(safeEventType) && !safeEventType.isBlank()) {
            events = trendEventRepository.findTop20ByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndEventTypeIgnoreCaseOrderByCreatedAtDesc(
                    safe(retailerKey).toUpperCase(),
                    safe(storeCode).toUpperCase(),
                    safeEventType
            );
        } else if (hasStoreScope) {
            events = trendEventRepository.findTop20ByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseOrderByCreatedAtDesc(
                    safe(retailerKey).toUpperCase(),
                    safe(storeCode).toUpperCase()
            );
        } else {
            events = trendEventRepository.findAll().stream()
                    .sorted(Comparator.comparing(
                            TrendEvent::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .limit(20)
                    .collect(Collectors.toList());
        }

        return events.stream()
                .filter(event -> "ALL".equalsIgnoreCase(safeEventType)
                        || safeEventType.isBlank()
                        || safe(event.getEventType()).equalsIgnoreCase(safeEventType))
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
        List<TrendEvent> events = loadTrendEventsForStore(retailerKey, storeCode, "ALL");

        Map<String, Long> scansByRetailer = events.stream()
                .filter(event -> "SCAN".equalsIgnoreCase(event.getEventType()))
                .collect(Collectors.groupingBy(
                        event -> safe(event.getRetailerName()),
                        Collectors.counting()
                ));

        Map<String, Long> savesByRetailer = events.stream()
                .filter(event -> "SAVE".equalsIgnoreCase(event.getEventType()))
                .collect(Collectors.groupingBy(
                        event -> safe(event.getRetailerName()),
                        Collectors.counting()
                ));

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

                    return new RetailerStatsDTO(
                            retailerName,
                            scans,
                            saves,
                            conversion
                    );
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


    public MerchantInventoryItemDTO updateMerchantInventoryItem(
            String rfid,
            String retailerKey,
            String storeCode,
            MerchantInventoryItemDTO updateRequest
    ) {
        if (rfid == null || rfid.isBlank()) {
            throw new IllegalArgumentException("RFID is required.");
        }

        if (updateRequest == null) {
            throw new IllegalArgumentException("Inventory update request body is required.");
        }

        Product product = findMerchantInventoryProductForUpdate(rfid, retailerKey, storeCode);

        product.setItemName(safe(updateRequest.getItemName()));
        product.setBrand(safe(updateRequest.getBrand()));
        product.setCategory(safe(updateRequest.getCategory()));
        product.setColor(safe(updateRequest.getColor()));
        product.setPrice(updateRequest.getPrice() == null ? 0.0 : Math.max(0.0, updateRequest.getPrice()));
        product.setImageUrl(safe(updateRequest.getImageUrl()));

        product.setSize(safe(updateRequest.getSize()));
        product.setFit(safe(updateRequest.getFit()));
        product.setMaterial(safe(updateRequest.getMaterial()));
        product.setGender(safe(updateRequest.getGender()));
        product.setSeason(safe(updateRequest.getSeason()));
        product.setOccasion(safe(updateRequest.getOccasion()));
        product.setStyleTags(safe(updateRequest.getStyleTags()));
        product.setPattern(safe(updateRequest.getPattern()));

        if (updateRequest.getStockQuantity() != null) {
            int stockQuantity = Math.max(0, updateRequest.getStockQuantity());
            product.setStockQuantity(stockQuantity);
        }

        if (updateRequest.getActive() != null) {
            product.setActive(Boolean.TRUE.equals(updateRequest.getActive()));
        }

        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        boolean active = Boolean.TRUE.equals(product.getActive());

        product.setAvailable(active && stockQuantity > 0);

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
        dto.setSize(safe(product.getSize()));
        dto.setFit(safe(product.getFit()));
        dto.setMaterial(safe(product.getMaterial()));
        dto.setGender(safe(product.getGender()));
        dto.setSeason(safe(product.getSeason()));
        dto.setOccasion(safe(product.getOccasion()));
        dto.setStyleTags(safe(product.getStyleTags()));
        dto.setPattern(safe(product.getPattern()));

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

    private ScanResultDTO buildScanResultFromProduct(
            Product product,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        String stylingAdvice;

        try {
            stylingAdvice = aiStylistService.generateAdvice(product, vibe);
        } catch (RuntimeException e) {
            stylingAdvice = "This item is a strong styling anchor and can be paired with complementary pieces for a polished look.";
        }

        String whyItWorks = generateWhyItWorks(product, vibe);

        List<RecommendationItemDTO> suggestions;

        try {
            suggestions = generateSmartSuggestions(product, vibe, preferences);
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
        scanResult.setMatchScore(calculateMainMatchScore(product, vibe, preferences));
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
            filteredSuggestions = generateAlternativeSuggestions(
                    product,
                    vibe,
                    fullOutfit,
                    0,
                    preferences
            );

            if (filteredSuggestions.isEmpty()) {
                filteredSuggestions = removeItemsAlreadyInFullOutfit(suggestions, fullOutfit);
            }
        } catch (RuntimeException e) {
            filteredSuggestions = suggestions;
        }

        scanResult.setSuggestions(filteredSuggestions);

        return scanResult;
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
        scanResult.setMatchScore(calculateMainMatchScore(scannedProduct, vibe, null));
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
        return findAvailableProductsForCategory(scannedProduct, normalizedCategory, null);
    }

    private List<Product> findAvailableProductsForCategory(
            Product scannedProduct,
            String normalizedCategory,
            CustomerPreferenceRequest preferences
    ) {
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
                .filter(product -> matchesCustomerPreferences(product, preferences))
                .collect(Collectors.toList());
    }

    private List<Product> findAvailableProductsForTargetCategories(
            Product scannedProduct,
            Set<String> targetCategories
    ) {
        return findAvailableProductsForTargetCategories(scannedProduct, targetCategories, null);
    }

    private List<Product> findAvailableProductsForTargetCategories(
            Product scannedProduct,
            Set<String> targetCategories,
            CustomerPreferenceRequest preferences
    ) {
        Map<String, Product> deduped = new LinkedHashMap<>();

        for (String category : targetCategories) {
            List<Product> products = findAvailableProductsForCategory(scannedProduct, category, preferences);

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
        return generateSmartSuggestions(scannedProduct, vibe, null);
    }

    private List<RecommendationItemDTO> generateSmartSuggestions(
            Product scannedProduct,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        return generateSmartSuggestionProducts(scannedProduct, vibe, preferences).stream()
                .map(candidate -> toRecommendationDto(scannedProduct, candidate, vibe, preferences))
                .collect(Collectors.toList());
    }

    private List<RecommendationItemDTO> generateSmartSuggestionsForVariation(
            Product scannedProduct,
            String vibe,
            int variation
    ) {
        return generateSmartSuggestionsForVariation(scannedProduct, vibe, variation, null);
    }

    private List<RecommendationItemDTO> generateSmartSuggestionsForVariation(
            Product scannedProduct,
            String vibe,
            int variation,
            CustomerPreferenceRequest preferences
    ) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);

        Map<String, List<Product>> groupedByCategory = findAvailableProductsForTargetCategories(scannedProduct, targetCategories, preferences).stream()
                .filter(p -> !safe(p.getRfid()).equalsIgnoreCase(safe(scannedProduct.getRfid())))
                .filter(p -> targetCategories.contains(normalizeCategory(p.getCategory())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe, preferences))
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
                .map(candidate -> toRecommendationDto(scannedProduct, candidate, vibe, preferences))
                .collect(Collectors.toList());
    }

    private List<Product> generateSmartSuggestionProducts(Product scannedProduct, String vibe) {
        return generateSmartSuggestionProducts(scannedProduct, vibe, null);
    }

    private List<Product> generateSmartSuggestionProducts(
            Product scannedProduct,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);

        return findAvailableProductsForTargetCategories(scannedProduct, targetCategories, preferences).stream()
                .filter(p -> !safe(p.getRfid()).equalsIgnoreCase(safe(scannedProduct.getRfid())))
                .filter(p -> targetCategories.contains(normalizeCategory(p.getCategory())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe, preferences))
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
        return generateAlternativeSuggestions(scannedProduct, vibe, fullOutfit, variationOffset, null);
    }

    private List<RecommendationItemDTO> generateAlternativeSuggestions(
            Product scannedProduct,
            String vibe,
            FullOutfitDTO fullOutfit,
            int variationOffset,
            CustomerPreferenceRequest preferences
    ) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);
        Set<String> excludedRfids = collectUsedRfids(scannedProduct, fullOutfit);

        Map<String, List<Product>> groupedByCategory = findAvailableProductsForTargetCategories(scannedProduct, targetCategories, preferences).stream()
                .filter(p -> !excludedRfids.contains(safe(p.getRfid())))
                .filter(p -> targetCategories.contains(normalizeCategory(p.getCategory())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe, preferences))
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
            alternatives.add(toRecommendationDto(scannedProduct, chosen, vibe, preferences));
        }

        return removeItemsAlreadyInFullOutfit(alternatives, fullOutfit);
    }

    private List<RecommendationItemDTO> generateSmartSwapSuggestions(
            Product scannedProduct,
            String vibe,
            FullOutfitDTO fullOutfit,
            String prioritizedCategory
    ) {
        return generateSmartSwapSuggestions(scannedProduct, vibe, fullOutfit, prioritizedCategory, null);
    }

    private List<RecommendationItemDTO> generateSmartSwapSuggestions(
            Product scannedProduct,
            String vibe,
            FullOutfitDTO fullOutfit,
            String prioritizedCategory,
            CustomerPreferenceRequest preferences
    ) {
        Set<String> targetCategories = getTargetCategories(scannedProduct.getCategory(), vibe);
        Set<String> excludedRfids = collectUsedRfids(scannedProduct, fullOutfit);

        if (prioritizedCategory == null || !targetCategories.contains(prioritizedCategory)) {
            return List.of();
        }

        return findAvailableProductsForCategory(scannedProduct, prioritizedCategory, preferences).stream()
                .filter(p -> !excludedRfids.contains(safe(p.getRfid())))
                .filter(p -> normalizeCategory(p.getCategory()).equals(prioritizedCategory))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSwapSuggestion(scannedProduct, p, vibe, prioritizedCategory, preferences))
                        .reversed()
                        .thenComparing(p -> safe(p.getRetailerName()))
                        .thenComparing(p -> safe(p.getStoreName()))
                        .thenComparing(p -> safe(p.getItemName())))
                .limit(3)
                .map(candidate -> toRecommendationDto(scannedProduct, candidate, vibe, preferences))
                .collect(Collectors.toList());
    }

    private int scoreSwapSuggestion(
            Product scannedProduct,
            Product candidate,
            String vibe,
            String prioritizedCategory
    ) {
        return scoreSwapSuggestion(scannedProduct, candidate, vibe, prioritizedCategory, null);
    }

    private int scoreSwapSuggestion(
            Product scannedProduct,
            Product candidate,
            String vibe,
            String prioritizedCategory,
            CustomerPreferenceRequest preferences
    ) {
        int score = scoreSuggestion(scannedProduct, candidate, vibe, preferences);
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

        score += calculatePreferenceScoreBoost(candidate, preferences);

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
        return findBestCandidateForCategory(scannedProduct, vibe, targetCategory, excludedRfids, null);
    }

    private Product findBestCandidateForCategory(
            Product scannedProduct,
            String vibe,
            String targetCategory,
            Set<String> excludedRfids,
            CustomerPreferenceRequest preferences
    ) {
        return findAvailableProductsForCategory(scannedProduct, targetCategory, preferences).stream()
                .filter(p -> !safe(p.getRfid()).equalsIgnoreCase(safe(scannedProduct.getRfid())))
                .filter(p -> normalizeCategory(p.getCategory()).equals(targetCategory))
                .filter(p -> !excludedRfids.contains(safe(p.getRfid())))
                .sorted(Comparator
                        .comparingInt((Product p) -> scoreSuggestion(scannedProduct, p, vibe, preferences))
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
        return findProductIfValidForContext(rfid, expectedCategory, scannedRfid, scannedProduct, null);
    }

    private Product findProductIfValidForContext(
            String rfid,
            String expectedCategory,
            String scannedRfid,
            Product scannedProduct,
            CustomerPreferenceRequest preferences
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

        if (!matchesCustomerPreferences(product, preferences)) {
            return null;
        }

        return product;
    }

    private RecommendationItemDTO toRecommendationDto(Product scannedProduct, Product candidate, String vibe) {
        return toRecommendationDto(scannedProduct, candidate, vibe, null);
    }

    private RecommendationItemDTO toRecommendationDto(
            Product scannedProduct,
            Product candidate,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        int styleMatch = calculateStyleMatch(scannedProduct, candidate, vibe);
        int colorMatch = calculateColorMatch(scannedProduct, candidate, preferences);
        int occasionMatch = calculateOccasionMatch(candidate, vibe);

        int preferenceMatch = calculatePreferenceMatchScore(candidate, preferences);
        int budgetMatch = calculateBudgetMatchScore(candidate, preferences);
        int sizeMatch = calculateSizeMatchScore(candidate, preferences);
        int fitMatch = calculateFitMatchScore(candidate, preferences);
        int materialMatch = calculateMaterialMatchScore(candidate, preferences);

        int overallMatch = clampScore(Math.round(
                (styleMatch * 0.30f)
                        + (colorMatch * 0.20f)
                        + (occasionMatch * 0.20f)
                        + (preferenceMatch * 0.30f)
        ));

        String reason = generateRecommendationReason(scannedProduct, candidate, vibe, preferences);
        String preferenceNote = generatePreferenceMatchNote(candidate, preferences);

        RecommendationItemDTO dto = new RecommendationItemDTO();

        dto.setRfid(safe(candidate.getRfid()));
        dto.setName(safe(candidate.getItemName()));
        dto.setBrand(safeBrand(candidate));
        dto.setCategory(safe(candidate.getCategory()));
        dto.setColor(safeColor(candidate));
        dto.setRetailer(safe(candidate.getRetailerName()));
        dto.setRetailerKey(safe(candidate.getRetailerKey()));
        dto.setStoreCode(safe(candidate.getStoreCode()));
        dto.setStoreName(safe(candidate.getStoreName()));

        dto.setPrice(safePrice(candidate.getPrice()));

        dto.setMatchScore(overallMatch);
        dto.setStyleMatch(styleMatch);
        dto.setColorMatch(colorMatch);
        dto.setOccasionMatch(occasionMatch);

        dto.setPreferenceMatch(preferenceMatch);
        dto.setBudgetMatch(budgetMatch);
        dto.setSizeMatch(sizeMatch);
        dto.setFitMatch(fitMatch);
        dto.setMaterialMatch(materialMatch);

        dto.setReason(reason);
        dto.setPreferenceNote(preferenceNote);
        dto.setImageUrl(safeImage(candidate.getImageUrl()));

        dto.setSize(safe(candidate.getSize()));
        dto.setFit(safe(candidate.getFit()));
        dto.setMaterial(safe(candidate.getMaterial()));
        dto.setGender(safe(candidate.getGender()));
        dto.setSeason(safe(candidate.getSeason()));
        dto.setOccasion(safe(candidate.getOccasion()));
        dto.setStyleTags(safe(candidate.getStyleTags()));
        dto.setPattern(safe(candidate.getPattern()));

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
        return calculateColorMatch(scannedProduct, candidate, null);
    }

    private int calculateColorMatch(
            Product scannedProduct,
            Product candidate,
            CustomerPreferenceRequest preferences
    ) {
        String scannedColor = safeLower(safeColor(scannedProduct));
        String candidateColor = safeLower(safeColor(candidate));

        int score;

        if (scannedColor.isBlank() || candidateColor.isBlank()) {
            score = 82;
        } else if (scannedColor.equals(candidateColor)) {
            score = 96;
        } else if (isNeutral(scannedColor) || isNeutral(candidateColor)) {
            score = 92;
        } else {
            score = 86;
        }

        if (preferences != null) {
            String favoriteColors = safeLower(preferences.getFavoriteColors());
            String avoidedColors = safeLower(preferences.getAvoidedColors());

            if (!favoriteColors.isBlank() && containsPreferenceToken(favoriteColors, candidateColor)) {
                score += 8;
            }

            if (!avoidedColors.isBlank() && containsPreferenceToken(avoidedColors, candidateColor)) {
                score -= 18;
            }
        }

        return clampScore(score);
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
        return generateRecommendationReason(scannedProduct, candidate, vibe, null);
    }

    private String generateRecommendationReason(
            Product scannedProduct,
            Product candidate,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        String scannedCategory = normalizeCategory(scannedProduct.getCategory());
        String candidateCategory = normalizeCategory(candidate.getCategory());
        String scannedColor = safeLower(safeColor(scannedProduct));
        String candidateColor = safeLower(safeColor(candidate));
        String vibeLower = safeLower(vibe);

        if (preferences != null) {
            String favoriteColors = safeLower(preferences.getFavoriteColors());
            String fitPreference = safeLower(preferences.getFitPreference());
            String sizeTop = safeLower(preferences.getSizeTop());
            String sizeBottom = safeLower(preferences.getSizeBottom());
            String shoeSize = safeLower(preferences.getShoeSize());

            if (!favoriteColors.isBlank() && containsPreferenceToken(favoriteColors, candidateColor)) {
                return "This piece matches the customer's preferred color direction while still supporting the full outfit.";
            }

            if (!fitPreference.isBlank() && safeLower(candidate.getFit()).contains(fitPreference)) {
                return "This piece supports the customer's preferred fit and keeps the outfit aligned with their profile.";
            }

            String candidateSize = safeLower(candidate.getSize());

            if ("tops".equals(candidateCategory) && !sizeTop.isBlank() && candidateSize.contains(sizeTop)) {
                return "This top aligns with the customer's preferred top size and supports the outfit.";

            }

            if ("bottoms".equals(candidateCategory) && !sizeBottom.isBlank() && candidateSize.contains(sizeBottom)) {
                return "This bottom aligns with the customer's preferred bottom size and supports the outfit.";

            }

            if ("shoes".equals(candidateCategory) && !shoeSize.isBlank() && candidateSize.contains(shoeSize)) {
                return "These shoes align with the customer's preferred shoe size and complete the outfit.";

            }

        }

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
        return calculateMainMatchScore(product, vibe, null);
    }

    private int calculateMainMatchScore(
            Product product,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
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

        score += calculatePreferenceScoreBoost(product, preferences);

        return clampScore(score);
    }

    private int scoreSuggestion(Product scannedProduct, Product candidate, String vibe) {
        return scoreSuggestion(scannedProduct, candidate, vibe, null);
    }

    private int scoreSuggestion(
            Product scannedProduct,
            Product candidate,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
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

        score += calculatePreferenceScoreBoost(candidate, preferences);

        return score;
    }

    private int calculatePreferenceScoreBoost(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasAnyPreference()) {
            return 0;
        }

        int boost = 0;

        boost += scoreToBoost(calculateBudgetMatchScore(product, preferences), 12);
        boost += scoreToBoost(calculateColorPreferenceMatchScore(product, preferences), 14);
        boost += scoreToBoost(calculateSizeMatchScore(product, preferences), 10);
        boost += scoreToBoost(calculateFitMatchScore(product, preferences), 10);
        boost += scoreToBoost(calculateGenderMatchScore(product, preferences), 6);
        boost += scoreToBoost(calculateMaterialMatchScore(product, preferences), 8);
        boost += scoreToBoost(calculateStyleKeywordMatchScore(product, preferences), 10);

        if (hasDislikedStyleMatch(product, preferences)) {
            boost -= 24;
        }

        if (hasDislikedMaterialMatch(product, preferences)) {
            boost -= 18;
        }

        return boost;
    }

    private int calculatePreferenceMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasAnyPreference()) {
            return 82;
        }

        List<Integer> scores = new ArrayList<>();

        if (preferences.hasBudgetPreference()) {
            scores.add(calculateBudgetMatchScore(product, preferences));
        }

        if (preferences.hasColorPreference()) {
            scores.add(calculateColorPreferenceMatchScore(product, preferences));
        }

        if (preferences.hasSizePreference()) {
            scores.add(calculateSizeMatchScore(product, preferences));
        }

        if (preferences.hasFitPreference()) {
            scores.add(calculateFitMatchScore(product, preferences));
        }

        if (!safe(preferences.getGenderStyle()).isBlank()) {
            scores.add(calculateGenderMatchScore(product, preferences));
        }

        if (preferences.hasMaterialPreference()) {
            scores.add(calculateMaterialMatchScore(product, preferences));
        }

        if (preferences.hasStyleKeywordPreference()) {
            scores.add(calculateStyleKeywordMatchScore(product, preferences));
        }

        if (scores.isEmpty()) {
            return 82;
        }

        int average = Math.round(
                (float) scores.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(82)
        );

        if (hasDislikedStyleMatch(product, preferences)) {
            average -= 18;
        }

        if (hasDislikedMaterialMatch(product, preferences)) {
            average -= 14;
        }

        return clampScore(average);
    }

    private int calculateBudgetMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasBudgetPreference()) {
            return 82;
        }

        double price = safePrice(product.getPrice());
        Double minBudget = toNullableDouble(preferences.getBudgetMin());
        Double maxBudget = toNullableDouble(preferences.getBudgetMax());

        if (price <= 0) {
            return 78;
        }

        boolean belowBudget = minBudget != null && price < minBudget;
        boolean aboveBudget = maxBudget != null && price > maxBudget;

        if (!belowBudget && !aboveBudget) {
            return 96;
        }

        if (belowBudget) {
            return 84;
        }

        double overBy = price - maxBudget;

        if (overBy <= 25) {
            return 86;
        }

        if (overBy <= 75) {
            return 78;
        }

        return 70;
    }

    private int calculateColorPreferenceMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasColorPreference()) {
            return 82;
        }

        String productColor = safeLower(safeColor(product));
        String favoriteColors = safeLower(preferences.getFavoriteColors());
        String avoidedColors = safeLower(preferences.getAvoidedColors());

        if (!avoidedColors.isBlank() && containsPreferenceToken(avoidedColors, productColor)) {
            return 70;
        }

        if (!favoriteColors.isBlank() && containsPreferenceToken(favoriteColors, productColor)) {
            return 96;
        }

        if (isNeutral(productColor)) {
            return 88;
        }

        return 82;
    }

    private int calculateSizeMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasSizePreference()) {
            return 82;
        }

        String productCategory = normalizeCategory(product.getCategory());
        String productSize = safeLower(product.getSize());

        if (productSize.isBlank()) {
            return 78;
        }

        if ("tops".equals(productCategory)) {
            String sizeTop = safeLower(preferences.getSizeTop());
            return !sizeTop.isBlank() && sizeMatches(productSize, sizeTop) ? 96 : 76;
        }

        if ("bottoms".equals(productCategory)) {
            String sizeBottom = safeLower(preferences.getSizeBottom());
            return !sizeBottom.isBlank() && sizeMatches(productSize, sizeBottom) ? 96 : 76;
        }

        if ("shoes".equals(productCategory)) {
            String shoeSize = safeLower(preferences.getShoeSize());
            return !shoeSize.isBlank() && sizeMatches(productSize, shoeSize) ? 96 : 76;
        }

        return 82;
    }

    private int calculateFitMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasFitPreference()) {
            return 82;
        }

        String productFit = safeLower(product.getFit());
        String fitPreference = safeLower(preferences.getFitPreference());

        if (productFit.isBlank()) {
            return 78;
        }

        if (productFit.contains(fitPreference) || fitPreference.contains(productFit)) {
            return 96;
        }

        if (areCompatibleFits(productFit, fitPreference)) {
            return 88;
        }

        return 76;
    }

    private int calculateGenderMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || safe(preferences.getGenderStyle()).isBlank()) {
            return 82;
        }

        String productGender = safeLower(product.getGender());
        String genderStyle = safeLower(preferences.getGenderStyle());

        if (genderStyle.isBlank() || "any".equals(genderStyle) || "all".equals(genderStyle)) {
            return 90;
        }

        if (productGender.isBlank()) {
            return 80;
        }

        if ("unisex".equals(productGender)
                || productGender.contains(genderStyle)
                || genderStyle.contains(productGender)) {
            return 96;
        }

        return 76;
    }

    private int calculateMaterialMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasMaterialPreference()) {
            return 82;
        }

        String productMaterial = safeLower(product.getMaterial());
        String preferredMaterials = safeLower(preferences.getPreferredMaterials());
        String dislikedMaterials = safeLower(preferences.getDislikedMaterials());

        if (productMaterial.isBlank()) {
            return 78;
        }

        if (!dislikedMaterials.isBlank() && containsPreferenceToken(dislikedMaterials, productMaterial)) {
            return 70;
        }

        if (!preferredMaterials.isBlank() && containsPreferenceToken(preferredMaterials, productMaterial)) {
            return 96;
        }

        return 82;
    }

    private int calculateStyleKeywordMatchScore(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasStyleKeywordPreference()) {
            return 82;
        }

        String styleKeywords = safeLower(preferences.getStyleKeywords());
        String dislikedStyles = safeLower(preferences.getDislikedStyles());

        String searchableProductText = buildSearchableProductText(product);

        if (!dislikedStyles.isBlank() && containsAnyPreferenceToken(dislikedStyles, searchableProductText)) {
            return 70;
        }

        if (!styleKeywords.isBlank() && containsAnyPreferenceToken(styleKeywords, searchableProductText)) {
            return 96;
        }

        return 82;
    }

    private int scoreToBoost(int score, int maxBoost) {
        if (score >= 94) {
            return maxBoost;
        }

        if (score >= 88) {
            return Math.round(maxBoost * 0.65f);
        }

        if (score >= 82) {
            return Math.round(maxBoost * 0.25f);
        }

        if (score >= 76) {
            return 0;
        }

        return -Math.round(maxBoost * 0.75f);
    }

    private String generatePreferenceMatchNote(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null || !preferences.hasAnyPreference()) {
            return "";
        }

        List<String> matched = new ArrayList<>();
        List<String> cautions = new ArrayList<>();

        String productColor = safeLower(safeColor(product));
        String productCategory = normalizeCategory(product.getCategory());
        String productSize = safeLower(product.getSize());
        String productFit = safeLower(product.getFit());
        String productMaterial = safeLower(product.getMaterial());
        String productGender = safeLower(product.getGender());

        if (!safe(preferences.getFavoriteColors()).isBlank()
                && containsPreferenceToken(preferences.getFavoriteColors(), productColor)) {
            matched.add("favorite color");
        }

        if (!safe(preferences.getAvoidedColors()).isBlank()
                && containsPreferenceToken(preferences.getAvoidedColors(), productColor)) {
            cautions.add("avoided color");
        }

        if ("tops".equals(productCategory)
                && !safe(preferences.getSizeTop()).isBlank()
                && sizeMatches(productSize, preferences.getSizeTop())) {
            matched.add("top size");
        }

        if ("bottoms".equals(productCategory)
                && !safe(preferences.getSizeBottom()).isBlank()
                && sizeMatches(productSize, preferences.getSizeBottom())) {
            matched.add("bottom size");
        }

        if ("shoes".equals(productCategory)
                && !safe(preferences.getShoeSize()).isBlank()
                && sizeMatches(productSize, preferences.getShoeSize())) {
            matched.add("shoe size");
        }

        if (!safe(preferences.getFitPreference()).isBlank()
                && !productFit.isBlank()
                && productFit.contains(safeLower(preferences.getFitPreference()))) {
            matched.add("fit preference");
        }

        if (!safe(preferences.getGenderStyle()).isBlank()
                && !productGender.isBlank()
                && ("any".equalsIgnoreCase(preferences.getGenderStyle())
                || "all".equalsIgnoreCase(preferences.getGenderStyle())
                || "unisex".equals(productGender)
                || productGender.contains(safeLower(preferences.getGenderStyle()))
                || safeLower(preferences.getGenderStyle()).contains(productGender))) {
            matched.add("gender/style preference");
        }

        if (!safe(preferences.getPreferredMaterials()).isBlank()
                && !productMaterial.isBlank()
                && containsPreferenceToken(preferences.getPreferredMaterials(), productMaterial)) {
            matched.add("preferred material");
        }

        if (!safe(preferences.getDislikedMaterials()).isBlank()
                && !productMaterial.isBlank()
                && containsPreferenceToken(preferences.getDislikedMaterials(), productMaterial)) {
            cautions.add("disliked material");
        }

        if (preferences.hasBudgetPreference()) {
            int budgetScore = calculateBudgetMatchScore(product, preferences);

            if (budgetScore >= 90) {
                matched.add("budget");
            } else if (budgetScore <= 78) {
                cautions.add("budget range");
            }
        }

        if (!safe(preferences.getStyleKeywords()).isBlank()
                && containsAnyPreferenceToken(preferences.getStyleKeywords(), buildSearchableProductText(product))) {
            matched.add("style keywords");
        }

        if (!safe(preferences.getDislikedStyles()).isBlank()
                && containsAnyPreferenceToken(preferences.getDislikedStyles(), buildSearchableProductText(product))) {
            cautions.add("disliked style");
        }

        if (!matched.isEmpty() && cautions.isEmpty()) {
            return "Matched your preferences because it aligns with "
                    + joinHumanReadable(matched)
                    + ".";
        }

        if (!matched.isEmpty()) {
            return "Matched your preferences for "
                    + joinHumanReadable(matched)
                    + ", but watch the "
                    + joinHumanReadable(cautions)
                    + ".";
        }

        if (!cautions.isEmpty()) {
            return "This is less aligned with your preferences because of "
                    + joinHumanReadable(cautions)
                    + ".";
        }

        return "This recommendation was balanced against your saved preferences.";
    }

    private boolean hasDislikedStyleMatch(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null) {
            return false;
        }

        String dislikedStyles = safeLower(preferences.getDislikedStyles());

        if (dislikedStyles.isBlank()) {
            return false;
        }

        return containsAnyPreferenceToken(dislikedStyles, buildSearchableProductText(product));
    }

    private boolean hasDislikedMaterialMatch(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null) {
            return false;
        }

        String dislikedMaterials = safeLower(preferences.getDislikedMaterials());
        String productMaterial = safeLower(product.getMaterial());

        if (dislikedMaterials.isBlank() || productMaterial.isBlank()) {
            return false;
        }

        return containsPreferenceToken(dislikedMaterials, productMaterial);
    }

    private String buildSearchableProductText(Product product) {
        if (product == null) {
            return "";
        }

        return String.join(" ",
                safeLower(product.getItemName()),
                safeLower(product.getBrand()),
                safeLower(product.getCategory()),
                safeLower(product.getColor()),
                safeLower(product.getFit()),
                safeLower(product.getMaterial()),
                safeLower(product.getGender()),
                safeLower(product.getSeason()),
                safeLower(product.getOccasion()),
                safeLower(product.getStyleTags()),
                safeLower(product.getPattern())
        );
    }

    private boolean areCompatibleFits(String productFit, String fitPreference) {
        String product = safeLower(productFit);
        String preferred = safeLower(fitPreference);

        if (product.isBlank() || preferred.isBlank()) {
            return false;
        }

        if ("regular".equals(preferred) && containsAny(product, "classic", "standard", "straight")) {
            return true;
        }

        if ("relaxed".equals(preferred) && containsAny(product, "loose", "oversized", "easy")) {
            return true;
        }

        if ("slim".equals(preferred) && containsAny(product, "tailored", "fitted", "skinny")) {
            return true;
        }

        if ("oversized".equals(preferred) && containsAny(product, "relaxed", "loose", "boxy")) {
            return true;
        }

        return false;
    }

    private String joinHumanReadable(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        if (values.size() == 2) {
            return values.get(0) + " and " + values.get(1);
        }

        return String.join(", ", values.subList(0, values.size() - 1))
                + ", and "
                + values.get(values.size() - 1);
    }

    private boolean containsPreferenceToken(String preferencesText, String value) {
        String safePreferencesText = safeLower(preferencesText);
        String safeValue = safeLower(value);

        if (safePreferencesText.isBlank() || safeValue.isBlank()) {
            return false;
        }

        String[] tokens = safePreferencesText.split("[,;/|]+");

        for (String token : tokens) {
            String cleanedToken = safeLower(token);

            if (cleanedToken.isBlank()) {
                continue;
            }

            if (safeValue.contains(cleanedToken) || cleanedToken.contains(safeValue)) {
                return true;
            }
        }

        return safePreferencesText.contains(safeValue);
    }

    private boolean sizeMatches(String productSize, String preferredSize) {
        String safeProductSize = safeLower(productSize);
        String safePreferredSize = safeLower(preferredSize);

        if (safeProductSize.isBlank() || safePreferredSize.isBlank()) {
            return false;
        }

        if (safeProductSize.equals(safePreferredSize)) {
            return true;
        }

        if (safeProductSize.contains(safePreferredSize)) {
            return true;
        }

        return containsPreferenceToken(safePreferredSize, safeProductSize);
    }

    private boolean containsAnyPreferenceToken(String preferencesText, String searchableText) {
        String safePreferencesText = safeLower(preferencesText);
        String safeSearchableText = safeLower(searchableText);

        if (safePreferencesText.isBlank() || safeSearchableText.isBlank()) {
            return false;
        }

        String[] tokens = safePreferencesText.split("[,;/|]+");

        for (String token : tokens) {
            String cleanedToken = safeLower(token);

            if (!cleanedToken.isBlank() && safeSearchableText.contains(cleanedToken)) {
                return true;
            }
        }

        return false;
    }

    private Double toNullableDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            String text = String.valueOf(value).trim();

            if (text.isBlank()) {
                return null;
            }

            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
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

    private void enrichLookResponseWithStylingNotes(
            LookResponseDTO response,
            Product scannedProduct,
            String vibe,
            FullOutfitDTO fullOutfit
    ) {
        enrichLookResponseWithStylingNotes(response, scannedProduct, vibe, fullOutfit, null);
    }

    private void enrichLookResponseWithStylingNotes(
            LookResponseDTO response,
            Product scannedProduct,
            String vibe,
            FullOutfitDTO fullOutfit,
            CustomerPreferenceRequest preferences
    ) {
        if (response == null || scannedProduct == null) {
            return;
        }

        response.setStylingNote(generateStylingNote(scannedProduct, vibe, fullOutfit));
        response.setOccasionNote(generateOccasionNote(scannedProduct, vibe));
        response.setSeasonNote(generateSeasonNote(scannedProduct, vibe));
        response.setColorNote(generateColorPairingNote(scannedProduct, fullOutfit));
        response.setFitNote(generateFitNote(scannedProduct));
        response.setMaterialNote(generateMaterialNote(scannedProduct));
        response.setPreferenceNote(generatePreferenceReadyNote(scannedProduct, vibe, preferences));
    }

    private String generateStylingNote(
            Product product,
            String vibe,
            FullOutfitDTO fullOutfit
    ) {
        String itemName = safe(product.getItemName());
        String category = normalizeCategory(product.getCategory());
        String vibeName = safe(vibe).isBlank() ? "casual" : safe(vibe).toLowerCase();

        String itemCopy = itemName.isBlank() ? "This piece" : itemName;

        String outfitDepth = fullOutfit == null
                ? "It can work as a flexible styling anchor."
                : "The generated look balances the anchor item with complementary categories for a complete outfit.";

        return itemCopy + " is styled for a " + vibeName + " direction. "
                + getCategoryStylingSentence(category)
                + " "
                + outfitDepth;
    }

    private String getCategoryStylingSentence(String normalizedCategory) {
        return switch (safeLower(normalizedCategory)) {
            case "tops" -> "As a top, it sets the visual tone near the face and works best when balanced with structured bottoms or clean footwear.";
            case "bottoms" -> "As a bottom, it grounds the silhouette and works best with a proportional top and shoes that match the outfit energy.";
            case "shoes" -> "As footwear, it finishes the outfit and should echo either the color, texture, or formality of the other pieces.";
            case "outerwear" -> "As outerwear, it frames the whole look and should support the outfit without overpowering the anchor item.";
            default -> "It works as a flexible anchor that can be styled across multiple outfit directions.";
        };
    }

    private String generateOccasionNote(Product product, String vibe) {
        String productOccasion = safe(product.getOccasion());
        String vibeLower = safeLower(vibe);

        if (!productOccasion.isBlank()) {
            return "Tagged for " + productOccasion
                    + ", which supports the selected "
                    + (vibeLower.isBlank() ? "styling" : vibeLower)
                    + " direction.";
        }

        return switch (vibeLower) {
            case "formal" -> "Best suited for polished settings, workwear styling, dinners, or elevated events.";
            case "date night" -> "Works well for evening plans where the outfit should feel confident but not overdone.";
            case "streetwear" -> "Best for casual social settings, weekend wear, and expressive everyday looks.";
            case "luxury" -> "Works for elevated retail styling, premium casualwear, or refined smart-casual occasions.";
            default -> "Strong for everyday wear, casual outings, and flexible store styling.";
        };
    }

    private String generateSeasonNote(Product product, String vibe) {
        String season = safe(product.getSeason());
        String material = safe(product.getMaterial());
        String category = normalizeCategory(product.getCategory());

        if (!season.isBlank() && !material.isBlank()) {
            return "Tagged for " + season + ", with " + material.toLowerCase()
                    + " giving the piece a clear seasonal texture.";
        }

        if (!season.isBlank()) {
            return "Tagged for " + season + ", making it easier to place in the right weather or seasonal story.";
        }

        if (!material.isBlank()) {
            return "The " + material.toLowerCase()
                    + " material helps determine whether this should be styled as a light, midweight, or cold-weather piece.";
        }

        return switch (category) {
            case "outerwear" -> "Outerwear naturally works well for transitional or cooler-weather styling.";
            case "tops" -> "This top can be layered or worn alone depending on weather and fabric weight.";
            case "bottoms" -> "These bottoms can move across seasons when paired with the right top and footwear.";
            case "shoes" -> "Footwear seasonality depends on texture, coverage, and color weight.";
            default -> "This piece can be adapted across seasons with the right layers.";
        };
    }

    private String generateColorPairingNote(Product product, FullOutfitDTO fullOutfit) {
        String color = safeColor(product);
        String colorLower = safeLower(color);

        if (isNeutral(colorLower)) {
            return color + " is a versatile neutral, so it pairs easily with black, white, denim, camel, navy, grey, and other grounded tones.";
        }

        if (containsAny(colorLower, "blue", "navy")) {
            return "Blue tones pair well with white, grey, camel, black, denim, and clean neutral footwear.";
        }

        if (containsAny(colorLower, "black")) {
            return "Black creates structure and works well with white, grey, denim, camel, metallic accents, or tonal black layers.";
        }

        if (containsAny(colorLower, "white", "cream")) {
            return "Light tones keep the outfit clean and pair well with navy, black, camel, denim, grey, or soft neutrals.";
        }

        if (containsAny(colorLower, "camel", "brown", "tan", "khaki")) {
            return "Warm earth tones pair well with white, navy, denim, black, olive, cream, and textured neutrals.";
        }

        return "Use one neutral anchor and one supporting tone to keep the outfit balanced around the item’s color.";
    }

    private String generateFitNote(Product product) {
        String fit = safe(product.getFit());
        String size = safe(product.getSize());
        String category = normalizeCategory(product.getCategory());

        if (!fit.isBlank() && !size.isBlank()) {
            return "Tagged as " + fit + " in size " + size
                    + ", so styling should preserve the intended silhouette.";
        }

        if (!fit.isBlank()) {
            return "The " + fit
                    + " fit should be balanced with pieces that do not fight the silhouette.";
        }

        if (!size.isBlank()) {
            return "Size " + size
                    + " is available, so the final outfit should keep proportion and comfort in mind.";
        }

        return switch (category) {
            case "tops" -> "Balance the top with bottoms that keep the outfit proportion clean.";
            case "bottoms" -> "Pair the bottom with a top that complements the rise, leg shape, and overall silhouette.";
            case "shoes" -> "Shoes should support comfort, proportion, and the outfit’s level of polish.";
            case "outerwear" -> "Outerwear should leave enough room for layering without making the look feel bulky.";
            default -> "Keep the silhouette balanced and comfortable across the full look.";
        };
    }

    private String generateMaterialNote(Product product) {
        String material = safe(product.getMaterial());
        String pattern = safe(product.getPattern());
        String tags = safe(product.getStyleTags());

        List<String> parts = new ArrayList<>();

        if (!material.isBlank()) {
            parts.add("material: " + material);
        }

        if (!pattern.isBlank()) {
            parts.add("pattern: " + pattern);
        }

        if (!tags.isBlank()) {
            parts.add("style tags: " + tags);
        }

        if (!parts.isEmpty()) {
            return String.join(", ", parts)
                    + ". Use these details to decide whether the outfit should feel clean, textured, polished, or expressive.";
        }

        return "Mix at least one smooth piece with one structured or textured piece to make the outfit feel intentional.";
    }

    private String generatePreferenceReadyNote(Product product, String vibe) {
        return generatePreferenceReadyNote(product, vibe, null);
    }

    private String generatePreferenceReadyNote(
            Product product,
            String vibe,
            CustomerPreferenceRequest preferences
    ) {
        String gender = safe(product.getGender());
        String vibeName = safe(vibe).isBlank() ? "selected" : safe(vibe).toLowerCase();

        if (preferences != null) {
            List<String> matchedPreferences = new ArrayList<>();

            if (!safe(preferences.getSizeTop()).isBlank()
                    || !safe(preferences.getSizeBottom()).isBlank()
                    || !safe(preferences.getShoeSize()).isBlank()) {
                matchedPreferences.add("size");
            }

            if (!safe(preferences.getFitPreference()).isBlank()) {
                matchedPreferences.add("fit");
            }

            if (!safe(preferences.getFavoriteColors()).isBlank()) {
                matchedPreferences.add("favorite colors");
            }

            if (!safe(preferences.getAvoidedColors()).isBlank()) {
                matchedPreferences.add("avoided colors");
            }

            if (preferences.getBudgetMin() != null || preferences.getBudgetMax() != null) {
                matchedPreferences.add("budget");
            }

            if (!safe(preferences.getStyleKeywords()).isBlank()) {
                matchedPreferences.add("style keywords");
            }

            if (!matchedPreferences.isEmpty()) {
                return "This " + vibeName + " recommendation has been filtered against customer preferences for "
                        + String.join(", ", matchedPreferences)
                        + ".";
            }
        }

        if (!gender.isBlank()) {
            return "Tagged for " + gender
                    + " styling and can later be matched against customer profile preferences like size, budget, color, and preferred fit.";
        }

        return "This " + vibeName
                + " recommendation is ready to connect with future customer preferences like size, budget, favorite colors, and preferred silhouettes.";
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
            String retailerKey,
            String storeCode
    ) {
        if (item == null) {
            return false;
        }

        String safeUserId = safe(userId);
        String safeTenantId = safe(tenantId);
        String safeRetailerKey = safe(retailerKey);
        String safeStoreCode = safe(storeCode);

        boolean userMatches = safeUserId.isBlank()
                || safe(item.getUserId()).equalsIgnoreCase(safeUserId);

        boolean tenantMatches = safeTenantId.isBlank()
                || safe(item.getTenantId()).equalsIgnoreCase(safeTenantId);

        boolean retailerMatches = safeRetailerKey.isBlank()
                || safe(item.getRetailerKey()).equalsIgnoreCase(safeRetailerKey);

        boolean storeMatches = safeStoreCode.isBlank()
                || safe(item.getStoreCode()).equalsIgnoreCase(safeStoreCode);

        return userMatches && tenantMatches && retailerMatches && storeMatches;
    }

    private List<TrendEvent> loadTrendEventsForStore(
            String retailerKey,
            String storeCode,
            String eventType
    ) {
        String safeRetailerKey = safe(retailerKey);
        String safeStoreCode = safe(storeCode);
        String safeEventType = safe(eventType);

        boolean hasStoreScope = !safeRetailerKey.isBlank()
                && !safeStoreCode.isBlank()
                && !"ALL".equalsIgnoreCase(safeRetailerKey)
                && !"ALL".equalsIgnoreCase(safeStoreCode);

        if (hasStoreScope && !"ALL".equalsIgnoreCase(safeEventType) && !safeEventType.isBlank()) {
            return trendEventRepository.findByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndEventTypeIgnoreCaseOrderByCreatedAtDesc(
                    safeRetailerKey.toUpperCase(),
                    safeStoreCode.toUpperCase(),
                    safeEventType
            );
        }

        if (hasStoreScope) {
            return trendEventRepository.findByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseOrderByCreatedAtDesc(
                    safeRetailerKey.toUpperCase(),
                    safeStoreCode.toUpperCase()
            );
        }

        return trendEventRepository.findAll().stream()
                .filter(event -> "ALL".equalsIgnoreCase(safeEventType)
                        || safeEventType.isBlank()
                        || safe(event.getEventType()).equalsIgnoreCase(safeEventType))
                .sorted(Comparator.comparing(
                        TrendEvent::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .collect(Collectors.toList());
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

        String safeText = safeLower(text);

        for (String keyword : keywords) {
            if (safeText.contains(safeLower(keyword))) {
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

    private boolean matchesCustomerPreferences(Product product, CustomerPreferenceRequest preferences) {
        if (product == null || preferences == null) {
            return true;
        }

        String productColor = safeLower(safeColor(product));
        String avoidedColors = safeLower(preferences.getAvoidedColors());

        if (!avoidedColors.isBlank() && containsPreferenceToken(avoidedColors, productColor)) {
            return false;
        }

        /*
         * Do not hard-block budget, size, fit, gender, material, or style.
         * Those should influence ranking and preferenceMatchScore instead.
         *
         * We only hard-block avoided colors because color avoidance is usually explicit.
         */
        return true;
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

    private void saveScanHistory(
            Product product,
            String vibe,
            String userId,
            String tenantId,
            String storeId,
            String userEmail,
            String retailerKey,
            String storeCode
    ) {
        if (product == null) {
            return;
        }

        try {
            ScanHistory scan = new ScanHistory();

            scan.setUserId(safe(userId));
            scan.setTenantId(safe(tenantId));
            scan.setStoreId(safe(storeId));
            scan.setUserEmail(safe(userEmail));

            String resolvedRetailerKey = safe(retailerKey).isBlank()
                    ? safe(product.getRetailerKey()).toUpperCase()
                    : safe(retailerKey).toUpperCase();

            String resolvedStoreCode = safe(storeCode).isBlank()
                    ? safe(product.getStoreCode()).toUpperCase()
                    : safe(storeCode).toUpperCase();

            scan.setRetailerKey(resolvedRetailerKey);
            scan.setRetailerName(safe(product.getRetailerName()));
            scan.setStoreCode(resolvedStoreCode);
            scan.setStoreName(safe(product.getStoreName()));

            scan.setRfid(safe(product.getRfid()));
            scan.setItemName(safe(product.getItemName()));
            scan.setBrand(safeBrand(product));
            scan.setCategory(safe(product.getCategory()));
            scan.setColor(safeColor(product));
            scan.setPrice(safePrice(product.getPrice()));
            scan.setImageUrl(safeImage(product.getImageUrl()));
            scan.setVibe(safe(vibe).isBlank() ? "Casual" : safe(vibe));

            scanHistoryRepository.save(scan);
        } catch (RuntimeException e) {
            /*
             * Scan history should never block the customer scan flow.
             * If analytics persistence fails, the scan result still returns.
             */
        }
    }

    private void saveTrendEvent(String eventType, Product product) {
        if (product == null) {
            return;
        }

        TrendEvent event = new TrendEvent();

        event.setEventType(safe(eventType).toUpperCase());
        event.setRetailerName(safe(product.getRetailerName()));
        event.setItemName(safe(product.getItemName()));
        event.setRetailerKey(safe(product.getRetailerKey()).toUpperCase());
        event.setStoreCode(safe(product.getStoreCode()).toUpperCase());
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
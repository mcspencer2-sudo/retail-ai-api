package com.retailai.controller;

import com.retailai.dto.ActivityDTO;
import com.retailai.dto.AnalyticsSummaryDTO;
import com.retailai.dto.BagQuantityRequest;
import com.retailai.dto.CustomerPreferenceRequest;
import com.retailai.dto.MerchantInventoryPageDTO;
import com.retailai.dto.LookResponseDTO;
import com.retailai.dto.RetailerStatsDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.dto.TrendDTO;
import com.retailai.model.BagSummaryResponse;
import com.retailai.service.AuthContextService;
import com.retailai.service.CustomerScanHistoryService;
import com.retailai.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/macy-stylist")
public class StylistController {

    private final InventoryService inventoryService;
    private final AuthContextService authContextService;
    private final CustomerScanHistoryService customerScanHistoryService;

    @Value("${retailai.demo-scan-mode:true}")
    private boolean demoScanMode;

    public StylistController(
            InventoryService inventoryService,
            AuthContextService authContextService,
            CustomerScanHistoryService customerScanHistoryService
    ) {
        this.inventoryService = inventoryService;
        this.authContextService = authContextService;
        this.customerScanHistoryService = customerScanHistoryService;
    }

    @GetMapping("/scan/{rfid}")
    public ScanResultDTO scanItem(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe,
            @RequestParam(required = false) String sizeTop,
            @RequestParam(required = false) String sizeBottom,
            @RequestParam(required = false) String shoeSize,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String favoriteColors,
            @RequestParam(required = false) String avoidedColors,
            @RequestParam(required = false) String fitPreference,
            @RequestParam(required = false) String genderStyle,
            @RequestParam(required = false) String preferredMaterials,
            @RequestParam(required = false) String dislikedMaterials,
            @RequestParam(required = false) String occasionPriority,
            @RequestParam(required = false) String styleKeywords,
            @RequestParam(required = false) String dislikedStyles,
            @RequestParam(required = false) String notes
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    retailerKey,
                    storeCode
            );

            CustomerPreferenceRequest preferences = buildCustomerPreferenceRequest(
                    sizeTop,
                    sizeBottom,
                    shoeSize,
                    budgetMin,
                    budgetMax,
                    favoriteColors,
                    avoidedColors,
                    fitPreference,
                    genderStyle,
                    preferredMaterials,
                    dislikedMaterials,
                    occasionPriority,
                    styleKeywords,
                    dislikedStyles,
                    notes
            );

            ScanResultDTO result = inventoryService.scanItem(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    normalizeOptional(auth.email()),
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe),
                    preferences
            );

            saveScanHistorySafely(
                    auth,
                    scanContext,
                    result,
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe)
            );

            return result;
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/scan/{requestedRetailerKey}/{rfid}")
    public ScanResultDTO scanItemLegacy(
            HttpServletRequest request,
            @PathVariable String requestedRetailerKey,
            @PathVariable String rfid,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe,
            @RequestParam(required = false) String sizeTop,
            @RequestParam(required = false) String sizeBottom,
            @RequestParam(required = false) String shoeSize,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String favoriteColors,
            @RequestParam(required = false) String avoidedColors,
            @RequestParam(required = false) String fitPreference,
            @RequestParam(required = false) String genderStyle,
            @RequestParam(required = false) String preferredMaterials,
            @RequestParam(required = false) String dislikedMaterials,
            @RequestParam(required = false) String occasionPriority,
            @RequestParam(required = false) String styleKeywords,
            @RequestParam(required = false) String dislikedStyles,
            @RequestParam(required = false) String notes
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    requestedRetailerKey,
                    storeCode
            );

            CustomerPreferenceRequest preferences = buildCustomerPreferenceRequest(
                    sizeTop,
                    sizeBottom,
                    shoeSize,
                    budgetMin,
                    budgetMax,
                    favoriteColors,
                    avoidedColors,
                    fitPreference,
                    genderStyle,
                    preferredMaterials,
                    dislikedMaterials,
                    occasionPriority,
                    styleKeywords,
                    dislikedStyles,
                    notes
            );

            ScanResultDTO result = inventoryService.scanItem(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    normalizeOptional(auth.email()),
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe),
                    preferences
            );

            saveScanHistorySafely(
                    auth,
                    scanContext,
                    result,
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe)
            );

            return result;
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @PostMapping("/save/{rfid}")
    public String saveToBag(
            HttpServletRequest request,
            @PathVariable String rfid
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            return inventoryService.saveToBag(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    normalizeOptional(auth.email()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase(),
                    normalizeRequired(rfid, "RFID is required.")
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/look/{rfid}")
    public LookResponseDTO createFullLook(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe,
            @RequestParam(required = false) String sizeTop,
            @RequestParam(required = false) String sizeBottom,
            @RequestParam(required = false) String shoeSize,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String favoriteColors,
            @RequestParam(required = false) String avoidedColors,
            @RequestParam(required = false) String fitPreference,
            @RequestParam(required = false) String genderStyle,
            @RequestParam(required = false) String preferredMaterials,
            @RequestParam(required = false) String dislikedMaterials,
            @RequestParam(required = false) String occasionPriority,
            @RequestParam(required = false) String styleKeywords,
            @RequestParam(required = false) String dislikedStyles,
            @RequestParam(required = false) String notes
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    retailerKey,
                    storeCode
            );

            CustomerPreferenceRequest preferences = buildCustomerPreferenceRequest(
                    sizeTop,
                    sizeBottom,
                    shoeSize,
                    budgetMin,
                    budgetMax,
                    favoriteColors,
                    avoidedColors,
                    fitPreference,
                    genderStyle,
                    preferredMaterials,
                    dislikedMaterials,
                    occasionPriority,
                    styleKeywords,
                    dislikedStyles,
                    notes
            );

            return inventoryService.createFullLook(
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe),
                    preferences
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/look/{rfid}/again")
    public LookResponseDTO generateAgain(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe,
            @RequestParam(defaultValue = "1") Integer variation,
            @RequestParam(required = false) String sizeTop,
            @RequestParam(required = false) String sizeBottom,
            @RequestParam(required = false) String shoeSize,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String favoriteColors,
            @RequestParam(required = false) String avoidedColors,
            @RequestParam(required = false) String fitPreference,
            @RequestParam(required = false) String genderStyle,
            @RequestParam(required = false) String preferredMaterials,
            @RequestParam(required = false) String dislikedMaterials,
            @RequestParam(required = false) String occasionPriority,
            @RequestParam(required = false) String styleKeywords,
            @RequestParam(required = false) String dislikedStyles,
            @RequestParam(required = false) String notes
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    retailerKey,
                    storeCode
            );

            CustomerPreferenceRequest preferences = buildCustomerPreferenceRequest(
                    sizeTop,
                    sizeBottom,
                    shoeSize,
                    budgetMin,
                    budgetMax,
                    favoriteColors,
                    avoidedColors,
                    fitPreference,
                    genderStyle,
                    preferredMaterials,
                    dislikedMaterials,
                    occasionPriority,
                    styleKeywords,
                    dislikedStyles,
                    notes
            );

            return inventoryService.generateAgain(
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe),
                    normalizeVariation(variation),
                    preferences
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }




    @GetMapping("/look/{rfid}/swap")
    public LookResponseDTO swapLookItem(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe,
            @RequestParam String swapCategory,
            @RequestParam(required = false) String currentTopRfid,
            @RequestParam(required = false) String currentBottomRfid,
            @RequestParam(required = false) String currentShoesRfid,
            @RequestParam(required = false) String currentOuterwearRfid,
            @RequestParam(required = false) String sizeTop,
            @RequestParam(required = false) String sizeBottom,
            @RequestParam(required = false) String shoeSize,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) String favoriteColors,
            @RequestParam(required = false) String avoidedColors,
            @RequestParam(required = false) String fitPreference,
            @RequestParam(required = false) String genderStyle,
            @RequestParam(required = false) String preferredMaterials,
            @RequestParam(required = false) String dislikedMaterials,
            @RequestParam(required = false) String occasionPriority,
            @RequestParam(required = false) String styleKeywords,
            @RequestParam(required = false) String dislikedStyles,
            @RequestParam(required = false) String notes
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    retailerKey,
                    storeCode
            );

            CustomerPreferenceRequest preferences = buildCustomerPreferenceRequest(
                    sizeTop,
                    sizeBottom,
                    shoeSize,
                    budgetMin,
                    budgetMax,
                    favoriteColors,
                    avoidedColors,
                    fitPreference,
                    genderStyle,
                    preferredMaterials,
                    dislikedMaterials,
                    occasionPriority,
                    styleKeywords,
                    dislikedStyles,
                    notes
            );

            return inventoryService.swapLookItem(
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe),
                    normalizeRequired(swapCategory, "Swap category is required."),
                    normalizeOptional(currentTopRfid),
                    normalizeOptional(currentBottomRfid),
                    normalizeOptional(currentShoesRfid),
                    normalizeOptional(currentOuterwearRfid),
                    preferences
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/bag")
    public BagSummaryResponse getBag(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            return inventoryService.getBagSummary(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @PatchMapping("/bag/{bagItemId}/quantity")
    public BagSummaryResponse updateBagItemQuantity(
            HttpServletRequest request,
            @PathVariable Long bagItemId,
            @RequestBody BagQuantityRequest requestBody
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            if (bagItemId == null) {
                throw new IllegalArgumentException("Bag item id is required.");
            }

            if (requestBody == null || requestBody.getQuantity() == null) {
                throw new IllegalArgumentException("Quantity is required.");
            }

            if (requestBody.getQuantity() < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1.");
            }

            return inventoryService.updateBagItemQuantity(
                    bagItemId,
                    requestBody.getQuantity(),
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @DeleteMapping("/bag/remove-unavailable")
    public Map<String, Object> removeUnavailableBagItems(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            int removedCount = inventoryService.removeUnavailableBagItems(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("removedCount", removedCount);
            response.put("message", removedCount + " unavailable item(s) removed.");

            return response;
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @DeleteMapping("/bag/{id}")
    public String removeBagItem(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            if (id == null) {
                throw new IllegalArgumentException("Bag item id is required.");
            }

            return inventoryService.removeBagItem(
                    id,
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @DeleteMapping("/bag")
    public String clearBag(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            return inventoryService.clearBag(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/associate/inventory")
    public MerchantInventoryPageDTO getAssociateInventory(
            HttpServletRequest request,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            String safeRetailerKey = normalizeOptional(retailerKey);
            String safeStoreCode = normalizeOptional(storeCode);

            /*
             * Smart mirror public read mode:
             * If retailerKey and storeCode are provided in the URL, use them directly.
             * This lets mirror.html read live Universal Stylist inventory without a JWT.
             */
            if (safeRetailerKey != null && safeStoreCode != null) {
                return inventoryService.getMerchantInventory(
                        safeRetailerKey.toUpperCase(),
                        safeStoreCode.toUpperCase(),
                        normalizeOptional(query),
                        normalizeOptional(category),
                        page,
                        size
                );
            }

            /*
             * Authenticated associate/admin fallback:
             * If query params are missing, use the signed-in user's store context.
             */
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            return inventoryService.getMerchantInventory(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase(),
                    normalizeOptional(query),
                    normalizeOptional(category),
                    page,
                    size
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/admin/trends")
    public List<TrendDTO> getTrends(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAdminAccess(request);

            return inventoryService.getTrends(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/admin/summary")
    public AnalyticsSummaryDTO getAnalyticsSummary(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAdminAccess(request);

            return inventoryService.getAnalyticsSummary(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/admin/activity")
    public List<ActivityDTO> getActivity(
            HttpServletRequest request,
            @RequestParam(defaultValue = "ALL") String eventType,
            @RequestParam(defaultValue = "ALL") String retailer
    ) {
        try {
            AuthContextService.AuthContext auth = requireAdminAccess(request);

            String safeEventType = normalizeOptional(eventType) == null
                    ? "ALL"
                    : eventType.trim();

            return inventoryService.getRecentActivity(
                    safeEventType,
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/admin/retailers")
    public List<RetailerStatsDTO> getRetailerStats(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAdminAccess(request);

            return inventoryService.getRetailerStats(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (SecurityException e) {
            throw forbidden(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/debug/auth")
    public Map<String, Object> debugAuth(
            HttpServletRequest request,
            Authentication authentication
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("authenticated", authentication != null);
        result.put("name", authentication != null ? authentication.getName() : null);
        result.put("authorities", authentication != null ? authentication.getAuthorities() : null);
        result.put("principal", authentication != null ? authentication.getPrincipal() : null);
        result.put("demoScanMode", demoScanMode);

        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            result.put("jwtUserId", auth.userId());
            result.put("jwtTenantId", auth.tenantId());
            result.put("jwtStoreId", auth.storeId());
            result.put("jwtEmail", auth.email());
            result.put("jwtRole", auth.role());
            result.put("jwtRetailerKey", auth.retailerKey());
            result.put("jwtStoreCode", auth.storeCode());
            result.put("canManageInventory", auth.canManageInventory());
        } catch (RuntimeException e) {
            result.put("jwtError", e.getMessage());
            result.put("jwtErrorType", e.getClass().getSimpleName());
            result.put("requestUserPrincipal", request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName());
        }

        return result;
    }

    private void saveScanHistorySafely(
            AuthContextService.AuthContext auth,
            ScanContext scanContext,
            ScanResultDTO result,
            String fallbackRfid,
            String vibe
    ) {
        if (auth == null || scanContext == null || result == null) {
            return;
        }

        try {
            String rfid = firstNonBlank(
                    getStringProperty(result, "rfid", "itemRfid", "productRfid", "id"),
                    fallbackRfid
            );

            if (rfid.isBlank()) {
                return;
            }

            customerScanHistoryService.saveScan(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    normalizeOptional(auth.email()),

                    scanContext.retailerKey(),
                    firstNonBlank(
                            getStringProperty(result, "retailerName", "retailer"),
                            scanContext.retailerKey()
                    ),

                    scanContext.storeCode(),
                    firstNonBlank(
                            getStringProperty(result, "storeName"),
                            scanContext.storeCode()
                    ),

                    rfid,
                    firstNonBlank(
                            getStringProperty(result, "itemName", "name"),
                            "Scanned Item"
                    ),
                    getStringProperty(result, "brand"),
                    getStringProperty(result, "category"),
                    getStringProperty(result, "color"),
                    getDoubleProperty(result, "price"),
                    getStringProperty(result, "imageUrl", "image", "image_url", "photoUrl", "productImageUrl"),
                    normalizeVibe(vibe),
                    getIntegerProperty(result, "matchScore", "score")
            );
        } catch (RuntimeException e) {
            System.err.println("Scan history save skipped: " + e.getMessage());
        }
    }

    private ScanContext resolveScanContext(
            AuthContextService.AuthContext auth,
            String requestedRetailerKey,
            String requestedStoreCode
    ) {
        if (auth == null) {
            throw new SecurityException("Authentication context is required.");
        }

        if (demoScanMode) {
            String safeRetailerKey = normalizeOptional(requestedRetailerKey);
            String safeStoreCode = normalizeOptional(requestedStoreCode);

            if (safeRetailerKey != null && safeStoreCode != null) {
                return new ScanContext(
                        safeRetailerKey.toUpperCase(),
                        safeStoreCode.toUpperCase()
                );
            }
        }

        return new ScanContext(
                normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
        );
    }

    private AuthContextService.AuthContext requireAuthenticated(HttpServletRequest request) {
        return authContextService.getAuthContext(request);
    }

    private AuthContextService.AuthContext requireAdminAccess(HttpServletRequest request) {
        AuthContextService.AuthContext auth = authContextService.getAuthContext(request);

        if (!auth.canManageInventory()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to view admin analytics."
            );
        }

        return auth;
    }

    private CustomerPreferenceRequest buildCustomerPreferenceRequest(
            String sizeTop,
            String sizeBottom,
            String shoeSize,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            String favoriteColors,
            String avoidedColors,
            String fitPreference,
            String genderStyle,
            String preferredMaterials,
            String dislikedMaterials,
            String occasionPriority,
            String styleKeywords,
            String dislikedStyles,
            String notes
    ) {
        CustomerPreferenceRequest preferences = new CustomerPreferenceRequest();

        preferences.setSizeTop(normalizeOptional(sizeTop));
        preferences.setSizeBottom(normalizeOptional(sizeBottom));
        preferences.setShoeSize(normalizeOptional(shoeSize));
        preferences.setBudgetMin(toDoubleOrNull(budgetMin));
        preferences.setBudgetMax(toDoubleOrNull(budgetMax));
        preferences.setFavoriteColors(normalizeOptional(favoriteColors));
        preferences.setAvoidedColors(normalizeOptional(avoidedColors));
        preferences.setFitPreference(normalizeOptional(fitPreference));
        preferences.setGenderStyle(normalizeOptional(genderStyle));
        preferences.setPreferredMaterials(normalizeOptional(preferredMaterials));
        preferences.setDislikedMaterials(normalizeOptional(dislikedMaterials));
        preferences.setOccasionPriority(normalizeOptional(occasionPriority));
        preferences.setStyleKeywords(normalizeOptional(styleKeywords));
        preferences.setDislikedStyles(normalizeOptional(dislikedStyles));
        preferences.setNotes(normalizeOptional(notes));

        return preferences;
    }

    private String getStringProperty(Object source, String... propertyNames) {
        Object value = getProperty(source, propertyNames);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Double getDoubleProperty(Object source, String... propertyNames) {
        Object value = getProperty(source, propertyNames);

        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number number) {
            return Math.max(0.0, number.doubleValue());
        }

        try {
            return Math.max(0.0, Double.parseDouble(String.valueOf(value).trim()));
        } catch (RuntimeException e) {
            return 0.0;
        }
    }

    private Integer getIntegerProperty(Object source, String... propertyNames) {
        Object value = getProperty(source, propertyNames);

        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }

        try {
            return Math.max(0, Integer.parseInt(String.valueOf(value).trim()));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private Object getProperty(Object source, String... propertyNames) {
        if (source == null || propertyNames == null) {
            return null;
        }

        for (String propertyName : propertyNames) {
            String normalizedPropertyName = normalizeOptional(propertyName);

            if (normalizedPropertyName == null) {
                continue;
            }

            Object value = invokeGetter(source, normalizedPropertyName);

            if (value != null && !String.valueOf(value).trim().isBlank()) {
                return value;
            }
        }

        return null;
    }

    private Object invokeGetter(Object source, String propertyName) {
        if (source == null || propertyName == null || propertyName.isBlank()) {
            return null;
        }

        String suffix = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

        String[] methodNames = {
                "get" + suffix,
                "is" + suffix
        };

        for (String methodName : methodNames) {
            try {
                Method method = source.getClass().getMethod(methodName);
                return method.invoke(source);
            } catch (ReflectiveOperationException ignored) {
                // Try next getter alias.
            }
        }

        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            String normalized = normalizeOptional(value);

            if (normalized != null) {
                return normalized;
            }
        }

        return "";
    }

    private Double toDoubleOrNull(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeVibe(String vibe) {
        String normalized = normalizeOptional(vibe);
        return normalized == null ? "Casual" : normalized;
    }

    private Integer normalizeVariation(Integer variation) {
        if (variation == null || variation < 1) {
            return 1;
        }

        return variation;
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private ResponseStatusException badRequest(Exception e) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }

    private ResponseStatusException unprocessableEntity(Exception e) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
    }

    private ResponseStatusException notFound(Exception e) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }

    private ResponseStatusException forbidden(Exception e) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    }

    private record ScanContext(
            String retailerKey,
            String storeCode
    ) {
    }
}
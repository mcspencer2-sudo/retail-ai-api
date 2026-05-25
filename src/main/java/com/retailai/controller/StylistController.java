package com.retailai.controller;

import com.retailai.dto.ActivityDTO;
import com.retailai.dto.AnalyticsSummaryDTO;
import com.retailai.dto.LookResponseDTO;
import com.retailai.dto.RetailerStatsDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.dto.TrendDTO;
import com.retailai.model.BagSummaryResponse;
import com.retailai.service.AuthContextService;
import com.retailai.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/macy-stylist")
public class StylistController {

    private final InventoryService inventoryService;
    private final AuthContextService authContextService;

    @Value("${retailai.demo-scan-mode:true}")
    private boolean demoScanMode;

    public StylistController(
            InventoryService inventoryService,
            AuthContextService authContextService
    ) {
        this.inventoryService = inventoryService;
        this.authContextService = authContextService;
    }

    /*
     * Secured production scan route:
     * GET /api/v1/macy-stylist/scan/{rfid}?vibe=Casual
     *
     * Always uses JWT retailer/store context.
     */
    @GetMapping("/scan/{rfid}")
    public ScanResultDTO scanItem(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestParam(defaultValue = "Casual") String vibe
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            return inventoryService.scanItem(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe)
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

    /*
     * Demo-compatible legacy scan route:
     * GET /api/v1/macy-stylist/scan/{retailerKey}/{rfid}?storeCode=...&vibe=...
     *
     * If retailai.demo-scan-mode=true:
     *   Uses the retailer/store supplied by the demo UI.
     *
     * If retailai.demo-scan-mode=false:
     *   Ignores supplied retailer/store and uses JWT context only.
     */
    @GetMapping("/scan/{requestedRetailerKey}/{rfid}")
    public ScanResultDTO scanItemLegacy(
            HttpServletRequest request,
            @PathVariable String requestedRetailerKey,
            @PathVariable String rfid,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    requestedRetailerKey,
                    storeCode
            );

            return inventoryService.scanItem(
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe)
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
            @RequestParam(defaultValue = "Casual") String vibe
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    retailerKey,
                    storeCode
            );

            return inventoryService.createFullLook(
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe)
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
            @RequestParam(defaultValue = "1") Integer variation
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    retailerKey,
                    storeCode
            );

            return inventoryService.generateAgain(
                    scanContext.retailerKey(),
                    scanContext.storeCode(),
                    normalizeRequired(rfid, "RFID is required."),
                    normalizeVibe(vibe),
                    normalizeVariation(variation)
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
            @RequestParam(required = false) String currentOuterwearRfid
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            ScanContext scanContext = resolveScanContext(
                    auth,
                    retailerKey,
                    storeCode
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
                    normalizeOptional(currentOuterwearRfid)
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

            return inventoryService.getRecentActivity(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase(),
                    normalizeOptional(eventType) == null ? "ALL" : eventType.trim()
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
        }

        return result;
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
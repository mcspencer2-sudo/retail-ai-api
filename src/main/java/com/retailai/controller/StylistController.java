package com.retailai.controller;

import com.retailai.dto.ActivityDTO;
import com.retailai.dto.AnalyticsSummaryDTO;
import com.retailai.dto.LookResponseDTO;
import com.retailai.dto.RetailerStatsDTO;
import com.retailai.dto.ScanResultDTO;
import com.retailai.dto.TrendDTO;
import com.retailai.model.BagSummaryResponse;
import com.retailai.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
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

    public StylistController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/scan/{retailerKey}/{rfid}")
    public ScanResultDTO scanItem(
            @PathVariable String retailerKey,
            @PathVariable String rfid,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe
    ) {
        try {
            return inventoryService.scanItem(retailerKey, storeCode, rfid, vibe);
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        }
    }

    @PostMapping("/save/{rfid}")
    public String saveToBag(@PathVariable String rfid) {
        try {
            return inventoryService.saveToBag(rfid);
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/look/{rfid}")
    public LookResponseDTO createFullLook(
            @PathVariable String rfid,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe
    ) {
        try {
            return inventoryService.createFullLook(retailerKey, storeCode, rfid, vibe);
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/look/{rfid}/again")
    public LookResponseDTO generateAgain(
            @PathVariable String rfid,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(defaultValue = "Casual") String vibe,
            @RequestParam(defaultValue = "1") Integer variation
    ) {
        try {
            return inventoryService.generateAgain(retailerKey, storeCode, rfid, vibe, variation);
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/look/{rfid}/swap")
    public LookResponseDTO swapLookItem(
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
            return inventoryService.swapLookItem(
                    retailerKey,
                    storeCode,
                    rfid,
                    vibe,
                    swapCategory,
                    currentTopRfid,
                    currentBottomRfid,
                    currentShoesRfid,
                    currentOuterwearRfid
            );
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @GetMapping("/bag")
    public BagSummaryResponse getBag() {
        try {
            return inventoryService.getBagSummary();
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        }
    }

    @DeleteMapping("/bag/{id}")
    public String removeBagItem(@PathVariable Long id) {
        try {
            return inventoryService.removeBagItem(id);
        } catch (IllegalArgumentException e) {
            throw badRequest(e);
        } catch (RuntimeException e) {
            throw notFound(e);
        }
    }

    @DeleteMapping("/bag")
    public String clearBag() {
        try {
            return inventoryService.clearBag();
        } catch (IllegalStateException e) {
            throw unprocessableEntity(e);
        }
    }

    @GetMapping("/admin/trends")
    public List<TrendDTO> getTrends() {
        return inventoryService.getTrends();
    }

    @GetMapping("/admin/summary")
    public AnalyticsSummaryDTO getAnalyticsSummary() {
        return inventoryService.getAnalyticsSummary();
    }

    @GetMapping("/admin/activity")
    public List<ActivityDTO> getActivity(
            @RequestParam(defaultValue = "ALL") String eventType,
            @RequestParam(defaultValue = "ALL") String retailer
    ) {
        return inventoryService.getRecentActivity(eventType, retailer);
    }

    @GetMapping("/admin/retailers")
    public List<RetailerStatsDTO> getRetailerStats() {
        return inventoryService.getRetailerStats();
    }

    @GetMapping("/debug/auth")
    public Map<String, Object> debugAuth(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("authenticated", authentication != null);
        result.put("name", authentication != null ? authentication.getName() : null);
        result.put("authorities", authentication != null ? authentication.getAuthorities() : null);
        result.put("principal", authentication != null ? authentication.getPrincipal() : null);

        return result;
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
}
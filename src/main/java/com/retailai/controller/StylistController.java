package com.retailai.controller;

import com.retailai.model.ActivityDTO;
import com.retailai.model.AnalyticsSummaryDTO;
import com.retailai.model.BagSummaryResponse;
import com.retailai.model.OutfitResponse;
import com.retailai.model.RetailerStatsDTO;
import com.retailai.model.TrendDTO;
import com.retailai.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/macy-stylist")
@CrossOrigin(origins = "*")
public class StylistController {

    private final InventoryService inventoryService;

    public StylistController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/scan/{retailerKey}/{rfid}")
    public OutfitResponse scanItem(
            @PathVariable String retailerKey,
            @PathVariable String rfid,
            @RequestParam(defaultValue = "Casual") String vibe
    ) {
        try {
            return inventoryService.scanItem(retailerKey, rfid, vibe);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/save/{rfid}")
    public String saveToBag(@PathVariable String rfid) {
        try {
            return inventoryService.saveToBag(rfid);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/bag")
    public BagSummaryResponse getBag() {
        return inventoryService.getBagSummary();
    }

    @DeleteMapping("/bag/{id}")
    public String removeBagItem(@PathVariable Long id) {
        try {
            return inventoryService.removeBagItem(id);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/bag")
    public String clearBag() {
        return inventoryService.clearBag();
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
}
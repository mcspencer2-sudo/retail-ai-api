package com.retailai.controller;

import com.retailai.dto.MerchantAnalyticsDTO;
import com.retailai.service.MerchantAnalyticsRange;
import com.retailai.service.MerchantAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant/analytics")
public class MerchantAnalyticsController {

    private final MerchantAnalyticsService merchantAnalyticsService;

    public MerchantAnalyticsController(MerchantAnalyticsService merchantAnalyticsService) {
        this.merchantAnalyticsService = merchantAnalyticsService;
    }

    @GetMapping
    public ResponseEntity<MerchantAnalyticsDTO> getMerchantAnalytics(
            @RequestParam(defaultValue = "WEEKLY") MerchantAnalyticsRange range
    ) {
        MerchantAnalyticsRange safeRange = range == null ? MerchantAnalyticsRange.WEEKLY : range;
        MerchantAnalyticsDTO analytics = merchantAnalyticsService.getMerchantAnalytics(safeRange);
        return ResponseEntity.ok(analytics);
    }
}
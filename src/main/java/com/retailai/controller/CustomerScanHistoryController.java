package com.retailai.controller;

import com.retailai.dto.ScanHistoryDTO;
import com.retailai.service.AuthContextService;
import com.retailai.service.CustomerScanHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/scan-history")
public class CustomerScanHistoryController {

    private final CustomerScanHistoryService customerScanHistoryService;
    private final AuthContextService authContextService;

    public CustomerScanHistoryController(
            CustomerScanHistoryService customerScanHistoryService,
            AuthContextService authContextService
    ) {
        this.customerScanHistoryService = customerScanHistoryService;
        this.authContextService = authContextService;
    }

    @GetMapping
    public ResponseEntity<?> getScanHistory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<ScanHistoryDTO> history = customerScanHistoryService.getScanHistory(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key."),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.")
            );

            return ResponseEntity.ok(history);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Scan history load failed: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearScanHistory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            String message = customerScanHistoryService.clearScanHistory(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key."),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.")
            );

            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Scan history clear failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScanHistoryItem(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            String message = customerScanHistoryService.deleteScanHistoryItem(
                    id,
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key."),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.")
            );

            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Scan history item delete failed: " + e.getMessage());
        }
    }

    private AuthContextService.AuthContext requireAuthenticated(HttpServletRequest request) {
        return authContextService.getAuthContext(request);
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

        return value.trim().toUpperCase();
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
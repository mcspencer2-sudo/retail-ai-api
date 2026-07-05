package com.retailai.controller;

import com.retailai.dto.InventoryImportJobDTO;
import com.retailai.dto.InventoryImportLogDTO;
import com.retailai.dto.InventoryImportResultDTO;
import com.retailai.dto.MerchantInventoryActiveUpdateDTO;
import com.retailai.dto.MerchantInventoryItemDTO;
import com.retailai.dto.MerchantInventoryPageDTO;
import com.retailai.dto.MerchantInventoryStockUpdateDTO;
import com.retailai.dto.MerchantSalesDashboardDTO;
import com.retailai.dto.StoreStaffDashboardDTO;
import com.retailai.service.AuthContextService;
import com.retailai.service.BulkInventoryImportService;
import com.retailai.service.DemoInventorySeedService;
import com.retailai.service.InventoryImportJobService;
import com.retailai.service.InventoryService;
import com.retailai.service.MerchantInventoryImportService;
import com.retailai.service.MerchantSalesService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/merchant/inventory")
public class MerchantInventoryController {

    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final MerchantInventoryImportService merchantInventoryImportService;
    private final InventoryService inventoryService;
    private final DemoInventorySeedService demoInventorySeedService;
    private final InventoryImportJobService inventoryImportJobService;
    private final BulkInventoryImportService bulkInventoryImportService;
    private final AuthContextService authContextService;
    private final MerchantSalesService merchantSalesService;

    public MerchantInventoryController(
            MerchantInventoryImportService merchantInventoryImportService,
            InventoryService inventoryService,
            DemoInventorySeedService demoInventorySeedService,
            InventoryImportJobService inventoryImportJobService,
            BulkInventoryImportService bulkInventoryImportService,
            AuthContextService authContextService,
            MerchantSalesService merchantSalesService
    ) {
        this.merchantInventoryImportService = merchantInventoryImportService;
        this.inventoryService = inventoryService;
        this.demoInventorySeedService = demoInventorySeedService;
        this.inventoryImportJobService = inventoryImportJobService;
        this.bulkInventoryImportService = bulkInventoryImportService;
        this.authContextService = authContextService;
        this.merchantSalesService = merchantSalesService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadInventory(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            validateCsvFile(file);

            String retailerKey = normalizeRequired(auth.retailerKey(), "Retailer key is missing.");
            String storeCode = normalizeRequired(auth.storeCode(), "Store code is missing.");
            String originalFilename = normalizeRequired(file.getOriginalFilename(), "CSV filename is required.");

            InventoryImportJobDTO job = inventoryImportJobService.createQueuedStandardJob(
                    originalFilename,
                    retailerKey,
                    storeCode
            );

            InventoryImportResultDTO result;

            try {
                inventoryImportJobService.markRunning(job.getJobId());

                result = merchantInventoryImportService.importCsv(
                        file,
                        retailerKey,
                        storeCode
                );

                int successCount = result == null ? 0 : result.getSuccessCount();
                int failureCount = result == null ? 0 : result.getFailureCount();
                int totalRows = successCount + failureCount;

                inventoryImportJobService.markCompleted(
                        job.getJobId(),
                        totalRows,
                        successCount,
                        failureCount
                );
            } catch (RuntimeException importError) {
                inventoryImportJobService.markFailed(job.getJobId(), safeMessage(importError));
                throw importError;
            }

            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Inventory upload failed: " + safeMessage(e));
        }
    }

    @PostMapping("/upload/bulk")
    public ResponseEntity<?> uploadInventoryBulk(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            validateCsvFile(file);

            String retailerKey = normalizeRequired(auth.retailerKey(), "Retailer key is missing.");
            String storeCode = normalizeRequired(auth.storeCode(), "Store code is missing.");
            String originalFilename = normalizeRequired(file.getOriginalFilename(), "CSV filename is required.");

            InventoryImportJobDTO job = inventoryImportJobService.createQueuedBulkJob(
                    originalFilename,
                    retailerKey,
                    storeCode
            );

            bulkInventoryImportService.startBulkImport(
                    job.getJobId(),
                    file,
                    retailerKey,
                    storeCode
            );

            return ResponseEntity.accepted().body(job);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Bulk inventory upload failed: " + safeMessage(e));
        }
    }

    @DeleteMapping("/cleanup-uploaded-test-products")
    public ResponseEntity<?> cleanupUploadedTestProducts(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            requireOwner(auth, "Only owners can clean uploaded test products.");

            String message = inventoryService.cleanupUploadedTestProducts(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing.")
            );

            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Cleanup failed: " + safeMessage(e));
        }
    }

    @DeleteMapping("/cleanup-uploaded-test-products/{storeCode}")
    public ResponseEntity<?> cleanupUploadedTestProductsForStore(
            HttpServletRequest request,
            @PathVariable String storeCode
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            requireOwner(auth, "Only owners can clean uploaded test products.");

            String safeStoreCode = normalizeRequired(storeCode, "Store code is required.");

            String message = inventoryService.cleanupUploadedTestProducts(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    safeStoreCode
            );

            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Cleanup failed: " + safeMessage(e));
        }
    }

    @GetMapping("/import-history")
    public ResponseEntity<?> getImportHistory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<InventoryImportLogDTO> history = merchantInventoryImportService.getImportHistory(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing.")
            );

            return ResponseEntity.ok(history);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Import history load failed: " + safeMessage(e));
        }
    }

    @GetMapping("/import-jobs")
    public ResponseEntity<?> getImportJobs(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<InventoryImportJobDTO> jobs = inventoryImportJobService.getRecentJobs(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing.")
            );

            return ResponseEntity.ok(jobs);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Import jobs load failed: " + safeMessage(e));
        }
    }

    @GetMapping("/import-jobs/{jobId}")
    public ResponseEntity<?> getImportJob(
            HttpServletRequest request,
            @PathVariable String jobId
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            String safeJobId = normalizeRequired(jobId, "Import job id is required.");

            InventoryImportJobDTO job = inventoryImportJobService.getJob(safeJobId);

            validateJobBelongsToAuthStore(job, auth);

            return ResponseEntity.ok(job);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return notFound(e);
        }
    }

    @PostMapping("/import-jobs/{jobId}/cancel")
    public ResponseEntity<?> cancelImportJob(
            HttpServletRequest request,
            @PathVariable String jobId
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            String safeJobId = normalizeRequired(jobId, "Import job id is required.");

            InventoryImportJobDTO existingJob = inventoryImportJobService.getJob(safeJobId);
            validateJobBelongsToAuthStore(existingJob, auth);

            InventoryImportJobDTO job = inventoryImportJobService.markCancelled(safeJobId);

            return ResponseEntity.ok(job);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return notFound(e);
        }
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoInventory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            requireOwner(auth, "Only owners can seed demo inventory.");

            String retailerKey = normalizeRequired(auth.retailerKey(), "Retailer key is missing.");
            String storeCode = normalizeRequired(auth.storeCode(), "Store code is missing.");

            String retailerName = auth.tenant() == null
                    ? retailerKey
                    : normalizeOptional(auth.tenant().getBusinessName());

            String storeName = auth.store() == null
                    ? storeCode
                    : normalizeOptional(auth.store().getStoreName());

            int count = demoInventorySeedService.seedDemoInventory(
                    retailerKey,
                    retailerName == null ? retailerKey : retailerName,
                    storeCode,
                    storeName == null ? storeCode : storeName
            );

            return ResponseEntity.ok("Demo inventory seeded successfully. Items loaded: " + count);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Demo inventory seed failed: " + safeMessage(e));
        }
    }

    @DeleteMapping("/demo")
    public ResponseEntity<?> clearDemoInventory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            requireOwner(auth, "Only owners can clear demo inventory.");

            String retailerKey = normalizeRequired(auth.retailerKey(), "Retailer key is missing.");
            String storeCode = normalizeRequired(auth.storeCode(), "Store code is missing.");

            int demoCount = demoInventorySeedService.clearDemoInventory(
                    retailerKey,
                    storeCode
            );

            String cleanupMessage = "";

            if (demoCount <= 0) {
                cleanupMessage = inventoryService.cleanupUploadedTestProducts(
                        retailerKey,
                        storeCode
                );
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("removedCount", demoCount);
            response.put("message", demoCount > 0
                    ? "Demo inventory cleared successfully. Items removed: " + demoCount
                    : "Demo inventory cleared successfully. Items removed: 0. " + cleanupMessage);

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Demo inventory clear failed: " + safeMessage(e));
        }
    }

    @GetMapping
    public ResponseEntity<?> getInventory(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            int safePage = Math.max(0, page);
            int safeSize = Math.max(1, Math.min(size, 50));

            MerchantInventoryPageDTO result = inventoryService.getMerchantInventory(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing."),
                    normalizeOptional(q),
                    normalizeOptional(category),
                    safePage,
                    safeSize
            );

            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Inventory load failed: " + safeMessage(e));
        }
    }

    @GetMapping({"/sales", "/sales/dashboard"})
    public ResponseEntity<?> getMerchantSalesDashboard(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            MerchantSalesDashboardDTO dashboard = merchantSalesService.getStoreSalesDashboard(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing.")
            );

            return ResponseEntity.ok(dashboard);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Merchant sales dashboard load failed: " + safeMessage(e));
        }
    }

    @GetMapping("/staff-dashboard")
    public ResponseEntity<?> getStoreStaffDashboard(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            StoreStaffDashboardDTO dashboard = inventoryService.getStoreStaffDashboard(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing.")
            );

            return ResponseEntity.ok(dashboard);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Store staff dashboard load failed: " + safeMessage(e));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportInventoryCsv(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            String csv = inventoryService.exportMerchantInventoryCsv(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing."),
                    normalizeOptional(q),
                    normalizeOptional(category)
            );

            String filename = "merchant-inventory-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Inventory export failed: " + safeMessage(e));
        }
    }

    @GetMapping("/export/low-stock")
    public ResponseEntity<?> exportLowStockInventoryCsv(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "3") Integer threshold
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            int safeThreshold = threshold == null ? 3 : Math.max(0, threshold);

            String csv = inventoryService.exportLowStockInventoryCsv(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing."),
                    normalizeOptional(q),
                    normalizeOptional(category),
                    safeThreshold
            );

            String filename = "merchant-low-stock-threshold-" + safeThreshold + "-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Low stock inventory export failed: " + safeMessage(e));
        }
    }

    @GetMapping("/export/reorder-report")
    public ResponseEntity<?> exportMerchantReorderReportCsv(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            String csv = inventoryService.exportMerchantReorderReportCsv(
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing."),
                    normalizeOptional(q),
                    normalizeOptional(category)
            );

            String filename = "merchant-reorder-report-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Reorder report failed: " + safeMessage(e));
        }
    }

    @PutMapping("/{rfid}")
    public ResponseEntity<?> updateInventoryItem(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestBody MerchantInventoryItemDTO updateRequest
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            String safeRfid = normalizeRequired(rfid, "RFID is required.");

            if (updateRequest == null) {
                throw new IllegalArgumentException("Inventory update request body is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.updateMerchantInventoryItem(
                    safeRfid,
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing."),
                    updateRequest
            );

            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return notFound(e);
        }
    }

    @PutMapping("/{rfid}/stock")
    public ResponseEntity<?> updateStock(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestBody MerchantInventoryStockUpdateDTO stockUpdateRequest
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            String safeRfid = normalizeRequired(rfid, "RFID is required.");

            if (stockUpdateRequest == null) {
                throw new IllegalArgumentException("Stock update request body is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.updateMerchantInventoryStock(
                    safeRfid,
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing."),
                    stockUpdateRequest.getStockQuantity()
            );

            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return notFound(e);
        }
    }

    @PatchMapping({"/{rfid}/status", "/{rfid}/active"})
    public ResponseEntity<?> updateActive(
            HttpServletRequest request,
            @PathVariable String rfid,
            @RequestBody MerchantInventoryActiveUpdateDTO activeUpdateRequest
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            String safeRfid = normalizeRequired(rfid, "RFID is required.");

            if (activeUpdateRequest == null) {
                throw new IllegalArgumentException("Active update request body is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.updateMerchantInventoryActive(
                    safeRfid,
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing."),
                    activeUpdateRequest.getActive()
            );

            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return notFound(e);
        }
    }

    @PostMapping("/{rfid}/resync")
    public ResponseEntity<?> resyncInventoryItem(
            HttpServletRequest request,
            @PathVariable String rfid
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            String safeRfid = normalizeRequired(rfid, "RFID is required.");

            MerchantInventoryItemDTO item = inventoryService.resyncMerchantInventoryItem(
                    safeRfid,
                    normalizeRequired(auth.retailerKey(), "Retailer key is missing."),
                    normalizeRequired(auth.storeCode(), "Store code is missing.")
            );

            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return notFound(e);
        }
    }

    private AuthContextService.AuthContext requireAuthenticated(HttpServletRequest request) {
        return authContextService.getAuthContext(request);
    }

    private AuthContextService.AuthContext requireInventoryManager(HttpServletRequest request) {
        AuthContextService.AuthContext auth = authContextService.getAuthContext(request);

        if (!auth.canManageInventory()) {
            throw new SecurityException("You do not have permission to manage inventory.");
        }

        return auth;
    }

    private void requireOwner(AuthContextService.AuthContext auth, String message) {
        if (auth == null || !auth.isOwner()) {
            throw new SecurityException(message);
        }
    }

    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required.");
        }

        String originalFilename = normalizeOptional(file.getOriginalFilename());

        if (originalFilename == null) {
            throw new IllegalArgumentException("CSV filename is required.");
        }

        if (!originalFilename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are supported.");
        }
    }

    private void validateJobBelongsToAuthStore(
            InventoryImportJobDTO job,
            AuthContextService.AuthContext auth
    ) {
        if (job == null) {
            throw new RuntimeException("Import job not found.");
        }

        String jobRetailerKey = normalizeOptional(job.getRetailerKey());
        String jobStoreCode = normalizeOptional(job.getStoreCode());

        if (jobRetailerKey == null || jobStoreCode == null) {
            throw new SecurityException("Import job is missing store context.");
        }

        String authRetailerKey = normalizeRequired(auth.retailerKey(), "Retailer key is missing.");
        String authStoreCode = normalizeRequired(auth.storeCode(), "Store code is missing.");

        boolean sameRetailer = jobRetailerKey.equalsIgnoreCase(authRetailerKey);
        boolean sameStore = jobStoreCode.equalsIgnoreCase(authStoreCode);

        if (!sameRetailer || !sameStore) {
            throw new SecurityException("Import job does not belong to your store.");
        }
    }

    private ResponseEntity<String> csvDownloadResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv == null ? "" : csv);
    }

    private String timestamp() {
        return LocalDateTime.now().format(EXPORT_TIMESTAMP_FORMATTER);
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

    private String safeMessage(Exception e) {
        String message = e == null ? "" : e.getMessage();
        return message == null || message.isBlank() ? "Request failed." : message;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message == null || message.isBlank() ? "Request failed." : message);
        return body;
    }

    private ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> forbidden(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> notFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(message));
    }
}
package com.retailai.controller;

import com.retailai.dto.InventoryImportJobDTO;
import com.retailai.dto.InventoryImportLogDTO;
import com.retailai.dto.InventoryImportResultDTO;
import com.retailai.dto.MerchantInventoryActiveUpdateDTO;
import com.retailai.dto.MerchantInventoryItemDTO;
import com.retailai.dto.MerchantInventoryPageDTO;
import com.retailai.dto.MerchantInventoryStockUpdateDTO;
import com.retailai.dto.StoreStaffDashboardDTO;
import com.retailai.service.AuthContextService;
import com.retailai.service.BulkInventoryImportService;
import com.retailai.service.DemoInventorySeedService;
import com.retailai.service.InventoryImportJobService;
import com.retailai.service.InventoryService;
import com.retailai.service.MerchantInventoryImportService;
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
import java.util.List;

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

    public MerchantInventoryController(
            MerchantInventoryImportService merchantInventoryImportService,
            InventoryService inventoryService,
            DemoInventorySeedService demoInventorySeedService,
            InventoryImportJobService inventoryImportJobService,
            BulkInventoryImportService bulkInventoryImportService,
            AuthContextService authContextService
    ) {
        this.merchantInventoryImportService = merchantInventoryImportService;
        this.inventoryService = inventoryService;
        this.demoInventorySeedService = demoInventorySeedService;
        this.inventoryImportJobService = inventoryImportJobService;
        this.bulkInventoryImportService = bulkInventoryImportService;
        this.authContextService = authContextService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadInventory(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            validateCsvFile(file);

            String originalFilename = file.getOriginalFilename();
            String retailerKey = auth.retailerKey();
            String storeCode = auth.storeCode();

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

                int successCount = result.getSuccessCount();
                int failureCount = result.getFailureCount();
                int totalRows = successCount + failureCount;

                inventoryImportJobService.markCompleted(
                        job.getJobId(),
                        totalRows,
                        successCount,
                        failureCount
                );
            } catch (RuntimeException importError) {
                inventoryImportJobService.markFailed(job.getJobId(), importError.getMessage());
                throw importError;
            }

            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Inventory upload failed: " + e.getMessage());
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

            String originalFilename = file.getOriginalFilename();
            String retailerKey = auth.retailerKey();
            String storeCode = auth.storeCode();

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Bulk inventory upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/cleanup-uploaded-test-products")
    public ResponseEntity<?> cleanupUploadedTestProducts(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            if (!auth.isOwner()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only owners can clean uploaded test products.");
            }

            String message = inventoryService.cleanupUploadedTestProducts(
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Cleanup failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/cleanup-uploaded-test-products/{storeCode}")
    public ResponseEntity<?> cleanupUploadedTestProductsForStore(
            HttpServletRequest request,
            @PathVariable String storeCode
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            if (!auth.isOwner()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only owners can clean uploaded test products.");
            }

            String safeStoreCode = normalizeRequired(storeCode, "Store code is required.");

            String message = inventoryService.cleanupUploadedTestProducts(
                    auth.retailerKey(),
                    safeStoreCode
            );

            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Cleanup failed: " + e.getMessage());
        }
    }

    @GetMapping("/import-history")
    public ResponseEntity<?> getImportHistory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<InventoryImportLogDTO> history = merchantInventoryImportService.getImportHistory(
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(history);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Import history load failed: " + e.getMessage());
        }
    }

    @GetMapping("/import-jobs")
    public ResponseEntity<?> getImportJobs(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<InventoryImportJobDTO> jobs = inventoryImportJobService.getRecentJobs(
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(jobs);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Import jobs load failed: " + e.getMessage());
        }
    }

    @GetMapping("/import-jobs/{jobId}")
    public ResponseEntity<?> getImportJob(
            HttpServletRequest request,
            @PathVariable String jobId
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            if (jobId == null || jobId.isBlank()) {
                return ResponseEntity.badRequest().body("Import job id is required.");
            }

            InventoryImportJobDTO job = inventoryImportJobService.getJob(jobId.trim());

            validateJobBelongsToAuthStore(job, auth);

            return ResponseEntity.ok(job);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/import-jobs/{jobId}/cancel")
    public ResponseEntity<?> cancelImportJob(
            HttpServletRequest request,
            @PathVariable String jobId
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            if (jobId == null || jobId.isBlank()) {
                return ResponseEntity.badRequest().body("Import job id is required.");
            }

            InventoryImportJobDTO existingJob = inventoryImportJobService.getJob(jobId.trim());
            validateJobBelongsToAuthStore(existingJob, auth);

            InventoryImportJobDTO job = inventoryImportJobService.markCancelled(jobId.trim());

            return ResponseEntity.ok(job);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoInventory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            if (!auth.isOwner()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only owners can seed demo inventory.");
            }

            int count = demoInventorySeedService.seedDemoInventory(
                    auth.retailerKey(),
                    auth.tenant().getBusinessName(),
                    auth.storeCode(),
                    auth.store().getStoreName()
            );

            return ResponseEntity.ok("Demo inventory seeded successfully. Items loaded: " + count);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Demo inventory seed failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/demo")
    public ResponseEntity<?> clearDemoInventory(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            if (!auth.isOwner()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only owners can clear demo inventory.");
            }

            int count = demoInventorySeedService.clearDemoInventory(
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok("Demo inventory cleared successfully. Items removed: " + count);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Demo inventory clear failed: " + e.getMessage());
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
                    auth.retailerKey(),
                    auth.storeCode(),
                    normalizeOptional(q),
                    normalizeOptional(category),
                    safePage,
                    safeSize
            );

            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Inventory load failed: " + e.getMessage());
        }
    }

    @GetMapping("/staff-dashboard")
    public ResponseEntity<?> getStoreStaffDashboard(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            StoreStaffDashboardDTO dashboard = inventoryService.getStoreStaffDashboard(
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(dashboard);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Store staff dashboard load failed: " + e.getMessage());
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
                    auth.retailerKey(),
                    auth.storeCode(),
                    normalizeOptional(q),
                    normalizeOptional(category)
            );

            String filename = "merchant-inventory-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Inventory export failed: " + e.getMessage());
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
                    auth.retailerKey(),
                    auth.storeCode(),
                    normalizeOptional(q),
                    normalizeOptional(category),
                    safeThreshold
            );

            String filename = "merchant-low-stock-threshold-" + safeThreshold + "-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Low stock inventory export failed: " + e.getMessage());
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
                    auth.retailerKey(),
                    auth.storeCode(),
                    normalizeOptional(q),
                    normalizeOptional(category)
            );

            String filename = "merchant-reorder-report-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Reorder report failed: " + e.getMessage());
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

            if (rfid == null || rfid.isBlank()) {
                return ResponseEntity.badRequest().body("RFID is required.");
            }

            if (stockUpdateRequest == null) {
                return ResponseEntity.badRequest().body("Stock update request body is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.updateMerchantInventoryStock(
                    rfid.trim(),
                    auth.retailerKey(),
                    auth.storeCode(),
                    stockUpdateRequest.getStockQuantity()
            );

            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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

            if (rfid == null || rfid.isBlank()) {
                return ResponseEntity.badRequest().body("RFID is required.");
            }

            if (activeUpdateRequest == null) {
                return ResponseEntity.badRequest().body("Active update request body is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.updateMerchantInventoryActive(
                    rfid.trim(),
                    auth.retailerKey(),
                    auth.storeCode(),
                    activeUpdateRequest.getActive()
            );

            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{rfid}/resync")
    public ResponseEntity<?> resyncInventoryItem(
            HttpServletRequest request,
            @PathVariable String rfid
    ) {
        try {
            AuthContextService.AuthContext auth = requireInventoryManager(request);

            if (rfid == null || rfid.isBlank()) {
                return ResponseEntity.badRequest().body("RFID is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.resyncMerchantInventoryItem(
                    rfid.trim(),
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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

    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
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

        boolean sameRetailer = jobRetailerKey.equalsIgnoreCase(auth.retailerKey());
        boolean sameStore = jobStoreCode.equalsIgnoreCase(auth.storeCode());

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
}
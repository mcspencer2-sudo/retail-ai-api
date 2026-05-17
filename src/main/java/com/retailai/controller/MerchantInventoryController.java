package com.retailai.controller;

import com.retailai.dto.InventoryImportJobDTO;
import com.retailai.dto.InventoryImportLogDTO;
import com.retailai.dto.InventoryImportResultDTO;
import com.retailai.dto.MerchantInventoryActiveUpdateDTO;
import com.retailai.dto.MerchantInventoryItemDTO;
import com.retailai.dto.MerchantInventoryPageDTO;
import com.retailai.dto.MerchantInventoryStockUpdateDTO;
import com.retailai.service.BulkInventoryImportService;
import com.retailai.service.DemoInventorySeedService;
import com.retailai.service.InventoryImportJobService;
import com.retailai.service.InventoryService;
import com.retailai.service.MerchantInventoryImportService;
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

    public MerchantInventoryController(
            MerchantInventoryImportService merchantInventoryImportService,
            InventoryService inventoryService,
            DemoInventorySeedService demoInventorySeedService,
            InventoryImportJobService inventoryImportJobService,
            BulkInventoryImportService bulkInventoryImportService
    ) {
        this.merchantInventoryImportService = merchantInventoryImportService;
        this.inventoryService = inventoryService;
        this.demoInventorySeedService = demoInventorySeedService;
        this.inventoryImportJobService = inventoryImportJobService;
        this.bulkInventoryImportService = bulkInventoryImportService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadInventory(
            @RequestParam("file") MultipartFile file,
            @RequestParam("retailerKey") String retailerKey,
            @RequestParam("storeCode") String storeCode
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("CSV file is required.");
            }

            if (retailerKey == null || retailerKey.isBlank()) {
                return ResponseEntity.badRequest().body("Retailer key is required.");
            }

            if (storeCode == null || storeCode.isBlank()) {
                return ResponseEntity.badRequest().body("Store code is required.");
            }

            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("Only .csv files are supported.");
            }

            String safeRetailerKey = retailerKey.trim();
            String safeStoreCode = storeCode.trim();

            InventoryImportJobDTO job = inventoryImportJobService.createQueuedJob(
                    originalFilename,
                    safeRetailerKey,
                    safeStoreCode
            );

            InventoryImportResultDTO result;

            try {
                inventoryImportJobService.markRunning(job.getJobId());

                result = merchantInventoryImportService.importCsv(
                        file,
                        safeRetailerKey,
                        safeStoreCode
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Inventory upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload/bulk")
    public ResponseEntity<?> uploadInventoryBulk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("retailerKey") String retailerKey,
            @RequestParam("storeCode") String storeCode
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("CSV file is required.");
            }

            if (retailerKey == null || retailerKey.isBlank()) {
                return ResponseEntity.badRequest().body("Retailer key is required.");
            }

            if (storeCode == null || storeCode.isBlank()) {
                return ResponseEntity.badRequest().body("Store code is required.");
            }

            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("Only .csv files are supported.");
            }

            String safeRetailerKey = retailerKey.trim();
            String safeStoreCode = storeCode.trim();

            InventoryImportJobDTO job = inventoryImportJobService.createQueuedJob(
                    originalFilename,
                    safeRetailerKey,
                    safeStoreCode
            );

            bulkInventoryImportService.startBulkImport(
                    job.getJobId(),
                    file,
                    safeRetailerKey,
                    safeStoreCode
            );

            return ResponseEntity.accepted().body(job);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Bulk inventory upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/import-history")
    public ResponseEntity<?> getImportHistory(
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode
    ) {
        try {
            List<InventoryImportLogDTO> history = merchantInventoryImportService.getImportHistory(
                    normalizeOptional(retailerKey),
                    normalizeOptional(storeCode)
            );

            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Import history load failed: " + e.getMessage());
        }
    }

    @GetMapping("/import-jobs")
    public ResponseEntity<?> getImportJobs(
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode
    ) {
        try {
            List<InventoryImportJobDTO> jobs = inventoryImportJobService.getRecentJobs(
                    normalizeOptional(retailerKey),
                    normalizeOptional(storeCode)
            );

            return ResponseEntity.ok(jobs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Import jobs load failed: " + e.getMessage());
        }
    }

    @GetMapping("/import-jobs/{jobId}")
    public ResponseEntity<?> getImportJob(
            @PathVariable String jobId
    ) {
        try {
            if (jobId == null || jobId.isBlank()) {
                return ResponseEntity.badRequest().body("Import job id is required.");
            }

            InventoryImportJobDTO job = inventoryImportJobService.getJob(jobId.trim());

            return ResponseEntity.ok(job);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoInventory() {
        try {
            int count = demoInventorySeedService.seedDemoInventory();

            return ResponseEntity.ok("Demo inventory seeded successfully. Items loaded: " + count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Demo inventory seed failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/demo")
    public ResponseEntity<?> clearDemoInventory() {
        try {
            int count = demoInventorySeedService.clearDemoInventory();

            return ResponseEntity.ok("Demo inventory cleared successfully. Items removed: " + count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Demo inventory clear failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getInventory(
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        try {
            int safePage = Math.max(0, page);
            int safeSize = Math.max(1, Math.min(size, 50));

            MerchantInventoryPageDTO result = inventoryService.getMerchantInventory(
                    normalizeOptional(retailerKey),
                    normalizeOptional(storeCode),
                    normalizeOptional(q),
                    normalizeOptional(category),
                    safePage,
                    safeSize
            );

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Inventory load failed: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportInventoryCsv(
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        try {
            String csv = inventoryService.exportMerchantInventoryCsv(
                    normalizeOptional(retailerKey),
                    normalizeOptional(storeCode),
                    normalizeOptional(q),
                    normalizeOptional(category)
            );

            String filename = "merchant-inventory-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Inventory export failed: " + e.getMessage());
        }
    }

    @GetMapping("/export/low-stock")
    public ResponseEntity<?> exportLowStockInventoryCsv(
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "3") Integer threshold
    ) {
        try {
            int safeThreshold = threshold == null ? 3 : Math.max(0, threshold);

            String csv = inventoryService.exportLowStockInventoryCsv(
                    normalizeOptional(retailerKey),
                    normalizeOptional(storeCode),
                    normalizeOptional(q),
                    normalizeOptional(category),
                    safeThreshold
            );

            String filename = "merchant-low-stock-threshold-" + safeThreshold + "-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Low stock inventory export failed: " + e.getMessage());
        }
    }

    @GetMapping("/export/reorder-report")
    public ResponseEntity<?> exportMerchantReorderReportCsv(
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        try {
            String csv = inventoryService.exportMerchantReorderReportCsv(
                    normalizeOptional(retailerKey),
                    normalizeOptional(storeCode),
                    normalizeOptional(q),
                    normalizeOptional(category)
            );

            String filename = "merchant-reorder-report-" + timestamp() + ".csv";

            return csvDownloadResponse(csv, filename);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Reorder report failed: " + e.getMessage());
        }
    }

    @PutMapping("/{rfid}/stock")
    public ResponseEntity<?> updateStock(
            @PathVariable String rfid,
            @RequestBody MerchantInventoryStockUpdateDTO request
    ) {
        try {
            if (rfid == null || rfid.isBlank()) {
                return ResponseEntity.badRequest().body("RFID is required.");
            }

            if (request == null) {
                return ResponseEntity.badRequest().body("Stock update request body is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.updateMerchantInventoryStock(
                    rfid.trim(),
                    normalizeOptional(request.getRetailerKey()),
                    normalizeOptional(request.getStoreCode()),
                    request.getStockQuantity()
            );

            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @PatchMapping({"/{rfid}/status", "/{rfid}/active"})
    public ResponseEntity<?> updateActive(
            @PathVariable String rfid,
            @RequestBody MerchantInventoryActiveUpdateDTO request
    ) {
        try {
            if (rfid == null || rfid.isBlank()) {
                return ResponseEntity.badRequest().body("RFID is required.");
            }

            if (request == null) {
                return ResponseEntity.badRequest().body("Active update request body is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.updateMerchantInventoryActive(
                    rfid.trim(),
                    normalizeOptional(request.getRetailerKey()),
                    normalizeOptional(request.getStoreCode()),
                    request.getActive()
            );

            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/{rfid}/resync")
    public ResponseEntity<?> resyncInventoryItem(
            @PathVariable String rfid,
            @RequestParam(required = false) String retailerKey,
            @RequestParam(required = false) String storeCode
    ) {
        try {
            if (rfid == null || rfid.isBlank()) {
                return ResponseEntity.badRequest().body("RFID is required.");
            }

            MerchantInventoryItemDTO item = inventoryService.resyncMerchantInventoryItem(
                    rfid.trim(),
                    normalizeOptional(retailerKey),
                    normalizeOptional(storeCode)
            );

            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    private ResponseEntity<String> csvDownloadResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private String timestamp() {
        return LocalDateTime.now().format(EXPORT_TIMESTAMP_FORMATTER);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
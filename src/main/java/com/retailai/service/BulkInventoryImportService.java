package com.retailai.service;

import com.retailai.dto.InventoryImportResultDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class BulkInventoryImportService {

    private final MerchantInventoryImportService merchantInventoryImportService;
    private final InventoryImportJobService inventoryImportJobService;

    public BulkInventoryImportService(
            MerchantInventoryImportService merchantInventoryImportService,
            InventoryImportJobService inventoryImportJobService
    ) {
        this.merchantInventoryImportService = merchantInventoryImportService;
        this.inventoryImportJobService = inventoryImportJobService;
    }

    @Async
    public void processBulkImport(
            String jobId,
            MultipartFile file,
            String retailerKey,
            String storeCode
    ) {
        try {
            inventoryImportJobService.markRunning(jobId);

            InventoryImportResultDTO result = merchantInventoryImportService.importCsv(
                    file,
                    retailerKey,
                    storeCode
            );

            int successCount = result.getSuccessCount();
            int failureCount = result.getFailureCount();
            int totalRows = successCount + failureCount;

            inventoryImportJobService.markCompleted(
                    jobId,
                    totalRows,
                    successCount,
                    failureCount
            );
        } catch (RuntimeException error) {
            inventoryImportJobService.markFailed(jobId, error.getMessage());
        }
    }
}
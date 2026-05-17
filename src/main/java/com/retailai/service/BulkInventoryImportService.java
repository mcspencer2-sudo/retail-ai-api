package com.retailai.service;

import com.retailai.dto.InventoryImportResultDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    public void startBulkImport(
            String jobId,
            MultipartFile file,
            String retailerKey,
            String storeCode
    ) {
        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();

            processBulkImport(
                    jobId,
                    fileBytes,
                    originalFilename,
                    contentType,
                    retailerKey,
                    storeCode
            );
        } catch (IOException error) {
            inventoryImportJobService.markFailed(
                    jobId,
                    "Unable to read uploaded CSV file: " + error.getMessage()
            );
        }
    }

    @Async
    public void processBulkImport(
            String jobId,
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            String retailerKey,
            String storeCode
    ) {
        try {
            inventoryImportJobService.markRunning(jobId);

            MultipartFile safeFile = new InMemoryMultipartFile(
                    "file",
                    originalFilename == null || originalFilename.isBlank()
                            ? "bulk-inventory.csv"
                            : originalFilename,
                    contentType == null || contentType.isBlank()
                            ? "text/csv"
                            : contentType,
                    fileBytes
            );

            InventoryImportResultDTO result = merchantInventoryImportService.importCsv(
                    safeFile,
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

    private static final class InMemoryMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private InMemoryMultipartFile(
                String name,
                String originalFilename,
                String contentType,
                byte[] content
        ) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content == null ? new byte[0] : content.clone();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
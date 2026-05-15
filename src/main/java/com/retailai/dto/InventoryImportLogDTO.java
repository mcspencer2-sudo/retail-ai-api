package com.retailai.dto;

import java.time.LocalDateTime;

public class InventoryImportLogDTO {

    private Long id;
    private String retailerKey;
    private String storeCode;
    private String originalFilename;
    private int successCount;
    private int failureCount;
    private int totalRows;
    private String status;
    private LocalDateTime createdAt;

    public InventoryImportLogDTO() {
    }

    public InventoryImportLogDTO(
            Long id,
            String retailerKey,
            String storeCode,
            String originalFilename,
            int successCount,
            int failureCount,
            int totalRows,
            String status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.retailerKey = retailerKey;
        this.storeCode = storeCode;
        this.originalFilename = originalFilename;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.totalRows = totalRows;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
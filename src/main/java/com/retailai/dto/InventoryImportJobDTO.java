package com.retailai.dto;

import java.time.LocalDateTime;

public class InventoryImportJobDTO {

    private Long id;
    private String jobId;
    private String originalFilename;
    private String retailerKey;
    private String storeCode;
    private String status;
    private String mode;
    private Integer totalRows;
    private Integer processedRows;
    private Integer successCount;
    private Integer failureCount;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public InventoryImportJobDTO() {
    }

    public InventoryImportJobDTO(
            Long id,
            String jobId,
            String originalFilename,
            String retailerKey,
            String storeCode,
            String status,
            String mode,
            Integer totalRows,
            Integer processedRows,
            Integer successCount,
            Integer failureCount,
            String message,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
        this.id = id;
        this.jobId = jobId;
        this.originalFilename = originalFilename;
        this.retailerKey = retailerKey;
        this.storeCode = storeCode;
        this.status = status;
        this.mode = mode;
        this.totalRows = totalRows;
        this.processedRows = processedRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.message = message;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = retailerKey;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getProcessedRows() {
        return processedRows;
    }

    public void setProcessedRows(Integer processedRows) {
        this.processedRows = processedRows;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
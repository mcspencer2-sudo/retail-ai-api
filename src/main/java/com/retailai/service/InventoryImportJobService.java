package com.retailai.service;

import com.retailai.dto.InventoryImportJobDTO;
import com.retailai.model.InventoryImportJob;
import com.retailai.model.InventoryImportJobStatus;
import com.retailai.repository.InventoryImportJobRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryImportJobService {

    private final InventoryImportJobRepository inventoryImportJobRepository;

    public InventoryImportJobService(InventoryImportJobRepository inventoryImportJobRepository) {
        this.inventoryImportJobRepository = inventoryImportJobRepository;
    }

    public InventoryImportJobDTO createQueuedJob(
            String originalFilename,
            String retailerKey,
            String storeCode
    ) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename is required.");
        }

        if (retailerKey == null || retailerKey.isBlank()) {
            throw new IllegalArgumentException("Retailer key is required.");
        }

        if (storeCode == null || storeCode.isBlank()) {
            throw new IllegalArgumentException("Store code is required.");
        }

        InventoryImportJob job = new InventoryImportJob();
        job.setOriginalFilename(originalFilename.trim());
        job.setRetailerKey(retailerKey.trim());
        job.setStoreCode(storeCode.trim());
        job.setStatus(InventoryImportJobStatus.QUEUED);
        job.setMessage("Import job queued.");

        InventoryImportJob saved = inventoryImportJobRepository.save(job);
        return toDto(saved);
    }

    public InventoryImportJobDTO markRunning(String jobId) {
        InventoryImportJob job = findJob(jobId);

        job.setStatus(InventoryImportJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setMessage("Import job is running.");

        InventoryImportJob saved = inventoryImportJobRepository.save(job);
        return toDto(saved);
    }

    public InventoryImportJobDTO updateProgress(
            String jobId,
            Integer totalRows,
            Integer processedRows,
            Integer successCount,
            Integer failureCount,
            String message
    ) {
        InventoryImportJob job = findJob(jobId);

        job.setTotalRows(safeInt(totalRows));
        job.setProcessedRows(safeInt(processedRows));
        job.setSuccessCount(safeInt(successCount));
        job.setFailureCount(safeInt(failureCount));

        if (message != null && !message.isBlank()) {
            job.setMessage(message.trim());
        }

        InventoryImportJob saved = inventoryImportJobRepository.save(job);
        return toDto(saved);
    }

    public InventoryImportJobDTO markCompleted(
            String jobId,
            Integer totalRows,
            Integer successCount,
            Integer failureCount
    ) {
        InventoryImportJob job = findJob(jobId);

        int safeFailureCount = safeInt(failureCount);

        job.setStatus(
                safeFailureCount > 0
                        ? InventoryImportJobStatus.COMPLETED_WITH_ERRORS
                        : InventoryImportJobStatus.COMPLETED
        );

        job.setTotalRows(safeInt(totalRows));
        job.setProcessedRows(safeInt(totalRows));
        job.setSuccessCount(safeInt(successCount));
        job.setFailureCount(safeFailureCount);
        job.setCompletedAt(LocalDateTime.now());

        job.setMessage(
                safeFailureCount > 0
                        ? "Import completed with row errors."
                        : "Import completed successfully."
        );

        InventoryImportJob saved = inventoryImportJobRepository.save(job);
        return toDto(saved);
    }

    public InventoryImportJobDTO markFailed(String jobId, String message) {
        InventoryImportJob job = findJob(jobId);

        job.setStatus(InventoryImportJobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        job.setMessage(
                message == null || message.isBlank()
                        ? "Import job failed."
                        : message.trim()
        );

        InventoryImportJob saved = inventoryImportJobRepository.save(job);
        return toDto(saved);
    }

    public InventoryImportJobDTO getJob(String jobId) {
        return toDto(findJob(jobId));
    }

    public List<InventoryImportJobDTO> getRecentJobs(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = retailerKey == null ? "" : retailerKey.trim();
        String safeStoreCode = storeCode == null ? "" : storeCode.trim();

        List<InventoryImportJob> jobs;

        if (!safeRetailerKey.isBlank() && !safeStoreCode.isBlank()) {
            jobs = inventoryImportJobRepository.findTop20ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeRetailerKey,
                    safeStoreCode
            );
        } else if (!safeRetailerKey.isBlank()) {
            jobs = inventoryImportJobRepository.findTop20ByRetailerKeyOrderByCreatedAtDesc(safeRetailerKey);
        } else {
            jobs = inventoryImportJobRepository.findTop20ByOrderByCreatedAtDesc();
        }

        return jobs.stream()
                .map(this::toDto)
                .toList();
    }

    private InventoryImportJob findJob(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Import job id is required.");
        }

        return inventoryImportJobRepository.findByJobId(jobId.trim())
                .orElseThrow(() -> new RuntimeException("Import job not found: " + jobId));
    }

    private InventoryImportJobDTO toDto(InventoryImportJob job) {
        return new InventoryImportJobDTO(
                job.getId(),
                job.getJobId(),
                job.getOriginalFilename(),
                job.getRetailerKey(),
                job.getStoreCode(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getSuccessCount(),
                job.getFailureCount(),
                job.getMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }
}
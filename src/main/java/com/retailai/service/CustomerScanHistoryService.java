package com.retailai.service;

import com.retailai.dto.ScanHistoryDTO;
import com.retailai.model.ScanHistory;
import com.retailai.repository.ScanHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerScanHistoryService {

    private final ScanHistoryRepository scanHistoryRepository;

    public CustomerScanHistoryService(ScanHistoryRepository scanHistoryRepository) {
        this.scanHistoryRepository = scanHistoryRepository;
    }

    @Transactional
    public ScanHistory saveScan(
            String userId,
            String tenantId,
            String storeId,
            String userEmail,
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName,
            String rfid,
            String itemName,
            String brand,
            String category,
            String color,
            Double price,
            String imageUrl,
            String vibe,
            Integer matchScore
    ) {
        String safeUserId = clean(userId);
        String safeTenantId = clean(tenantId);
        String safeStoreId = clean(storeId);
        String safeUserEmail = clean(userEmail);

        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required to save scan history.");
        String safeRetailerName = clean(retailerName);
        String safeStoreCode = requireUpper(storeCode, "Store code is required to save scan history.");
        String safeStoreName = clean(storeName);

        String safeRfid = requireUpper(rfid, "RFID is required to save scan history.");
        String safeItemName = clean(itemName);
        String safeBrand = clean(brand);
        String safeCategory = clean(category);
        String safeColor = clean(color);
        Double safePrice = normalizePrice(price);
        String safeImageUrl = clean(imageUrl);
        String safeVibe = normalizeVibe(vibe);
        Integer safeMatchScore = normalizeScore(matchScore);

        deleteExistingScanForSameRfid(
                safeUserId,
                safeTenantId,
                safeRetailerKey,
                safeStoreCode,
                safeRfid
        );

        ScanHistory scan = new ScanHistory();

        scan.setUserId(safeUserId);
        scan.setTenantId(safeTenantId);
        scan.setStoreId(safeStoreId);
        scan.setUserEmail(safeUserEmail);

        scan.setRetailerKey(safeRetailerKey);
        scan.setRetailerName(safeRetailerName);
        scan.setStoreCode(safeStoreCode);
        scan.setStoreName(safeStoreName);

        scan.setRfid(safeRfid);
        scan.setItemName(safeItemName);
        scan.setBrand(safeBrand);
        scan.setCategory(safeCategory);
        scan.setColor(safeColor);
        scan.setPrice(safePrice);
        scan.setImageUrl(safeImageUrl);
        scan.setVibe(safeVibe);
        scan.setMatchScore(safeMatchScore);

        return scanHistoryRepository.save(scan);
    }

    @Transactional
    public ScanHistoryDTO saveScanAsDTO(
            String userId,
            String tenantId,
            String storeId,
            String userEmail,
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName,
            String rfid,
            String itemName,
            String brand,
            String category,
            String color,
            Double price,
            String imageUrl,
            String vibe,
            Integer matchScore
    ) {
        ScanHistory saved = saveScan(
                userId,
                tenantId,
                storeId,
                userEmail,
                retailerKey,
                retailerName,
                storeCode,
                storeName,
                rfid,
                itemName,
                brand,
                category,
                color,
                price,
                imageUrl,
                vibe,
                matchScore
        );

        return ScanHistoryDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ScanHistoryDTO> getScanHistory(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeUserId = clean(userId);
        String safeTenantId = clean(tenantId);
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        List<ScanHistory> history;

        if (!safeUserId.isBlank()) {
            history = scanHistoryRepository.findTop30ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            );
        } else if (!safeTenantId.isBlank()) {
            history = scanHistoryRepository.findTop30ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            );
        } else {
            history = scanHistoryRepository.findTop30ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeRetailerKey,
                    safeStoreCode
            );
        }

        return history.stream()
                .map(ScanHistoryDTO::fromEntity)
                .toList();
    }

    @Transactional
    public String clearScanHistory(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeUserId = clean(userId);
        String safeTenantId = clean(tenantId);
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        if (!safeUserId.isBlank()) {
            scanHistoryRepository.deleteByUserIdAndRetailerKeyAndStoreCode(
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Scan history cleared.";
        }

        if (!safeTenantId.isBlank()) {
            scanHistoryRepository.deleteByTenantIdAndRetailerKeyAndStoreCode(
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Scan history cleared.";
        }

        scanHistoryRepository.deleteByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );

        return "Scan history cleared.";
    }

    @Transactional
    public String deleteScanHistoryItem(
            Long scanHistoryId,
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        if (scanHistoryId == null) {
            throw new IllegalArgumentException("Scan history id is required.");
        }

        String safeUserId = clean(userId);
        String safeTenantId = clean(tenantId);
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        if (!safeUserId.isBlank()) {
            scanHistoryRepository.deleteByIdAndUserIdAndRetailerKeyAndStoreCode(
                    scanHistoryId,
                    safeUserId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Scan history item removed.";
        }

        if (!safeTenantId.isBlank()) {
            scanHistoryRepository.deleteByIdAndTenantIdAndRetailerKeyAndStoreCode(
                    scanHistoryId,
                    safeTenantId,
                    safeRetailerKey,
                    safeStoreCode
            );

            return "Scan history item removed.";
        }

        scanHistoryRepository.deleteByIdAndRetailerKeyAndStoreCode(
                scanHistoryId,
                safeRetailerKey,
                safeStoreCode
        );

        return "Scan history item removed.";
    }

    @Transactional(readOnly = true)
    public List<ScanHistory> getRecentScansForUserStore(
            String userId,
            String retailerKey,
            String storeCode
    ) {
        String safeUserId = requireText(userId, "User id is required.");
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        return scanHistoryRepository.findTop30ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                safeUserId,
                safeRetailerKey,
                safeStoreCode
        );
    }

    @Transactional(readOnly = true)
    public List<ScanHistory> getRecentScansForTenantStore(
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeTenantId = requireText(tenantId, "Tenant id is required.");
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        return scanHistoryRepository.findTop30ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                safeTenantId,
                safeRetailerKey,
                safeStoreCode
        );
    }

    @Transactional(readOnly = true)
    public List<ScanHistory> getRecentScansForStore(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        return scanHistoryRepository.findTop30ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                safeRetailerKey,
                safeStoreCode
        );
    }

    @Transactional
    public void clearScansForUserStore(
            String userId,
            String retailerKey,
            String storeCode
    ) {
        String safeUserId = requireText(userId, "User id is required.");
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        scanHistoryRepository.deleteByUserIdAndRetailerKeyAndStoreCode(
                safeUserId,
                safeRetailerKey,
                safeStoreCode
        );
    }

    @Transactional
    public void clearScansForTenantStore(
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeTenantId = requireText(tenantId, "Tenant id is required.");
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        scanHistoryRepository.deleteByTenantIdAndRetailerKeyAndStoreCode(
                safeTenantId,
                safeRetailerKey,
                safeStoreCode
        );
    }

    @Transactional
    public void clearScansForStore(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = requireUpper(retailerKey, "Retailer key is required.");
        String safeStoreCode = requireUpper(storeCode, "Store code is required.");

        scanHistoryRepository.deleteByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );
    }

    private void deleteExistingScanForSameRfid(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode,
            String rfid
    ) {
        if (!userId.isBlank()) {
            scanHistoryRepository.deleteByUserIdAndRetailerKeyAndStoreCodeAndRfid(
                    userId,
                    retailerKey,
                    storeCode,
                    rfid
            );

            return;
        }

        if (!tenantId.isBlank()) {
            scanHistoryRepository.deleteByTenantIdAndRetailerKeyAndStoreCodeAndRfid(
                    tenantId,
                    retailerKey,
                    storeCode,
                    rfid
            );

            return;
        }

        scanHistoryRepository.deleteByRetailerKeyAndStoreCodeAndRfid(
                retailerKey,
                storeCode,
                rfid
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String requireText(String value, String message) {
        String cleaned = clean(value);

        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return cleaned;
    }

    private String requireUpper(String value, String message) {
        return requireText(value, message).toUpperCase();
    }

    private String normalizeVibe(String vibe) {
        String cleaned = clean(vibe);
        return cleaned.isBlank() ? "Casual" : cleaned;
    }

    private Double normalizePrice(Double price) {
        if (price == null) {
            return 0.0;
        }

        return Math.max(0.0, price);
    }

    private Integer normalizeScore(Integer score) {
        if (score == null) {
            return 0;
        }

        return Math.max(0, Math.min(100, score));
    }
}
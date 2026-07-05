package com.retailai.repository;

import com.retailai.model.BagItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BagItemRepository extends JpaRepository<BagItem, Long> {

    List<BagItem> findByUserId(String userId);

    List<BagItem> findByUserIdAndRetailerKeyAndStoreCode(
            String userId,
            String retailerKey,
            String storeCode
    );

    List<BagItem> findByTenantIdAndRetailerKeyAndStoreCode(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<BagItem> findByTenantIdAndStoreCode(
            String tenantId,
            String storeCode
    );

    List<BagItem> findByRetailerKeyAndStoreCode(
            String retailerKey,
            String storeCode
    );

    List<BagItem> findTop100ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<BagItem> findByRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    List<BagItem> findTop100ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode
    );

    List<BagItem> findByUserIdAndRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    List<BagItem> findTop100ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<BagItem> findByTenantIdAndRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    long countByRetailerKeyAndStoreCode(
            String retailerKey,
            String storeCode
    );

    long countByRetailerKeyAndStoreCodeAndCreatedAtAfter(
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    long countByUserIdAndRetailerKeyAndStoreCode(
            String userId,
            String retailerKey,
            String storeCode
    );

    long countByUserIdAndRetailerKeyAndStoreCodeAndCreatedAtAfter(
            String userId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    long countByTenantIdAndRetailerKeyAndStoreCode(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    long countByTenantIdAndRetailerKeyAndStoreCodeAndCreatedAtAfter(
            String tenantId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    Optional<BagItem> findByUserIdAndRfidIgnoreCase(
            String userId,
            String rfid
    );

    Optional<BagItem> findByUserIdAndRetailerKeyAndStoreCodeAndRfidIgnoreCase(
            String userId,
            String retailerKey,
            String storeCode,
            String rfid
    );

    Optional<BagItem> findByTenantIdAndRetailerKeyAndStoreCodeAndRfidIgnoreCase(
            String tenantId,
            String retailerKey,
            String storeCode,
            String rfid
    );

    Optional<BagItem> findByTenantIdAndStoreCodeAndRfidIgnoreCase(
            String tenantId,
            String storeCode,
            String rfid
    );

    Optional<BagItem> findByRetailerKeyAndStoreCodeAndRfidIgnoreCase(
            String retailerKey,
            String storeCode,
            String rfid
    );

    Optional<BagItem> findByIdAndUserId(
            Long id,
            String userId
    );

    Optional<BagItem> findByIdAndUserIdAndRetailerKeyAndStoreCode(
            Long id,
            String userId,
            String retailerKey,
            String storeCode
    );

    Optional<BagItem> findByIdAndTenantIdAndRetailerKeyAndStoreCode(
            Long id,
            String tenantId,
            String retailerKey,
            String storeCode
    );

    Optional<BagItem> findByIdAndTenantIdAndStoreCode(
            Long id,
            String tenantId,
            String storeCode
    );

    Optional<BagItem> findByIdAndRetailerKeyAndStoreCode(
            Long id,
            String retailerKey,
            String storeCode
    );

    @Transactional
    void deleteByUserId(String userId);

    @Transactional
    void deleteByUserIdAndRetailerKeyAndStoreCode(
            String userId,
            String retailerKey,
            String storeCode
    );

    @Transactional
    void deleteByTenantIdAndRetailerKeyAndStoreCode(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    @Transactional
    void deleteByTenantIdAndStoreCode(
            String tenantId,
            String storeCode
    );

    @Transactional
    void deleteByRetailerKeyAndStoreCode(
            String retailerKey,
            String storeCode
    );
}
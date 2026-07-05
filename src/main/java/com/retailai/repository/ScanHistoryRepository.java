package com.retailai.repository;

import com.retailai.model.ScanHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScanHistoryRepository extends JpaRepository<ScanHistory, Long> {

    List<ScanHistory> findTop30ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop50ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop100ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode,
            Pageable pageable
    );

    Optional<ScanHistory> findByIdAndUserIdAndRetailerKeyAndStoreCode(
            Long id,
            String userId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop30ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop50ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop100ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode,
            Pageable pageable
    );

    Optional<ScanHistory> findByIdAndTenantIdAndRetailerKeyAndStoreCode(
            Long id,
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop30ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop50ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findTop100ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<ScanHistory> findByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            Pageable pageable
    );

    Optional<ScanHistory> findByIdAndRetailerKeyAndStoreCode(
            Long id,
            String retailerKey,
            String storeCode
    );

    long countByUserIdAndRetailerKeyAndStoreCode(
            String userId,
            String retailerKey,
            String storeCode
    );

    long countByTenantIdAndRetailerKeyAndStoreCode(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    long countByRetailerKeyAndStoreCode(
            String retailerKey,
            String storeCode
    );

    long countByUserIdAndRetailerKeyAndStoreCodeAndCreatedAtAfter(
            String userId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    long countByTenantIdAndRetailerKeyAndStoreCodeAndCreatedAtAfter(
            String tenantId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    long countByRetailerKeyAndStoreCodeAndCreatedAtAfter(
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    List<ScanHistory> findByUserIdAndRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    List<ScanHistory> findByTenantIdAndRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

    List<ScanHistory> findByRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            LocalDateTime createdAt
    );

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
    void deleteByRetailerKeyAndStoreCode(
            String retailerKey,
            String storeCode
    );

    @Transactional
    void deleteByUserIdAndRetailerKeyAndStoreCodeAndRfid(
            String userId,
            String retailerKey,
            String storeCode,
            String rfid
    );

    @Transactional
    void deleteByTenantIdAndRetailerKeyAndStoreCodeAndRfid(
            String tenantId,
            String retailerKey,
            String storeCode,
            String rfid
    );

    @Transactional
    void deleteByRetailerKeyAndStoreCodeAndRfid(
            String retailerKey,
            String storeCode,
            String rfid
    );

    @Transactional
    void deleteByIdAndUserIdAndRetailerKeyAndStoreCode(
            Long id,
            String userId,
            String retailerKey,
            String storeCode
    );

    @Transactional
    void deleteByIdAndTenantIdAndRetailerKeyAndStoreCode(
            Long id,
            String tenantId,
            String retailerKey,
            String storeCode
    );

    @Transactional
    void deleteByIdAndRetailerKeyAndStoreCode(
            Long id,
            String retailerKey,
            String storeCode
    );
}
package com.retailai.repository;

import com.retailai.model.RetailOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RetailOrderRepository extends JpaRepository<RetailOrder, Long> {

    Optional<RetailOrder> findByOrderNumber(String orderNumber);

    Optional<RetailOrder> findByOrderNumberAndRetailerKeyAndStoreCode(
            String orderNumber,
            String retailerKey,
            String storeCode
    );

    boolean existsByOrderNumber(String orderNumber);

    List<RetailOrder> findByUserIdOrderByCreatedAtDesc(String userId);

    List<RetailOrder> findByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findTop20ByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String userId,
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findByTenantIdAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String storeCode
    );

    List<RetailOrder> findByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findTop20ByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findTop20ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findByRetailerKeyAndStoreCodeAndCreatedAtBetweenOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByRetailerKeyAndStoreCodeAndCreatedAtBetween(
            String retailerKey,
            String storeCode,
            LocalDateTime start,
            LocalDateTime end
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
}
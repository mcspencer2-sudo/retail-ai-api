package com.retailai.repository;

import com.retailai.model.RetailOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetailOrderRepository extends JpaRepository<RetailOrder, Long> {

    Optional<RetailOrder> findByOrderNumber(String orderNumber);

    Optional<RetailOrder> findByOrderNumberAndRetailerKeyAndStoreCode(
            String orderNumber,
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findByUserIdOrderByCreatedAtDesc(String userId);

    List<RetailOrder> findByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<RetailOrder> findByTenantIdAndStoreCodeOrderByCreatedAtDesc(
            String tenantId,
            String storeCode
    );
}
package com.retailai.repository;

import com.retailai.model.BagItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BagItemRepository extends JpaRepository<BagItem, Long> {

    List<BagItem> findByUserId(String userId);

    List<BagItem> findByTenantIdAndStoreCode(String tenantId, String storeCode);

    List<BagItem> findByRetailerKeyAndStoreCode(String retailerKey, String storeCode);

    Optional<BagItem> findByUserIdAndRfidIgnoreCase(String userId, String rfid);

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

    Optional<BagItem> findByIdAndUserId(Long id, String userId);

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

    void deleteByUserId(String userId);

    void deleteByTenantIdAndStoreCode(String tenantId, String storeCode);

    void deleteByRetailerKeyAndStoreCode(String retailerKey, String storeCode);
}
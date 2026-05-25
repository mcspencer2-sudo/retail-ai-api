package com.retailai.repository;

import com.retailai.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreCode(String storeCode);

    Optional<Store> findFirstByTenantIdAndActiveTrue(Long tenantId);

    Optional<Store> findFirstByTenantIdAndRetailerKeyAndActiveTrue(
            Long tenantId,
            String retailerKey
    );

    List<Store> findByTenantId(Long tenantId);

    List<Store> findByTenantIdAndActiveTrue(Long tenantId);

    List<Store> findByRetailerKey(String retailerKey);

    List<Store> findByRetailerKeyAndActiveTrue(String retailerKey);

    boolean existsByStoreCode(String storeCode);

    boolean existsByRetailerKey(String retailerKey);

    boolean existsByTenantIdAndStoreCode(Long tenantId, String storeCode);

    boolean existsByTenantIdAndRetailerKey(Long tenantId, String retailerKey);
}
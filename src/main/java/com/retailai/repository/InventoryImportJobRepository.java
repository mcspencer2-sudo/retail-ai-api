package com.retailai.repository;

import com.retailai.model.InventoryImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryImportJobRepository extends JpaRepository<InventoryImportJob, Long> {

    Optional<InventoryImportJob> findByJobId(String jobId);

    List<InventoryImportJob> findTop20ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<InventoryImportJob> findTop20ByRetailerKeyOrderByCreatedAtDesc(String retailerKey);

    List<InventoryImportJob> findTop20ByOrderByCreatedAtDesc();
}
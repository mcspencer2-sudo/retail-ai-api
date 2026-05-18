package com.retailai.repository;

import com.retailai.model.InventoryImportLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryImportLogRepository extends JpaRepository<InventoryImportLog, Long> {

    List<InventoryImportLog> findTop10ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<InventoryImportLog> findTop10ByRetailerKeyOrderByCreatedAtDesc(String retailerKey);

    List<InventoryImportLog> findTop10ByOrderByCreatedAtDesc();

}
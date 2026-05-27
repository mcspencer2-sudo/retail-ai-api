package com.retailai.repository;

import com.retailai.model.RetailOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetailOrderItemRepository extends JpaRepository<RetailOrderItem, Long> {

    List<RetailOrderItem> findByRetailerKeyAndStoreCode(String retailerKey, String storeCode);

    List<RetailOrderItem> findByRfidAndRetailerKeyAndStoreCode(
            String rfid,
            String retailerKey,
            String storeCode
    );
}
package com.retailai.repository;

import com.retailai.model.TrendEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TrendEventRepository extends JpaRepository<TrendEvent, Long> {

    List<TrendEvent> findByEventTypeIgnoreCase(String eventType);

    List<TrendEvent> findByRetailerNameIgnoreCase(String retailerName);

    List<TrendEvent> findByEventTypeIgnoreCaseAndRetailerNameIgnoreCase(
            String eventType,
            String retailerName
    );

    List<TrendEvent> findByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<TrendEvent> findByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndEventTypeIgnoreCaseOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            String eventType
    );

    List<TrendEvent> findTop20ByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode
    );

    List<TrendEvent> findTop20ByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndEventTypeIgnoreCaseOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            String eventType
    );

    List<TrendEvent> findByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            LocalDateTime start,
            LocalDateTime end
    );

    List<TrendEvent> findByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndEventTypeIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            String retailerKey,
            String storeCode,
            String eventType,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndEventTypeIgnoreCase(
            String retailerKey,
            String storeCode,
            String eventType
    );

    long countByRetailerKeyIgnoreCaseAndStoreCodeIgnoreCaseAndEventTypeIgnoreCaseAndCreatedAtBetween(
            String retailerKey,
            String storeCode,
            String eventType,
            LocalDateTime start,
            LocalDateTime end
    );
}
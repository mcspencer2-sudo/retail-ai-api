package com.retailai.repository;

import com.retailai.model.TrendEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrendEventRepository extends JpaRepository<TrendEvent, Long> {
    List<TrendEvent> findByEventTypeIgnoreCase(String eventType);
    List<TrendEvent> findByRetailerNameIgnoreCase(String retailerName);
    List<TrendEvent> findByEventTypeIgnoreCaseAndRetailerNameIgnoreCase(String eventType, String retailerName);
}
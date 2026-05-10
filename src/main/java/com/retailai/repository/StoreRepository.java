package com.retailai.repository;

import com.retailai.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByTenantId(Long tenantId);
}
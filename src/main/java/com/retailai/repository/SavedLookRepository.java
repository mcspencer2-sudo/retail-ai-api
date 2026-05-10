package com.retailai.repository;

import com.retailai.model.SavedLook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedLookRepository extends JpaRepository<SavedLook, Long> {

    List<SavedLook> findByTenantIdAndUserEmailOrderBySavedAtDesc(Long tenantId, String userEmail);

    Optional<SavedLook> findByIdAndTenantIdAndUserEmail(Long id, Long tenantId, String userEmail);
}
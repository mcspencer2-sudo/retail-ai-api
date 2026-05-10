package com.retailai.repository;

import com.retailai.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByEmail(String email);
    Optional<Tenant> findBySlug(String slug);
}
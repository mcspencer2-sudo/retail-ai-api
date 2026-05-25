package com.retailai.repository;

import com.retailai.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findByEmail(String email);

    Optional<Tenant> findBySlugAndActiveTrue(String slug);

    Optional<Tenant> findByEmailAndActiveTrue(String email);

    List<Tenant> findByActiveTrue();

    boolean existsBySlug(String slug);

    boolean existsByEmail(String email);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
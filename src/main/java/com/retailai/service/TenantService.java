package com.retailai.service;

import com.retailai.model.Tenant;
import com.retailai.repository.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant getTenantOrThrow(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
    }

    public Tenant getTenantBySlugOrThrow(String slug) {
        String normalizedSlug = normalizeSlug(slug);

        return tenantRepository.findBySlug(normalizedSlug)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
    }

    public Tenant createTenant(String businessName, String slug, String email) {
        String normalizedBusinessName = safe(businessName);
        String normalizedSlug = normalizeSlug(slug);
        String normalizedEmail = safeLower(email);

        if (normalizedBusinessName.isBlank()) {
            throw new IllegalArgumentException("Business name is required.");
        }

        if (normalizedSlug.isBlank()) {
            throw new IllegalArgumentException("Business slug is required.");
        }

        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Business email is required.");
        }

        if (tenantRepository.findBySlug(normalizedSlug).isPresent()) {
            throw new IllegalArgumentException("Business slug already exists.");
        }

        Tenant tenant = new Tenant();
        tenant.setBusinessName(normalizedBusinessName);
        tenant.setSlug(normalizedSlug);
        tenant.setEmail(normalizedEmail);
        tenant.setPlan("STARTER");
        tenant.setActive(true);

        return tenantRepository.save(tenant);
    }

    public Tenant deactivateTenant(Long tenantId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setActive(false);
        return tenantRepository.save(tenant);
    }

    public Tenant activateTenant(Long tenantId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setActive(true);
        return tenantRepository.save(tenant);
    }

    private String normalizeSlug(String value) {
        return safe(value)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String safeLower(String value) {
        return safe(value).toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
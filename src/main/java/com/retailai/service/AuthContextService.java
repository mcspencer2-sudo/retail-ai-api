package com.retailai.service;

import com.retailai.model.Store;
import com.retailai.model.Tenant;
import com.retailai.repository.StoreRepository;
import com.retailai.repository.TenantRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthContextService {

    private final JwtService jwtService;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;

    public AuthContextService(
            JwtService jwtService,
            TenantRepository tenantRepository,
            StoreRepository storeRepository
    ) {
        this.jwtService = jwtService;
        this.tenantRepository = tenantRepository;
        this.storeRepository = storeRepository;
    }

    public AuthContext getAuthContext(HttpServletRequest request) {
        String token = extractBearerToken(request);

        if (!jwtService.isTokenValid(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        Claims claims = jwtService.extractClaims(token);

        Long tenantId = getLongClaim(claims, "tenantId");
        Long userId = getLongClaim(claims, "userId");
        Long storeId = getLongClaim(claims, "storeId");

        String email = claims.getSubject();
        String role = getStringClaim(claims, "role");
        String retailerKey = getStringClaim(claims, "retailerKey");
        String storeCode = getStringClaim(claims, "storeCode");

        if (tenantId == null) {
            throw new RuntimeException("Token is missing tenant context");
        }

        if (storeId == null && storeCode.isBlank()) {
            throw new RuntimeException("Token is missing store context");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (!tenant.isActive()) {
            throw new RuntimeException("Tenant account is inactive");
        }

        Store store = storeId != null
                ? storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"))
                : storeRepository.findByStoreCode(storeCode)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        if (!tenantId.equals(store.getTenantId())) {
            throw new RuntimeException("Store does not belong to tenant");
        }

        if (!store.isActive()) {
            throw new RuntimeException("Store is inactive");
        }

        if (retailerKey.isBlank()) {
            retailerKey = safe(store.getRetailerKey());
        }

        if (storeCode.isBlank()) {
            storeCode = safe(store.getStoreCode());
        }

        return new AuthContext(
                userId,
                tenantId,
                store.getId(),
                email,
                role,
                retailerKey,
                storeCode,
                tenant,
                store
        );
    }

    public String extractBearerToken(HttpServletRequest request) {
        if (request == null) {
            throw new RuntimeException("Request is required");
        }

        String header = request.getHeader("Authorization");

        if (header == null || header.isBlank()) {
            throw new RuntimeException("Authorization header is required");
        }

        if (!header.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header must use Bearer token");
        }

        String token = header.substring(7).trim();

        if (token.isBlank()) {
            throw new RuntimeException("Bearer token is missing");
        }

        return token;
    }

    private Long getLongClaim(Claims claims, String name) {
        Object value = claims.get(name);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private String getStringClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record AuthContext(
            Long userId,
            Long tenantId,
            Long storeId,
            String email,
            String role,
            String retailerKey,
            String storeCode,
            Tenant tenant,
            Store store
    ) {
        public boolean isOwner() {
            String normalizedRole = role == null ? "" : role.trim().replace("ROLE_", "").toUpperCase();
            return "OWNER".equals(normalizedRole);
        }

        public boolean isManager() {
            String normalizedRole = role == null ? "" : role.trim().replace("ROLE_", "").toUpperCase();
            return "MANAGER".equals(normalizedRole);
        }

        public boolean isStaff() {
            String normalizedRole = role == null ? "" : role.trim().replace("ROLE_", "").toUpperCase();
            return "STAFF".equals(normalizedRole);
        }

        public boolean canManageInventory() {
            return isOwner() || isManager();
        }
    }
}
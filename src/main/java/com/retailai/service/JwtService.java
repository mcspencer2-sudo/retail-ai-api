package com.retailai.service;

import com.retailai.model.AppUser;
import com.retailai.model.Store;
import com.retailai.model.Tenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${retailai.jwt.secret:mcspencer-super-secret-jwt-key-2026-secure-key-12345}")
    private String jwtSecret;

    @Value("${retailai.jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${retailai.jwt.debug:false}")
    private boolean jwtDebug;

    private SecretKey key;

    @PostConstruct
    public void init() {
        String cleanSecret = safe(jwtSecret);

        if (cleanSecret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters for HS256.");
        }

        this.key = Keys.hmacShaKeyFor(cleanSecret.getBytes(StandardCharsets.UTF_8));

        System.out.println("JWT SERVICE ACTIVE");
        System.out.println("JWT secret length: " + cleanSecret.length());
        System.out.println("JWT secret prefix: " + cleanSecret.substring(0, Math.min(8, cleanSecret.length())));
    }

    public String generateToken(String email, String role) {
        String normalizedEmail = safeLower(email);

        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        String normalizedRole = normalizeRole(role);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", normalizedEmail);

        if (!normalizedRole.isBlank()) {
            claims.put("role", normalizedRole);
        }

        return buildToken(normalizedEmail, claims);
    }

    public String generateToken(AppUser user, Tenant tenant, Store store) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String userEmail = safeLower(user.getEmail());

        if (userEmail.isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or blank");
        }

        String normalizedRole = normalizeRole(user.getRole());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", userEmail);
        claims.put("fullName", safe(user.getFullName()));
        claims.put("role", normalizedRole);
        claims.put("tenantId", user.getTenantId());
        claims.put("userActive", user.isActive());

        if (tenant != null) {
            claims.put("businessName", safe(tenant.getBusinessName()));
            claims.put("tenantSlug", safe(tenant.getSlug()));
            claims.put("tenantEmail", safeLower(tenant.getEmail()));
            claims.put("plan", safeUpper(tenant.getPlan()));
            claims.put("tenantActive", tenant.isActive());
        }

        if (store != null) {
            claims.put("storeId", store.getId());
            claims.put("storeCode", safeUpper(store.getStoreCode()));
            claims.put("storeName", safe(store.getStoreName()));
            claims.put("location", safe(store.getLocation()));
            claims.put("retailerKey", safeUpper(store.getRetailerKey()));
            claims.put("storeActive", store.isActive());
        }

        return buildToken(userEmail, claims);
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);

        String subject = safeLower(claims.getSubject());

        if (!subject.isBlank()) {
            return subject;
        }

        String emailClaim = safeLower(String.valueOf(claims.get("email")));
        return emailClaim.isBlank() || "null".equals(emailClaim) ? null : emailClaim;
    }

    public String extractRole(String token) {
        String role = extractStringClaim(token, "role");
        return normalizeRole(role);
    }

    public Long extractUserId(String token) {
        return extractLongClaim(token, "userId");
    }

    public Long extractTenantId(String token) {
        return extractLongClaim(token, "tenantId");
    }

    public Long extractStoreId(String token) {
        return extractLongClaim(token, "storeId");
    }

    public String extractRetailerKey(String token) {
        return extractStringClaim(token, "retailerKey");
    }

    public String extractStoreCode(String token) {
        return extractStringClaim(token, "storeCode");
    }

    public String extractStoreName(String token) {
        return extractStringClaim(token, "storeName");
    }

    public String extractBusinessName(String token) {
        return extractStringClaim(token, "businessName");
    }

    public String extractTenantSlug(String token) {
        return extractStringClaim(token, "tenantSlug");
    }

    public String extractPlan(String token) {
        return extractStringClaim(token, "plan");
    }

    public boolean extractTenantActive(String token) {
        return extractBooleanClaim(token, "tenantActive");
    }

    public boolean extractStoreActive(String token) {
        return extractBooleanClaim(token, "storeActive");
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String subject = safe(claims.getSubject());
            Date expiration = claims.getExpiration();

            return !subject.isBlank()
                    && expiration != null
                    && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            if (jwtDebug) {
                System.out.println("JWT validation failed: " + e.getMessage());
            }

            return false;
        }
    }

    public Claims extractClaims(String token) {
        return extractAllClaims(token);
    }

    private String buildToken(String subject, Map<String, Object> claims) {
        ensureKeyReady();

        String normalizedSubject = safeLower(subject);

        if (normalizedSubject.isBlank()) {
            throw new IllegalArgumentException("Token subject cannot be null or blank");
        }

        long safeExpirationMs = expirationMs <= 0 ? 86_400_000L : expirationMs;

        Date now = new Date();
        Date expiry = new Date(now.getTime() + safeExpirationMs);

        Map<String, Object> safeClaims = claims == null
                ? new HashMap<>()
                : new HashMap<>(claims);

        safeClaims.put("email", normalizedSubject);

        return Jwts.builder()
                .setClaims(safeClaims)
                .setSubject(normalizedSubject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        ensureKeyReady();

        String cleanToken = cleanToken(token);

        if (cleanToken.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken)
                .getBody();
    }

    private String extractStringClaim(String token, String claimName) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get(claimName);

        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();

        return text.isBlank() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private Long extractLongClaim(String token, String claimName) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get(claimName);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            String text = String.valueOf(value).trim();

            if (text.isBlank() || "null".equalsIgnoreCase(text)) {
                return null;
            }

            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean extractBooleanClaim(String token, String claimName) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get(claimName);

        if (value == null) {
            return false;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        return Boolean.parseBoolean(String.valueOf(value));
    }

    private void ensureKeyReady() {
        if (key == null) {
            init();
        }
    }

    private String cleanToken(String token) {
        String cleaned = safe(token);

        if (cleaned.startsWith("Bearer ")) {
            return cleaned.substring(7).trim();
        }

        return cleaned;
    }

    private String normalizeRole(String role) {
        String normalized = safeUpper(role).replace("ROLE_", "").trim();

        if (normalized.isBlank()) {
            return "";
        }

        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeLower(String value) {
        return safe(value).toLowerCase();
    }

    private String safeUpper(String value) {
        return safe(value).toUpperCase();
    }
}
package com.retailai.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final long EXPIRATION_MS = 1000L * 60L * 60L * 24L;

    /*
     * Keep this long enough for HS256.
     * Later, move it to application.properties or an environment variable.
     */
    private static final String SECRET =
            "universal-stylist-local-dev-secret-key-change-before-production-2026";

    public String generateToken(
            String email,
            String role,
            String userId,
            String tenantId,
            String storeId,
            String businessName,
            String tenantSlug,
            String plan,
            String retailerKey,
            String storeCode,
            String storeName,
            String location,
            boolean canManageInventory
    ) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("email", safe(email));
        claims.put("role", safe(role));
        claims.put("userId", safe(userId));
        claims.put("tenantId", safe(tenantId));
        claims.put("storeId", safe(storeId));
        claims.put("businessName", safe(businessName));
        claims.put("tenantSlug", safe(tenantSlug));
        claims.put("plan", safe(plan));
        claims.put("retailerKey", safe(retailerKey));
        claims.put("storeCode", safe(storeCode));
        claims.put("storeName", safe(storeName));
        claims.put("location", safe(location));
        claims.put("canManageInventory", canManageInventory);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(safe(email))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, getSigningKey())
                .compact();
    }

    public String generateToken(String email, String role) {
        return generateToken(
                email,
                role,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                false
        );
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(cleanToken(token))
                .getBody();
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return firstNonBlank(
                asString(claims.get("email")),
                claims.getSubject()
        );
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("role"));
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("userId"));
    }

    public String extractTenantId(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("tenantId"));
    }

    public String extractStoreId(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("storeId"));
    }

    public String extractRetailerKey(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("retailerKey"));
    }

    public String extractStoreCode(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("storeCode"));
    }

    public String extractStoreName(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("storeName"));
    }

    public String extractLocation(String token) {
        Claims claims = extractAllClaims(token);
        return asString(claims.get("location"));
    }

    public boolean extractCanManageInventory(String token) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get("canManageInventory");

        if (value instanceof Boolean bool) {
            return bool;
        }

        return Boolean.parseBoolean(asString(value));
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();

            return expiration != null && expiration.after(new Date());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        return isTokenValid(token);
    }

    private byte[] getSigningKey() {
        return SECRET.getBytes(StandardCharsets.UTF_8);
    }

    private String cleanToken(String token) {
        String cleaned = safe(token);

        if (cleaned.startsWith("Bearer ")) {
            return cleaned.substring(7).trim();
        }

        return cleaned;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }

        if (second != null && !second.isBlank()) {
            return second.trim();
        }

        return "";
    }
}
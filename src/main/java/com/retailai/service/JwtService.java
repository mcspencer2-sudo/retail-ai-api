package com.retailai.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String SECRET =
            "mcspencer-super-secret-jwt-key-2026-secure-key-12345";

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000;

    private final SecretKey key =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String generateToken(String email, String role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        String normalizedRole = role == null ? "" : role.trim().replace("ROLE_", "");

        Map<String, Object> claims = new HashMap<>();
        if (!normalizedRole.isBlank()) {
            claims.put("role", normalizedRole);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email.trim())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get("role", String.class);
        return role == null ? null : role.trim().replace("ROLE_", "");
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String subject = claims.getSubject();
            Date expiration = claims.getExpiration();

            return subject != null
                    && !subject.isBlank()
                    && expiration != null
                    && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token.trim())
                .getBody();
    }
}
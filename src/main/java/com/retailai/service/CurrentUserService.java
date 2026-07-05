package com.retailai.service;

import com.retailai.model.AppUser;
import com.retailai.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class CurrentUserService {

    private final HttpServletRequest request;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;

    public CurrentUserService(
            HttpServletRequest request,
            JwtService jwtService,
            AppUserRepository appUserRepository
    ) {
        this.request = request;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
    }

    public AppUser getCurrentUser() {
        String token = extractBearerToken();

        if (token.isBlank() || !jwtService.isTokenValid(token)) {
            throw new RuntimeException("Invalid or missing token");
        }

        String email = clean(jwtService.extractEmail(token));

        if (email.isBlank()) {
            throw new RuntimeException("Token does not contain a valid email");
        }

        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
    }

    public Optional<AppUser> getCurrentUserOptional() {
        try {
            String token = extractBearerToken();

            if (token.isBlank() || !jwtService.isTokenValid(token)) {
                return Optional.empty();
            }

            String email = clean(jwtService.extractEmail(token));

            if (email.isBlank()) {
                return Optional.empty();
            }

            return appUserRepository.findByEmail(email);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Long getCurrentTenantId() {
        return getCurrentUserOptional()
                .map(AppUser::getTenantId)
                .orElse(null);
    }

    public String getCurrentUserEmail() {
        String token = extractBearerToken();

        if (!token.isBlank() && jwtService.isTokenValid(token)) {
            String email = clean(jwtService.extractEmail(token));

            if (!email.isBlank()) {
                return email;
            }
        }

        return getCurrentUserOptional()
                .map(AppUser::getEmail)
                .map(this::clean)
                .orElse("");
    }

    public String getRetailerKey() {
        String fromHeader = firstNonBlank(
                request.getHeader("X-Retailer-Key"),
                request.getHeader("X-Retailer"),
                request.getHeader("retailerKey")
        );

        if (!fromHeader.isBlank()) {
            return cleanUpper(fromHeader);
        }

        String fromParam = firstNonBlank(
                request.getParameter("retailerKey"),
                request.getParameter("retailer")
        );

        if (!fromParam.isBlank()) {
            return cleanUpper(fromParam);
        }

        String tenantId = getCurrentTenantId() == null ? "" : String.valueOf(getCurrentTenantId());

        if (!tenantId.isBlank()) {
            return cleanUpper(tenantId);
        }

        return "";
    }

    public String getStoreCode() {
        String fromHeader = firstNonBlank(
                request.getHeader("X-Store-Code"),
                request.getHeader("X-Store"),
                request.getHeader("storeCode")
        );

        if (!fromHeader.isBlank()) {
            return cleanUpper(fromHeader);
        }

        String fromParam = firstNonBlank(
                request.getParameter("storeCode"),
                request.getParameter("store")
        );

        if (!fromParam.isBlank()) {
            return cleanUpper(fromParam);
        }

        return "";
    }

    public boolean isAuthenticated() {
        String token = extractBearerToken();
        return !token.isBlank() && jwtService.isTokenValid(token);
    }

    private String extractBearerToken() {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            return "";
        }

        if (!authHeader.startsWith("Bearer ")) {
            return "";
        }

        return authHeader.substring(7).trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            String cleaned = clean(value);

            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }

        return "";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanUpper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }
}
package com.retailai.service;

import com.retailai.model.AppUser;
import com.retailai.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final HttpServletRequest request;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;

    public CurrentUserService(HttpServletRequest request,
                              JwtService jwtService,
                              AppUserRepository appUserRepository) {
        this.request = request;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
    }

    public AppUser getCurrentUser() {
        String token = extractBearerToken();

        if (token.isBlank() || !jwtService.isTokenValid(token)) {
            throw new RuntimeException("Invalid or missing token");
        }

        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Token does not contain a valid email");
        }

        return appUserRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public Long getCurrentTenantId() {
        return getCurrentUser().getTenantId();
    }

    public String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
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
}
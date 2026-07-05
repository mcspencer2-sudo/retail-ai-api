package com.retailai.controller;

import com.retailai.customer.CustomerPreference;
import com.retailai.dto.CustomerPreferenceRequest;
import com.retailai.service.AuthContextService;
import com.retailai.service.CustomerPreferenceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/preferences")
public class CustomerPreferenceController {

    private final CustomerPreferenceService customerPreferenceService;
    private final AuthContextService authContextService;

    public CustomerPreferenceController(
            CustomerPreferenceService customerPreferenceService,
            AuthContextService authContextService
    ) {
        this.customerPreferenceService = customerPreferenceService;
        this.authContextService = authContextService;
    }

    @GetMapping
    public ResponseEntity<CustomerPreference> getPreferences(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            CustomerPreference preference = customerPreferenceService.getPreferences(
                    toText(auth.userId()),
                    normalizeOptional(auth.email()),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(preference);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (RuntimeException e) {
            return internalServerError(e);
        }
    }

    @PutMapping
    public ResponseEntity<CustomerPreference> savePreferences(
            HttpServletRequest request,
            @RequestBody CustomerPreferenceRequest preferenceRequest
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            CustomerPreference saved = customerPreferenceService.savePreferences(
                    toText(auth.userId()),
                    normalizeOptional(auth.email()),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase(),
                    preferenceRequest == null ? new CustomerPreferenceRequest() : preferenceRequest
            );

            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (RuntimeException e) {
            return internalServerError(e);
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> resetPreferences(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            customerPreferenceService.resetPreferences(
                    toText(auth.userId()),
                    normalizeOptional(auth.email()),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    e
            );
        } catch (SecurityException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    e.getMessage(),
                    e
            );
        } catch (RuntimeException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage(),
                    e
            );
        }
    }

    private AuthContextService.AuthContext requireAuthenticated(HttpServletRequest request) {
        return authContextService.getAuthContext(request);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim();
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private ResponseEntity<CustomerPreference> badRequest(Exception e) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                e
        );
    }

    private ResponseEntity<CustomerPreference> forbidden(Exception e) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN,
                e.getMessage(),
                e
        );
    }

    private ResponseEntity<CustomerPreference> internalServerError(Exception e) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.getMessage(),
                e
        );
    }
}
package com.retailai.service;

import com.retailai.model.AppUser;
import com.retailai.model.LoginRequest;
import com.retailai.model.SignupRequest;
import com.retailai.model.Store;
import com.retailai.model.Tenant;
import com.retailai.repository.AppUserRepository;
import com.retailai.repository.StoreRepository;
import com.retailai.repository.TenantRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaaSAuthService {

    private static final String DEFAULT_PLAN = "STARTER";
    private static final String OWNER_ROLE = "OWNER";

    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SaaSAuthService(
            TenantRepository tenantRepository,
            StoreRepository storeRepository,
            AppUserRepository appUserRepository,
            JwtService jwtService
    ) {
        this.tenantRepository = tenantRepository;
        this.storeRepository = storeRepository;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String signup(SignupRequest request) {
        if (request == null) {
            throw new RuntimeException("Signup request is required");
        }

        String businessName = safe(request.getBusinessName());
        String slug = normalizeSlug(request.getSlug());
        String tenantEmail = safeLower(request.getTenantEmail());
        String storeName = safe(request.getStoreName());
        String location = safe(request.getLocation());
        String retailerKey = safeUpper(request.getRetailerKey());
        String fullName = safe(request.getFullName());
        String userEmail = safeLower(request.getUserEmail());
        String password = safe(request.getPassword());

        validateSignupFields(
                businessName,
                slug,
                tenantEmail,
                storeName,
                location,
                retailerKey,
                fullName,
                userEmail,
                password
        );

        validateSignupUniqueness(
                slug,
                tenantEmail,
                retailerKey,
                storeName,
                userEmail
        );

        Tenant tenant = createTenant(
                businessName,
                slug,
                tenantEmail
        );

        Store store = createDefaultStore(
                tenant,
                retailerKey,
                storeName,
                location
        );

        createOwnerUser(
                tenant,
                fullName,
                userEmail,
                password
        );

        return "Signup successful for " + tenant.getBusinessName()
                + " using store " + store.getStoreCode();
    }

    public String login(LoginRequest request) {
        if (request == null) {
            throw new RuntimeException("Login request is required");
        }

        String email = safeLower(request.getEmail());
        String password = safe(request.getPassword());

        if (email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (password.isBlank()) {
            throw new RuntimeException("Password is required");
        }

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isUserActive(user)) {
            throw new RuntimeException("User account is inactive");
        }

        boolean passwordMatches = passwordEncoder.matches(
                password,
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid password");
        }

        Long tenantId = user.getTenantId();

        if (tenantId == null) {
            throw new RuntimeException("User is not assigned to a tenant");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found for user"));

        if (!tenant.isActive()) {
            throw new RuntimeException("Tenant account is inactive");
        }

        Store store = findDefaultStoreForTenant(tenantId);

        if (store == null) {
            throw new RuntimeException("No active store found for tenant");
        }

        return jwtService.generateToken(user, tenant, store);
    }

    private void validateSignupFields(
            String businessName,
            String slug,
            String tenantEmail,
            String storeName,
            String location,
            String retailerKey,
            String fullName,
            String userEmail,
            String password
    ) {
        if (businessName.isBlank()) {
            throw new RuntimeException("Business name is required");
        }

        if (slug.isBlank()) {
            throw new RuntimeException("Business slug is required");
        }

        if (!slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new RuntimeException("Business slug can only contain lowercase letters, numbers, and hyphens");
        }

        if (tenantEmail.isBlank()) {
            throw new RuntimeException("Business email is required");
        }

        if (!isValidEmail(tenantEmail)) {
            throw new RuntimeException("Business email is invalid");
        }

        if (storeName.isBlank()) {
            throw new RuntimeException("Store name is required");
        }

        if (location.isBlank()) {
            throw new RuntimeException("Location is required");
        }

        if (retailerKey.isBlank()) {
            throw new RuntimeException("Retailer key is required");
        }

        if (!retailerKey.matches("^[A-Z0-9_-]{3,40}$")) {
            throw new RuntimeException("Retailer key can only contain uppercase letters, numbers, underscores, and hyphens");
        }

        if (fullName.isBlank()) {
            throw new RuntimeException("Full name is required");
        }

        if (userEmail.isBlank()) {
            throw new RuntimeException("User email is required");
        }

        if (!isValidEmail(userEmail)) {
            throw new RuntimeException("User email is invalid");
        }

        if (password.isBlank()) {
            throw new RuntimeException("Password is required");
        }

        if (password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
    }

    private void validateSignupUniqueness(
            String slug,
            String tenantEmail,
            String retailerKey,
            String storeName,
            String userEmail
    ) {
        if (tenantRepository.existsBySlug(slug)) {
            throw new RuntimeException("Business slug already exists");
        }

        if (tenantRepository.existsByEmail(tenantEmail)) {
            throw new RuntimeException("Business email already exists");
        }

        if (appUserRepository.findByEmail(userEmail).isPresent()) {
            throw new RuntimeException("User email already exists");
        }

        if (storeRepository.existsByRetailerKey(retailerKey)) {
            throw new RuntimeException("Retailer key already exists");
        }

        String defaultStoreCode = buildStoreCode(retailerKey, storeName);

        if (storeRepository.existsByStoreCode(defaultStoreCode)) {
            throw new RuntimeException("Store code already exists");
        }
    }

    private Tenant createTenant(
            String businessName,
            String slug,
            String tenantEmail
    ) {
        Tenant tenant = new Tenant();

        tenant.setBusinessName(businessName);
        tenant.setSlug(slug);
        tenant.setEmail(tenantEmail);
        tenant.setPlan(DEFAULT_PLAN);
        tenant.setActive(true);

        return tenantRepository.save(tenant);
    }

    private Store createDefaultStore(
            Tenant tenant,
            String retailerKey,
            String storeName,
            String location
    ) {
        Store store = new Store();

        store.setTenantId(tenant.getId());
        store.setStoreCode(buildStoreCode(retailerKey, storeName));
        store.setStoreName(storeName);
        store.setLocation(location);
        store.setRetailerKey(retailerKey);
        store.setActive(true);

        return storeRepository.save(store);
    }

    private AppUser createOwnerUser(
            Tenant tenant,
            String fullName,
            String userEmail,
            String password
    ) {
        AppUser user = new AppUser();

        user.setTenantId(tenant.getId());
        user.setFullName(fullName);
        user.setEmail(userEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(OWNER_ROLE);
        user.setActive(true);

        return appUserRepository.save(user);
    }

    private Store findDefaultStoreForTenant(Long tenantId) {
        if (tenantId == null) {
            return null;
        }

        return storeRepository.findFirstByTenantIdAndActiveTrue(tenantId)
                .orElse(null);
    }

    private boolean isUserActive(AppUser user) {
        return user != null && user.isActive();
    }

    private String buildStoreCode(String retailerKey, String storeName) {
        String normalizedName = safe(storeName)
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalizedName.isBlank()) {
            normalizedName = "STORE";
        }

        return safeUpper(retailerKey) + "-" + normalizedName;
    }

    private String normalizeSlug(String value) {
        return safe(value)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private boolean isValidEmail(String email) {
        return email != null
                && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
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
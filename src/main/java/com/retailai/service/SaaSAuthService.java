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

    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SaaSAuthService(TenantRepository tenantRepository,
                           StoreRepository storeRepository,
                           AppUserRepository appUserRepository,
                           JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.storeRepository = storeRepository;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String signup(SignupRequest request) {
        String businessName = safe(request.getBusinessName());
        String slug = safeLower(request.getSlug());
        String tenantEmail = safeLower(request.getTenantEmail());
        String storeName = safe(request.getStoreName());
        String location = safe(request.getLocation());
        String retailerKey = safeUpper(request.getRetailerKey());
        String fullName = safe(request.getFullName());
        String userEmail = safeLower(request.getUserEmail());
        String password = safe(request.getPassword());

        System.out.println("SIGNUP DEBUG");
        System.out.println("slug = " + slug);
        System.out.println("tenantEmail = " + tenantEmail);
        System.out.println("retailerKey = " + retailerKey);
        System.out.println("userEmail = " + userEmail);

        if (businessName.isBlank()) throw new RuntimeException("Business name is required");
        if (slug.isBlank()) throw new RuntimeException("Business slug is required");
        if (tenantEmail.isBlank()) throw new RuntimeException("Business email is required");
        if (storeName.isBlank()) throw new RuntimeException("Store name is required");
        if (location.isBlank()) throw new RuntimeException("Location is required");
        if (retailerKey.isBlank()) throw new RuntimeException("Retailer key is required");
        if (fullName.isBlank()) throw new RuntimeException("Full name is required");
        if (userEmail.isBlank()) throw new RuntimeException("User email is required");
        if (password.isBlank()) throw new RuntimeException("Password is required");

        if (tenantRepository.findBySlug(slug).isPresent()) {
            throw new RuntimeException("Business slug already exists");
        }

        if (appUserRepository.findByEmail(userEmail).isPresent()) {
            throw new RuntimeException("User email already exists");
        }

        if (tenantRepository.findAll().stream()
                .anyMatch(t -> safeLower(t.getEmail()).equals(tenantEmail))) {
            throw new RuntimeException("Business email already exists");
        }

        if (storeRepository.findAll().stream()
                .anyMatch(s -> safeUpper(s.getRetailerKey()).equals(retailerKey))) {
            throw new RuntimeException("Retailer key already exists");
        }

        System.out.println("About to save tenant...");
        Tenant tenant = new Tenant();
        tenant.setBusinessName(businessName);
        tenant.setSlug(slug);
        tenant.setEmail(tenantEmail);
        tenant.setPlan("STARTER");
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        System.out.println("Tenant saved with id = " + tenant.getId());

        System.out.println("About to save store...");
        Store store = new Store();
        store.setTenantId(tenant.getId());
        store.setStoreCode(buildStoreCode(retailerKey, storeName));
        store.setStoreName(storeName);
        store.setLocation(location);
        store.setRetailerKey(retailerKey);
        store.setActive(true);
        storeRepository.save(store);

        System.out.println("Store saved.");

        System.out.println("About to save user...");
        AppUser user = new AppUser();
        user.setTenantId(tenant.getId());
        user.setFullName(fullName);
        user.setEmail(userEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("OWNER");
        user.setActive(true);
        appUserRepository.save(user);

        System.out.println("User saved.");

        return "Signup successful for " + tenant.getBusinessName();
    }

    public String login(LoginRequest request) {
        String email = safeLower(request.getEmail());
        String password = safe(request.getPassword());

        if (email.isBlank() || password.isBlank()) {
            throw new RuntimeException("Email and password are required");
        }

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isUserActive(user)) {
            throw new RuntimeException("User account is inactive");
        }

        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        if (!matches) {
            throw new RuntimeException("Invalid password");
        }

        return jwtService.generateToken(user.getEmail(), user.getRole());
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
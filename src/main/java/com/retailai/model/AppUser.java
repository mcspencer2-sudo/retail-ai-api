package com.retailai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 40)
    private String role; // OWNER, MANAGER, STAFF, CUSTOMER

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 80)
    private String retailerKey;

    @Column(length = 160)
    private String retailerName;

    @Column(length = 80)
    private String storeCode;

    @Column(length = 160)
    private String storeName;

    public AppUser() {
    }

    @PrePersist
    @PreUpdate
    public void normalizeBeforeSave() {
        fullName = clean(fullName);
        email = cleanLower(email);
        passwordHash = clean(passwordHash);
        role = cleanUpper(role);

        retailerKey = cleanUpper(retailerKey);
        retailerName = clean(retailerName);
        storeCode = cleanUpper(storeCode);
        storeName = clean(storeName);

        if (role.isBlank()) {
            role = "STAFF";
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getFullName() {
        return clean(fullName);
    }

    public void setFullName(String fullName) {
        this.fullName = clean(fullName);
    }

    public String getEmail() {
        return clean(email);
    }

    public void setEmail(String email) {
        this.email = cleanLower(email);
    }

    public String getPasswordHash() {
        return clean(passwordHash);
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = clean(passwordHash);
    }

    public String getRole() {
        return clean(role);
    }

    public void setRole(String role) {
        this.role = cleanUpper(role);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getRetailerKey() {
        return clean(retailerKey);
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = cleanUpper(retailerKey);
    }

    public String getRetailerName() {
        return clean(retailerName);
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
    }

    public String getStoreCode() {
        return clean(storeCode);
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = cleanUpper(storeCode);
    }

    public String getStoreName() {
        return clean(storeName);
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public boolean isOwner() {
        return "OWNER".equalsIgnoreCase(getRole());
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(getRole());
    }

    public boolean isStaff() {
        return "STAFF".equalsIgnoreCase(getRole());
    }

    public boolean isCustomer() {
        return "CUSTOMER".equalsIgnoreCase(getRole());
    }

    public boolean isMerchantUser() {
        String normalizedRole = getRole().toUpperCase();

        return normalizedRole.equals("OWNER")
                || normalizedRole.equals("MANAGER")
                || normalizedRole.equals("STAFF")
                || normalizedRole.equals("ADMIN")
                || normalizedRole.equals("MERCHANT");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanUpper(String value) {
        return clean(value).toUpperCase();
    }

    private String cleanLower(String value) {
        return clean(value).toLowerCase();
    }
}
package com.retailai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, unique = true)
    private String storeCode;

    @Column(nullable = false)
    private String storeName;

    private String location;

    @Column(nullable = false)
    private String retailerKey;

    @Column(nullable = false)
    private boolean active = true;

    public Store() {
    }

    public Store(Long tenantId,
                 String storeCode,
                 String storeName,
                 String location,
                 String retailerKey,
                 boolean active) {
        this.tenantId = tenantId;
        this.storeCode = storeCode;
        this.storeName = storeName;
        this.location = location;
        this.retailerKey = retailerKey;
        this.active = active;
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

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = retailerKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
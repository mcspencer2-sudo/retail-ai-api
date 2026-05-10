package com.retailai.dto;

public class MerchantInventoryActiveUpdateDTO {

    private String retailerKey;
    private String storeCode;
    private Boolean active;

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = retailerKey;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
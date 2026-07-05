package com.retailai.dto;

public class MerchantInventoryActiveUpdateDTO {

    private Boolean active;

    public MerchantInventoryActiveUpdateDTO() {
    }

    public MerchantInventoryActiveUpdateDTO(Boolean active) {
        this.active = active;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean hasActiveValue() {
        return active != null;
    }
}
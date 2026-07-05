package com.retailai.dto;

public class MerchantInventoryStockUpdateDTO {

    private Integer stockQuantity;

    public MerchantInventoryStockUpdateDTO() {
    }

    public MerchantInventoryStockUpdateDTO(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public boolean hasValidStockQuantity() {
        return stockQuantity != null && stockQuantity >= 0;
    }
}
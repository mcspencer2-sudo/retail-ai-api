package com.retailai.dto;

public class BagQuantityRequest {
    private Integer quantity;

    public BagQuantityRequest() {
    }

    public BagQuantityRequest(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
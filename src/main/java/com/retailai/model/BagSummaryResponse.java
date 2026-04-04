package com.retailai.model;

import java.util.List;

public class BagSummaryResponse {
    private List<BagItem> items;
    private double subtotal;
    private double tax;
    private double total;

    public BagSummaryResponse() {
    }

    public BagSummaryResponse(List<BagItem> items, double subtotal, double tax, double total) {
        this.items = items;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
    }

    public List<BagItem> getItems() {
        return items;
    }

    public void setItems(List<BagItem> items) {
        this.items = items;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
package com.retailai.model;

import java.util.ArrayList;
import java.util.List;

public class BagSummaryResponse {

    private List<BagItem> items = new ArrayList<>();
    private int itemCount;
    private double subtotal;
    private double tax;
    private double total;

    public BagSummaryResponse() {
    }

    public BagSummaryResponse(
            List<BagItem> items,
            double subtotal,
            double tax,
            double total
    ) {
        setItems(items);
        setSubtotal(subtotal);
        setTax(tax);
        setTotal(total);
        recalculateItemCount();
    }

    public List<BagItem> getItems() {
        return items;
    }

    public void setItems(List<BagItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
        recalculateItemCount();
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = Math.max(0, itemCount);
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = roundMoney(Math.max(0.0, subtotal));
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = roundMoney(Math.max(0.0, tax));
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = roundMoney(Math.max(0.0, total));
    }

    private void recalculateItemCount() {
        if (this.items == null || this.items.isEmpty()) {
            this.itemCount = 0;
            return;
        }

        this.itemCount = this.items.stream()
                .mapToInt(item -> item == null ? 0 : Math.max(1, item.getQuantity()))
                .sum();
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
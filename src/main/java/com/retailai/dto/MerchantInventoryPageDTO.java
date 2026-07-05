package com.retailai.dto;

import java.util.ArrayList;
import java.util.List;

public class MerchantInventoryPageDTO {

    private List<MerchantInventoryItemDTO> items = new ArrayList<>();

    private long totalItems = 0;
    private int page = 0;
    private int size = 12;
    private int totalPages = 1;

    private long totalStockUnits = 0;
    private long lowStockCount = 0;
    private long outOfStockCount = 0;
    private long activeCount = 0;
    private long inactiveCount = 0;

    private Double inventoryValue = 0.0;

    private String query = "";
    private String category = "";
    private String retailerKey = "";
    private String storeCode = "";
    private String storeName = "";

    public MerchantInventoryPageDTO() {
    }

    public MerchantInventoryPageDTO(
            List<MerchantInventoryItemDTO> items,
            long totalItems,
            int page,
            int size,
            int totalPages
    ) {
        this.items = safeItems(items);
        this.totalItems = Math.max(0, totalItems);
        this.page = Math.max(0, page);
        this.size = Math.max(1, size);
        this.totalPages = Math.max(1, totalPages);
        recomputeSummaryFromItems();
    }

    public List<MerchantInventoryItemDTO> getItems() {
        return items;
    }

    public void setItems(List<MerchantInventoryItemDTO> items) {
        this.items = safeItems(items);
        recomputeSummaryFromItems();
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = Math.max(0, totalItems);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.max(1, size);
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = Math.max(1, totalPages);
    }

    public long getTotalStockUnits() {
        return totalStockUnits;
    }

    public void setTotalStockUnits(long totalStockUnits) {
        this.totalStockUnits = Math.max(0, totalStockUnits);
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = Math.max(0, lowStockCount);
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(long outOfStockCount) {
        this.outOfStockCount = Math.max(0, outOfStockCount);
    }

    public long getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(long activeCount) {
        this.activeCount = Math.max(0, activeCount);
    }

    public long getInactiveCount() {
        return inactiveCount;
    }

    public void setInactiveCount(long inactiveCount) {
        this.inactiveCount = Math.max(0, inactiveCount);
    }

    public Double getInventoryValue() {
        return inventoryValue;
    }

    public void setInventoryValue(Double inventoryValue) {
        this.inventoryValue = inventoryValue == null ? 0.0 : Math.max(0.0, inventoryValue);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = clean(query);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey).toUpperCase();
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode).toUpperCase();
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public boolean hasNextPage() {
        return page + 1 < totalPages;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public void recomputeSummaryFromItems() {
        List<MerchantInventoryItemDTO> safeItems = safeItems(items);

        long stockUnits = 0;
        long lowStock = 0;
        long outOfStock = 0;
        long active = 0;
        long inactive = 0;
        double value = 0.0;

        for (MerchantInventoryItemDTO item : safeItems) {
            if (item == null) {
                continue;
            }

            int stock = item.getStockQuantity() == null ? 0 : Math.max(0, item.getStockQuantity());
            double price = item.getPrice() == null ? 0.0 : Math.max(0.0, item.getPrice());

            stockUnits += stock;
            value += stock * price;

            if (Boolean.TRUE.equals(item.getOutOfStock()) || stock <= 0) {
                outOfStock++;
            } else if (Boolean.TRUE.equals(item.getLowStock())) {
                lowStock++;
            }

            if (Boolean.FALSE.equals(item.getActive())) {
                inactive++;
            } else {
                active++;
            }
        }

        this.totalStockUnits = stockUnits;
        this.lowStockCount = lowStock;
        this.outOfStockCount = outOfStock;
        this.activeCount = active;
        this.inactiveCount = inactive;
        this.inventoryValue = value;
    }

    private List<MerchantInventoryItemDTO> safeItems(List<MerchantInventoryItemDTO> items) {
        return items == null ? new ArrayList<>() : items;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
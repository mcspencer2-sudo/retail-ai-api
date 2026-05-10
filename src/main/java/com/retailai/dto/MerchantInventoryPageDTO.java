package com.retailai.dto;

import java.util.List;

public class MerchantInventoryPageDTO {

    private List<MerchantInventoryItemDTO> items;
    private long totalItems;
    private int page;
    private int size;
    private int totalPages;

    public List<MerchantInventoryItemDTO> getItems() {
        return items;
    }

    public void setItems(List<MerchantInventoryItemDTO> items) {
        this.items = items;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
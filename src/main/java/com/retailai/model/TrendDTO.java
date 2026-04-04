package com.retailai.model;

public class TrendDTO {
    private String store;
    private String item;
    private int count;

    public TrendDTO() {
    }

    public TrendDTO(String store, String item, int count) {
        this.store = store;
        this.item = item;
        this.count = count;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
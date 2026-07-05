package com.retailai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "retail_order_items",
        indexes = {
                @Index(name = "idx_retail_order_item_rfid", columnList = "rfid"),
                @Index(name = "idx_retail_order_item_retailer_store", columnList = "retailerKey,storeCode"),
                @Index(name = "idx_retail_order_item_order_id", columnList = "order_id")
        }
)
public class RetailOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120)
    private String rfid;

    @Column(length = 220)
    private String itemName;

    @Column(length = 80)
    private String category;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Double unitPrice = 0.0;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Double lineTotal = 0.0;

    @Column(nullable = false, length = 80)
    private String retailerKey;

    @Column(length = 160)
    private String retailerName;

    @Column(nullable = false, length = 80)
    private String storeCode;

    @Column(length = 160)
    private String storeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RetailOrder order;

    public RetailOrderItem() {
    }

    @PrePersist
    @PreUpdate
    public void beforeSave() {
        unitPrice = money(unitPrice);
        quantity = quantity == null ? 1 : Math.max(1, quantity);
        lineTotal = roundMoney(unitPrice * quantity);
    }

    public Long getId() {
        return id;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = clean(rfid);
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = clean(itemName);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = clean(category);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = clean(imageUrl);
    }

    public Double getUnitPrice() {
        return money(unitPrice);
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = money(unitPrice);
        this.lineTotal = roundMoney(this.unitPrice * getQuantity());
    }

    public Integer getQuantity() {
        return quantity == null ? 1 : Math.max(1, quantity);
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity == null ? 1 : Math.max(1, quantity);
        this.lineTotal = roundMoney(getUnitPrice() * this.quantity);
    }

    public Double getLineTotal() {
        return money(lineTotal);
    }

    public void setLineTotal(Double lineTotal) {
        this.lineTotal = money(lineTotal);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey);
    }

    public String getRetailerName() {
        return retailerName;
    }

    public void setRetailerName(String retailerName) {
        this.retailerName = clean(retailerName);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode);
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public RetailOrder getOrder() {
        return order;
    }

    public void setOrder(RetailOrder order) {
        this.order = order;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Double money(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, value);
    }

    private Double roundMoney(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.round(Math.max(0.0, value) * 100.0) / 100.0;
    }
}
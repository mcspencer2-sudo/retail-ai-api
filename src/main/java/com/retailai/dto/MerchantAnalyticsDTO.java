package com.retailai.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MerchantAnalyticsDTO {

    private String range = "WEEKLY";
    private String retailerKey = "";
    private String storeCode = "";
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private Double revenue = 0.0;
    private Double subtotalRevenue = 0.0;
    private Double taxTotal = 0.0;
    private Integer orderCount = 0;
    private Integer itemsSold = 0;
    private Double averageOrderValue = 0.0;

    private Integer scanCount = 0;
    private Integer saveCount = 0;
    private Double conversionRate = 0.0;

    private Integer lowStockCount = 0;
    private Integer outOfStockCount = 0;
    private Double inventoryValueAtRisk = 0.0;

    private MerchantAnalyticsItemDTO topSellingItem = new MerchantAnalyticsItemDTO();
    private MerchantAnalyticsItemDTO topScannedItem = new MerchantAnalyticsItemDTO();
    private MerchantAnalyticsItemDTO topSavedItem = new MerchantAnalyticsItemDTO();

    private List<MerchantAnalyticsItemDTO> lowStockPriority = new ArrayList<>();
    private List<MerchantAnalyticsRecentSaleDTO> recentSales = new ArrayList<>();

    private List<MerchantAnalyticsChartPointDTO> revenueTrend = new ArrayList<>();
    private List<MerchantAnalyticsChartPointDTO> scanTrend = new ArrayList<>();
    private List<MerchantAnalyticsChartPointDTO> saveTrend = new ArrayList<>();

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = clean(range);
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = clean(retailerKey);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = clean(storeCode);
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = safeDouble(revenue);
    }

    public Double getSubtotalRevenue() {
        return subtotalRevenue;
    }

    public void setSubtotalRevenue(Double subtotalRevenue) {
        this.subtotalRevenue = safeDouble(subtotalRevenue);
    }

    public Double getTaxTotal() {
        return taxTotal;
    }

    public void setTaxTotal(Double taxTotal) {
        this.taxTotal = safeDouble(taxTotal);
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = safeInteger(orderCount);
    }

    public Integer getItemsSold() {
        return itemsSold;
    }

    public void setItemsSold(Integer itemsSold) {
        this.itemsSold = safeInteger(itemsSold);
    }

    public Double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(Double averageOrderValue) {
        this.averageOrderValue = safeDouble(averageOrderValue);
    }

    public Integer getScanCount() {
        return scanCount;
    }

    public void setScanCount(Integer scanCount) {
        this.scanCount = safeInteger(scanCount);
    }

    public Integer getSaveCount() {
        return saveCount;
    }

    public void setSaveCount(Integer saveCount) {
        this.saveCount = safeInteger(saveCount);
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = safeDouble(conversionRate);
    }

    public Integer getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(Integer lowStockCount) {
        this.lowStockCount = safeInteger(lowStockCount);
    }

    public Integer getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(Integer outOfStockCount) {
        this.outOfStockCount = safeInteger(outOfStockCount);
    }

    public Double getInventoryValueAtRisk() {
        return inventoryValueAtRisk;
    }

    public void setInventoryValueAtRisk(Double inventoryValueAtRisk) {
        this.inventoryValueAtRisk = safeDouble(inventoryValueAtRisk);
    }

    public MerchantAnalyticsItemDTO getTopSellingItem() {
        return topSellingItem;
    }

    public void setTopSellingItem(MerchantAnalyticsItemDTO topSellingItem) {
        this.topSellingItem = topSellingItem == null ? new MerchantAnalyticsItemDTO() : topSellingItem;
    }

    public MerchantAnalyticsItemDTO getTopScannedItem() {
        return topScannedItem;
    }

    public void setTopScannedItem(MerchantAnalyticsItemDTO topScannedItem) {
        this.topScannedItem = topScannedItem == null ? new MerchantAnalyticsItemDTO() : topScannedItem;
    }

    public MerchantAnalyticsItemDTO getTopSavedItem() {
        return topSavedItem;
    }

    public void setTopSavedItem(MerchantAnalyticsItemDTO topSavedItem) {
        this.topSavedItem = topSavedItem == null ? new MerchantAnalyticsItemDTO() : topSavedItem;
    }

    public List<MerchantAnalyticsItemDTO> getLowStockPriority() {
        return lowStockPriority;
    }

    public void setLowStockPriority(List<MerchantAnalyticsItemDTO> lowStockPriority) {
        this.lowStockPriority = lowStockPriority == null ? new ArrayList<>() : lowStockPriority;
    }

    public List<MerchantAnalyticsRecentSaleDTO> getRecentSales() {
        return recentSales;
    }

    public void setRecentSales(List<MerchantAnalyticsRecentSaleDTO> recentSales) {
        this.recentSales = recentSales == null ? new ArrayList<>() : recentSales;
    }

    public List<MerchantAnalyticsChartPointDTO> getRevenueTrend() {
        return revenueTrend;
    }

    public void setRevenueTrend(List<MerchantAnalyticsChartPointDTO> revenueTrend) {
        this.revenueTrend = revenueTrend == null ? new ArrayList<>() : revenueTrend;
    }

    public List<MerchantAnalyticsChartPointDTO> getScanTrend() {
        return scanTrend;
    }

    public void setScanTrend(List<MerchantAnalyticsChartPointDTO> scanTrend) {
        this.scanTrend = scanTrend == null ? new ArrayList<>() : scanTrend;
    }

    public List<MerchantAnalyticsChartPointDTO> getSaveTrend() {
        return saveTrend;
    }

    public void setSaveTrend(List<MerchantAnalyticsChartPointDTO> saveTrend) {
        this.saveTrend = saveTrend == null ? new ArrayList<>() : saveTrend;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer safeInteger(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private Double safeDouble(Double value) {
        return value == null ? 0.0 : Math.max(0.0, value);
    }
}
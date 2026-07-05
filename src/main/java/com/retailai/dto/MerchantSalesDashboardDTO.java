package com.retailai.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MerchantSalesDashboardDTO {

    private MerchantSalesSummaryDTO summary = new MerchantSalesSummaryDTO();
    private List<OrderResponseDTO> recentOrders = new ArrayList<>();

    private String retailerKey = "";
    private String storeCode = "";
    private String storeName = "";
    private String period = "ALL";

    private Double revenue = 0.0;
    private Double subtotal = 0.0;
    private Double tax = 0.0;
    private Double averageOrderValue = 0.0;

    private Integer orderCount = 0;
    private Integer itemCount = 0;
    private Integer scanCount = 0;
    private Integer saveCount = 0;
    private Integer checkoutCount = 0;

    private Double scanToSaveConversionRate = 0.0;
    private Double saveToCheckoutConversionRate = 0.0;
    private Double scanToCheckoutConversionRate = 0.0;

    private String topSellingItem = "";
    private String topSellingRfid = "";
    private Integer topSellingQuantity = 0;

    private String topScannedItem = "";
    private String topScannedRfid = "";
    private Integer topScannedCount = 0;

    private String topSavedItem = "";
    private String topSavedRfid = "";
    private Integer topSavedCount = 0;

    private Integer lowStockCount = 0;
    private Integer outOfStockCount = 0;
    private Double inventoryValueAtRisk = 0.0;

    private List<MerchantAnalyticsChartPointDTO> revenueChart = new ArrayList<>();
    private List<MerchantAnalyticsChartPointDTO> scanChart = new ArrayList<>();
    private List<MerchantAnalyticsChartPointDTO> saveChart = new ArrayList<>();
    private List<MerchantLowStockItemDTO> lowStockPriorityItems = new ArrayList<>();

    public MerchantSalesDashboardDTO() {
    }

    public MerchantSalesDashboardDTO(
            MerchantSalesSummaryDTO summary,
            List<OrderResponseDTO> recentOrders
    ) {
        this.summary = summary == null ? new MerchantSalesSummaryDTO() : summary;
        this.recentOrders = recentOrders == null ? new ArrayList<>() : recentOrders;
        syncFromSummary();
    }

    public MerchantSalesSummaryDTO getSummary() {
        return summary;
    }

    public void setSummary(MerchantSalesSummaryDTO summary) {
        this.summary = summary == null ? new MerchantSalesSummaryDTO() : summary;
        syncFromSummary();
    }

    public List<OrderResponseDTO> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderResponseDTO> recentOrders) {
        this.recentOrders = recentOrders == null ? new ArrayList<>() : recentOrders;
    }

    public String getRetailerKey() {
        return retailerKey;
    }

    public void setRetailerKey(String retailerKey) {
        this.retailerKey = upper(retailerKey);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = upper(storeCode);
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = clean(storeName);
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        String cleaned = upper(period);
        this.period = cleaned.isBlank() ? "ALL" : cleaned;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = money(revenue);
        recomputeAverageOrderValue();
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = money(subtotal);
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = money(tax);
    }

    public Double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(Double averageOrderValue) {
        this.averageOrderValue = money(averageOrderValue);
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = positiveInt(orderCount);
        recomputeAverageOrderValue();
        recomputeConversionRates();
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = positiveInt(itemCount);
    }

    public Integer getScanCount() {
        return scanCount;
    }

    public void setScanCount(Integer scanCount) {
        this.scanCount = positiveInt(scanCount);
        recomputeConversionRates();
    }

    public Integer getSaveCount() {
        return saveCount;
    }

    public void setSaveCount(Integer saveCount) {
        this.saveCount = positiveInt(saveCount);
        recomputeConversionRates();
    }

    public Integer getCheckoutCount() {
        return checkoutCount;
    }

    public void setCheckoutCount(Integer checkoutCount) {
        this.checkoutCount = positiveInt(checkoutCount);
        recomputeConversionRates();
    }

    public Double getScanToSaveConversionRate() {
        return scanToSaveConversionRate;
    }

    public void setScanToSaveConversionRate(Double scanToSaveConversionRate) {
        this.scanToSaveConversionRate = percentage(scanToSaveConversionRate);
    }

    public Double getSaveToCheckoutConversionRate() {
        return saveToCheckoutConversionRate;
    }

    public void setSaveToCheckoutConversionRate(Double saveToCheckoutConversionRate) {
        this.saveToCheckoutConversionRate = percentage(saveToCheckoutConversionRate);
    }

    public Double getScanToCheckoutConversionRate() {
        return scanToCheckoutConversionRate;
    }

    public void setScanToCheckoutConversionRate(Double scanToCheckoutConversionRate) {
        this.scanToCheckoutConversionRate = percentage(scanToCheckoutConversionRate);
    }

    public String getTopSellingItem() {
        return topSellingItem;
    }

    public void setTopSellingItem(String topSellingItem) {
        this.topSellingItem = clean(topSellingItem);
    }

    public String getTopSellingRfid() {
        return topSellingRfid;
    }

    public void setTopSellingRfid(String topSellingRfid) {
        this.topSellingRfid = upper(topSellingRfid);
    }

    public Integer getTopSellingQuantity() {
        return topSellingQuantity;
    }

    public void setTopSellingQuantity(Integer topSellingQuantity) {
        this.topSellingQuantity = positiveInt(topSellingQuantity);
    }

    public String getTopScannedItem() {
        return topScannedItem;
    }

    public void setTopScannedItem(String topScannedItem) {
        this.topScannedItem = clean(topScannedItem);
    }

    public String getTopScannedRfid() {
        return topScannedRfid;
    }

    public void setTopScannedRfid(String topScannedRfid) {
        this.topScannedRfid = upper(topScannedRfid);
    }

    public Integer getTopScannedCount() {
        return topScannedCount;
    }

    public void setTopScannedCount(Integer topScannedCount) {
        this.topScannedCount = positiveInt(topScannedCount);
    }

    public String getTopSavedItem() {
        return topSavedItem;
    }

    public void setTopSavedItem(String topSavedItem) {
        this.topSavedItem = clean(topSavedItem);
    }

    public String getTopSavedRfid() {
        return topSavedRfid;
    }

    public void setTopSavedRfid(String topSavedRfid) {
        this.topSavedRfid = upper(topSavedRfid);
    }

    public Integer getTopSavedCount() {
        return topSavedCount;
    }

    public void setTopSavedCount(Integer topSavedCount) {
        this.topSavedCount = positiveInt(topSavedCount);
    }

    public Integer getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(Integer lowStockCount) {
        this.lowStockCount = positiveInt(lowStockCount);
    }

    public Integer getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(Integer outOfStockCount) {
        this.outOfStockCount = positiveInt(outOfStockCount);
    }

    public Double getInventoryValueAtRisk() {
        return inventoryValueAtRisk;
    }

    public void setInventoryValueAtRisk(Double inventoryValueAtRisk) {
        this.inventoryValueAtRisk = money(inventoryValueAtRisk);
    }

    public List<MerchantAnalyticsChartPointDTO> getRevenueChart() {
        return revenueChart;
    }

    public void setRevenueChart(List<MerchantAnalyticsChartPointDTO> revenueChart) {
        this.revenueChart = revenueChart == null ? new ArrayList<>() : revenueChart;
    }

    public List<MerchantAnalyticsChartPointDTO> getScanChart() {
        return scanChart;
    }

    public void setScanChart(List<MerchantAnalyticsChartPointDTO> scanChart) {
        this.scanChart = scanChart == null ? new ArrayList<>() : scanChart;
    }

    public List<MerchantAnalyticsChartPointDTO> getSaveChart() {
        return saveChart;
    }

    public void setSaveChart(List<MerchantAnalyticsChartPointDTO> saveChart) {
        this.saveChart = saveChart == null ? new ArrayList<>() : saveChart;
    }

    public List<MerchantLowStockItemDTO> getLowStockPriorityItems() {
        return lowStockPriorityItems;
    }

    public void setLowStockPriorityItems(List<MerchantLowStockItemDTO> lowStockPriorityItems) {
        this.lowStockPriorityItems = lowStockPriorityItems == null ? new ArrayList<>() : lowStockPriorityItems;
    }

    private void syncFromSummary() {
        if (summary == null) {
            return;
        }

        setRevenue(summary.getTotalRevenue());
        setOrderCount(toSafeInt(summary.getTotalOrders()));
        setItemCount(toSafeInt(summary.getTotalItemsSold()));
        setAverageOrderValue(summary.getAverageOrderValue());
    }

    private void recomputeAverageOrderValue() {
        int orders = orderCount == null ? 0 : orderCount;
        double safeRevenue = revenue == null ? 0.0 : revenue;

        this.averageOrderValue = orders <= 0 ? 0.0 : money(safeRevenue / orders);
    }

    private void recomputeConversionRates() {
        int scans = scanCount == null ? 0 : scanCount;
        int saves = saveCount == null ? 0 : saveCount;
        int checkouts = checkoutCount == null ? 0 : checkoutCount;

        this.scanToSaveConversionRate = scans <= 0 ? 0.0 : percentage((saves * 100.0) / scans);
        this.saveToCheckoutConversionRate = saves <= 0 ? 0.0 : percentage((checkouts * 100.0) / saves);
        this.scanToCheckoutConversionRate = scans <= 0 ? 0.0 : percentage((checkouts * 100.0) / scans);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String upper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private Integer positiveInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private Integer toSafeInt(long value) {
        if (value <= 0) {
            return 0;
        }

        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private Double money(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 0.0;
        }

        return Math.max(0.0, Math.round(value * 100.0) / 100.0);
    }

    private Double money(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, Math.round(value * 100.0) / 100.0);
    }

    private Double percentage(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 0.0;
        }

        double bounded = Math.max(0.0, Math.min(100.0, value));
        return Math.round(bounded * 100.0) / 100.0;
    }
}
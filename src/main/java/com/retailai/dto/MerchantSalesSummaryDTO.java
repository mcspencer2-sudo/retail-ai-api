package com.retailai.dto;

public class MerchantSalesSummaryDTO {

    private long totalOrders;
    private long totalItemsSold;
    private double totalRevenue;
    private double averageOrderValue;

    public MerchantSalesSummaryDTO() {
    }

    public MerchantSalesSummaryDTO(
            long totalOrders,
            long totalItemsSold,
            double totalRevenue,
            double averageOrderValue
    ) {
        this.totalOrders = Math.max(0, totalOrders);
        this.totalItemsSold = Math.max(0, totalItemsSold);
        this.totalRevenue = roundMoney(totalRevenue);
        this.averageOrderValue = roundMoney(averageOrderValue);
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = Math.max(0, totalOrders);
    }

    public long getTotalItemsSold() {
        return totalItemsSold;
    }

    public void setTotalItemsSold(long totalItemsSold) {
        this.totalItemsSold = Math.max(0, totalItemsSold);
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = roundMoney(totalRevenue);
    }

    public double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(double averageOrderValue) {
        this.averageOrderValue = roundMoney(averageOrderValue);
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
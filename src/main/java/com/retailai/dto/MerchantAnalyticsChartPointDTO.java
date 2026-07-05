package com.retailai.dto;

public class MerchantAnalyticsChartPointDTO {

    private String label = "";
    private Integer count = 0;
    private Double value = 0.0;

    public MerchantAnalyticsChartPointDTO() {
    }

    public MerchantAnalyticsChartPointDTO(String label, Integer count, Double value) {
        this.label = clean(label);
        this.count = count == null ? 0 : Math.max(0, count);
        this.value = value == null ? 0.0 : Math.max(0.0, value);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = clean(label);
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count == null ? 0 : Math.max(0, count);
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value == null ? 0.0 : Math.max(0.0, value);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
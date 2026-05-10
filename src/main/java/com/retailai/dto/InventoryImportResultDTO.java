package com.retailai.dto;

import java.util.ArrayList;
import java.util.List;

public class InventoryImportResultDTO {

    private int successCount;
    private int failureCount;
    private List<InventoryImportErrorDTO> errors = new ArrayList<>();

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<InventoryImportErrorDTO> getErrors() {
        return errors;
    }

    public void setErrors(List<InventoryImportErrorDTO> errors) {
        this.errors = errors;
    }

    public void addError(int rowNumber, String message) {
        this.errors.add(new InventoryImportErrorDTO(rowNumber, message));
        this.failureCount++;
    }

    public void addSuccess() {
        this.successCount++;
    }
}
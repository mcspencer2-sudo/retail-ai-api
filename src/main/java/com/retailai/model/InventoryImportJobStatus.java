package com.retailai.model;

public final class InventoryImportJobStatus {

    public static final String QUEUED = "QUEUED";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS";
    public static final String FAILED = "FAILED";

    private InventoryImportJobStatus() {
    }
}
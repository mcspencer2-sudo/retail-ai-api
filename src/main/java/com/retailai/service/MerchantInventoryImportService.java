package com.retailai.service;

import com.retailai.dto.InventoryImportErrorDTO;
import com.retailai.dto.InventoryImportLogDTO;
import com.retailai.dto.InventoryImportResultDTO;
import com.retailai.model.InventoryImportLog;
import com.retailai.model.Product;
import com.retailai.repository.InventoryImportLogRepository;
import com.retailai.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MerchantInventoryImportService {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "rfid",
            "item_name",
            "brand",
            "category",
            "color",
            "price",
            "image_url",
            "stock_quantity",
            "retailer_key",
            "retailer_name",
            "store_code",
            "store_name",
            "active",
            "available"
    );

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "tops",
            "bottoms",
            "shoes",
            "outerwear"
    );

    private final ProductRepository productRepository;
    private final InventoryImportLogRepository inventoryImportLogRepository;

    public MerchantInventoryImportService(
            ProductRepository productRepository,
            InventoryImportLogRepository inventoryImportLogRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryImportLogRepository = inventoryImportLogRepository;
    }

    public InventoryImportResultDTO importCsv(
            MultipartFile file,
            String selectedRetailerKey,
            String selectedStoreCode
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required.");
        }

        if (selectedRetailerKey == null || selectedRetailerKey.isBlank()) {
            throw new IllegalArgumentException("Retailer key is required.");
        }

        if (selectedStoreCode == null || selectedStoreCode.isBlank()) {
            throw new IllegalArgumentException("Store code is required.");
        }

        List<InventoryImportErrorDTO> errors = new ArrayList<>();
        Set<String> seenRfidsInFile = new HashSet<>();

        int successCount = 0;
        int failureCount = 0;
        int totalRows = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();

            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file is empty.");
            }

            List<String> headers = parseCsvLine(stripBom(headerLine));
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);

            validateRequiredHeaders(headerIndex);

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);

                if (isBlankRow(values)) {
                    continue;
                }

                totalRows++;

                try {
                    Product product = buildProductFromRow(
                            rowNumber,
                            values,
                            headerIndex,
                            selectedRetailerKey.trim(),
                            selectedStoreCode.trim(),
                            seenRfidsInFile
                    );

                    productRepository.save(product);
                    successCount++;
                } catch (IllegalArgumentException rowError) {
                    failureCount++;
                    errors.add(new InventoryImportErrorDTO(rowNumber, rowError.getMessage()));
                }
            }
        } catch (IllegalArgumentException e) {
            saveImportLog(
                    file,
                    selectedRetailerKey,
                    selectedStoreCode,
                    successCount,
                    failureCount,
                    totalRows,
                    "FAILED"
            );

            throw e;
        } catch (Exception e) {
            saveImportLog(
                    file,
                    selectedRetailerKey,
                    selectedStoreCode,
                    successCount,
                    failureCount,
                    totalRows,
                    "FAILED"
            );

            throw new RuntimeException("Could not parse inventory CSV: " + e.getMessage(), e);
        }

        String status = failureCount > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED";

        saveImportLog(
                file,
                selectedRetailerKey,
                selectedStoreCode,
                successCount,
                failureCount,
                totalRows,
                status
        );

        InventoryImportResultDTO result = new InventoryImportResultDTO();
        result.setSuccessCount(successCount);
        result.setFailureCount(failureCount);
        result.setErrors(errors);

        return result;
    }

    public List<InventoryImportLogDTO> getImportHistory(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = retailerKey == null ? "" : retailerKey.trim();
        String safeStoreCode = storeCode == null ? "" : storeCode.trim();

        List<InventoryImportLog> logs;

        if (!safeRetailerKey.isBlank() && !safeStoreCode.isBlank()) {
            logs = inventoryImportLogRepository.findTop10ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeRetailerKey,
                    safeStoreCode
            );
        } else if (!safeRetailerKey.isBlank()) {
            logs = inventoryImportLogRepository.findTop10ByRetailerKeyOrderByCreatedAtDesc(safeRetailerKey);
        } else {
            logs = inventoryImportLogRepository.findTop10ByOrderByCreatedAtDesc();
        }

        return logs.stream()
                .map(this::toImportLogDto)
                .collect(Collectors.toList());
    }

    private void saveImportLog(
            MultipartFile file,
            String retailerKey,
            String storeCode,
            int successCount,
            int failureCount,
            int totalRows,
            String status
    ) {
        InventoryImportLog log = new InventoryImportLog();
        log.setRetailerKey(retailerKey == null ? "" : retailerKey.trim());
        log.setStoreCode(storeCode == null ? "" : storeCode.trim());
        log.setOriginalFilename(file == null || file.getOriginalFilename() == null
                ? "inventory.csv"
                : file.getOriginalFilename());
        log.setSuccessCount(successCount);
        log.setFailureCount(failureCount);
        log.setTotalRows(totalRows);
        log.setStatus(status);
        log.setCreatedAt(LocalDateTime.now());

        inventoryImportLogRepository.save(log);
    }

    private InventoryImportLogDTO toImportLogDto(InventoryImportLog log) {
        return new InventoryImportLogDTO(
                log.getId(),
                log.getRetailerKey(),
                log.getStoreCode(),
                log.getOriginalFilename(),
                log.getSuccessCount(),
                log.getFailureCount(),
                log.getTotalRows(),
                log.getStatus(),
                log.getCreatedAt()
        );
    }

    private Product buildProductFromRow(
            int rowNumber,
            List<String> values,
            Map<String, Integer> headerIndex,
            String selectedRetailerKey,
            String selectedStoreCode,
            Set<String> seenRfidsInFile
    ) {
        String rfid = requiredValue(values, headerIndex, "rfid");
        String itemName = requiredValue(values, headerIndex, "item_name");
        String brand = requiredValue(values, headerIndex, "brand");
        String category = requiredValue(values, headerIndex, "category");
        String color = optionalValue(values, headerIndex, "color");
        String imageUrl = optionalValue(values, headerIndex, "image_url");
        String retailerKey = requiredValue(values, headerIndex, "retailer_key");
        String retailerName = requiredValue(values, headerIndex, "retailer_name");
        String storeCode = requiredValue(values, headerIndex, "store_code");
        String storeName = requiredValue(values, headerIndex, "store_name");

        Double price = parsePrice(requiredValue(values, headerIndex, "price"));
        Integer stockQuantity = parseStockQuantity(requiredValue(values, headerIndex, "stock_quantity"));
        Boolean active = parseBooleanFlag(requiredValue(values, headerIndex, "active"), "active");
        Boolean available = parseBooleanFlag(requiredValue(values, headerIndex, "available"), "available");

        validateRfid(rfid);

        String normalizedRfid = rfid.trim().toUpperCase(Locale.ROOT);

        if (!seenRfidsInFile.add(normalizedRfid)) {
            throw new IllegalArgumentException("Duplicate RFID in CSV: " + rfid);
        }

        validateCategory(category);
        validateRetailerMatchesUpload(retailerKey, selectedRetailerKey);
        validateStoreMatchesUpload(storeCode, selectedStoreCode);

        boolean finalAvailable = Boolean.TRUE.equals(active)
                && Boolean.TRUE.equals(available)
                && stockQuantity > 0;

        Product product = productRepository.findById(rfid.trim()).orElse(new Product());

        product.setRfid(rfid.trim());
        product.setItemName(itemName.trim());
        product.setBrand(brand.trim());
        product.setCategory(normalizeDisplayCategory(category));
        product.setColor(color.isBlank() ? "Neutral" : color.trim());
        product.setPrice(price);
        product.setImageUrl(imageUrl.isBlank() ? defaultImageUrl(itemName) : imageUrl.trim());
        product.setStockQuantity(stockQuantity);
        product.setRetailerKey(retailerKey.trim());
        product.setRetailerName(retailerName.trim());
        product.setStoreCode(storeCode.trim());
        product.setStoreName(storeName.trim());
        product.setActive(active);
        product.setAvailable(finalAvailable);

        return product;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> missing = REQUIRED_HEADERS.stream()
                .filter(header -> !headerIndex.containsKey(header))
                .sorted()
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("CSV is missing required header(s): " + String.join(", ", missing));
        }
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String normalized = normalizeHeader(headers.get(i));
            if (!normalized.isBlank()) {
                index.put(normalized, i);
            }
        }

        return index;
    }

    private String requiredValue(List<String> values, Map<String, Integer> headerIndex, String header) {
        String value = optionalValue(values, headerIndex, header);

        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing required value for column: " + header);
        }

        return value;
    }

    private String optionalValue(List<String> values, Map<String, Integer> headerIndex, String header) {
        Integer index = headerIndex.get(header);

        if (index == null || index < 0 || index >= values.size()) {
            return "";
        }

        return values.get(index) == null ? "" : values.get(index).trim();
    }

    private void validateRfid(String rfid) {
        if (rfid == null || rfid.isBlank()) {
            throw new IllegalArgumentException("RFID is required.");
        }

        String cleaned = rfid.trim();

        if (cleaned.length() < 3 || cleaned.length() > 64) {
            throw new IllegalArgumentException("RFID must be between 3 and 64 characters.");
        }

        if (!cleaned.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("RFID may only contain letters, numbers, underscores, or hyphens.");
        }
    }

    private void validateCategory(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid category '" + category + "'. Allowed values: Tops, Bottoms, Shoes, Outerwear.");
        }
    }

    private void validateRetailerMatchesUpload(String csvRetailerKey, String selectedRetailerKey) {
        if (!csvRetailerKey.trim().equalsIgnoreCase(selectedRetailerKey.trim())) {
            throw new IllegalArgumentException(
                    "CSV retailer_key '" + csvRetailerKey + "' does not match selected retailer '" + selectedRetailerKey + "'."
            );
        }
    }

    private void validateStoreMatchesUpload(String csvStoreCode, String selectedStoreCode) {
        if (!csvStoreCode.trim().equalsIgnoreCase(selectedStoreCode.trim())) {
            throw new IllegalArgumentException(
                    "CSV store_code '" + csvStoreCode + "' does not match selected store '" + selectedStoreCode + "'."
            );
        }
    }

    private Double parsePrice(String rawPrice) {
        try {
            double price = Double.parseDouble(rawPrice.trim());

            if (price < 0) {
                throw new IllegalArgumentException("Price cannot be negative.");
            }

            return price;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price value: " + rawPrice);
        }
    }

    private Integer parseStockQuantity(String rawStockQuantity) {
        try {
            int stockQuantity = Integer.parseInt(rawStockQuantity.trim());

            if (stockQuantity < 0) {
                throw new IllegalArgumentException("Stock quantity cannot be negative.");
            }

            return stockQuantity;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stock_quantity value: " + rawStockQuantity);
        }
    }

    private Boolean parseBooleanFlag(String value, String columnName) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "true", "yes", "y", "1" -> true;
            case "false", "no", "n", "0" -> false;
            default -> throw new IllegalArgumentException(
                    "Invalid boolean value for " + columnName + ": " + value + ". Use TRUE/FALSE, yes/no, or 1/0."
            );
        };
    }

    private String normalizeDisplayCategory(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "tops" -> "Tops";
            case "bottoms" -> "Bottoms";
            case "shoes" -> "Shoes";
            case "outerwear" -> "Outerwear";
            default -> category == null ? "" : category.trim();
        };
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }

        return header
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
    }

    private boolean isBlankRow(List<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }

        return values.stream().allMatch(value -> value == null || value.trim().isBlank());
    }

    private String stripBom(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\uFEFF", "");
    }

    private String defaultImageUrl(String itemName) {
        String safeName = itemName == null || itemName.isBlank()
                ? "inventory-item"
                : itemName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        return "/images/products/" + safeName + ".jpg";
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();

        if (line == null) {
            return values;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        values.add(current.toString().trim());

        return values;
    }
}
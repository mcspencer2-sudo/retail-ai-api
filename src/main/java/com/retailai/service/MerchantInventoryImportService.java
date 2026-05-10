package com.retailai.service;

import com.retailai.dto.InventoryImportResultDTO;
import com.retailai.model.Product;
import com.retailai.repository.ProductRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MerchantInventoryImportService {

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "tops", "bottoms", "shoes", "outerwear"
    );

    private final ProductRepository productRepository;

    public MerchantInventoryImportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public InventoryImportResultDTO importCsv(MultipartFile file, String retailerKey, String storeCode) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required.");
        }

        if (isBlank(retailerKey)) {
            throw new IllegalArgumentException("retailerKey is required.");
        }

        if (isBlank(storeCode)) {
            throw new IllegalArgumentException("storeCode is required.");
        }

        InventoryImportResultDTO result = new InventoryImportResultDTO();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();

            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file is empty.");
            }

            Map<String, Integer> headerMap = buildHeaderMap(parseCsvLine(headerLine));
            validateRequiredHeaders(headerMap);

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    List<String> columns = parseCsvLine(line);

                    Product product = buildProduct(
                            columns,
                            headerMap,
                            retailerKey.trim(),
                            storeCode.trim(),
                            rowNumber
                    );

                    productRepository.save(product);
                    result.addSuccess();
                } catch (IllegalArgumentException e) {
                    result.addError(rowNumber, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    result.addError(rowNumber, "Unexpected import error: " + e.getMessage());
                }
            }

            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV file.", e);
        }
    }

    private Product buildProduct(
            List<String> columns,
            Map<String, Integer> headerMap,
            String retailerKey,
            String storeCode,
            int rowNumber
    ) {
        String rfid = getRequired(columns, headerMap, "rfid", rowNumber);
        String itemName = getRequired(columns, headerMap, "item_name", rowNumber);
        String brand = getOptional(columns, headerMap, "brand");
        String categoryRaw = getRequired(columns, headerMap, "category", rowNumber);
        String color = getOptional(columns, headerMap, "color");
        String priceRaw = getRequired(columns, headerMap, "price", rowNumber);
        String imageUrl = getOptional(columns, headerMap, "image_url");
        String stockRaw = getRequired(columns, headerMap, "stock_quantity", rowNumber);
        String retailerName = getOptional(columns, headerMap, "retailer_name");
        String storeName = getOptional(columns, headerMap, "store_name");
        String availableRaw = getOptional(columns, headerMap, "available");
        String activeRaw = getOptional(columns, headerMap, "active");

        String normalizedCategory = normalizeCategory(categoryRaw);

        if (!VALID_CATEGORIES.contains(normalizedCategory)) {
            throw new IllegalArgumentException(
                    "Invalid category: " + categoryRaw + ". Allowed categories: tops, bottoms, shoes, outerwear."
            );
        }

        double price = parsePrice(priceRaw);

        if (price < 0) {
            throw new IllegalArgumentException("price cannot be negative.");
        }

        int stockQuantity = parseStock(stockRaw);

        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stock_quantity cannot be negative.");
        }

        String cleanImageUrl = normalizeImageUrl(imageUrl);

        if (!isBlank(cleanImageUrl)) {
            validateImageUrl(cleanImageUrl);
        }

        Product product = productRepository.findById(rfid).orElseGet(Product::new);

        product.setRfid(rfid);
        product.setItemName(itemName);
        product.setBrand(isBlank(brand) ? defaultRetailerName(retailerKey) : brand.trim());
        product.setCategory(capitalizeCategory(normalizedCategory));
        product.setColor(isBlank(color) ? "Neutral" : color.trim());
        product.setPrice(price);
        product.setImageUrl(cleanImageUrl);
        product.setRetailerKey(retailerKey.trim());
        product.setRetailerName(isBlank(retailerName) ? defaultRetailerName(retailerKey) : retailerName.trim());
        product.setStoreCode(storeCode.trim());
        product.setStoreName(isBlank(storeName) ? defaultStoreName(storeCode) : storeName.trim());
        product.setStockQuantity(stockQuantity);

        boolean active = parseBooleanDefault(activeRaw, true);
        boolean available = parseBooleanDefault(availableRaw, stockQuantity > 0);

        product.setActive(active);
        product.setAvailable(active && available && stockQuantity > 0);

        return product;
    }

    private Map<String, Integer> buildHeaderMap(List<String> headers) {
        Map<String, Integer> headerMap = new LinkedHashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            headerMap.put(normalizeHeader(headers.get(i)), i);
        }

        return headerMap;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerMap) {
        List<String> required = List.of(
                "rfid",
                "item_name",
                "category",
                "price",
                "stock_quantity"
        );

        for (String header : required) {
            if (!headerMap.containsKey(header)) {
                throw new IllegalArgumentException("Missing required CSV header: " + header);
            }
        }
    }

    private String getRequired(
            List<String> columns,
            Map<String, Integer> headerMap,
            String header,
            int rowNumber
    ) {
        String value = getOptional(columns, headerMap, header);

        if (isBlank(value)) {
            throw new IllegalArgumentException("Missing required value for '" + header + "'.");
        }

        return value.trim();
    }

    private String getOptional(List<String> columns, Map<String, Integer> headerMap, String header) {
        Integer index = headerMap.get(normalizeHeader(header));

        if (index == null || index < 0 || index >= columns.size()) {
            return "";
        }

        return columns.get(index);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (ch == ',' && !insideQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        values.add(current.toString().trim());
        return values;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCategory(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "top", "tops", "shirt", "shirts", "tee", "t-shirt", "hoodie", "blouse", "sweater" -> "tops";
            case "bottom", "bottoms", "pants", "trousers", "jeans", "cargo", "shorts", "skirt" -> "bottoms";
            case "shoe", "shoes", "sneaker", "sneakers", "boot", "boots", "loafer", "loafers", "heel", "heels", "sandal", "sandals" -> "shoes";
            case "outerwear", "coat", "jacket", "blazer", "parka", "cardigan", "overshirt" -> "outerwear";
            default -> normalized;
        };
    }

    private String capitalizeCategory(String value) {
        return switch (value) {
            case "tops" -> "Tops";
            case "bottoms" -> "Bottoms";
            case "shoes" -> "Shoes";
            case "outerwear" -> "Outerwear";
            default -> value;
        };
    }

    private double parsePrice(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid price: " + value);
        }
    }

    private int parseStock(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid stock_quantity: " + value);
        }
    }

    private boolean parseBooleanDefault(String value, boolean fallback) {
        if (isBlank(value)) {
            return fallback;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return normalized.equals("true")
                || normalized.equals("1")
                || normalized.equals("yes")
                || normalized.equals("y");
    }

    private String normalizeImageUrl(String imageUrl) {
        if (isBlank(imageUrl)) {
            return "";
        }

        return imageUrl.trim();
    }

    private void validateImageUrl(String imageUrl) {
        if (isExternalUrl(imageUrl)) {
            validateExternalUrlFormat(imageUrl);
            return;
        }

        if (!imageUrl.startsWith("/")) {
            throw new IllegalArgumentException(
                    "image_url must start with '/' for local files or use http/https. Got: " + imageUrl
            );
        }

        if (!imageUrl.startsWith("/images/products/")) {
            throw new IllegalArgumentException(
                    "Local image_url must start with /images/products/. Got: " + imageUrl
            );
        }

        if (imageUrl.contains("..")) {
            throw new IllegalArgumentException("image_url cannot contain '..'. Got: " + imageUrl);
        }

        if (!hasSupportedImageExtension(imageUrl)) {
            throw new IllegalArgumentException(
                    "image_url must end with .jpg, .jpeg, .png, .webp, or .gif. Got: " + imageUrl
            );
        }

        if (!localStaticImageExists(imageUrl)) {
            throw new IllegalArgumentException(
                    "Image file not found in src/main/resources/static for path: " + imageUrl
            );
        }
    }

    private boolean isExternalUrl(String imageUrl) {
        String lower = imageUrl.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private void validateExternalUrlFormat(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);

            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Invalid external image_url: " + imageUrl);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid external image_url: " + imageUrl);
        }
    }

    private boolean hasSupportedImageExtension(String imageUrl) {
        String lower = imageUrl.toLowerCase(Locale.ROOT);

        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif");
    }

    private boolean localStaticImageExists(String imageUrl) {
        try {
            String decoded = URLDecoder.decode(imageUrl, StandardCharsets.UTF_8);
            String classpathLocation = "static" + decoded;

            ClassPathResource resource = new ClassPathResource(classpathLocation);

            return resource.exists() && resource.isReadable();
        } catch (Exception e) {
            return false;
        }
    }

    private String defaultRetailerName(String retailerKey) {
        return switch (retailerKey.trim().toUpperCase(Locale.ROOT)) {
            case "MACY001" -> "Macy's";
            case "ZARA001" -> "Zara";
            case "NORD001" -> "Nordstrom";
            case "NIKE001" -> "Nike";
            default -> retailerKey.trim();
        };
    }

    private String defaultStoreName(String storeCode) {
        return switch (storeCode.trim().toUpperCase(Locale.ROOT)) {
            case "MACY-NYC-01" -> "Herald Square";
            case "MACY-BK-02" -> "Brooklyn";
            case "ZARA-SOHO-01" -> "SoHo";
            case "ZARA-5TH-02" -> "5th Avenue";
            case "NORD-NYC-01" -> "57th Street";
            case "NORD-WTC-02" -> "World Trade Center";
            case "NIKE-NYC-01" -> "Nike NYC";
            case "NIKE-SOHO-02" -> "Nike SoHo";
            default -> storeCode.trim();
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
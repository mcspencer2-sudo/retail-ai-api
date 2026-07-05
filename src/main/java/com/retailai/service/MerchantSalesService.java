package com.retailai.service;

import com.retailai.dto.MerchantAnalyticsChartPointDTO;
import com.retailai.dto.MerchantLowStockItemDTO;
import com.retailai.dto.MerchantSalesDashboardDTO;
import com.retailai.dto.MerchantSalesSummaryDTO;
import com.retailai.dto.OrderResponseDTO;
import com.retailai.model.BagItem;
import com.retailai.model.Product;
import com.retailai.model.ScanHistory;
import com.retailai.repository.BagItemRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.ScanHistoryRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MerchantSalesService {

    private static final int LOW_STOCK_THRESHOLD = 3;
    private static final int IDEAL_STOCK_LEVEL = 12;

    private final OrderService orderService;
    private final ScanHistoryRepository scanHistoryRepository;
    private final ProductRepository productRepository;
    private final BagItemRepository bagItemRepository;

    public MerchantSalesService(
            OrderService orderService,
            ScanHistoryRepository scanHistoryRepository,
            ProductRepository productRepository,
            BagItemRepository bagItemRepository
    ) {
        this.orderService = orderService;
        this.scanHistoryRepository = scanHistoryRepository;
        this.productRepository = productRepository;
        this.bagItemRepository = bagItemRepository;
    }

    public MerchantSalesDashboardDTO getStoreSalesDashboard(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeUpper(retailerKey);
        String safeStoreCode = normalizeUpper(storeCode);

        if (safeRetailerKey.isBlank()) {
            throw new IllegalArgumentException("Retailer key is required.");
        }

        if (safeStoreCode.isBlank()) {
            throw new IllegalArgumentException("Store code is required.");
        }

        List<OrderResponseDTO> orders = orderService.getRecentStoreOrders(
                safeRetailerKey,
                safeStoreCode
        );

        List<OrderResponseDTO> safeOrders = orders == null ? new ArrayList<>() : orders;

        long totalOrders = safeOrders.size();

        long totalItemsSold = safeOrders.stream()
                .mapToLong(this::extractOrderItemCount)
                .sum();

        double subtotal = safeOrders.stream()
                .mapToDouble(order -> safeDouble(readValue(order, "getSubtotal")))
                .sum();

        double tax = safeOrders.stream()
                .mapToDouble(order -> safeDouble(readValue(order, "getTax")))
                .sum();

        double totalRevenue = safeOrders.stream()
                .mapToDouble(order -> safeDouble(readValue(order, "getTotal")))
                .sum();

        double averageOrderValue = totalOrders == 0
                ? 0.0
                : totalRevenue / totalOrders;

        MerchantSalesSummaryDTO summary = new MerchantSalesSummaryDTO(
                totalOrders,
                totalItemsSold,
                totalRevenue,
                averageOrderValue
        );

        TopSellingResult topSelling = computeTopSellingItem(safeOrders);

        List<ScanHistory> recentScans = scanHistoryRepository
                .findTop100ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                        safeRetailerKey,
                        safeStoreCode
                );

        List<ScanHistory> safeScans = recentScans == null ? new ArrayList<>() : recentScans;

        long scanCount = scanHistoryRepository.countByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );

        TopScannedResult topScanned = computeTopScannedItem(safeScans);

        List<BagItem> recentSaves = bagItemRepository
                .findTop100ByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                        safeRetailerKey,
                        safeStoreCode
                );

        List<BagItem> safeSaves = recentSaves == null ? new ArrayList<>() : recentSaves;

        long saveCount = bagItemRepository.countByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );

        TopSavedResult topSaved = computeTopSavedItem(safeSaves);

        long lowStockCount = productRepository
                .countByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityLessThanEqualAndStockQuantityGreaterThan(
                        safeRetailerKey,
                        safeStoreCode,
                        LOW_STOCK_THRESHOLD,
                        0
                );

        long outOfStockCount = productRepository
                .countByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityLessThanEqual(
                        safeRetailerKey,
                        safeStoreCode,
                        0
                );

        List<Product> lowStockProducts = productRepository
                .findTop10ByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(
                        safeRetailerKey,
                        safeStoreCode,
                        LOW_STOCK_THRESHOLD
                );

        List<Product> safeLowStockProducts = lowStockProducts == null
                ? new ArrayList<>()
                : lowStockProducts;

        double inventoryValueAtRisk = computeInventoryValueAtRisk(safeLowStockProducts);
        List<MerchantLowStockItemDTO> lowStockPriorityItems = buildLowStockPriorityItems(safeLowStockProducts);

        MerchantSalesDashboardDTO dashboard = new MerchantSalesDashboardDTO(
                summary,
                safeOrders.stream().limit(20).toList()
        );

        dashboard.setRetailerKey(safeRetailerKey);
        dashboard.setStoreCode(safeStoreCode);
        dashboard.setPeriod("RECENT");

        dashboard.setRevenue(totalRevenue);
        dashboard.setSubtotal(subtotal);
        dashboard.setTax(tax);
        dashboard.setAverageOrderValue(averageOrderValue);

        dashboard.setOrderCount((int) Math.min(Integer.MAX_VALUE, totalOrders));
        dashboard.setCheckoutCount((int) Math.min(Integer.MAX_VALUE, totalOrders));
        dashboard.setItemCount((int) Math.min(Integer.MAX_VALUE, totalItemsSold));

        dashboard.setTopSellingItem(topSelling.itemName());
        dashboard.setTopSellingRfid(topSelling.rfid());
        dashboard.setTopSellingQuantity(topSelling.quantity());

        dashboard.setRevenueChart(buildRevenueChart(safeOrders));

        dashboard.setScanCount((int) Math.min(Integer.MAX_VALUE, scanCount));
        dashboard.setTopScannedItem(topScanned.itemName());
        dashboard.setTopScannedRfid(topScanned.rfid());
        dashboard.setTopScannedCount(topScanned.count());
        dashboard.setScanChart(buildScanChart(safeRetailerKey, safeStoreCode));

        dashboard.setSaveCount((int) Math.min(Integer.MAX_VALUE, saveCount));
        dashboard.setTopSavedItem(topSaved.itemName());
        dashboard.setTopSavedRfid(topSaved.rfid());
        dashboard.setTopSavedCount(topSaved.count());
        dashboard.setSaveChart(buildSaveChart(safeRetailerKey, safeStoreCode));

        dashboard.setLowStockCount((int) Math.min(Integer.MAX_VALUE, lowStockCount));
        dashboard.setOutOfStockCount((int) Math.min(Integer.MAX_VALUE, outOfStockCount));
        dashboard.setInventoryValueAtRisk(inventoryValueAtRisk);
        dashboard.setLowStockPriorityItems(lowStockPriorityItems);

        return dashboard;
    }

    private long extractOrderItemCount(OrderResponseDTO order) {
        if (order == null) {
            return 0;
        }

        Object itemCount = readValue(order, "getItemCount");
        long declaredItemCount = safeLong(itemCount);

        if (declaredItemCount > 0) {
            return declaredItemCount;
        }

        List<?> items = extractOrderItems(order);

        if (items.isEmpty()) {
            return 0;
        }

        return items.stream()
                .mapToLong(item -> {
                    long quantity = safeLong(readFirstValue(
                            item,
                            "getQuantity",
                            "getQty",
                            "getCount"
                    ));

                    return Math.max(1, quantity);
                })
                .sum();
    }

    private TopSellingResult computeTopSellingItem(List<OrderResponseDTO> orders) {
        Map<String, TopSellingCounter> counters = new LinkedHashMap<>();

        for (OrderResponseDTO order : orders) {
            List<?> items = extractOrderItems(order);

            for (Object item : items) {
                String rfid = cleanText(readFirstValue(
                        item,
                        "getRfid",
                        "getItemRfid",
                        "getProductRfid"
                )).toUpperCase();

                String itemName = cleanText(readFirstValue(
                        item,
                        "getItemName",
                        "getName",
                        "getProductName"
                ));

                if (itemName.isBlank()) {
                    itemName = "Unknown Item";
                }

                String key = !rfid.isBlank()
                        ? rfid
                        : itemName.toLowerCase(Locale.ROOT);

                int quantity = (int) Math.max(
                        1,
                        safeLong(readFirstValue(
                                item,
                                "getQuantity",
                                "getQty",
                                "getCount"
                        ))
                );

                TopSellingCounter counter = counters.getOrDefault(
                        key,
                        new TopSellingCounter(rfid, itemName, 0)
                );

                counter.quantity += quantity;
                counters.put(key, counter);
            }
        }

        return counters.values()
                .stream()
                .max(Comparator.comparingInt(counter -> counter.quantity))
                .map(counter -> new TopSellingResult(
                        counter.itemName,
                        counter.rfid,
                        counter.quantity
                ))
                .orElse(new TopSellingResult("", "", 0));
    }

    private TopScannedResult computeTopScannedItem(List<ScanHistory> scans) {
        Map<String, TopScannedCounter> counters = new LinkedHashMap<>();

        for (ScanHistory scan : scans) {
            if (scan == null) {
                continue;
            }

            String rfid = cleanText(scan.getRfid()).toUpperCase();

            if (rfid.isBlank()) {
                continue;
            }

            String itemName = cleanText(scan.getItemName());

            if (itemName.isBlank()) {
                itemName = "Unknown Item";
            }

            TopScannedCounter counter = counters.getOrDefault(
                    rfid,
                    new TopScannedCounter(rfid, itemName, 0)
            );

            counter.count += 1;
            counters.put(rfid, counter);
        }

        return counters.values()
                .stream()
                .max(Comparator.comparingInt(counter -> counter.count))
                .map(counter -> new TopScannedResult(
                        counter.itemName,
                        counter.rfid,
                        counter.count
                ))
                .orElse(new TopScannedResult("", "", 0));
    }

    private TopSavedResult computeTopSavedItem(List<BagItem> saves) {
        Map<String, TopSavedCounter> counters = new LinkedHashMap<>();

        for (BagItem save : saves) {
            if (save == null) {
                continue;
            }

            String rfid = cleanText(save.getRfid()).toUpperCase();

            if (rfid.isBlank()) {
                continue;
            }

            String itemName = cleanText(save.getItemName());

            if (itemName.isBlank()) {
                itemName = "Unknown Item";
            }

            int quantity = Math.max(1, (int) safeLong(save.getQuantity()));

            TopSavedCounter counter = counters.getOrDefault(
                    rfid,
                    new TopSavedCounter(rfid, itemName, 0)
            );

            counter.count += quantity;
            counters.put(rfid, counter);
        }

        return counters.values()
                .stream()
                .max(Comparator.comparingInt(counter -> counter.count))
                .map(counter -> new TopSavedResult(
                        counter.itemName,
                        counter.rfid,
                        counter.count
                ))
                .orElse(new TopSavedResult("", "", 0));
    }

    private double computeInventoryValueAtRisk(List<Product> products) {
        return products.stream()
                .mapToDouble(product -> {
                    int stockQuantity = (int) safeLong(readFirstValue(
                            product,
                            "getStockQuantity",
                            "getStock",
                            "getQuantity",
                            "getInventoryCount"
                    ));

                    double price = safeDouble(readFirstValue(
                            product,
                            "getPrice",
                            "getUnitPrice"
                    ));

                    int reorderQuantity = Math.max(0, IDEAL_STOCK_LEVEL - stockQuantity);

                    return reorderQuantity * price;
                })
                .sum();
    }

    private List<MerchantLowStockItemDTO> buildLowStockPriorityItems(List<Product> products) {
        return products.stream()
                .map(this::buildLowStockPriorityItem)
                .toList();
    }

    private MerchantLowStockItemDTO buildLowStockPriorityItem(Product product) {
        MerchantLowStockItemDTO dto = new MerchantLowStockItemDTO();

        String rfid = cleanText(readFirstValue(
                product,
                "getRfid",
                "getItemRfid",
                "getProductRfid",
                "getId"
        )).toUpperCase();

        String itemName = cleanText(readFirstValue(
                product,
                "getItemName",
                "getName",
                "getProductName"
        ));

        String brand = cleanText(readFirstValue(product, "getBrand"));
        String category = cleanText(readFirstValue(product, "getCategory"));
        String color = cleanText(readFirstValue(product, "getColor"));

        int stockQuantity = (int) safeLong(readFirstValue(
                product,
                "getStockQuantity",
                "getStock",
                "getQuantity",
                "getInventoryCount"
        ));

        int reorderThreshold = (int) safeLong(readFirstValue(
                product,
                "getReorderThreshold",
                "getLowStockThreshold"
        ));

        if (reorderThreshold <= 0) {
            reorderThreshold = LOW_STOCK_THRESHOLD;
        }

        int idealStockLevel = (int) safeLong(readFirstValue(
                product,
                "getIdealStockLevel",
                "getTargetStockLevel"
        ));

        if (idealStockLevel <= 0) {
            idealStockLevel = IDEAL_STOCK_LEVEL;
        }

        int suggestedReorderQuantity = Math.max(0, idealStockLevel - stockQuantity);

        double price = safeDouble(readFirstValue(
                product,
                "getPrice",
                "getUnitPrice"
        ));

        double valueAtRisk = suggestedReorderQuantity * price;

        String alert = stockQuantity <= 0
                ? "Out of stock — reorder immediately."
                : "Low stock — suggested reorder: " + suggestedReorderQuantity + " units.";

        setIfPossible(dto, "setRfid", String.class, rfid);
        setIfPossible(dto, "setItemName", String.class, itemName);
        setIfPossible(dto, "setName", String.class, itemName);
        setIfPossible(dto, "setBrand", String.class, brand);
        setIfPossible(dto, "setCategory", String.class, category);
        setIfPossible(dto, "setColor", String.class, color);
        setIfPossible(dto, "setStockQuantity", Integer.class, stockQuantity);
        setIfPossible(dto, "setCurrentStock", Integer.class, stockQuantity);
        setIfPossible(dto, "setReorderThreshold", Integer.class, reorderThreshold);
        setIfPossible(dto, "setIdealStockLevel", Integer.class, idealStockLevel);
        setIfPossible(dto, "setSuggestedReorderQuantity", Integer.class, suggestedReorderQuantity);
        setIfPossible(dto, "setPrice", Double.class, price);
        setIfPossible(dto, "setValueAtRisk", Double.class, valueAtRisk);
        setIfPossible(dto, "setInventoryValueAtRisk", Double.class, valueAtRisk);
        setIfPossible(dto, "setInventoryAlert", String.class, alert);
        setIfPossible(dto, "setAlert", String.class, alert);

        return dto;
    }

    private List<MerchantAnalyticsChartPointDTO> buildRevenueChart(List<OrderResponseDTO> orders) {
        Map<String, ChartAccumulator> dailyRevenue = new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String label = buildChartLabel(date);

            dailyRevenue.put(label, new ChartAccumulator());
        }

        for (OrderResponseDTO order : orders) {
            LocalDate date = extractOrderDate(order);

            if (date == null) {
                continue;
            }

            String label = buildChartLabel(date);

            if (!dailyRevenue.containsKey(label)) {
                continue;
            }

            ChartAccumulator accumulator = dailyRevenue.get(label);
            accumulator.count += 1;
            accumulator.value += safeDouble(readValue(order, "getTotal"));
        }

        return dailyRevenue.entrySet()
                .stream()
                .map(entry -> new MerchantAnalyticsChartPointDTO(
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().value
                ))
                .toList();
    }

    private List<MerchantAnalyticsChartPointDTO> buildScanChart(
            String retailerKey,
            String storeCode
    ) {
        Map<String, ChartAccumulator> dailyScans = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String label = buildChartLabel(date);

            dailyScans.put(label, new ChartAccumulator());
        }

        List<ScanHistory> scans = scanHistoryRepository
                .findByRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
                        retailerKey,
                        storeCode,
                        sevenDaysAgo
                );

        List<ScanHistory> safeScans = scans == null ? new ArrayList<>() : scans;

        for (ScanHistory scan : safeScans) {
            LocalDate date = extractScanDate(scan);

            if (date == null) {
                continue;
            }

            String label = buildChartLabel(date);

            if (!dailyScans.containsKey(label)) {
                continue;
            }

            ChartAccumulator accumulator = dailyScans.get(label);
            accumulator.count += 1;
            accumulator.value += 1.0;
        }

        return dailyScans.entrySet()
                .stream()
                .map(entry -> new MerchantAnalyticsChartPointDTO(
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().value
                ))
                .toList();
    }

    private List<MerchantAnalyticsChartPointDTO> buildSaveChart(
            String retailerKey,
            String storeCode
    ) {
        Map<String, ChartAccumulator> dailySaves = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String label = buildChartLabel(date);

            dailySaves.put(label, new ChartAccumulator());
        }

        List<BagItem> saves = bagItemRepository
                .findByRetailerKeyAndStoreCodeAndCreatedAtAfterOrderByCreatedAtDesc(
                        retailerKey,
                        storeCode,
                        sevenDaysAgo
                );

        List<BagItem> safeSaves = saves == null ? new ArrayList<>() : saves;

        for (BagItem save : safeSaves) {
            LocalDate date = extractBagItemDate(save);

            if (date == null) {
                continue;
            }

            String label = buildChartLabel(date);

            if (!dailySaves.containsKey(label)) {
                continue;
            }

            int quantity = Math.max(1, (int) safeLong(save.getQuantity()));

            ChartAccumulator accumulator = dailySaves.get(label);
            accumulator.count += quantity;
            accumulator.value += quantity;
        }

        return dailySaves.entrySet()
                .stream()
                .map(entry -> new MerchantAnalyticsChartPointDTO(
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().value
                ))
                .toList();
    }

    private LocalDate extractOrderDate(OrderResponseDTO order) {
        Object rawDate = readFirstValue(
                order,
                "getCreatedAt",
                "getOrderedAt",
                "getCheckoutAt",
                "getCompletedAt",
                "getDate"
        );

        if (rawDate == null) {
            return null;
        }

        if (rawDate instanceof LocalDate localDate) {
            return localDate;
        }

        if (rawDate instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }

        String text = cleanText(rawDate);

        if (text.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(text).toLocalDate();
        } catch (RuntimeException ignored) {
            // Try plain date next.
        }

        try {
            return LocalDate.parse(text);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDate extractScanDate(ScanHistory scan) {
        if (scan == null || scan.getCreatedAt() == null) {
            return null;
        }

        return scan.getCreatedAt().toLocalDate();
    }

    private LocalDate extractBagItemDate(BagItem bagItem) {
        if (bagItem == null || bagItem.getCreatedAt() == null) {
            return null;
        }

        return bagItem.getCreatedAt().toLocalDate();
    }

    private String buildChartLabel(LocalDate date) {
        if (date == null) {
            return "";
        }

        return date.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.US);
    }

    private List<?> extractOrderItems(OrderResponseDTO order) {
        Object items = readFirstValue(
                order,
                "getItems",
                "getOrderItems",
                "getLineItems"
        );

        if (items instanceof List<?> list) {
            return list;
        }

        return new ArrayList<>();
    }

    private Object readFirstValue(Object target, String... getterNames) {
        if (target == null || getterNames == null) {
            return null;
        }

        for (String getterName : getterNames) {
            Object value = readValue(target, getterName);

            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Object readValue(Object target, String getterName) {
        if (target == null || getterName == null || getterName.isBlank()) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(getterName);
            return method.invoke(target);
        } catch (RuntimeException ignored) {
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setIfPossible(
            Object target,
            String setterName,
            Class<?> parameterType,
            Object value
    ) {
        if (target == null || setterName == null || setterName.isBlank()) {
            return;
        }

        try {
            Method setter = target.getClass().getMethod(setterName, parameterType);
            setter.invoke(target, value);
        } catch (RuntimeException ignored) {
            // Setter does not exist on this DTO shape.
        } catch (Exception ignored) {
            // Setter does not exist on this DTO shape.
        }
    }

    private long safeLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Number number) {
            return Math.max(0L, number.longValue());
        }

        try {
            return Math.max(0L, Long.parseLong(String.valueOf(value).trim()));
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private double safeDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number number) {
            return Math.max(0.0, number.doubleValue());
        }

        try {
            return Math.max(0.0, Double.parseDouble(String.valueOf(value).trim()));
        } catch (RuntimeException e) {
            return 0.0;
        }
    }

    private String cleanText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static class TopSellingCounter {
        private final String rfid;
        private final String itemName;
        private int quantity;

        private TopSellingCounter(String rfid, String itemName, int quantity) {
            this.rfid = rfid == null ? "" : rfid.trim().toUpperCase();
            this.itemName = itemName == null ? "" : itemName.trim();
            this.quantity = Math.max(0, quantity);
        }
    }

    private static class TopScannedCounter {
        private final String rfid;
        private final String itemName;
        private int count;

        private TopScannedCounter(String rfid, String itemName, int count) {
            this.rfid = rfid == null ? "" : rfid.trim().toUpperCase();
            this.itemName = itemName == null ? "" : itemName.trim();
            this.count = Math.max(0, count);
        }
    }

    private static class TopSavedCounter {
        private final String rfid;
        private final String itemName;
        private int count;

        private TopSavedCounter(String rfid, String itemName, int count) {
            this.rfid = rfid == null ? "" : rfid.trim().toUpperCase();
            this.itemName = itemName == null ? "" : itemName.trim();
            this.count = Math.max(0, count);
        }
    }

    private static class ChartAccumulator {
        private int count = 0;
        private double value = 0.0;
    }

    private record TopSellingResult(
            String itemName,
            String rfid,
            Integer quantity
    ) {
    }

    private record TopScannedResult(
            String itemName,
            String rfid,
            Integer count
    ) {
    }

    private record TopSavedResult(
            String itemName,
            String rfid,
            Integer count
    ) {
    }
}
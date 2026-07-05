package com.retailai.service;

import com.retailai.dto.MerchantAnalyticsChartPointDTO;
import com.retailai.dto.MerchantAnalyticsDTO;
import com.retailai.dto.MerchantAnalyticsItemDTO;
import com.retailai.dto.MerchantAnalyticsRecentSaleDTO;
import com.retailai.model.AppUser;
import com.retailai.model.Product;
import com.retailai.model.RetailOrder;
import com.retailai.model.RetailOrderItem;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.RetailOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MerchantAnalyticsService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final String DEFAULT_STORE_CODE = "DEFAULT";

    private final RetailOrderRepository retailOrderRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public MerchantAnalyticsService(
            RetailOrderRepository retailOrderRepository,
            ProductRepository productRepository,
            CurrentUserService currentUserService
    ) {
        this.retailOrderRepository = retailOrderRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    public MerchantAnalyticsDTO getMerchantAnalytics(MerchantAnalyticsRange range) {
        MerchantAnalyticsRange safeRange = range == null ? MerchantAnalyticsRange.WEEKLY : range;

        AppUser currentUser = currentUserService.getCurrentUser();

        String retailerKey = resolveRetailerKey(currentUser);
        String storeCode = resolveStoreCode(currentUser, retailerKey);

        DateWindow window = resolveWindow(safeRange);

        List<RetailOrder> orders =
                retailOrderRepository.findByRetailerKeyAndStoreCodeAndCreatedAtBetweenOrderByCreatedAtDesc(
                        retailerKey,
                        storeCode,
                        window.start(),
                        window.end()
                );

        List<Product> products = productRepository.findByRetailerKeyAndStoreCode(
                retailerKey,
                storeCode
        );

        List<Product> activeProducts = products.stream()
                .filter(product -> Boolean.TRUE.equals(product.getActive()))
                .toList();

        BigDecimal subtotalRevenue = orders.stream()
                .map(this::safeSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxTotal = orders.stream()
                .map(this::safeTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = orders.stream()
                .map(this::safeTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int orderCount = orders.size();

        int itemsSold = orders.stream()
                .mapToInt(this::countItems)
                .sum();

        BigDecimal averageOrderValue = orderCount == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        int scanCount = activeProducts.stream()
                .mapToInt(this::safeScanCount)
                .sum();

        int saveCount = activeProducts.stream()
                .mapToInt(this::safeSaveCount)
                .sum();

        double conversionRate = scanCount == 0
                ? 0.0
                : roundDouble((orderCount * 100.0) / scanCount);

        long lowStockCount = activeProducts.stream()
                .filter(this::isLowStock)
                .count();

        long outOfStockCount = activeProducts.stream()
                .filter(this::isOutOfStock)
                .count();

        BigDecimal inventoryValueAtRisk = activeProducts.stream()
                .filter(this::isOutOfStock)
                .map(product -> safePrice(product).multiply(BigDecimal.valueOf(Math.max(1, safeStock(product)))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MerchantAnalyticsItemDTO topSellingItem = buildTopSellingItem(orders)
                .orElse(new MerchantAnalyticsItemDTO());

        MerchantAnalyticsItemDTO topScannedItem = activeProducts.stream()
                .max(Comparator.comparingInt(this::safeScanCount))
                .map(product -> toItemDTO(product, safeScanCount(product), "scans"))
                .orElse(new MerchantAnalyticsItemDTO());

        MerchantAnalyticsItemDTO topSavedItem = activeProducts.stream()
                .max(Comparator.comparingInt(this::safeSaveCount))
                .map(product -> toItemDTO(product, safeSaveCount(product), "saves"))
                .orElse(new MerchantAnalyticsItemDTO());

        List<MerchantAnalyticsItemDTO> lowStockPriority = activeProducts.stream()
                .filter(this::isLowStock)
                .sorted(Comparator.comparingInt(this::safeStock))
                .limit(8)
                .map(product -> toItemDTO(product, safeStock(product), "stock"))
                .toList();

        List<MerchantAnalyticsRecentSaleDTO> recentSales = orders.stream()
                .limit(10)
                .map(this::toRecentSaleDTO)
                .toList();

        List<MerchantAnalyticsChartPointDTO> revenueTrend = buildRevenueTrend(orders, window, safeRange);
        List<MerchantAnalyticsChartPointDTO> scanTrend = buildProductTrend(activeProducts, window, safeRange, true);
        List<MerchantAnalyticsChartPointDTO> saveTrend = buildProductTrend(activeProducts, window, safeRange, false);

        MerchantAnalyticsDTO dto = new MerchantAnalyticsDTO();

        dto.setRange(safeRange.name());
        dto.setRetailerKey(retailerKey);
        dto.setStoreCode(storeCode);
        dto.setStartAt(window.start());
        dto.setEndAt(window.end());

        dto.setRevenue(toDouble(totalRevenue));
        dto.setSubtotalRevenue(toDouble(subtotalRevenue));
        dto.setTaxTotal(toDouble(taxTotal));
        dto.setOrderCount(orderCount);
        dto.setItemsSold(itemsSold);
        dto.setAverageOrderValue(toDouble(averageOrderValue));

        dto.setScanCount(scanCount);
        dto.setSaveCount(saveCount);
        dto.setConversionRate(conversionRate);

        dto.setLowStockCount((int) lowStockCount);
        dto.setOutOfStockCount((int) outOfStockCount);
        dto.setInventoryValueAtRisk(toDouble(inventoryValueAtRisk));

        dto.setTopSellingItem(topSellingItem);
        dto.setTopScannedItem(topScannedItem);
        dto.setTopSavedItem(topSavedItem);

        dto.setLowStockPriority(lowStockPriority);
        dto.setRecentSales(recentSales);

        dto.setRevenueTrend(revenueTrend);
        dto.setScanTrend(scanTrend);
        dto.setSaveTrend(saveTrend);

        return dto;
    }

    private String resolveRetailerKey(AppUser user) {
        String reflectedRetailerKey = cleanUpper(readStringGetter(user, "getRetailerKey"));

        if (!reflectedRetailerKey.isBlank()) {
            return reflectedRetailerKey;
        }

        Long tenantId = user == null ? null : user.getTenantId();

        if (tenantId != null) {
            return String.valueOf(tenantId);
        }

        throw new RuntimeException("Unable to resolve retailer key for merchant analytics");
    }

    private String resolveStoreCode(AppUser user, String retailerKey) {
        String reflectedStoreCode = cleanUpper(readStringGetter(user, "getStoreCode"));

        if (!reflectedStoreCode.isBlank()) {
            return reflectedStoreCode;
        }

        List<Product> productsForRetailer = productRepository.findByRetailerKey(retailerKey);

        Optional<String> productStoreCode = productsForRetailer.stream()
                .map(Product::getStoreCode)
                .map(this::cleanUpper)
                .filter(value -> !value.isBlank())
                .findFirst();

        if (productStoreCode.isPresent()) {
            return productStoreCode.get();
        }

        return DEFAULT_STORE_CODE;
    }

    private DateWindow resolveWindow(MerchantAnalyticsRange range) {
        LocalDate today = LocalDate.now();

        return switch (range) {
            case DAILY -> new DateWindow(
                    today.atStartOfDay(),
                    today.atTime(LocalTime.MAX)
            );
            case WEEKLY -> {
                LocalDate start = today.with(DayOfWeek.MONDAY);
                LocalDate end = start.plusDays(6);

                yield new DateWindow(
                        start.atStartOfDay(),
                        end.atTime(LocalTime.MAX)
                );
            }
            case MONTHLY -> {
                YearMonth month = YearMonth.from(today);

                yield new DateWindow(
                        month.atDay(1).atStartOfDay(),
                        month.atEndOfMonth().atTime(LocalTime.MAX)
                );
            }
        };
    }

    private List<MerchantAnalyticsChartPointDTO> buildRevenueTrend(
            List<RetailOrder> orders,
            DateWindow window,
            MerchantAnalyticsRange range
    ) {
        List<MerchantAnalyticsChartPointDTO> points = createEmptyPoints(window, range);

        Map<String, BigDecimal> revenueByLabel = new HashMap<>();
        Map<String, Integer> countByLabel = new HashMap<>();

        for (RetailOrder order : orders) {
            LocalDateTime createdAt = safeCreatedAt(order);
            String label = trendLabel(createdAt, range);

            revenueByLabel.merge(label, safeTotal(order), BigDecimal::add);
            countByLabel.merge(label, 1, Integer::sum);
        }

        return points.stream()
                .map(point -> new MerchantAnalyticsChartPointDTO(
                        point.getLabel(),
                        countByLabel.getOrDefault(point.getLabel(), 0),
                        toDouble(revenueByLabel.getOrDefault(point.getLabel(), BigDecimal.ZERO))
                ))
                .toList();
    }

    private List<MerchantAnalyticsChartPointDTO> buildProductTrend(
            List<Product> products,
            DateWindow window,
            MerchantAnalyticsRange range,
            boolean scans
    ) {
        List<MerchantAnalyticsChartPointDTO> points = createEmptyPoints(window, range);

        int total = products.stream()
                .mapToInt(product -> scans ? safeScanCount(product) : safeSaveCount(product))
                .sum();

        if (points.isEmpty()) {
            return points;
        }

        int base = total / points.size();
        int remainder = total % points.size();

        List<MerchantAnalyticsChartPointDTO> result = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            int count = base + (i >= points.size() - remainder ? 1 : 0);

            result.add(new MerchantAnalyticsChartPointDTO(
                    points.get(i).getLabel(),
                    count,
                    (double) count
            ));
        }

        return result;
    }

    private List<MerchantAnalyticsChartPointDTO> createEmptyPoints(
            DateWindow window,
            MerchantAnalyticsRange range
    ) {
        List<MerchantAnalyticsChartPointDTO> points = new ArrayList<>();

        if (range == MerchantAnalyticsRange.DAILY) {
            for (int hour = 0; hour < 24; hour += 4) {
                points.add(new MerchantAnalyticsChartPointDTO(
                        String.format("%02d:00", hour),
                        0,
                        0.0
                ));
            }

            return points;
        }

        LocalDate start = window.start().toLocalDate();
        LocalDate end = window.end().toLocalDate();
        LocalDate cursor = start;

        while (!cursor.isAfter(end)) {
            points.add(new MerchantAnalyticsChartPointDTO(
                    trendLabel(cursor.atStartOfDay(), range),
                    0,
                    0.0
            ));

            cursor = cursor.plusDays(1);
        }

        return points;
    }

    private String trendLabel(LocalDateTime dateTime, MerchantAnalyticsRange range) {
        LocalDateTime safeDateTime = dateTime == null ? LocalDateTime.now() : dateTime;

        if (range == MerchantAnalyticsRange.DAILY) {
            int bucket = (safeDateTime.getHour() / 4) * 4;
            return String.format("%02d:00", bucket);
        }

        if (range == MerchantAnalyticsRange.MONTHLY) {
            return String.valueOf(safeDateTime.getDayOfMonth());
        }

        return safeDateTime.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.US);
    }

    private Optional<MerchantAnalyticsItemDTO> buildTopSellingItem(List<RetailOrder> orders) {
        Map<String, SoldItemAccumulator> soldItems = new HashMap<>();

        for (RetailOrder order : orders) {
            List<RetailOrderItem> items = safeOrderItems(order);

            for (RetailOrderItem item : items) {
                String rfid = clean(item.getRfid());

                if (rfid.isBlank()) {
                    continue;
                }

                SoldItemAccumulator accumulator = soldItems.computeIfAbsent(
                        rfid,
                        ignored -> new SoldItemAccumulator()
                );

                accumulator.rfid = rfid;
                accumulator.name = clean(item.getItemName());
                accumulator.category = clean(item.getCategory());
                accumulator.imageUrl = clean(item.getImageUrl());
                accumulator.quantity += Math.max(1, safeInteger(item.getQuantity()));
                accumulator.revenue = accumulator.revenue.add(safeItemTotal(item));
            }
        }

        return soldItems.values()
                .stream()
                .max(Comparator.comparingInt(SoldItemAccumulator::quantity))
                .map(accumulator -> {
                    MerchantAnalyticsItemDTO dto = new MerchantAnalyticsItemDTO();

                    dto.setRfid(accumulator.rfid);
                    dto.setName(accumulator.name);
                    dto.setBrand(accumulator.brand);
                    dto.setCategory(accumulator.category);
                    dto.setImageUrl(accumulator.imageUrl);
                    dto.setMetricLabel("sold");
                    dto.setMetricValue(accumulator.quantity);
                    dto.setRevenue(toDouble(accumulator.revenue));

                    return dto;
                });
    }

    private MerchantAnalyticsItemDTO toItemDTO(Product product, int metricValue, String metricLabel) {
        MerchantAnalyticsItemDTO dto = new MerchantAnalyticsItemDTO();

        dto.setRfid(clean(product.getRfid()));
        dto.setName(clean(product.getItemName()));
        dto.setBrand(clean(product.getBrand()));
        dto.setCategory(clean(product.getCategory()));
        dto.setColor(clean(product.getColor()));
        dto.setImageUrl(clean(product.getImageUrl()));
        dto.setPrice(toDouble(safePrice(product)));
        dto.setStockQuantity(safeStock(product));
        dto.setMetricLabel(clean(metricLabel));
        dto.setMetricValue(Math.max(0, metricValue));

        return dto;
    }

    private MerchantAnalyticsRecentSaleDTO toRecentSaleDTO(RetailOrder order) {
        MerchantAnalyticsRecentSaleDTO dto = new MerchantAnalyticsRecentSaleDTO();

        dto.setOrderNumber(clean(order.getOrderNumber()));
        dto.setReceiptNumber(clean(order.getOrderNumber()));
        dto.setCustomerName(clean(order.getEmail()));
        dto.setCreatedAt(safeCreatedAt(order));
        dto.setStatus(clean(order.getStatus()));
        dto.setItemCount(countItems(order));
        dto.setSubtotal(toDouble(safeSubtotal(order)));
        dto.setTax(toDouble(safeTax(order)));
        dto.setTotal(toDouble(safeTotal(order)));

        List<String> itemNames = safeOrderItems(order)
                .stream()
                .map(RetailOrderItem::getItemName)
                .map(this::clean)
                .filter(value -> !value.isBlank())
                .limit(4)
                .toList();

        dto.setItemNames(itemNames);

        return dto;
    }

    private boolean isLowStock(Product product) {
        int stock = safeStock(product);
        return stock > 0 && stock <= LOW_STOCK_THRESHOLD;
    }

    private boolean isOutOfStock(Product product) {
        return safeStock(product) <= 0 || !Boolean.TRUE.equals(product.getAvailable());
    }

    private int safeStock(Product product) {
        if (product == null) {
            return 0;
        }

        return Math.max(0, safeInteger(product.getStockQuantity()));
    }

    private int safeScanCount(Product product) {
        return Math.max(0, readIntegerGetter(product, "getScanCount"));
    }

    private int safeSaveCount(Product product) {
        return Math.max(0, readIntegerGetter(product, "getSaveCount"));
    }

    private int countItems(RetailOrder order) {
        if (order == null) {
            return 0;
        }

        Integer directCount = order.getItemCount();

        if (directCount != null && directCount > 0) {
            return directCount;
        }

        return safeOrderItems(order)
                .stream()
                .mapToInt(item -> Math.max(1, safeInteger(item.getQuantity())))
                .sum();
    }

    private List<RetailOrderItem> safeOrderItems(RetailOrder order) {
        if (order == null || order.getItems() == null) {
            return List.of();
        }

        return order.getItems()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private BigDecimal safePrice(Product product) {
        if (product == null || product.getPrice() == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(Math.max(0.0, product.getPrice()));
    }

    private BigDecimal safeSubtotal(RetailOrder order) {
        if (order == null || order.getSubtotal() == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(Math.max(0.0, order.getSubtotal()));
    }

    private BigDecimal safeTax(RetailOrder order) {
        if (order == null || order.getTax() == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(Math.max(0.0, order.getTax()));
    }

    private BigDecimal safeTotal(RetailOrder order) {
        if (order == null || order.getTotal() == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(Math.max(0.0, order.getTotal()));
    }

    private BigDecimal safeItemTotal(RetailOrderItem item) {
        if (item == null || item.getLineTotal() == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(Math.max(0.0, item.getLineTotal()));
    }

    private LocalDateTime safeCreatedAt(RetailOrder order) {
        if (order == null || order.getCreatedAt() == null) {
            return LocalDateTime.now();
        }

        return order.getCreatedAt();
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private String readStringGetter(Object target, String getterName) {
        if (target == null || getterName == null || getterName.isBlank()) {
            return "";
        }

        try {
            Method method = target.getClass().getMethod(getterName);
            Object value = method.invoke(target);
            return value == null ? "" : String.valueOf(value).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private int readIntegerGetter(Object target, String getterName) {
        if (target == null || getterName == null || getterName.isBlank()) {
            return 0;
        }

        try {
            Method method = target.getClass().getMethod(getterName);
            Object value = method.invoke(target);

            if (value instanceof Number number) {
                return Math.max(0, number.intValue());
            }

            if (value instanceof String text && !text.isBlank()) {
                return Math.max(0, Integer.parseInt(text.trim()));
            }

            return 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanUpper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private double toDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }

        return value
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double roundDouble(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record DateWindow(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }

    private static class SoldItemAccumulator {
        private String rfid = "";
        private String name = "";
        private String brand = "";
        private String category = "";
        private String imageUrl = "";
        private int quantity = 0;
        private BigDecimal revenue = BigDecimal.ZERO;

        private int quantity() {
            return quantity;
        }
    }
}
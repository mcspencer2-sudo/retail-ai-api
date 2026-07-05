package com.retailai.service;

import com.retailai.dto.OrderItemDTO;
import com.retailai.dto.OrderResponseDTO;
import com.retailai.model.BagItem;
import com.retailai.model.Product;
import com.retailai.model.RetailOrder;
import com.retailai.model.RetailOrderItem;
import com.retailai.model.TrendEvent;
import com.retailai.repository.BagItemRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.RetailOrderRepository;
import com.retailai.repository.TrendEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final double TAX_RATE = 0.0825;

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_REFUNDED = "REFUNDED";

    private final RetailOrderRepository retailOrderRepository;
    private final ProductRepository productRepository;
    private final BagItemRepository bagItemRepository;
    private final TrendEventRepository trendEventRepository;

    public OrderService(
            RetailOrderRepository retailOrderRepository,
            ProductRepository productRepository,
            BagItemRepository bagItemRepository,
            TrendEventRepository trendEventRepository
    ) {
        this.retailOrderRepository = retailOrderRepository;
        this.productRepository = productRepository;
        this.bagItemRepository = bagItemRepository;
        this.trendEventRepository = trendEventRepository;
    }

    @Transactional
    public OrderResponseDTO checkout(
            String userId,
            String tenantId,
            String storeId,
            String email,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase(Locale.ROOT);
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase(Locale.ROOT);

        List<BagItem> bagItems = loadScopedBagItems(
                userId,
                tenantId,
                safeRetailerKey,
                safeStoreCode
        );

        if (bagItems.isEmpty()) {
            throw new IllegalStateException("Your bag is empty.");
        }

        List<CheckoutLine> checkoutLines = buildCheckoutLines(
                bagItems,
                safeRetailerKey,
                safeStoreCode
        );

        validateCheckoutLines(checkoutLines);

        RetailOrder order = new RetailOrder();

        order.setOrderNumber(generateOrderNumber());
        order.setUserId(safe(userId));
        order.setTenantId(safe(tenantId));
        order.setStoreId(safe(storeId));
        order.setEmail(safe(email));
        order.setRetailerKey(safeRetailerKey);
        order.setStoreCode(safeStoreCode);
        order.setStatus(STATUS_COMPLETED);
        order.setCreatedAt(LocalDateTime.now());

        double subtotal = 0.0;
        String orderRetailerName = "";
        String orderStoreName = "";

        for (CheckoutLine checkoutLine : checkoutLines) {
            Product product = checkoutLine.product();
            BagItem sampleBagItem = checkoutLine.sampleBagItem();
            int quantity = checkoutLine.quantity();

            int currentStock = safeInteger(product.getStockQuantity());
            int newStock = Math.max(0, currentStock - quantity);

            product.setStockQuantity(newStock);
            product.setAvailable(Boolean.TRUE.equals(product.getActive()) && newStock > 0);
            productRepository.save(product);

            double unitPrice = product.getPrice() == null
                    ? safeMoney(sampleBagItem.getPrice())
                    : safeMoney(product.getPrice());

            double lineTotal = roundMoney(unitPrice * quantity);

            RetailOrderItem orderItem = new RetailOrderItem();
            orderItem.setRfid(product.getRfid());
            orderItem.setItemName(firstNonBlank(product.getItemName(), sampleBagItem.getItemName()));
            orderItem.setCategory(firstNonBlank(product.getCategory(), sampleBagItem.getCategory()));
            orderItem.setImageUrl(firstNonBlank(product.getImageUrl(), sampleBagItem.getImageUrl()));
            orderItem.setUnitPrice(roundMoney(unitPrice));
            orderItem.setQuantity(quantity);
            orderItem.setLineTotal(lineTotal);
            orderItem.setRetailerKey(firstNonBlank(product.getRetailerKey(), sampleBagItem.getRetailerKey()).toUpperCase(Locale.ROOT));
            orderItem.setRetailerName(firstNonBlank(product.getRetailerName(), sampleBagItem.getRetailerName()));
            orderItem.setStoreCode(firstNonBlank(product.getStoreCode(), sampleBagItem.getStoreCode()).toUpperCase(Locale.ROOT));
            orderItem.setStoreName(firstNonBlank(product.getStoreName(), sampleBagItem.getStoreName()));

            order.addItem(orderItem);

            subtotal += lineTotal;

            if (orderRetailerName.isBlank()) {
                orderRetailerName = firstNonBlank(product.getRetailerName(), sampleBagItem.getRetailerName());
            }

            if (orderStoreName.isBlank()) {
                orderStoreName = firstNonBlank(product.getStoreName(), sampleBagItem.getStoreName());
            }

            saveTrendEvent("PURCHASE", product);
        }

        double roundedSubtotal = roundMoney(subtotal);
        double roundedTax = roundMoney(roundedSubtotal * TAX_RATE);
        double roundedTotal = roundMoney(roundedSubtotal + roundedTax);

        order.setRetailerName(orderRetailerName);
        order.setStoreName(orderStoreName);
        order.setItemCount(
                order.getItems() == null
                        ? 0
                        : order.getItems()
                        .stream()
                        .mapToInt(item -> Math.max(1, safeInteger(item.getQuantity())))
                        .sum()
        );
        order.setSubtotal(roundedSubtotal);
        order.setTax(roundedTax);
        order.setTotal(roundedTotal);

        RetailOrder savedOrder = retailOrderRepository.save(order);

        clearScopedBag(
                userId,
                tenantId,
                safeRetailerKey,
                safeStoreCode
        );

        return toOrderResponseDto(savedOrder);
    }


    @Transactional
    public Map<String, Object> validateCheckout(
            String userId,
            String tenantId,
            String storeId,
            String email,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.")
                .toUpperCase(Locale.ROOT);

        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.")
                .toUpperCase(Locale.ROOT);

        List<BagItem> bagItems = loadScopedBagItems(
                userId,
                tenantId,
                safeRetailerKey,
                safeStoreCode
        );

        if (bagItems.isEmpty()) {
            throw new IllegalStateException("Your bag is empty.");
        }

        List<CheckoutLine> checkoutLines = buildCheckoutLines(
                bagItems,
                safeRetailerKey,
                safeStoreCode
        );

        validateCheckoutLines(checkoutLines);

        List<Map<String, Object>> items = new ArrayList<>();

        double subtotal = 0.0;
        int itemCount = 0;

        for (CheckoutLine checkoutLine : checkoutLines) {
            Product product = checkoutLine.product();
            BagItem sampleBagItem = checkoutLine.sampleBagItem();
            int quantity = Math.max(1, checkoutLine.quantity());

            double unitPrice = product.getPrice() == null
                    ? safeMoney(sampleBagItem.getPrice())
                    : safeMoney(product.getPrice());

            double lineTotal = roundMoney(unitPrice * quantity);

            subtotal += lineTotal;
            itemCount += quantity;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rfid", product.getRfid());
            item.put("itemName", firstNonBlank(product.getItemName(), sampleBagItem.getItemName()));
            item.put("category", firstNonBlank(product.getCategory(), sampleBagItem.getCategory()));
            item.put("imageUrl", firstNonBlank(product.getImageUrl(), sampleBagItem.getImageUrl()));
            item.put("unitPrice", roundMoney(unitPrice));
            item.put("quantity", quantity);
            item.put("lineTotal", lineTotal);
            item.put("retailerKey", firstNonBlank(product.getRetailerKey(), sampleBagItem.getRetailerKey()));
            item.put("retailerName", firstNonBlank(product.getRetailerName(), sampleBagItem.getRetailerName()));
            item.put("storeCode", firstNonBlank(product.getStoreCode(), sampleBagItem.getStoreCode()));
            item.put("storeName", firstNonBlank(product.getStoreName(), sampleBagItem.getStoreName()));
            item.put("active", Boolean.TRUE.equals(product.getActive()));
            item.put("available", Boolean.TRUE.equals(product.getAvailable()));
            item.put("stockQuantity", safeInteger(product.getStockQuantity()));

            items.add(item);
        }

        double roundedSubtotal = roundMoney(subtotal);
        double roundedTax = roundMoney(roundedSubtotal * TAX_RATE);
        double roundedTotal = roundMoney(roundedSubtotal + roundedTax);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("valid", true);
        response.put("message", "Checkout validation passed.");
        response.put("itemCount", itemCount);
        response.put("subtotal", roundedSubtotal);
        response.put("tax", roundedTax);
        response.put("total", roundedTotal);
        response.put("retailerKey", safeRetailerKey);
        response.put("storeCode", safeStoreCode);
        response.put("items", items);

        return response;
    }

    @Transactional
    public int reorderToBag(
            String orderNumber,
            String userId,
            String tenantId,
            String storeId,
            String email,
            String retailerKey,
            String storeCode
    ) {
        String safeOrderNumber = normalizeRequired(orderNumber, "Order number is required.");
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase(Locale.ROOT);
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase(Locale.ROOT);

        RetailOrder order = retailOrderRepository.findByOrderNumberAndRetailerKeyAndStoreCode(
                safeOrderNumber,
                safeRetailerKey,
                safeStoreCode
        ).orElseThrow(() -> new RuntimeException("Order not found: " + safeOrderNumber));

        List<RetailOrderItem> orderItems = order.getItems() == null
                ? new ArrayList<>()
                : order.getItems();

        if (orderItems.isEmpty()) {
            throw new IllegalStateException("This order has no items to buy again.");
        }

        List<BagItem> existingBagItems = loadScopedBagItems(
                userId,
                tenantId,
                safeRetailerKey,
                safeStoreCode
        );

        Set<String> existingRfids = existingBagItems.stream()
                .map(item -> safe(item.getRfid()).toUpperCase(Locale.ROOT))
                .filter(rfid -> !rfid.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> skippedUnavailable = new ArrayList<>();
        int addedCount = 0;

        for (RetailOrderItem orderItem : orderItems) {
            String rfid = safe(orderItem.getRfid());

            if (rfid.isBlank()) {
                continue;
            }

            String normalizedRfid = rfid.toUpperCase(Locale.ROOT);

            if (existingRfids.contains(normalizedRfid)) {
                continue;
            }

            Product product = productRepository.findByRfidAndRetailerKeyAndStoreCode(
                    normalizedRfid,
                    safeRetailerKey,
                    safeStoreCode
            ).orElse(null);

            if (!isPurchasable(product)) {
                skippedUnavailable.add(rfid);
                continue;
            }

            BagItem bagItem = new BagItem();

            bagItem.setUserId(safe(userId));
            bagItem.setTenantId(safe(tenantId));
            bagItem.setStoreId(safe(storeId));
            bagItem.setEmail(safe(email));

            bagItem.setRfid(product.getRfid());
            bagItem.setItemName(firstNonBlank(product.getItemName(), orderItem.getItemName()));
            bagItem.setCategory(firstNonBlank(product.getCategory(), orderItem.getCategory()));
            bagItem.setImageUrl(firstNonBlank(product.getImageUrl(), orderItem.getImageUrl()));
            bagItem.setPrice(product.getPrice() == null
                    ? safeMoney(orderItem.getUnitPrice())
                    : safeMoney(product.getPrice())
            );

            bagItem.setRetailerKey(firstNonBlank(product.getRetailerKey(), orderItem.getRetailerKey()).toUpperCase(Locale.ROOT));
            bagItem.setRetailerName(firstNonBlank(product.getRetailerName(), orderItem.getRetailerName()));
            bagItem.setStoreCode(firstNonBlank(product.getStoreCode(), orderItem.getStoreCode()).toUpperCase(Locale.ROOT));
            bagItem.setStoreName(firstNonBlank(product.getStoreName(), orderItem.getStoreName()));
            bagItem.setVibe("Buy Again");

            bagItemRepository.save(bagItem);

            existingRfids.add(normalizedRfid);
            addedCount += 1;

            saveTrendEvent("BUY_AGAIN", product);
        }

        if (addedCount <= 0) {
            if (!skippedUnavailable.isEmpty()) {
                throw new IllegalStateException("No items from this order are currently available to buy again.");
            }

            throw new IllegalStateException("All available items from this order are already in your bag.");
        }

        return addedCount;
    }

    @Transactional
    public List<OrderResponseDTO> getRecentOrders(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase(Locale.ROOT);
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase(Locale.ROOT);

        List<RetailOrder> orders;

        if (!safe(userId).isBlank()) {
            orders = retailOrderRepository.findByUserIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safe(userId),
                    safeRetailerKey,
                    safeStoreCode
            );
        } else if (!safe(tenantId).isBlank()) {
            orders = retailOrderRepository.findByTenantIdAndRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safe(tenantId),
                    safeRetailerKey,
                    safeStoreCode
            );
        } else {
            orders = retailOrderRepository.findByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                    safeRetailerKey,
                    safeStoreCode
            );
        }

        return orders.stream()
                .filter(order -> safe(order.getRetailerKey()).equalsIgnoreCase(safeRetailerKey))
                .filter(order -> safe(order.getStoreCode()).equalsIgnoreCase(safeStoreCode))
                .sorted(Comparator.comparing(
                        RetailOrder::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .map(this::toOrderResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<OrderResponseDTO> getRecentStoreOrders(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase(Locale.ROOT);
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase(Locale.ROOT);

        return retailOrderRepository.findByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                        safeRetailerKey,
                        safeStoreCode
                )
                .stream()
                .map(this::toOrderResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<OrderResponseDTO> searchStoreOrders(
            String retailerKey,
            String storeCode,
            String query,
            String status
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase(Locale.ROOT);
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase(Locale.ROOT);
        String safeQuery = safe(query).toLowerCase(Locale.ROOT);
        String safeStatus = safe(status).toUpperCase(Locale.ROOT);

        return retailOrderRepository.findByRetailerKeyAndStoreCodeOrderByCreatedAtDesc(
                        safeRetailerKey,
                        safeStoreCode
                )
                .stream()
                .filter(order -> safeStatus.isBlank() || safe(order.getStatus()).equalsIgnoreCase(safeStatus))
                .filter(order -> matchesOrderSearch(order, safeQuery))
                .map(this::toOrderResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDTO getOrderByOrderNumber(
            String orderNumber,
            String retailerKey,
            String storeCode
    ) {
        RetailOrder order = loadStoreOrder(orderNumber, retailerKey, storeCode);
        return toOrderResponseDto(order);
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(
            String orderNumber,
            String retailerKey,
            String storeCode,
            String requestedStatus
    ) {
        RetailOrder order = loadStoreOrder(orderNumber, retailerKey, storeCode);
        String targetStatus = normalizeOrderStatus(requestedStatus);
        String currentStatus = safe(order.getStatus()).toUpperCase(Locale.ROOT);

        if (currentStatus.equals(targetStatus)) {
            return toOrderResponseDto(order);
        }

        validateStatusTransition(currentStatus, targetStatus);

        order.setStatus(targetStatus);

        if (STATUS_CANCELLED.equals(targetStatus)
                || STATUS_RETURNED.equals(targetStatus)
                || STATUS_REFUNDED.equals(targetStatus)) {
            restoreStockForOrder(order);
        }

        RetailOrder saved = retailOrderRepository.save(order);
        saveOrderTrendEvent(targetStatus, saved);

        return toOrderResponseDto(saved);
    }

    @Transactional
    public Map<String, Object> simulateReceiptDelivery(
            String orderNumber,
            String retailerKey,
            String storeCode
    ) {
        RetailOrder order = loadStoreOrder(orderNumber, retailerKey, storeCode);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderNumber", order.getOrderNumber());
        response.put("email", safe(order.getEmail()).isBlank() ? "demo-customer@example.com" : safe(order.getEmail()));
        response.put("status", "SENT");
        response.put("message", "Simulated receipt delivery completed.");

        return response;
    }

    private RetailOrder loadStoreOrder(
            String orderNumber,
            String retailerKey,
            String storeCode
    ) {
        String safeOrderNumber = normalizeRequired(orderNumber, "Order number is required.");
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.").toUpperCase(Locale.ROOT);
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.").toUpperCase(Locale.ROOT);

        return retailOrderRepository.findByOrderNumberAndRetailerKeyAndStoreCode(
                safeOrderNumber,
                safeRetailerKey,
                safeStoreCode
        ).orElseThrow(() -> new RuntimeException("Order not found: " + safeOrderNumber));
    }

    private List<CheckoutLine> buildCheckoutLines(
            List<BagItem> bagItems,
            String retailerKey,
            String storeCode
    ) {
        Map<String, List<BagItem>> groupedByRfid = bagItems.stream()
                .filter(item -> item != null && !safe(item.getRfid()).isBlank())
                .collect(Collectors.groupingBy(
                        item -> safe(item.getRfid()).toUpperCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CheckoutLine> lines = new ArrayList<>();

        for (Map.Entry<String, List<BagItem>> entry : groupedByRfid.entrySet()) {
            String rfid = entry.getKey();
            List<BagItem> groupedItems = entry.getValue();

            Product product = productRepository.findByRfidAndRetailerKeyAndStoreCode(
                    rfid,
                    retailerKey,
                    storeCode
            ).orElseThrow(() -> new RuntimeException(
                    "Product no longer exists in this store for RFID: " + rfid
            ));

            lines.add(new CheckoutLine(
                    product,
                    groupedItems.get(0),
                    Math.max(1, groupedItems.size())
            ));
        }

        if (lines.isEmpty()) {
            throw new IllegalStateException("Your bag does not contain valid items.");
        }

        return lines;
    }

    private void validateCheckoutLines(List<CheckoutLine> checkoutLines) {
        List<String> errors = new ArrayList<>();

        for (CheckoutLine checkoutLine : checkoutLines) {
            Product product = checkoutLine.product();
            int quantity = checkoutLine.quantity();

            if (product == null) {
                errors.add("A product in your bag no longer exists.");
                continue;
            }

            String itemName = firstNonBlank(product.getItemName(), product.getRfid());
            int stock = safeInteger(product.getStockQuantity());

            if (!Boolean.TRUE.equals(product.getActive())) {
                errors.add(itemName + " is inactive.");
                continue;
            }

            if (!Boolean.TRUE.equals(product.getAvailable())) {
                errors.add(itemName + " is unavailable.");
                continue;
            }

            if (stock <= 0) {
                errors.add(itemName + " is out of stock.");
                continue;
            }

            if (stock < quantity) {
                errors.add(itemName + " only has " + stock + " left in stock.");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join(" ", errors));
        }
    }

    private void restoreStockForOrder(RetailOrder order) {
        if (order == null || order.getItems() == null) {
            return;
        }

        for (RetailOrderItem item : order.getItems()) {
            if (item == null || safe(item.getRfid()).isBlank()) {
                continue;
            }

            Product product = productRepository.findByRfidAndRetailerKeyAndStoreCode(
                    safe(item.getRfid()).toUpperCase(Locale.ROOT),
                    safe(order.getRetailerKey()).toUpperCase(Locale.ROOT),
                    safe(order.getStoreCode()).toUpperCase(Locale.ROOT)
            ).orElse(null);

            if (product == null) {
                continue;
            }

            int restoredStock = safeInteger(product.getStockQuantity()) + Math.max(1, safeInteger(item.getQuantity()));
            product.setStockQuantity(restoredStock);
            product.setAvailable(Boolean.TRUE.equals(product.getActive()) && restoredStock > 0);

            productRepository.save(product);
        }
    }

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        String safeCurrent = safe(currentStatus).toUpperCase(Locale.ROOT);
        String safeTarget = safe(targetStatus).toUpperCase(Locale.ROOT);

        if (STATUS_COMPLETED.equals(safeCurrent)) {
            if (STATUS_CANCELLED.equals(safeTarget)
                    || STATUS_RETURNED.equals(safeTarget)
                    || STATUS_REFUNDED.equals(safeTarget)) {
                return;
            }
        }

        if (STATUS_RETURNED.equals(safeCurrent) && STATUS_REFUNDED.equals(safeTarget)) {
            return;
        }

        throw new IllegalStateException(
                "Cannot change order status from " + safeCurrent + " to " + safeTarget + "."
        );
    }

    private String normalizeOrderStatus(String status) {
        String safeStatus = normalizeRequired(status, "Order status is required.").toUpperCase(Locale.ROOT);

        return switch (safeStatus) {
            case STATUS_COMPLETED, STATUS_CANCELLED, STATUS_RETURNED, STATUS_REFUNDED -> safeStatus;
            default -> throw new IllegalArgumentException("Unsupported order status: " + safeStatus);
        };
    }

    private boolean matchesOrderSearch(RetailOrder order, String query) {
        String safeQuery = safe(query).toLowerCase(Locale.ROOT);

        if (safeQuery.isBlank()) {
            return true;
        }

        if (safe(order.getOrderNumber()).toLowerCase(Locale.ROOT).contains(safeQuery)) {
            return true;
        }

        if (safe(order.getEmail()).toLowerCase(Locale.ROOT).contains(safeQuery)) {
            return true;
        }

        if (safe(order.getStoreName()).toLowerCase(Locale.ROOT).contains(safeQuery)) {
            return true;
        }

        if (order.getItems() == null) {
            return false;
        }

        return order.getItems()
                .stream()
                .anyMatch(item ->
                        safe(item.getItemName()).toLowerCase(Locale.ROOT).contains(safeQuery)
                                || safe(item.getRfid()).toLowerCase(Locale.ROOT).contains(safeQuery)
                                || safe(item.getCategory()).toLowerCase(Locale.ROOT).contains(safeQuery)
                );
    }

    private List<BagItem> loadScopedBagItems(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = safe(retailerKey).toUpperCase(Locale.ROOT);
        String safeStoreCode = safe(storeCode).toUpperCase(Locale.ROOT);

        if (!safe(userId).isBlank()) {
            return bagItemRepository.findByUserIdAndRetailerKeyAndStoreCode(
                    safe(userId),
                    safeRetailerKey,
                    safeStoreCode
            );
        }

        if (!safe(tenantId).isBlank()) {
            return bagItemRepository.findByTenantIdAndRetailerKeyAndStoreCode(
                    safe(tenantId),
                    safeRetailerKey,
                    safeStoreCode
            );
        }

        return bagItemRepository.findByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );
    }

    private void clearScopedBag(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = safe(retailerKey).toUpperCase(Locale.ROOT);
        String safeStoreCode = safe(storeCode).toUpperCase(Locale.ROOT);

        if (!safe(userId).isBlank()) {
            bagItemRepository.deleteByUserIdAndRetailerKeyAndStoreCode(
                    safe(userId),
                    safeRetailerKey,
                    safeStoreCode
            );
            return;
        }

        if (!safe(tenantId).isBlank()) {
            bagItemRepository.deleteByTenantIdAndRetailerKeyAndStoreCode(
                    safe(tenantId),
                    safeRetailerKey,
                    safeStoreCode
            );
            return;
        }

        bagItemRepository.deleteByRetailerKeyAndStoreCode(
                safeRetailerKey,
                safeStoreCode
        );
    }

    private boolean isPurchasable(Product product) {
        if (product == null) {
            return false;
        }

        if (!Boolean.TRUE.equals(product.getActive())) {
            return false;
        }

        if (!Boolean.TRUE.equals(product.getAvailable())) {
            return false;
        }

        return safeInteger(product.getStockQuantity()) > 0;
    }

    private void saveTrendEvent(String eventType, Product product) {
        if (product == null) {
            return;
        }

        TrendEvent event = new TrendEvent();

        event.setEventType(eventType);
        event.setRetailerName(product.getRetailerName());
        event.setRetailerKey(product.getRetailerKey());
        event.setStoreCode(product.getStoreCode());
        event.setItemName(product.getItemName());
        event.setCreatedAt(LocalDateTime.now());

        trendEventRepository.save(event);
    }

    private void saveOrderTrendEvent(String eventType, RetailOrder order) {
        if (order == null) {
            return;
        }

        TrendEvent event = new TrendEvent();

        event.setEventType(eventType);
        event.setRetailerName(order.getRetailerName());
        event.setRetailerKey(order.getRetailerKey());
        event.setStoreCode(order.getStoreCode());
        event.setItemName(order.getOrderNumber());
        event.setCreatedAt(LocalDateTime.now());

        trendEventRepository.save(event);
    }

    private OrderResponseDTO toOrderResponseDto(RetailOrder order) {
        OrderResponseDTO dto = new OrderResponseDTO();

        if (order == null) {
            return dto;
        }

        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setRetailerKey(order.getRetailerKey());
        dto.setRetailerName(order.getRetailerName());
        dto.setStoreCode(order.getStoreCode());
        dto.setStoreName(order.getStoreName());
        dto.setItemCount(order.getItemCount());
        dto.setSubtotal(order.getSubtotal());
        dto.setTax(order.getTax());
        dto.setTotal(order.getTotal());
        dto.setCreatedAt(order.getCreatedAt());

        List<OrderItemDTO> itemDtos = order.getItems() == null
                ? new ArrayList<>()
                : order.getItems()
                .stream()
                .map(this::toOrderItemDto)
                .collect(Collectors.toList());

        dto.setItems(itemDtos);

        if (dto.getItemCount() == 0 && !itemDtos.isEmpty()) {
            dto.setItemCount(
                    itemDtos.stream()
                            .mapToInt(item -> Math.max(1, item.getQuantity() == null ? 1 : item.getQuantity()))
                            .sum()
            );
        }

        return dto;
    }

    private OrderItemDTO toOrderItemDto(RetailOrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();

        if (item == null) {
            return dto;
        }

        dto.setRfid(item.getRfid());
        dto.setItemName(item.getItemName());
        dto.setCategory(item.getCategory());
        dto.setImageUrl(item.getImageUrl());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(item.getLineTotal());
        dto.setRetailerKey(item.getRetailerKey());
        dto.setRetailerName(item.getRetailerName());
        dto.setStoreCode(item.getStoreCode());
        dto.setStoreName(item.getStoreName());

        return dto;
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now()
                .toString()
                .replace("-", "")
                .replace(":", "")
                .replace(".", "")
                .replace("T", "");

        if (timestamp.length() > 14) {
            timestamp = timestamp.substring(0, 14);
        }

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase(Locale.ROOT);

        return "US-" + timestamp + "-" + suffix;
    }

    private double roundMoney(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.round(Math.max(0.0, value) * 100.0) / 100.0;
    }

    private double safeMoney(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, value);
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = safe(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String firstNonBlank(String first, String second) {
        String safeFirst = safe(first);

        if (!safeFirst.isBlank()) {
            return safeFirst;
        }

        return safe(second);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record CheckoutLine(
            Product product,
            BagItem sampleBagItem,
            int quantity
    ) {
    }
}
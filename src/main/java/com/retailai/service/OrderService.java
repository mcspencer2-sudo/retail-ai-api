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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final double TAX_RATE = 0.0825;

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
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.");
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.");

        List<BagItem> bagItems = loadScopedBagItems(
                userId,
                tenantId,
                safeRetailerKey,
                safeStoreCode
        );

        if (bagItems.isEmpty()) {
            throw new IllegalStateException("Your bag is empty.");
        }

        RetailOrder order = new RetailOrder();

        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setTenantId(tenantId);
        order.setStoreId(storeId);
        order.setEmail(email);
        order.setRetailerKey(safeRetailerKey);
        order.setStoreCode(safeStoreCode);
        order.setStatus("COMPLETED");
        order.setCreatedAt(LocalDateTime.now());

        double subtotal = 0.0;
        String orderRetailerName = "";
        String orderStoreName = "";

        for (BagItem bagItem : bagItems) {
            String rfid = normalizeRequired(bagItem.getRfid(), "Bag item is missing RFID.");

            Product product = productRepository.findByRfidAndRetailerKeyAndStoreCode(
                    rfid,
                    safeRetailerKey,
                    safeStoreCode
            ).orElseThrow(() -> new RuntimeException(
                    "Product no longer exists in this store for RFID: " + rfid
            ));

            validatePurchasable(product);

            int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

            product.setStockQuantity(currentStock - 1);
            product.setAvailable(Boolean.TRUE.equals(product.getActive()) && product.getStockQuantity() > 0);

            productRepository.save(product);

            double unitPrice = product.getPrice() == null
                    ? bagItem.getPrice()
                    : product.getPrice();

            double lineTotal = unitPrice;

            RetailOrderItem orderItem = new RetailOrderItem();
            orderItem.setRfid(product.getRfid());
            orderItem.setItemName(firstNonBlank(product.getItemName(), bagItem.getItemName()));
            orderItem.setCategory(firstNonBlank(product.getCategory(), bagItem.getCategory()));
            orderItem.setImageUrl(firstNonBlank(product.getImageUrl(), bagItem.getImageUrl()));
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(1);
            orderItem.setLineTotal(lineTotal);
            orderItem.setRetailerKey(firstNonBlank(product.getRetailerKey(), bagItem.getRetailerKey()));
            orderItem.setRetailerName(firstNonBlank(product.getRetailerName(), bagItem.getRetailerName()));
            orderItem.setStoreCode(firstNonBlank(product.getStoreCode(), bagItem.getStoreCode()));
            orderItem.setStoreName(firstNonBlank(product.getStoreName(), bagItem.getStoreName()));

            order.addItem(orderItem);

            subtotal += lineTotal;

            if (orderRetailerName.isBlank()) {
                orderRetailerName = firstNonBlank(product.getRetailerName(), bagItem.getRetailerName());
            }

            if (orderStoreName.isBlank()) {
                orderStoreName = firstNonBlank(product.getStoreName(), bagItem.getStoreName());
            }

            savePurchaseTrendEvent(product);
        }

        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        order.setRetailerName(orderRetailerName);
        order.setStoreName(orderStoreName);
        order.setItemCount(bagItems.size());
        order.setSubtotal(roundMoney(subtotal));
        order.setTax(roundMoney(tax));
        order.setTotal(roundMoney(total));

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
    public List<OrderResponseDTO> getRecentOrders(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.");
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.");

        List<RetailOrder> orders;

        if (!safe(userId).isBlank()) {
            orders = retailOrderRepository.findByUserIdOrderByCreatedAtDesc(safe(userId));
        } else if (!safe(tenantId).isBlank()) {
            orders = retailOrderRepository.findByTenantIdAndStoreCodeOrderByCreatedAtDesc(
                    safe(tenantId),
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
                .map(this::toOrderResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDTO getOrderByOrderNumber(
            String orderNumber,
            String retailerKey,
            String storeCode
    ) {
        String safeOrderNumber = normalizeRequired(orderNumber, "Order number is required.");
        String safeRetailerKey = normalizeRequired(retailerKey, "Retailer key is required.");
        String safeStoreCode = normalizeRequired(storeCode, "Store code is required.");

        RetailOrder order = retailOrderRepository.findByOrderNumberAndRetailerKeyAndStoreCode(
                safeOrderNumber,
                safeRetailerKey,
                safeStoreCode
        ).orElseThrow(() -> new RuntimeException("Order not found: " + safeOrderNumber));

        return toOrderResponseDto(order);
    }

    private List<BagItem> loadScopedBagItems(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        if (!safe(userId).isBlank()) {
            return bagItemRepository.findByUserId(safe(userId)).stream()
                    .filter(item -> safe(item.getRetailerKey()).equalsIgnoreCase(retailerKey))
                    .filter(item -> safe(item.getStoreCode()).equalsIgnoreCase(storeCode))
                    .collect(Collectors.toList());
        }

        if (!safe(tenantId).isBlank()) {
            return bagItemRepository.findByTenantIdAndStoreCode(
                            safe(tenantId),
                            storeCode
                    ).stream()
                    .filter(item -> safe(item.getRetailerKey()).equalsIgnoreCase(retailerKey))
                    .collect(Collectors.toList());
        }

        return bagItemRepository.findByRetailerKeyAndStoreCode(retailerKey, storeCode);
    }

    private void clearScopedBag(
            String userId,
            String tenantId,
            String retailerKey,
            String storeCode
    ) {
        if (!safe(userId).isBlank()) {
            List<BagItem> itemsToDelete = bagItemRepository.findByUserId(safe(userId)).stream()
                    .filter(item -> safe(item.getRetailerKey()).equalsIgnoreCase(retailerKey))
                    .filter(item -> safe(item.getStoreCode()).equalsIgnoreCase(storeCode))
                    .collect(Collectors.toList());

            bagItemRepository.deleteAll(itemsToDelete);
            return;
        }

        if (!safe(tenantId).isBlank()) {
            List<BagItem> itemsToDelete = bagItemRepository.findByTenantIdAndStoreCode(
                            safe(tenantId),
                            storeCode
                    ).stream()
                    .filter(item -> safe(item.getRetailerKey()).equalsIgnoreCase(retailerKey))
                    .collect(Collectors.toList());

            bagItemRepository.deleteAll(itemsToDelete);
            return;
        }

        bagItemRepository.deleteByRetailerKeyAndStoreCode(retailerKey, storeCode);
    }

    private void validatePurchasable(Product product) {
        if (product == null) {
            throw new RuntimeException("Product not found.");
        }

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException("Product is inactive: " + product.getRfid());
        }

        if (!Boolean.TRUE.equals(product.getAvailable())) {
            throw new IllegalStateException("Product is unavailable: " + product.getRfid());
        }

        int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        if (stock <= 0) {
            throw new IllegalStateException("Product is out of stock: " + product.getRfid());
        }
    }

    private void savePurchaseTrendEvent(Product product) {
        TrendEvent event = new TrendEvent();
        event.setEventType("PURCHASE");
        event.setRetailerName(product.getRetailerName());
        event.setRetailerKey(product.getRetailerKey());
        event.setStoreCode(product.getStoreCode());
        event.setItemName(product.getItemName());
        event.setCreatedAt(LocalDateTime.now());

        trendEventRepository.save(event);
    }

    private OrderResponseDTO toOrderResponseDto(RetailOrder order) {
        OrderResponseDTO dto = new OrderResponseDTO();

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

        List<OrderItemDTO> itemDtos = order.getItems().stream()
                .map(this::toOrderItemDto)
                .collect(Collectors.toList());

        dto.setItems(itemDtos);

        return dto;
    }

    private OrderItemDTO toOrderItemDto(RetailOrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();

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
                .replace("T", "")
                .substring(0, 14);

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase(Locale.ROOT);

        return "US-" + timestamp + "-" + suffix;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
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
}
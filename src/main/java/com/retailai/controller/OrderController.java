package com.retailai.controller;

import com.retailai.dto.OrderResponseDTO;
import com.retailai.service.AuthContextService;
import com.retailai.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private static final HttpStatusCode UNPROCESSABLE_ENTITY =
            HttpStatusCode.valueOf(422);

    private final OrderService orderService;
    private final AuthContextService authContextService;

    public OrderController(
            OrderService orderService,
            AuthContextService authContextService
    ) {
        this.orderService = orderService;
        this.authContextService = authContextService;
    }

    @PostMapping("/checkout/validate")
    public ResponseEntity<?> validateCheckout(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            Map<String, Object> validation = orderService.validateCheckout(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    toText(auth.email()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(validation);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (IllegalStateException e) {
            return unprocessableEntity(e);
        } catch (RuntimeException e) {
            return internalServerError("Checkout validation failed: " + safeMessage(e), e);
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            OrderResponseDTO order = orderService.checkout(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    toText(auth.email()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (IllegalStateException e) {
            return unprocessableEntity(e);
        } catch (RuntimeException e) {
            return internalServerError("Checkout failed: " + safeMessage(e), e);
        }
    }

    @GetMapping("/my-history")
    public ResponseEntity<?> getMyOrderHistory(HttpServletRequest request) {
        return getRecentOrders(request);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getOrderHistory(HttpServletRequest request) {
        return getRecentOrders(request);
    }

    @GetMapping
    public ResponseEntity<?> getRecentOrders(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<OrderResponseDTO> orders = orderService.getRecentOrders(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(orders);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Order history load failed: " + safeMessage(e), e);
        }
    }

    @GetMapping("/store/recent")
    public ResponseEntity<?> getRecentStoreOrders(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<OrderResponseDTO> orders = orderService.getRecentStoreOrders(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(orders);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Store order activity load failed: " + safeMessage(e), e);
        }
    }

    @GetMapping("/store/search")
    public ResponseEntity<?> searchStoreOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String status
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<OrderResponseDTO> orders = orderService.searchStoreOrders(
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase(),
                    query,
                    status
            );

            return ResponseEntity.ok(orders);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Order search failed: " + safeMessage(e), e);
        }
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<?> getOrder(
            HttpServletRequest request,
            @PathVariable String orderNumber
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            OrderResponseDTO order = orderService.getOrderByOrderNumber(
                    normalizeRequired(orderNumber, "Order number is required."),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(safeMessage(e)));
        }
    }

    @PostMapping("/{orderNumber}/buy-again")
    public ResponseEntity<?> buyAgain(
            HttpServletRequest request,
            @PathVariable String orderNumber
    ) {
        return reorderToBag(request, orderNumber, "Buy Again failed: ");
    }

    @PostMapping("/{orderNumber}/reorder")
    public ResponseEntity<?> reorder(
            HttpServletRequest request,
            @PathVariable String orderNumber
    ) {
        return reorderToBag(request, orderNumber, "Reorder failed: ");
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<?> cancelOrder(
            HttpServletRequest request,
            @PathVariable String orderNumber
    ) {
        return updateOrderStatus(request, orderNumber, "CANCELLED", "Order cancelled.");
    }

    @PostMapping("/{orderNumber}/return")
    public ResponseEntity<?> returnOrder(
            HttpServletRequest request,
            @PathVariable String orderNumber
    ) {
        return updateOrderStatus(request, orderNumber, "RETURNED", "Order marked as returned.");
    }

    @PostMapping("/{orderNumber}/refund")
    public ResponseEntity<?> refundOrder(
            HttpServletRequest request,
            @PathVariable String orderNumber
    ) {
        return updateOrderStatus(request, orderNumber, "REFUNDED", "Order refunded.");
    }

    @PostMapping("/{orderNumber}/send-receipt")
    public ResponseEntity<?> sendReceipt(
            HttpServletRequest request,
            @PathVariable String orderNumber
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            Map<String, Object> response = orderService.simulateReceiptDelivery(
                    normalizeRequired(orderNumber, "Order number is required."),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return internalServerError("Receipt delivery failed: " + safeMessage(e), e);
        }
    }

    private ResponseEntity<?> updateOrderStatus(
            HttpServletRequest request,
            String orderNumber,
            String status,
            String message
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            OrderResponseDTO order = orderService.updateOrderStatus(
                    normalizeRequired(orderNumber, "Order number is required."),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase(),
                    status
            );

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", message);
            response.put("order", order);

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (IllegalStateException e) {
            return unprocessableEntity(e);
        } catch (RuntimeException e) {
            return internalServerError("Order status update failed: " + safeMessage(e), e);
        }
    }

    private ResponseEntity<?> reorderToBag(
            HttpServletRequest request,
            String orderNumber,
            String failurePrefix
    ) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            int addedCount = orderService.reorderToBag(
                    normalizeRequired(orderNumber, "Order number is required."),
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    toText(auth.email()),
                    normalizeRequired(auth.retailerKey(), "Store context is missing retailer key.").toUpperCase(),
                    normalizeRequired(auth.storeCode(), "Store context is missing store code.").toUpperCase()
            );

            return ResponseEntity.ok(buildBuyAgainResponse(
                    addedCount,
                    addedCount + " item(s) added back to your bag."
            ));
        } catch (SecurityException e) {
            return forbidden(e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (IllegalStateException e) {
            return unprocessableEntity(e);
        } catch (RuntimeException e) {
            return internalServerError(failurePrefix + safeMessage(e), e);
        }
    }

    private AuthContextService.AuthContext requireAuthenticated(HttpServletRequest request) {
        return authContextService.getAuthContext(request);
    }

    private Map<String, Object> buildBuyAgainResponse(int addedCount, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("addedCount", Math.max(addedCount, 0));
        response.put("message", message);
        return response;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message == null || message.isBlank() ? "Request failed." : message);
        return response;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = toText(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String toText(Object value) {
        return Objects.toString(value, "").trim();
    }

    private String safeMessage(Exception e) {
        String message = e == null ? "" : e.getMessage();
        return message == null || message.isBlank() ? "Request failed." : message;
    }

    private ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> forbidden(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> unprocessableEntity(Exception e) {
        return ResponseEntity.status(UNPROCESSABLE_ENTITY).body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> internalServerError(String message, Exception e) {
        log.error(message, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(message));
    }
}
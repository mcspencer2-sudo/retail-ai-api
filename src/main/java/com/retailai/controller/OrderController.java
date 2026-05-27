package com.retailai.controller;

import com.retailai.dto.OrderResponseDTO;
import com.retailai.service.AuthContextService;
import com.retailai.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthContextService authContextService;

    public OrderController(
            OrderService orderService,
            AuthContextService authContextService
    ) {
        this.orderService = orderService;
        this.authContextService = authContextService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            OrderResponseDTO order = orderService.checkout(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    toText(auth.storeId()),
                    auth.email(),
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Checkout failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getRecentOrders(HttpServletRequest request) {
        try {
            AuthContextService.AuthContext auth = requireAuthenticated(request);

            List<OrderResponseDTO> orders = orderService.getRecentOrders(
                    toText(auth.userId()),
                    toText(auth.tenantId()),
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(orders);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Order history load failed: " + e.getMessage());
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
                    orderNumber,
                    auth.retailerKey(),
                    auth.storeCode()
            );

            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    private AuthContextService.AuthContext requireAuthenticated(HttpServletRequest request) {
        return authContextService.getAuthContext(request);
    }

    private String toText(Object value) {
        return Objects.toString(value, "");
    }
}
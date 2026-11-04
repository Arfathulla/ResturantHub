package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.dto.OrderResponseDto;
import com.restauranthub.dto.PlaceOrderRequestDto;
import com.restauranthub.dto.UpdateOrderStatusRequestDto;
import com.restauranthub.entity.User;
import com.restauranthub.service.AuthService;
import com.restauranthub.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/orders", "/api/orders"})
@RequiredArgsConstructor
@Tag(name = "Food Orders", description = "Endpoints for order checkout, lifecycle progression, and live order tracking")
public class OrderRestController {

    private final OrderService orderService;
    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Place a new food order from current cart with stock validation and coupon redemption")
    public ResponseEntity<ApiResponse<OrderResponseDto>> placeOrder(@Valid @RequestBody PlaceOrderRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        OrderResponseDto response = orderService.placeOrder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping({"/{orderIdentifier}", "/{orderIdentifier}/details"})
    @Operation(summary = "Get order tracking and item details by order number or order ID")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrder(@PathVariable String orderIdentifier) {
        User user = authService.getCurrentAuthenticatedUser();
        OrderResponseDto response = orderService.getOrderByNumber(orderIdentifier, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping({"/{orderIdentifier}/tracking", "/{orderIdentifier}/track"})
    @Operation(summary = "Get real-time tracking information for a specific order with customer ownership validation")
    public ResponseEntity<ApiResponse<OrderResponseDto.DeliveryTrackingDto>> getOrderTracking(@PathVariable String orderIdentifier) {
        User user = authService.getCurrentAuthenticatedUser();
        OrderResponseDto.DeliveryTrackingDto tracking = orderService.getOrderTracking(orderIdentifier, user);
        return ResponseEntity.ok(ApiResponse.success(tracking));
    }

    @GetMapping({"/my-orders", "/me"})
    @Operation(summary = "Get all past and active food orders for current customer")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getMyOrders() {
        User user = authService.getCurrentAuthenticatedUser();
        List<OrderResponseDto> orders = orderService.getCustomerOrders(user.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping({"/active", "/active-deliveries"})
    @Operation(summary = "Get currently active food delivery orders for the authenticated customer")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getActiveDeliveries() {
        User user = authService.getCurrentAuthenticatedUser();
        List<OrderResponseDto> activeOrders = orderService.getCustomerActiveOrders(user.getId());
        return ResponseEntity.ok(ApiResponse.success(activeOrders));
    }

    @RequestMapping(
            value = "/{orderIdentifier}/status",
            method = {RequestMethod.PATCH, RequestMethod.PUT}
    )
    @Operation(summary = "Update order progress status (Kitchen / Staff / Admin authorization enforced)")
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateStatus(
            @PathVariable String orderIdentifier,
            @Valid @RequestBody UpdateOrderStatusRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        OrderResponseDto updated = orderService.updateOrderStatus(orderIdentifier, request, user);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updated));
    }
}

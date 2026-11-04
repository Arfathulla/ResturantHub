package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.dto.OrderResponseDto;
import com.restauranthub.entity.Order;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.entity.enums.OrderType;
import com.restauranthub.repository.OrderRepository;
import com.restauranthub.service.AuthService;
import com.restauranthub.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

@RestController
@RequestMapping({"/api/v1/tracking", "/api/tracking"})
@RequiredArgsConstructor
@Tag(name = "Delivery Tracking", description = "Real-time delivery status stream and tracking endpoints")
public class OrderTrackingController {

    private static final Logger log = LoggerFactory.getLogger(OrderTrackingController.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuthService authService;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * SSE endpoint: GET /api/v1/tracking/{orderNumber}/stream
     * Returns server-sent events with live order status updates directly from MySQL.
     */
    @GetMapping(value = "/{orderNumber}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderStatus(@PathVariable String orderNumber) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 min timeout

        User currentUser;
        try {
            currentUser = authService.getCurrentAuthenticatedUser();
        } catch (Exception e) {
            log.warn("Unauthenticated SSE tracking attempt for order: {}", orderNumber);
            sendErrorAndComplete(emitter, "Authentication required to track orders.");
            return emitter;
        }

        Order order;
        try {
            order = orderService.findOrderByNumberOrId(orderNumber);
        } catch (Exception e) {
            log.warn("SSE tracking attempted for non-existent order: {}", orderNumber);
            sendErrorAndComplete(emitter, "No active deliveries. Place an order to track your delivery.");
            return emitter;
        }

        // Ownership validation: customer can only track their own orders
        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_MANAGER") ||
                               a.getAuthority().equals("ROLE_STAFF") ||
                               a.getAuthority().equals("ROLE_KITCHEN"));

        if (!isStaffOrAdmin && !order.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to track order {} owned by user {}",
                    currentUser.getEmail(), orderNumber, order.getUser().getEmail());
            sendErrorAndComplete(emitter, "Access denied: You do not own this order.");
            return emitter;
        }

        final String resolvedOrderNumber = order.getOrderNumber();

        // If order is already in a terminal state, send current status once and complete
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.DELIVERED ||
            currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.REFUNDED) {
            try {
                String statusJson = buildStatusJson(order);
                emitter.send(SseEmitter.event().name("status-update").data(statusJson));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        emitters.put(resolvedOrderNumber, emitter);

        // Schedule periodic DB polling with safe cancellation
        ScheduledFuture<?> pollFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                Order currentOrder = orderRepository.findByOrderNumber(resolvedOrderNumber).orElse(null);
                if (currentOrder == null) {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"message\":\"Order not found\"}"));
                    emitter.complete();
                    return;
                }

                String statusJson = buildStatusJson(currentOrder);
                emitter.send(SseEmitter.event().name("status-update").data(statusJson));

                OrderStatus st = currentOrder.getStatus();
                if (st == OrderStatus.COMPLETED || st == OrderStatus.DELIVERED ||
                    st == OrderStatus.CANCELLED || st == OrderStatus.REFUNDED) {
                    emitter.complete();
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("Error polling order status for {}: {}", resolvedOrderNumber, e.getMessage());
                emitter.completeWithError(e);
            }
        }, 0, 5, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            emitters.remove(resolvedOrderNumber);
            pollFuture.cancel(true);
        });
        emitter.onTimeout(() -> {
            emitters.remove(resolvedOrderNumber);
            pollFuture.cancel(true);
        });
        emitter.onError(e -> {
            emitters.remove(resolvedOrderNumber);
            pollFuture.cancel(true);
        });

        return emitter;
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get current delivery tracking snapshot for an order")
    public ResponseEntity<ApiResponse<OrderResponseDto.DeliveryTrackingDto>> getTrackingSnapshot(
            @PathVariable String orderNumber) {
        User user = authService.getCurrentAuthenticatedUser();
        OrderResponseDto.DeliveryTrackingDto tracking = orderService.getOrderTracking(orderNumber, user);
        return ResponseEntity.ok(ApiResponse.success(tracking));
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data("{\"message\":\"" + message + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private String buildStatusJson(Order order) {
        String etaMessage = getEtaMessage(order);
        String statusLabel = order.getStatus().getDisplayLabel();
        int progressPercent = OrderService.getProgressPercent(order.getStatus());

        return String.format(
            "{\"orderNumber\":\"%s\",\"status\":\"%s\",\"statusLabel\":\"%s\",\"progress\":%d,\"etaMessage\":\"%s\",\"orderType\":\"%s\",\"timestamp\":\"%s\"}",
            order.getOrderNumber(),
            order.getStatus().name(),
            statusLabel,
            progressPercent,
            etaMessage,
            order.getOrderType().name(),
            LocalDateTime.now().toString()
        );
    }

    private String getEtaMessage(Order order) {
        OrderStatus norm = OrderStatus.fromString(order.getStatus().name());
        return switch (norm) {
            case PLACED -> "Waiting for restaurant to confirm order...";
            case ACCEPTED -> "Restaurant confirmed! Kitchen is preparing your order...";
            case PREPARING -> "Chef is freshly preparing your dishes 🔥";
            case READY_FOR_PICKUP -> order.getOrderType() == OrderType.DELIVERY
                ? "Order packed! Awaiting delivery partner pickup..."
                : "Your order is ready at the counter! 🎉";
            case OUT_FOR_DELIVERY -> "Out for delivery! On the way to your address 🛵";
            case COMPLETED -> "Delivered! Enjoy your meal 🍽️";
            case CANCELLED -> "Order has been cancelled.";
            default -> "Processing your order...";
        };
    }
}

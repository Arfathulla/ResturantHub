package com.restauranthub.controller;

import com.restauranthub.entity.Order;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.entity.enums.OrderType;
import com.restauranthub.repository.OrderRepository;
import com.restauranthub.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class OrderTrackingController {

    private static final Logger log = LoggerFactory.getLogger(OrderTrackingController.class);

    private final OrderRepository orderRepository;
    private final AuthService authService;

    // Store active emitters per order number
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * SSE endpoint: GET /api/v1/tracking/{orderNumber}/stream
     * Returns server-sent events with live order status updates
     */
    @GetMapping(value = "/{orderNumber}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderStatus(@PathVariable String orderNumber) {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 min timeout

        emitters.put(orderNumber, emitter);

        emitter.onCompletion(() -> emitters.remove(orderNumber));
        emitter.onTimeout(() -> emitters.remove(orderNumber));
        emitter.onError(e -> emitters.remove(orderNumber));

        // Poll DB every 5 seconds and push updates
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
                if (order == null) {
                    emitter.send(SseEmitter.event().name("error").data("{\"message\":\"Order not found\"}"));
                    emitter.complete();
                    return;
                }

                String statusJson = buildStatusJson(order);
                emitter.send(SseEmitter.event().name("status-update").data(statusJson));

                // If terminal status, complete the stream
                if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
                    emitter.complete();
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 5, TimeUnit.SECONDS);

        return emitter;
    }

    private String buildStatusJson(Order order) {
        String etaMessage = getEtaMessage(order);
        String driverInfo = getDriverInfo(order);
        String statusLabel = getStatusLabel(order.getStatus());
        int progressPercent = getProgressPercent(order.getStatus());

        return String.format(
            "{\"orderNumber\":\"%s\",\"status\":\"%s\",\"statusLabel\":\"%s\",\"progress\":%d,\"etaMessage\":\"%s\",\"driverInfo\":\"%s\",\"orderType\":\"%s\",\"timestamp\":\"%s\"}",
            order.getOrderNumber(),
            order.getStatus().name(),
            statusLabel,
            progressPercent,
            etaMessage,
            driverInfo,
            order.getOrderType().name(),
            LocalDateTime.now().toString()
        );
    }

    private String getEtaMessage(Order order) {
        return switch (order.getStatus()) {
            case PLACED -> "Waiting for restaurant to accept... (est. 1-2 min)";
            case ACCEPTED -> "Restaurant confirmed! Kitchen is preparing your order...";
            case PREPARING -> "Your food is being freshly prepared \uD83D\uDD25 (est. 8-12 min)";
            case READY_FOR_PICKUP -> order.getOrderType() == OrderType.DELIVERY
                ? "Order packed! Delivery partner picking up..."
                : "Your order is ready at the counter! \uD83C\uDF89";
            case OUT_FOR_DELIVERY -> "Ravi Kumar is on the way! Est. 15-20 min \uD83D\uDEF5";
            case COMPLETED -> "Delivered! Enjoy your meal \uD83C\uDF7D\uFE0F Rate your experience";
            case CANCELLED -> "Order cancelled. Refund initiated within 3-5 business days.";
            default -> "Processing your order...";
        };
    }

    private String getDriverInfo(Order order) {
        if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            return "Ravi Kumar \u00B7 KA-05-AB-1234 \u00B7 \u260E +91-9876543210";
        }
        return "";
    }

    private String getStatusLabel(OrderStatus status) {
        return switch (status) {
            case PLACED -> "Order Placed";
            case ACCEPTED -> "Confirmed";
            case PREPARING -> "Preparing";
            case READY_FOR_PICKUP -> "Ready";
            case OUT_FOR_DELIVERY -> "On the Way";
            case COMPLETED -> "Delivered";
            case CANCELLED -> "Cancelled";
            default -> status.name();
        };
    }

    private int getProgressPercent(OrderStatus status) {
        return switch (status) {
            case PLACED -> 10;
            case ACCEPTED -> 25;
            case PREPARING -> 50;
            case READY_FOR_PICKUP -> 70;
            case OUT_FOR_DELIVERY -> 85;
            case COMPLETED -> 100;
            case CANCELLED -> 0;
            default -> 0;
        };
    }
}

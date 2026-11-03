package com.restauranthub.service;

import com.restauranthub.entity.Order;
import com.restauranthub.entity.OrderStatusHistory;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.entity.enums.OrderType;
import com.restauranthub.repository.OrderRepository;
import com.restauranthub.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@EnableAsync
public class OrderStatusProgressionService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusProgressionService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;

    /**
     * Simulates realistic order lifecycle progression asynchronously.
     * PLACED → ACCEPTED (25s) → PREPARING (90s) → READY_FOR_PICKUP (3min) → OUT_FOR_DELIVERY (if delivery, 4min) → COMPLETED (5.5min)
     */
    @Async
    public void startOrderProgression(String orderNumber) {
        try {
            log.info("Starting realistic order progression for: {}", orderNumber);

            // Stage 1: ACCEPTED after ~25 seconds (restaurant confirms order)
            TimeUnit.SECONDS.sleep(25);
            advanceStatus(orderNumber, OrderStatus.ACCEPTED, "Order accepted by restaurant — kitchen notified");

            // Stage 2: PREPARING after ~90 seconds
            TimeUnit.SECONDS.sleep(90);
            advanceStatus(orderNumber, OrderStatus.PREPARING, "Chef has started preparing your order");

            // Stage 3: READY_FOR_PICKUP after ~3 minutes
            TimeUnit.SECONDS.sleep(180);
            advanceStatus(orderNumber, OrderStatus.READY_FOR_PICKUP, "Order is ready! Packed and sealed.");

            // Stage 4: OUT_FOR_DELIVERY only if delivery order, after ~1 minute
            Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
            if (order != null && order.getOrderType() == OrderType.DELIVERY) {
                TimeUnit.SECONDS.sleep(60);
                advanceStatus(orderNumber, OrderStatus.OUT_FOR_DELIVERY, "Delivery partner Ravi Kumar (\uD83D\uDEF5 KA-05-AB-1234) has picked up your order");

                // Stage 5: COMPLETED after ~4 minutes for delivery
                TimeUnit.SECONDS.sleep(240);
            } else {
                // For Dine-In/Takeaway — complete sooner
                TimeUnit.SECONDS.sleep(60);
            }

            advanceStatus(orderNumber, OrderStatus.COMPLETED, "Order delivered successfully! Enjoy your meal \uD83C\uDF7D\uFE0F");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Order progression interrupted for: {}", orderNumber);
        } catch (Exception e) {
            log.error("Error in order progression for {}: {}", orderNumber, e.getMessage());
        }
    }

    @Transactional
    public void advanceStatus(String orderNumber, OrderStatus newStatus, String comment) {
        orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
            OrderStatus current = order.getStatus();
            if (current == OrderStatus.CANCELLED || current == OrderStatus.COMPLETED) {
                return; // Terminal state, don't advance
            }
            OrderStatus prev = order.getStatus();
            order.setStatus(newStatus);
            orderRepository.save(order);

            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .fromStatus(prev)
                    .toStatus(newStatus)
                    .changedByUserId(1L) // System user
                    .comment(comment)
                    .build();
            statusHistoryRepository.save(history);
            log.info("Order {} advanced: {} → {} | {}", orderNumber, prev, newStatus, comment);
        });
    }
}

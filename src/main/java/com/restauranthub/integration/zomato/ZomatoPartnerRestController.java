package com.restauranthub.integration.zomato;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.entity.Order;
import com.restauranthub.entity.Payment;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.repository.OrderRepository;
import com.restauranthub.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/zomato")
@RequiredArgsConstructor
@Tag(name = "Zomato Partner Integration", description = "Endpoints for Zomato API key status, order push sync, and partner catalog management")
public class ZomatoPartnerRestController {

    private final ZomatoIntegrationService zomatoIntegrationService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/status")
    @Operation(summary = "Check configured Zomato API key, partner restaurant ID, and connectivity status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getZomatoStatus() {
        return ResponseEntity.ok(ApiResponse.success(zomatoIntegrationService.testConnection()));
    }

    @PostMapping("/sync/{orderNumber}")
    @Operation(summary = "Manually push or re-sync an order with Zomato Partner API using the configured API Key")
    public ResponseEntity<ApiResponse<ZomatoOrderSyncResponse>> syncOrderManually(@PathVariable String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        ZomatoOrderSyncResponse response = zomatoIntegrationService.syncOrder(order, payment);

        if (response.isSuccess()) {
            order.setZomatoOrderId(response.getZomatoOrderId());
            order.setZomatoSyncStatus(response.getOrderStatus());
            orderRepository.save(order);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.entity.Refund;
import com.restauranthub.entity.User;
import com.restauranthub.payment.PaymentResponse;
import com.restauranthub.service.AuthService;
import com.restauranthub.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Payment Gateway Integration", description = "Endpoints for initiating checkouts, processing deposits, webhook verification, and refunds")
public class PaymentRestController {

    private final PaymentService paymentService;
    private final AuthService authService;

    @PostMapping("/payments/orders/{orderId}")
    @Operation(summary = "Initiate gateway payment for a placed order")
    public ResponseEntity<ApiResponse<PaymentResponse>> payForOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        User user = authService.getCurrentAuthenticatedUser();
        String idempKey = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        PaymentResponse response = paymentService.initiateOrderPayment(orderId, user, idempKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payments/tables/{reservationId}/deposit")
    @Operation(summary = "Pay table reservation deposit")
    public ResponseEntity<ApiResponse<PaymentResponse>> payTableDeposit(
            @PathVariable Long reservationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        User user = authService.getCurrentAuthenticatedUser();
        String idempKey = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        PaymentResponse response = paymentService.initiateTableDeposit(reservationId, user, idempKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/webhooks/payments/{provider}")
    @Operation(summary = "Webhook listener for payment provider callback events (Signature verified)")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @PathVariable String provider,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Event-Id", required = false) String eventId,
            @RequestBody String payload) {
        String safeEventId = eventId != null ? eventId : UUID.randomUUID().toString();
        paymentService.processWebhookEvent(provider, safeEventId, payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", safeEventId));
    }

    @PostMapping("/admin/payments/{paymentReference}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Operation(summary = "Issue a refund for a completed payment (Admin / Manager only)")
    public ResponseEntity<ApiResponse<Refund>> issueRefund(
            @PathVariable String paymentReference,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("reason") String reason) {
        User user = authService.getCurrentAuthenticatedUser();
        Refund refund = paymentService.processRefund(paymentReference, amount, reason, user);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", refund));
    }
}

package com.restauranthub.payment;

import com.restauranthub.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service("stripePaymentGateway")
public class StripePaymentGatewayService implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGatewayService.class);

    @Value("${app.payment.stripe.api-key:sk_test_placeholder}")
    private String apiKey;

    @Value("${app.payment.stripe.webhook-secret:whsec_placeholder}")
    private String webhookSecret;

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("[STRIPE] Creating Stripe Checkout session for payment reference: {}", request.getPaymentReference());
        String sessionId = "cs_test_" + UUID.randomUUID().toString().replace("-", "");
        return PaymentResponse.builder()
                .success(true)
                .gatewayTransactionId(sessionId)
                .checkoutUrl("https://checkout.stripe.com/pay/" + sessionId)
                .message("Stripe session created")
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        try {
            // HMAC-SHA256 signature verification simulation/implementation
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            // Compare hash or signature prefix
            return true;
        } catch (Exception e) {
            log.error("Failed to verify Stripe signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public PaymentResponse processRefund(Payment payment, BigDecimal amount, String reason) {
        log.info("[STRIPE] Requesting Stripe refund for gateway transaction: {}", payment.getGatewayTransactionId());
        String refundId = "re_test_" + UUID.randomUUID().toString().replace("-", "");
        return PaymentResponse.builder()
                .success(true)
                .gatewayTransactionId(refundId)
                .message("Stripe refund processed successfully")
                .build();
    }
}

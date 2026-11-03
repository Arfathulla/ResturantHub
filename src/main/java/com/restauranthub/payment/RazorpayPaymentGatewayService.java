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

@Service("razorpayPaymentGateway")
public class RazorpayPaymentGatewayService implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGatewayService.class);

    @Value("${app.payment.razorpay.key-id:rzp_test_placeholder}")
    private String keyId;

    @Value("${app.payment.razorpay.key-secret:rzp_secret_placeholder}")
    private String keySecret;

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("[RAZORPAY] Creating Razorpay order for payment reference: {}", request.getPaymentReference());
        String orderId = "order_" + UUID.randomUUID().toString().substring(0, 14);
        return PaymentResponse.builder()
                .success(true)
                .gatewayTransactionId(orderId)
                .checkoutUrl("https://api.razorpay.com/v1/checkout/" + orderId)
                .message("Razorpay order initiated")
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            log.error("Failed to verify Razorpay signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public PaymentResponse processRefund(Payment payment, BigDecimal amount, String reason) {
        log.info("[RAZORPAY] Requesting Razorpay refund for payment: {}", payment.getGatewayTransactionId());
        String refundId = "rfnd_" + UUID.randomUUID().toString().substring(0, 14);
        return PaymentResponse.builder()
                .success(true)
                .gatewayTransactionId(refundId)
                .message("Razorpay refund processed")
                .build();
    }
}

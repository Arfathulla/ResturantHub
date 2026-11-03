package com.restauranthub.payment;

import com.restauranthub.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service("mockPaymentGateway")
public class MockPaymentGatewayService implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayService.class);

    @Value("${app.payment.mock.auto-succeed:true}")
    private boolean autoSucceed;

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        String txId = "mock_tx_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[MOCK GATEWAY] Initiating payment ref: {} for amount: {} {}",
                request.getPaymentReference(), request.getAmount(), request.getCurrency());

        return PaymentResponse.builder()
                .success(autoSucceed)
                .gatewayTransactionId(txId)
                .checkoutUrl("https://checkout.restauranthub.local/mock/" + txId)
                .message(autoSucceed ? "Mock payment pre-authorized successfully" : "Mock payment declined")
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        log.info("[MOCK GATEWAY] Verifying mock webhook signature: {}", signatureHeader);
        return signatureHeader != null && signatureHeader.startsWith("mock_sig_");
    }

    @Override
    public PaymentResponse processRefund(Payment payment, BigDecimal amount, String reason) {
        String refundId = "mock_ref_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[MOCK GATEWAY] Processing mock refund of ${} for payment: {} (Reason: {})",
                amount, payment.getPaymentReference(), reason);

        return PaymentResponse.builder()
                .success(true)
                .gatewayTransactionId(refundId)
                .message("Mock refund processed successfully")
                .build();
    }
}

package com.restauranthub.payment;

import com.restauranthub.entity.Payment;

import java.math.BigDecimal;

public interface PaymentGatewayService {

    PaymentResponse initiatePayment(PaymentRequest request);

    boolean verifyWebhookSignature(String payload, String signatureHeader);

    PaymentResponse processRefund(Payment payment, BigDecimal amount, String reason);
}

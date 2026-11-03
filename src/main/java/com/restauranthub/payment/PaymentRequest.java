package com.restauranthub.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private String customerEmail;
    private String description;
    private Map<String, String> metadata;
}

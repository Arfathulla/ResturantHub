package com.restauranthub.integration.zomato;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZomatoOrderSyncRequest {

    private String partnerRestaurantId;
    private String orderId;
    private String orderNumber;
    private String orderType;
    private Instant placedAt;

    private CustomerInfo customer;
    private String deliveryAddress;
    private String specialInstructions;

    private List<ZomatoItem> items;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;

    private PaymentInfo payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZomatoItem {
        private Long itemId;
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private List<String> addons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String paymentReference;
        private String method;
        private String transactionId;
        private String status;
        private BigDecimal amount;
    }
}

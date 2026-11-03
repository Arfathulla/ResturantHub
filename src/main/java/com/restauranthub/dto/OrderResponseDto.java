package com.restauranthub.dto;

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
public class OrderResponseDto {

    private Long id;
    private String orderNumber;
    private Long branchId;
    private String branchName;
    private String orderType;
    private String status;
    private Long tableReservationId;
    private String deliveryAddress;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal finalTotal;
    private String appliedCouponCode;
    private String specialInstructions;
    private String paymentReference;
    private String paymentMethod;
    private String paymentDetails;
    private String paymentStatus;
    private String gatewayTransactionId;
    private String zomatoOrderId;
    private String zomatoSyncStatus;
    private boolean zomatoSyncSuccess;
    private List<OrderItemDto> items;
    private Instant createdAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemDto {
        private Long id;
        private String itemName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal totalPrice;
        private List<String> addonNames;
        private String specialInstructions;
    }
}

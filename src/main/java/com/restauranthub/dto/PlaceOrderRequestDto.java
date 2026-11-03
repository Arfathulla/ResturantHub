package com.restauranthub.dto;

import com.restauranthub.entity.enums.OrderType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequestDto {

    @NotNull(message = "Order type is required (DINE_IN, TAKEAWAY, DELIVERY)")
    private OrderType orderType;

    private Long tableReservationId; // Required if orderType is DINE_IN

    private String deliveryAddress; // Required if orderType is DELIVERY

    private String couponCode;

    private String specialInstructions;

    @Builder.Default
    private String paymentMethod = "CARD"; // CARD, UPI, NETBANKING, MOCK, COD

    private String cardNumber;

    private String cardHolderName;

    private String cardExpiry;

    private String cardCvv;

    private String upiId;

    private String bankName;

    private String idempotencyKey;
}

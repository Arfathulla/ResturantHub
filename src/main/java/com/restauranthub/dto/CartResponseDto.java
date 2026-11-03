package com.restauranthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {

    private Long id;
    private Long branchId;
    private String branchName;
    private List<CartItemResponseDto> items;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal finalTotal;
    private String appliedCouponCode;
}

package com.restauranthub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequestDto {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Menu item ID is required")
    private Long menuItemId;

    private Long variantId;

    private List<Long> addonIds;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private String specialInstructions;
}

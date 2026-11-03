package com.restauranthub.dto;

import com.restauranthub.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequestDto {

    @NotNull(message = "New order status is required")
    private OrderStatus newStatus;

    private String comment;
}

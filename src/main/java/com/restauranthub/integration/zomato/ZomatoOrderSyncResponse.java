package com.restauranthub.integration.zomato;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZomatoOrderSyncResponse {

    private boolean success;
    private String zomatoOrderId;
    private String restaurantId;
    private String orderStatus;
    private Integer estimatedDeliveryMinutes;
    private String trackingUrl;
    private String message;
    private Instant syncedAt;
}

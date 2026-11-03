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
public class DashboardMetricsDto {

    private long totalReservations;
    private long totalTableReservations;
    private long totalRoomReservations;
    private long totalHallReservations;
    private long totalParkingReservations;

    private long totalOrders;
    private long activeOrdersCount;
    private BigDecimal totalRevenueRecognized;

    private List<LowStockItemDto> lowStockAlerts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LowStockItemDto {
        private Long menuItemId;
        private String itemName;
        private Integer quantityInStock;
        private Integer threshold;
    }
}

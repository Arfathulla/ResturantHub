package com.restauranthub.service;

import com.restauranthub.dto.DashboardMetricsDto;
import com.restauranthub.entity.Order;
import com.restauranthub.entity.Payment;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.entity.enums.PaymentStatus;
import com.restauranthub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final TableReservationRepository tableReservationRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final HallReservationRepository hallReservationRepository;
    private final ParkingReservationRepository parkingReservationRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics() {
        long tables = tableReservationRepository.count();
        long rooms = roomReservationRepository.count();
        long halls = hallReservationRepository.count();
        long parking = parkingReservationRepository.count();
        long totalReservations = tables + rooms + halls + parking;

        List<Order> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();
        long activeOrders = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.COMPLETED &&
                             o.getStatus() != OrderStatus.CANCELLED &&
                             o.getStatus() != OrderStatus.REFUNDED)
                .count();

        // Revenue recognition: Only count COMPLETED payments
        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardMetricsDto.LowStockItemDto> lowStock = inventoryItemRepository.findLowStockItems().stream()
                .map(i -> new DashboardMetricsDto.LowStockItemDto(
                        i.getMenuItem().getId(),
                        i.getMenuItem().getName(),
                        i.getQuantityInStock(),
                        i.getLowStockThreshold()
                ))
                .collect(Collectors.toList());

        return DashboardMetricsDto.builder()
                .totalReservations(totalReservations)
                .totalTableReservations(tables)
                .totalRoomReservations(rooms)
                .totalHallReservations(halls)
                .totalParkingReservations(parking)
                .totalOrders(totalOrders)
                .activeOrdersCount(activeOrders)
                .totalRevenueRecognized(totalRevenue)
                .lowStockAlerts(lowStock)
                .build();
    }
}

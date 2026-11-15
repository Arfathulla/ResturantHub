package com.restauranthub.service;

import com.restauranthub.dto.CartResponseDto;
import com.restauranthub.dto.OrderResponseDto;
import com.restauranthub.dto.PlaceOrderRequestDto;
import com.restauranthub.dto.UpdateOrderStatusRequestDto;
import com.restauranthub.entity.*;
import com.restauranthub.entity.enums.InventoryMovementType;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.entity.enums.OrderType;
import com.restauranthub.entity.enums.PaymentProvider;
import com.restauranthub.entity.enums.PaymentStatus;
import com.restauranthub.entity.enums.ReservationStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.exception.UnauthorizedException;
import com.restauranthub.integration.zomato.ZomatoIntegrationService;
import com.restauranthub.integration.zomato.ZomatoOrderSyncResponse;
import com.restauranthub.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository movementRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final TableReservationRepository tableReservationRepository;
    private final PaymentRepository paymentRepository;
    private final ZomatoIntegrationService zomatoIntegrationService;
    private final DeliveryTrackingRepository deliveryTrackingRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponseDto placeOrder(User user, PlaceOrderRequestDto request) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("CART_EMPTY", "You do not have an active cart."));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("CART_EMPTY", "Cannot place an order with an empty cart.");
        }

        TableReservation tableReservation = null;
        if (request.getOrderType() == OrderType.DINE_IN) {
            if (request.getTableReservationId() == null) {
                throw new BadRequestException("RESERVATION_REQUIRED", "Dine-in orders must be linked to a valid table reservation.");
            }
            tableReservation = tableReservationRepository.findById(request.getTableReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("TableReservation", "id", request.getTableReservationId()));
            if (!tableReservation.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("Cannot link order to another customer's table reservation.");
            }
            if (tableReservation.getStatus() != ReservationStatus.CONFIRMED && tableReservation.getStatus() != ReservationStatus.CHECKED_IN) {
                throw new BadRequestException("INVALID_RESERVATION_STATUS",
                        "Table reservation must be CONFIRMED or CHECKED_IN to place dine-in orders.");
            }
        }

        if (request.getOrderType() == OrderType.DELIVERY) {
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isBlank()) {
                throw new BadRequestException("ADDRESS_REQUIRED", "Delivery address is required for delivery orders.");
            }
        }

        // Concurrency-safe inventory validation and deduction
        for (CartItem cartItem : cart.getItems()) {
            InventoryItem invItem = inventoryItemRepository.findByMenuItemIdWithLock(cartItem.getMenuItem().getId())
                    .orElse(null);

            if (invItem != null) {
                if (invItem.getQuantityInStock() < cartItem.getQuantity()) {
                    throw new BadRequestException("INSUFFICIENT_STOCK",
                            String.format("Item '%s' has insufficient stock (%d available, %d requested).",
                                    cartItem.getMenuItem().getName(), invItem.getQuantityInStock(), cartItem.getQuantity()));
                }

                invItem.setQuantityInStock(invItem.getQuantityInStock() - cartItem.getQuantity());
                inventoryItemRepository.save(invItem);

                InventoryMovement movement = InventoryMovement.builder()
                        .inventoryItem(invItem)
                        .quantityDelta(-cartItem.getQuantity())
                        .movementType(InventoryMovementType.ORDER_CONSUMPTION)
                        .referenceNote("Order placement deduction")
                        .build();
                movementRepository.save(movement);
            }
        }

        // Server-side financial calculations
        CartResponseDto cartTotals = cartService.calculateCartTotals(cart, request.getCouponCode());
        BigDecimal deliveryFee = (request.getOrderType() == OrderType.DELIVERY) ? new BigDecimal("50.00") : BigDecimal.ZERO;
        BigDecimal finalTotal = cartTotals.getFinalTotal().add(deliveryFee);

        Coupon coupon = null;
        if (cartTotals.getAppliedCouponCode() != null) {
            coupon = couponRepository.findByCodeWithLock(cartTotals.getAppliedCouponCode()).orElse(null);
            if (coupon != null) {
                coupon.setUsedCount(coupon.getUsedCount() + 1);
                couponRepository.save(coupon);
            }
        }

        String orderNumber = "ORD-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .branch(cart.getBranch())
                .orderType(request.getOrderType())
                .status(OrderStatus.PLACED)
                .tableReservation(tableReservation)
                .deliveryAddress(request.getDeliveryAddress())
                .subtotal(cartTotals.getSubtotal())
                .taxAmount(cartTotals.getTaxAmount())
                .discountAmount(cartTotals.getDiscountAmount())
                .deliveryFee(deliveryFee)
                .finalTotal(finalTotal)
                .appliedCoupon(coupon)
                .specialInstructions(request.getSpecialInstructions())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Snapshot order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            BigDecimal base = ci.getMenuItem().getBasePrice();
            BigDecimal variantDelta = (ci.getVariant() != null) ? ci.getVariant().getPriceDelta() : BigDecimal.ZERO;
            BigDecimal addonsTotal = ci.getAddons().stream()
                    .map(MenuItemAddon::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal unitPrice = base.add(variantDelta).add(addonsTotal);
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));

            OrderItem oi = OrderItem.builder()
                    .order(savedOrder)
                    .menuItem(ci.getMenuItem())
                    .variant(ci.getVariant())
                    .itemNameSnapshot(ci.getMenuItem().getName())
                    .unitPriceSnapshot(unitPrice)
                    .quantity(ci.getQuantity())
                    .totalPrice(itemTotal)
                    .specialInstructions(ci.getSpecialInstructions())
                    .build();

            List<OrderItemAddon> orderAddons = ci.getAddons().stream()
                    .map(a -> OrderItemAddon.builder()
                            .orderItem(oi)
                            .addonNameSnapshot(a.getAddonName())
                            .addonPriceSnapshot(a.getPrice())
                            .build())
                    .collect(Collectors.toList());

            oi.setAddons(orderAddons);
            orderItems.add(oi);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setOrderItems(orderItems);

        // Record Coupon Redemption
        if (coupon != null) {
            CouponRedemption redemption = CouponRedemption.builder()
                    .coupon(coupon)
                    .user(user)
                    .order(savedOrder)
                    .build();
            couponRedemptionRepository.save(redemption);
        }

        // Record Initial State Transition in status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .fromStatus(null)
                .toStatus(OrderStatus.PLACED)
                .changedByUserId(user.getId())
                .comment("Order placed successfully by customer")
                .build();
        statusHistoryRepository.save(history);

        // Process Payment record
        String paymentMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod().toUpperCase() : "CARD";
        PaymentProvider provider = switch (paymentMethod) {
            case "STRIPE" -> PaymentProvider.STRIPE;
            case "RAZORPAY" -> PaymentProvider.RAZORPAY;
            default -> PaymentProvider.MOCK;
        };

        String paymentRef = "PAY-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String idempKey = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey() : "idemp_pay_" + UUID.randomUUID().toString().replace("-", "");

        String details = formatPaymentDetails(request);
        String gatewayTxId = "txn_" + paymentMethod.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 12);

        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .user(user)
                .order(savedOrder)
                .amount(finalTotal)
                .currency("INR")
                .provider(provider)
                .paymentMethod(paymentMethod)
                .paymentDetails(details)
                .gatewayTransactionId(gatewayTxId)
                .status(paymentMethod.equals("COD") ? PaymentStatus.PENDING : PaymentStatus.COMPLETED)
                .idempotencyKey(idempKey)
                .build();

        if (paymentRepository != null) {
            payment = paymentRepository.save(payment);
        }

        savedOrder.setPaymentMethod(paymentMethod);

        // Sync with Zomato Partner API if configured
        if (zomatoIntegrationService != null) {
            try {
                ZomatoOrderSyncResponse zomatoResp = zomatoIntegrationService.syncOrder(savedOrder, payment);
                if (zomatoResp != null && zomatoResp.isSuccess()) {
                    savedOrder.setZomatoOrderId(zomatoResp.getZomatoOrderId());
                    savedOrder.setZomatoSyncStatus(zomatoResp.getOrderStatus());
                }
            } catch (Exception ex) {
                log.warn("Zomato partner sync encountered non-blocking warning: {}", ex.getMessage());
            }
        }
        savedOrder = orderRepository.save(savedOrder);

        // CREATE AND PERSIST DELIVERY TRACKING RECORD IN MYSQL
        if (deliveryTrackingRepository != null) {
            DeliveryTracking tracking = DeliveryTracking.builder()
                    .order(savedOrder)
                    .status(savedOrder.getStatus())
                    .statusUpdatedAt(Instant.now())
                    .build();
            deliveryTrackingRepository.save(tracking);
        }

        // Clear user cart
        cartService.clearCart(user);

        log.info("Order placed & paid successfully: {} by user: {} (Final Total: ₹{}, Method: {}, Zomato ID: {})",
                orderNumber, user.getEmail(), finalTotal, paymentMethod, savedOrder.getZomatoOrderId());
        return mapToDto(savedOrder);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(String orderIdentifier, UpdateOrderStatusRequestDto request, User currentUser) {
        Order order = findOrderByNumberOrId(orderIdentifier);
        String orderNumber = order.getOrderNumber();

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        // Enforce state transitions and permissions
        boolean isStaffOrKitchenOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_MANAGER") ||
                               a.getAuthority().equals("ROLE_STAFF") ||
                               a.getAuthority().equals("ROLE_KITCHEN"));

        if (!isStaffOrKitchenOrAdmin) {
            // Customer can only cancel prior to preparation
            if (newStatus == OrderStatus.CANCELLED) {
                if (!order.getUser().getId().equals(currentUser.getId())) {
                    throw new UnauthorizedException("Cannot cancel another customer's order.");
                }
                if (currentStatus != OrderStatus.PLACED && currentStatus != OrderStatus.ORDER_PLACED &&
                    currentStatus != OrderStatus.ACCEPTED && currentStatus != OrderStatus.ORDER_CONFIRMED) {
                    throw new BadRequestException("INVALID_STATUS", "Orders that are already in preparation cannot be cancelled.");
                }
            } else {
                throw new UnauthorizedException("Customers cannot modify order fulfillment statuses.");
            }
        }

        // Validate allowed state transitions
        validateStatusTransition(currentStatus, newStatus);

        // If cancelling, restock inventory items
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryItemRepository.findByMenuItemIdWithLock(item.getMenuItem().getId()).ifPresent(inv -> {
                    inv.setQuantityInStock(inv.getQuantityInStock() + item.getQuantity());
                    inventoryItemRepository.save(inv);

                    InventoryMovement movement = InventoryMovement.builder()
                            .inventoryItem(inv)
                            .quantityDelta(item.getQuantity())
                            .movementType(InventoryMovementType.RESTOCK)
                            .referenceNote("Order cancellation restock (" + orderNumber + ")")
                            .build();
                    movementRepository.save(movement);
                });
            }
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        // Update status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(updated)
                .fromStatus(currentStatus)
                .toStatus(newStatus)
                .changedByUserId(currentUser.getId())
                .comment(request.getComment() != null ? request.getComment() : "Status updated to " + newStatus)
                .build();
        statusHistoryRepository.save(history);

        // Update DeliveryTracking record in MySQL
        if (deliveryTrackingRepository != null) {
            DeliveryTracking tracking = deliveryTrackingRepository.findByOrderId(updated.getId())
                    .orElseGet(() -> DeliveryTracking.builder().order(updated).build());
            tracking.setStatus(newStatus);
            tracking.setStatusUpdatedAt(Instant.now());
            deliveryTrackingRepository.save(tracking);
        }

        if (updated.getZomatoOrderId() != null && zomatoIntegrationService != null) {
            try {
                zomatoIntegrationService.updateOrderStatus(updated.getZomatoOrderId(), newStatus);
            } catch (Exception e) {
                log.warn("Failed to notify Zomato of order status change: {}", e.getMessage());
            }
        }

        log.info("Order: {} transition from {} to {} by user: {}",
                orderNumber, currentStatus, newStatus, currentUser.getEmail());
        return mapToDto(updated);
    }

    @Transactional
    public OrderResponseDto advanceOrderStatus(String orderIdentifier, User currentUser) {
        Order order = findOrderByNumberOrId(orderIdentifier);
        OrderStatus currentStatus = order.getStatus();
        OrderStatus normFrom = OrderStatus.fromString(currentStatus.name());

        OrderStatus nextStatus = switch (normFrom) {
            case PENDING_PAYMENT -> OrderStatus.PLACED;
            case PLACED -> OrderStatus.ACCEPTED;
            case ACCEPTED -> OrderStatus.PREPARING;
            case PREPARING -> OrderStatus.READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> (order.getOrderType() != null && order.getOrderType() != OrderType.DELIVERY)
                    ? OrderStatus.COMPLETED : OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> OrderStatus.COMPLETED;
            case COMPLETED, DELIVERED -> OrderStatus.COMPLETED;
            case CANCELLED -> OrderStatus.CANCELLED;
            default -> OrderStatus.COMPLETED;
        };

        if (normFrom == nextStatus) {
            return mapToDto(order); // already at terminal state
        }

        order.setStatus(nextStatus);
        Order updated = orderRepository.save(order);

        // Update status history
        Long changedBy = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : order.getUser().getId();
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(updated)
                .fromStatus(currentStatus)
                .toStatus(nextStatus)
                .changedByUserId(changedBy)
                .comment("Delivery progress advanced to " + nextStatus.getDisplayLabel())
                .build();
        statusHistoryRepository.save(history);

        // Update DeliveryTracking record in MySQL
        if (deliveryTrackingRepository != null) {
            DeliveryTracking tracking = deliveryTrackingRepository.findByOrderId(updated.getId())
                    .orElseGet(() -> DeliveryTracking.builder().order(updated).build());
            tracking.setStatus(nextStatus);
            tracking.setStatusUpdatedAt(Instant.now());

            if (nextStatus == OrderStatus.OUT_FOR_DELIVERY) {
                if (tracking.getDriverName() == null || tracking.getDriverName().isBlank()) {
                    tracking.setDriverName("Kiran Gowda");
                    tracking.setDriverPhone("+91 99002 65432");
                    tracking.setVehicleNumber("KA-01-EQ-4921");
                    tracking.setCurrentLocation("Intermediate Ring Road, Domlur");
                    tracking.setEstimatedDeliveryMinutes(12);
                }
            } else if (nextStatus == OrderStatus.COMPLETED || nextStatus == OrderStatus.DELIVERED) {
                tracking.setCurrentLocation("Delivered at Customer Address");
                tracking.setEstimatedDeliveryMinutes(0);
            }

            deliveryTrackingRepository.save(tracking);
        }

        log.info("Order {} status advanced from {} to {} in MySQL database", order.getOrderNumber(), currentStatus, nextStatus);
        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderByNumber(String orderNumber, User currentUser) {
        Order order = findOrderByNumberOrId(orderNumber);

        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_MANAGER") ||
                               a.getAuthority().equals("ROLE_STAFF") ||
                               a.getAuthority().equals("ROLE_KITCHEN"));

        if (!isStaffOrAdmin && !order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: You do not own this order.");
        }

        return mapToDto(order);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto.DeliveryTrackingDto getOrderTracking(String orderIdentifier, User currentUser) {
        Order order = findOrderByNumberOrId(orderIdentifier);

        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_MANAGER") ||
                               a.getAuthority().equals("ROLE_STAFF") ||
                               a.getAuthority().equals("ROLE_KITCHEN"));

        if (!isStaffOrAdmin && !order.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: You do not own this order.");
        }

        DeliveryTracking tracking = null;
        if (deliveryTrackingRepository != null && order.getId() != null) {
            tracking = deliveryTrackingRepository.findByOrderId(order.getId()).orElse(null);
        }

        OrderStatus status = tracking != null ? tracking.getStatus() : order.getStatus();
        Instant statusTime = tracking != null ? tracking.getStatusUpdatedAt()
                : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt());

        return OrderResponseDto.DeliveryTrackingDto.builder()
                .trackingId(tracking != null ? tracking.getId() : null)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(status.name())
                .statusLabel(status.getDisplayLabel())
                .progressPercent(getProgressPercent(status))
                .driverName(tracking != null ? tracking.getDriverName() : null)
                .driverPhone(tracking != null ? tracking.getDriverPhone() : null)
                .vehicleNumber(tracking != null ? tracking.getVehicleNumber() : null)
                .currentLocation(tracking != null ? tracking.getCurrentLocation() : null)
                .statusUpdatedAt(statusTime)
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getCustomerOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getCustomerActiveOrders(Long userId) {
        List<OrderStatus> terminal = Arrays.asList(
                OrderStatus.COMPLETED, OrderStatus.DELIVERED,
                OrderStatus.CANCELLED, OrderStatus.REFUNDED
        );
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> !terminal.contains(o.getStatus()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getBranchLiveOrders(Long branchId) {
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.PLACED, OrderStatus.ORDER_PLACED,
                OrderStatus.ACCEPTED, OrderStatus.ORDER_CONFIRMED,
                OrderStatus.PREPARING, OrderStatus.RESTAURANT_PREPARING,
                OrderStatus.READY_FOR_PICKUP, OrderStatus.FOOD_READY,
                OrderStatus.OUT_FOR_DELIVERY
        );
        return orderRepository.findByBranchIdAndStatusInOrderByCreatedAtAsc(branchId, activeStatuses).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Order findOrderByNumberOrId(String identifier) {
        if (identifier == null || identifier.trim().isBlank()) {
            throw new ResourceNotFoundException("Order", "id", identifier);
        }
        String trimmed = identifier.trim();
        if (trimmed.matches("^\\d+$")) {
            Long orderId = Long.parseLong(trimmed);
            return orderRepository.findById(orderId)
                    .orElseGet(() -> orderRepository.findByOrderNumber(trimmed)
                            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId)));
        }
        return orderRepository.findByOrderNumber(trimmed)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", trimmed));
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        OrderStatus normFrom = OrderStatus.fromString(from.name());
        OrderStatus normTo = OrderStatus.fromString(to.name());

        if (normFrom == normTo) return;
        if (normFrom == OrderStatus.COMPLETED || normFrom == OrderStatus.CANCELLED || normFrom == OrderStatus.REFUNDED) {
            throw new BadRequestException("TERMINAL_STATUS", "Order is in a terminal state (" + from + ") and cannot be modified.");
        }

        boolean valid = switch (normFrom) {
            case PENDING_PAYMENT -> normTo == OrderStatus.PLACED || normTo == OrderStatus.PAYMENT_FAILED || normTo == OrderStatus.CANCELLED;
            case PLACED -> normTo == OrderStatus.ACCEPTED || normTo == OrderStatus.CANCELLED;
            case ACCEPTED -> normTo == OrderStatus.PREPARING || normTo == OrderStatus.CANCELLED;
            case PREPARING -> normTo == OrderStatus.READY_FOR_PICKUP || normTo == OrderStatus.CANCELLED;
            case READY_FOR_PICKUP -> normTo == OrderStatus.OUT_FOR_DELIVERY || normTo == OrderStatus.COMPLETED || normTo == OrderStatus.CANCELLED;
            case OUT_FOR_DELIVERY -> normTo == OrderStatus.COMPLETED || normTo == OrderStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException("INVALID_TRANSITION",
                    String.format("Invalid order state transition from %s to %s", from, to));
        }
    }

    public static int getProgressPercent(OrderStatus status) {
        if (status == null) return 0;
        OrderStatus norm = OrderStatus.fromString(status.name());
        return switch (norm) {
            case PLACED -> 15;
            case ACCEPTED -> 35;
            case PREPARING -> 55;
            case READY_FOR_PICKUP -> 75;
            case OUT_FOR_DELIVERY -> 90;
            case COMPLETED -> 100;
            case CANCELLED, REFUNDED, PAYMENT_FAILED -> 0;
            default -> 10;
        };
    }

    private OrderResponseDto mapToDto(Order order) {
        List<OrderResponseDto.OrderItemDto> items = order.getOrderItems().stream()
                .map(oi -> {
                    List<String> addons = oi.getAddons().stream()
                            .map(OrderItemAddon::getAddonNameSnapshot)
                            .collect(Collectors.toList());
                    return new OrderResponseDto.OrderItemDto(
                            oi.getId(),
                            oi.getItemNameSnapshot(),
                            oi.getUnitPriceSnapshot(),
                            oi.getQuantity(),
                            oi.getTotalPrice(),
                            addons,
                            oi.getSpecialInstructions()
                    );
                })
                .collect(Collectors.toList());

        Payment payment = null;
        if (paymentRepository != null && order.getId() != null) {
            payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        }
        if (payment == null && order.getPayments() != null && !order.getPayments().isEmpty()) {
            payment = order.getPayments().get(0);
        }

        String paymentRef = payment != null ? payment.getPaymentReference() : null;
        String paymentMethod = payment != null ? payment.getPaymentMethod() : order.getPaymentMethod();
        String paymentDetails = payment != null ? payment.getPaymentDetails() : null;
        String paymentStatus = payment != null ? payment.getStatus().name() :
                (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.ORDER_PLACED ? "PAID" : "PENDING");
        String gatewayTxId = payment != null ? payment.getGatewayTransactionId() : null;

        // Map delivery tracking information
        OrderResponseDto.DeliveryTrackingDto trackingDto = null;
        if (deliveryTrackingRepository != null && order.getId() != null) {
            DeliveryTracking tracking = deliveryTrackingRepository.findByOrderId(order.getId()).orElse(null);
            if (tracking != null) {
                trackingDto = OrderResponseDto.DeliveryTrackingDto.builder()
                        .trackingId(tracking.getId())
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .status(tracking.getStatus().name())
                        .statusLabel(tracking.getStatus().getDisplayLabel())
                        .progressPercent(getProgressPercent(tracking.getStatus()))
                        .driverName(tracking.getDriverName())
                        .driverPhone(tracking.getDriverPhone())
                        .vehicleNumber(tracking.getVehicleNumber())
                        .currentLocation(tracking.getCurrentLocation())
                        .statusUpdatedAt(tracking.getStatusUpdatedAt())
                        .build();
            }
        }

        if (trackingDto == null) {
            trackingDto = OrderResponseDto.DeliveryTrackingDto.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .status(order.getStatus().name())
                    .statusLabel(order.getStatus().getDisplayLabel())
                    .progressPercent(getProgressPercent(order.getStatus()))
                    .statusUpdatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt())
                    .build();
        }

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .branchId(order.getBranch().getId())
                .branchName(order.getBranch().getName())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .tableReservationId(order.getTableReservation() != null ? order.getTableReservation().getId() : null)
                .deliveryAddress(order.getDeliveryAddress())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .deliveryFee(order.getDeliveryFee())
                .finalTotal(order.getFinalTotal())
                .appliedCouponCode(order.getAppliedCoupon() != null ? order.getAppliedCoupon().getCode() : null)
                .specialInstructions(order.getSpecialInstructions())
                .paymentReference(paymentRef)
                .paymentMethod(paymentMethod)
                .paymentDetails(paymentDetails)
                .paymentStatus(paymentStatus)
                .gatewayTransactionId(gatewayTxId)
                .zomatoOrderId(order.getZomatoOrderId())
                .zomatoSyncStatus(order.getZomatoSyncStatus())
                .zomatoSyncSuccess(order.getZomatoOrderId() != null)
                .items(items)
                .tracking(trackingDto)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String formatPaymentDetails(PlaceOrderRequestDto request) {
        String method = (request.getPaymentMethod() != null) ? request.getPaymentMethod().toUpperCase() : "CARD";
        return switch (method) {
            case "CARD" -> {
                String cardNum = request.getCardNumber();
                String last4 = (cardNum != null && cardNum.replaceAll("\\s+", "").length() >= 4) ?
                        cardNum.replaceAll("\\s+", "").substring(cardNum.replaceAll("\\s+", "").length() - 4) : "4242";
                String holder = (request.getCardHolderName() != null && !request.getCardHolderName().isBlank()) ?
                        request.getCardHolderName() : "Cardholder";
                yield "Credit/Debit Card (•••• " + last4 + ", " + holder + ")";
            }
            case "UPI" -> {
                String vpa = (request.getUpiId() != null && !request.getUpiId().isBlank()) ?
                        request.getUpiId() : "customer@okhdfcbank";
                yield "UPI Payment (" + vpa + ")";
            }
            case "NETBANKING" -> {
                String bank = (request.getBankName() != null && !request.getBankName().isBlank()) ?
                        request.getBankName() : "HDFC Bank";
                yield "Net Banking (" + bank + ")";
            }
            case "COD" -> "Cash on Delivery / Counter Pay";
            default -> "Instant Express Checkout (Demo)";
        };
    }
}

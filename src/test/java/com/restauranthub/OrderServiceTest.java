package com.restauranthub;

import com.restauranthub.dto.CartResponseDto;
import com.restauranthub.dto.OrderResponseDto;
import com.restauranthub.dto.PlaceOrderRequestDto;
import com.restauranthub.dto.UpdateOrderStatusRequestDto;
import com.restauranthub.entity.*;
import com.restauranthub.entity.enums.OrderStatus;
import com.restauranthub.entity.enums.OrderType;
import com.restauranthub.entity.enums.UserStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.repository.*;
import com.restauranthub.service.CartService;
import com.restauranthub.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartService cartService;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    @Mock
    private TableReservationRepository tableReservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private com.restauranthub.integration.zomato.ZomatoIntegrationService zomatoIntegrationService;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private RestaurantBranch branch;
    private MenuItem menuItem;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Walker")
                .status(UserStatus.ACTIVE)
                .build();

        branch = RestaurantBranch.builder()
                .id(1L)
                .name("Downtown Gourmet")
                .build();

        menuItem = MenuItem.builder()
                .id(10L)
                .name("Truffle Risotto")
                .basePrice(new BigDecimal("24.00"))
                .taxRatePercent(new BigDecimal("5.00"))
                .branch(branch)
                .isAvailable(true)
                .build();

        cartItem = CartItem.builder()
                .id(100L)
                .menuItem(menuItem)
                .quantity(2)
                .build();

        cart = Cart.builder()
                .id(50L)
                .user(customer)
                .branch(branch)
                .items(new ArrayList<>(Collections.singletonList(cartItem)))
                .build();
    }

    @Test
    @DisplayName("Should successfully place a takeaway order and deduct inventory stock")
    void testPlaceOrderSuccess() {
        PlaceOrderRequestDto request = PlaceOrderRequestDto.builder()
                .orderType(OrderType.TAKEAWAY)
                .specialInstructions("Extra hot")
                .build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        InventoryItem invItem = InventoryItem.builder()
                .id(20L)
                .menuItem(menuItem)
                .quantityInStock(15)
                .build();
        when(inventoryItemRepository.findByMenuItemIdWithLock(10L)).thenReturn(Optional.of(invItem));

        CartResponseDto cartTotals = CartResponseDto.builder()
                .subtotal(new BigDecimal("48.00"))
                .taxAmount(new BigDecimal("2.40"))
                .discountAmount(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("50.40"))
                .build();
        when(cartService.calculateCartTotals(cart, null)).thenReturn(cartTotals);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            return o;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto response = orderService.placeOrder(customer, request);

        assertNotNull(response);
        assertEquals("PLACED", response.getStatus());
        assertEquals(new BigDecimal("50.40"), response.getFinalTotal());

        // Verify inventory was deducted from 15 to 13
        assertEquals(13, invItem.getQuantityInStock());
        verify(inventoryItemRepository, times(1)).save(invItem);
        verify(movementRepository, times(1)).save(any(InventoryMovement.class));
        verify(cartService, times(1)).clearCart(customer);
    }

    @Test
    @DisplayName("Should reject order if requested item exceeds available inventory")
    void testPlaceOrderInsufficientStock() {
        PlaceOrderRequestDto request = PlaceOrderRequestDto.builder()
                .orderType(OrderType.TAKEAWAY)
                .build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        InventoryItem invItem = InventoryItem.builder()
                .id(20L)
                .menuItem(menuItem)
                .quantityInStock(1) // Only 1 in stock, but cart requests 2
                .build();
        when(inventoryItemRepository.findByMenuItemIdWithLock(10L)).thenReturn(Optional.of(invItem));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderService.placeOrder(customer, request));
        assertTrue(ex.getMessage().contains("insufficient stock"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should enforce state machine: Reject invalid transition from PLACED directly to COMPLETED")
    void testInvalidStatusTransition() {
        Order order = Order.builder()
                .id(500L)
                .orderNumber("ORD-12345")
                .user(customer)
                .status(OrderStatus.PLACED)
                .build();

        when(orderRepository.findByOrderNumber("ORD-12345")).thenReturn(Optional.of(order));

        User staffUser = User.builder()
                .id(2L)
                .roles(Collections.singleton(Role.builder().name("ROLE_STAFF").build()))
                .build();

        UpdateOrderStatusRequestDto dto = new UpdateOrderStatusRequestDto(OrderStatus.COMPLETED, "Skipping straight to completed");

        assertThrows(BadRequestException.class, () -> orderService.updateOrderStatus("ORD-12345", dto, staffUser));
    }
}

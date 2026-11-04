package com.restauranthub.controller;

import com.restauranthub.dto.CartItemRequestDto;
import com.restauranthub.dto.CartResponseDto;
import com.restauranthub.dto.OrderResponseDto;
import com.restauranthub.dto.PlaceOrderRequestDto;
import com.restauranthub.entity.User;
import com.restauranthub.exception.ApiException;
import com.restauranthub.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class FoodOrderingWebController {

    private final MenuService menuService;
    private final CartService cartService;
    private final OrderService orderService;
    private final BranchService branchService;
    private final TableReservationService tableReservationService;
    private final AuthService authService;

    @GetMapping("/menu")
    public String showMenuPage(@RequestParam(value = "branchId", defaultValue = "10") Long branchId,
                               @RequestParam(value = "query", required = false) String query,
                               Model model) {
        model.addAttribute("branches", branchService.getAllActiveBranches());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("menuItems", menuService.searchMenuItems(branchId, query));
        model.addAttribute("searchQuery", query);
        return "menu";
    }

    @GetMapping("/cart")
    public String showCartPage(@RequestParam(value = "couponCode", required = false) String couponCode,
                               Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        CartResponseDto cart = cartService.getCart(user, couponCode);
        model.addAttribute("cart", cart);
        model.addAttribute("couponCode", couponCode);
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@ModelAttribute CartItemRequestDto dto, RedirectAttributes redirectAttributes) {
        try {
            User user = authService.getCurrentAuthenticatedUser();
            cartService.addItemToCart(user, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Item added to your order cart!");
        } catch (ApiException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/menu?branchId=" + dto.getBranchId();
    }

    @PostMapping("/cart/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        User user = authService.getCurrentAuthenticatedUser();
        cartService.removeItemFromCart(user, itemId);
        redirectAttributes.addFlashAttribute("infoMessage", "Item removed from cart.");
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(@RequestParam(value = "couponCode", required = false) String couponCode, Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        CartResponseDto cart = cartService.getCart(user, couponCode);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        PlaceOrderRequestDto orderDto = new PlaceOrderRequestDto();
        orderDto.setCouponCode(couponCode);

        model.addAttribute("cart", cart);
        model.addAttribute("orderDto", orderDto);
        model.addAttribute("userReservations", tableReservationService.getCustomerReservations(user.getId()));
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processOrderCheckout(@Valid @ModelAttribute("orderDto") PlaceOrderRequestDto dto,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cartService.getCart(user, dto.getCouponCode()));
            model.addAttribute("userReservations", tableReservationService.getCustomerReservations(user.getId()));
            return "checkout";
        }

        try {
            OrderResponseDto response = orderService.placeOrder(user, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order placed successfully! Reference: " + response.getOrderNumber());
            return "redirect:/orders/" + response.getOrderNumber();
        } catch (ApiException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("cart", cartService.getCart(user, dto.getCouponCode()));
            model.addAttribute("userReservations", tableReservationService.getCustomerReservations(user.getId()));
            return "checkout";
        }
    }

    @GetMapping({"/orders/tracking", "/tracking", "/delivery-tracker"})
    public String showDeliveryTrackerPage(Model model) {
        User user = null;
        try {
            user = authService.getCurrentAuthenticatedUser();
        } catch (Exception ignored) {
        }

        if (user != null) {
            List<OrderResponseDto> activeOrders = orderService.getCustomerActiveOrders(user.getId());
            model.addAttribute("activeOrders", activeOrders);
            model.addAttribute("hasActiveOrders", !activeOrders.isEmpty());
            model.addAttribute("isAuthenticated", true);
        } else {
            model.addAttribute("activeOrders", Collections.emptyList());
            model.addAttribute("hasActiveOrders", false);
            model.addAttribute("isAuthenticated", false);
        }
        return "orders/delivery-tracker";
    }

    @GetMapping("/orders/{orderNumber}")
    public String showOrderTrackingPage(@PathVariable String orderNumber,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        try {
            User user = authService.getCurrentAuthenticatedUser();
            OrderResponseDto order = orderService.getOrderByNumber(orderNumber, user);
            model.addAttribute("order", order);
            return "orders/order-tracking";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders/tracking";
        }
    }

    @GetMapping("/orders/my-orders")
    public String showMyOrders(Model model) {
        User user = authService.getCurrentAuthenticatedUser();
        List<OrderResponseDto> allOrders = orderService.getCustomerOrders(user.getId());
        List<OrderResponseDto> activeOrders = orderService.getCustomerActiveOrders(user.getId());
        model.addAttribute("orders", allOrders);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("hasActiveOrders", !activeOrders.isEmpty());
        return "orders/my-orders";
    }
}

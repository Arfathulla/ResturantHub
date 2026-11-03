package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.dto.CartItemRequestDto;
import com.restauranthub.dto.CartResponseDto;
import com.restauranthub.entity.User;
import com.restauranthub.service.AuthService;
import com.restauranthub.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "Endpoints for managing persistent customer carts with server-calculated price rules")
public class CartRestController {

    private final CartService cartService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "Get current customer cart with optional coupon code applied")
    public ResponseEntity<ApiResponse<CartResponseDto>> getCart(
            @RequestParam(value = "couponCode", required = false) String couponCode) {
        User user = authService.getCurrentAuthenticatedUser();
        CartResponseDto cart = cartService.getCart(user, couponCode);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item with optional variant and addons to the cart")
    public ResponseEntity<ApiResponse<CartResponseDto>> addItem(@Valid @RequestBody CartItemRequestDto request) {
        User user = authService.getCurrentAuthenticatedUser();
        CartResponseDto updatedCart = cartService.addItemToCart(user, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", updatedCart));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove an item from the cart")
    public ResponseEntity<ApiResponse<CartResponseDto>> removeItem(@PathVariable Long itemId) {
        User user = authService.getCurrentAuthenticatedUser();
        CartResponseDto updatedCart = cartService.removeItemFromCart(user, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", updatedCart));
    }

    @DeleteMapping
    @Operation(summary = "Clear all items from customer cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        User user = authService.getCurrentAuthenticatedUser();
        cartService.clearCart(user);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}

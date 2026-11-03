package com.restauranthub.service;

import com.restauranthub.dto.CartItemRequestDto;
import com.restauranthub.dto.CartItemResponseDto;
import com.restauranthub.dto.CartResponseDto;
import com.restauranthub.entity.*;
import com.restauranthub.entity.enums.DiscountType;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemVariantRepository variantRepository;
    private final MenuItemAddonRepository addonRepository;
    private final RestaurantBranchRepository branchRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public CartResponseDto getCart(User user, String couponCode) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isEmpty() || cartOpt.get().getItems().isEmpty()) {
            return CartResponseDto.builder()
                    .items(Collections.emptyList())
                    .subtotal(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .deliveryFee(BigDecimal.ZERO)
                    .finalTotal(BigDecimal.ZERO)
                    .build();
        }

        return calculateCartTotals(cartOpt.get(), couponCode);
    }

    @Transactional
    public CartResponseDto addItemToCart(User user, CartItemRequestDto request) {
        RestaurantBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", request.getBranchId()));

        MenuItem menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", request.getMenuItemId()));

        if (!menuItem.getBranch().getId().equals(branch.getId())) {
            throw new BadRequestException("BRANCH_MISMATCH", "Item does not belong to the selected restaurant branch.");
        }
        if (!menuItem.isAvailable()) {
            throw new BadRequestException("ITEM_UNAVAILABLE", "Item is currently out of stock or unavailable.");
        }

        MenuItemVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", "id", request.getVariantId()));
            if (!variant.getMenuItem().getId().equals(menuItem.getId())) {
                throw new BadRequestException("INVALID_VARIANT", "Selected variant does not belong to this menu item.");
            }
        }

        Set<MenuItemAddon> addons = new HashSet<>();
        if (request.getAddonIds() != null && !request.getAddonIds().isEmpty()) {
            for (Long addonId : request.getAddonIds()) {
                MenuItemAddon addon = addonRepository.findById(addonId)
                        .orElseThrow(() -> new ResourceNotFoundException("MenuItemAddon", "id", addonId));
                if (!addon.getMenuItem().getId().equals(menuItem.getId())) {
                    throw new BadRequestException("INVALID_ADDON", "Selected addon does not belong to this menu item.");
                }
                addons.add(addon);
            }
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> Cart.builder()
                        .user(user)
                        .branch(branch)
                        .build());

        // Multi-branch rule: Cart cannot hold items from multiple branches simultaneously
        if (!cart.getBranch().getId().equals(branch.getId())) {
            cart.setBranch(branch);
            cart.getItems().clear(); // Reset cart if switching branches
        }

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .menuItem(menuItem)
                .variant(variant)
                .quantity(request.getQuantity())
                .specialInstructions(request.getSpecialInstructions())
                .addons(addons)
                .build();

        cart.getItems().add(cartItem);
        Cart savedCart = cartRepository.save(cart);

        return calculateCartTotals(savedCart, null);
    }

    @Transactional
    public CartResponseDto removeItemFromCart(User user, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        Cart savedCart = cartRepository.save(cart);
        return calculateCartTotals(savedCart, null);
    }

    @Transactional
    public void clearCart(User user) {
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    public CartResponseDto calculateCartTotals(Cart cart, String couponCode) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        List<CartItemResponseDto> itemResponses = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            BigDecimal base = item.getMenuItem().getBasePrice();
            BigDecimal variantDelta = (item.getVariant() != null) ? item.getVariant().getPriceDelta() : BigDecimal.ZERO;
            BigDecimal addonsTotal = item.getAddons().stream()
                    .map(MenuItemAddon::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal unitPrice = base.add(variantDelta).add(addonsTotal);
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(itemTotal);

            // Item-level tax calculation
            BigDecimal itemTax = itemTotal.multiply(item.getMenuItem().getTaxRatePercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            taxAmount = taxAmount.add(itemTax);

            List<String> addonNames = item.getAddons().stream()
                    .map(MenuItemAddon::getAddonName)
                    .collect(Collectors.toList());

            itemResponses.add(CartItemResponseDto.builder()
                    .id(item.getId())
                    .menuItemId(item.getMenuItem().getId())
                    .itemName(item.getMenuItem().getName())
                    .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                    .variantName(item.getVariant() != null ? item.getVariant().getVariantName() : null)
                    .addonNames(addonNames)
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .totalPrice(itemTotal)
                    .specialInstructions(item.getSpecialInstructions())
                    .build());
        }

        // Coupon calculation
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCode = null;

        if (couponCode != null && !couponCode.isBlank()) {
            Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode.trim().toUpperCase());
            if (couponOpt.isPresent() && couponOpt.get().isValidNow()) {
                Coupon c = couponOpt.get();
                if (subtotal.compareTo(c.getMinOrderAmount()) >= 0) {
                    if (c.getDiscountType() == DiscountType.PERCENTAGE) {
                        discount = subtotal.multiply(c.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (c.getMaxDiscountAmount() != null && discount.compareTo(c.getMaxDiscountAmount()) > 0) {
                            discount = c.getMaxDiscountAmount();
                        }
                    } else {
                        discount = c.getDiscountValue().min(subtotal);
                    }
                    appliedCode = c.getCode();
                }
            }
        }

        BigDecimal finalTotal = subtotal.add(taxAmount).subtract(discount).max(BigDecimal.ZERO);

        return CartResponseDto.builder()
                .id(cart.getId())
                .branchId(cart.getBranch().getId())
                .branchName(cart.getBranch().getName())
                .items(itemResponses)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .discountAmount(discount)
                .deliveryFee(BigDecimal.ZERO)
                .finalTotal(finalTotal)
                .appliedCouponCode(appliedCode)
                .build();
    }
}

package com.restauranthub.service;

import com.restauranthub.dto.MenuItemDto;
import com.restauranthub.entity.*;
import com.restauranthub.entity.enums.InventoryMovementType;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository categoryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional(readOnly = true)
    public List<MenuItemDto> getMenuByBranch(Long branchId) {
        return menuItemRepository.findByBranchIdAndIsAvailableTrue(branchId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> getMenuByBranchAndCategory(Long branchId, Long categoryId) {
        return menuItemRepository.findByBranchIdAndCategoryIdAndIsAvailableTrue(branchId, categoryId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> searchMenuItems(Long branchId, String query) {
        if (query == null || query.isBlank()) {
            return getMenuByBranch(branchId);
        }
        return menuItemRepository.searchMenuItems(branchId, query.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Cross-branch search across all Bangalore branches — used by the live AJAX search API.
     */
    @Transactional(readOnly = true)
    public List<MenuItemDto> searchAllBranches(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return menuItemRepository.searchAllBranches(query.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MenuItemDto getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
        return mapToDto(item);
    }

    @Transactional
    public void adjustStock(Long menuItemId, int delta, InventoryMovementType type, String note) {
        InventoryItem item = inventoryItemRepository.findByMenuItemIdWithLock(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "menuItemId", menuItemId));

        int newQuantity = item.getQuantityInStock() + delta;
        if (newQuantity < 0) {
            throw new BadRequestException("INSUFFICIENT_STOCK",
                    "Cannot reduce stock below 0. Current stock: " + item.getQuantityInStock());
        }

        item.setQuantityInStock(newQuantity);
        inventoryItemRepository.save(item);

        InventoryMovement movement = InventoryMovement.builder()
                .inventoryItem(item)
                .quantityDelta(delta)
                .movementType(type)
                .referenceNote(note)
                .build();
        inventoryMovementRepository.save(movement);
    }

    private MenuItemDto mapToDto(MenuItem item) {
        List<MenuItemDto.VariantDto> variants = item.getVariants().stream()
                .map(v -> new MenuItemDto.VariantDto(v.getId(), v.getVariantName(), v.getPriceDelta()))
                .collect(Collectors.toList());

        List<MenuItemDto.AddonDto> addons = item.getAddons().stream()
                .map(a -> new MenuItemDto.AddonDto(a.getId(), a.getAddonName(), a.getPrice()))
                .collect(Collectors.toList());

        Integer inStock = (item.getInventoryItem() != null) ? item.getInventoryItem().getQuantityInStock() : null;

        return MenuItemDto.builder()
                .id(item.getId())
                .branchId(item.getBranch().getId())
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .name(item.getName())
                .description(item.getDescription())
                .basePrice(item.getBasePrice())
                .imageUrl(item.getImageUrl())
                .isVegetarian(item.isVegetarian())
                .isVegan(item.isVegan())
                .isGlutenFree(item.isGlutenFree())
                .allergenInfo(item.getAllergenInfo())
                .prepTimeMinutes(item.getPrepTimeMinutes())
                .taxRatePercent(item.getTaxRatePercent())
                .isAvailable(item.isAvailable())
                .quantityInStock(inStock)
                .variants(variants)
                .addons(addons)
                .build();
    }
}

package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.dto.MenuItemDto;
import com.restauranthub.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Menu & Food Catalog", description = "Endpoints for discovering branch-specific menus, dish descriptions, dietary classifications, and addon customizations")
public class MenuRestController {

    private final MenuService menuService;

    @GetMapping("/branches/{branchId}/menu")
    @Operation(summary = "Get complete active menu for a specific branch")
    public ResponseEntity<ApiResponse<List<MenuItemDto>>> getBranchMenu(
            @PathVariable Long branchId,
            @RequestParam(value = "query", required = false) String query) {
        List<MenuItemDto> items = menuService.searchMenuItems(branchId, query);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Live AJAX search endpoint used by the menu page's JavaScript.
     * Searches within a branch (or all Bangalore branches if no branchId).
     * GET /api/v1/menu/search?branchId=10&query=biryani
     */
    @GetMapping("/menu/search")
    @Operation(summary = "Live search menu items by query across branch — used by AJAX auto-suggest")
    public ResponseEntity<ApiResponse<List<MenuItemDto>>> searchMenu(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "query", required = false) String query) {
        List<MenuItemDto> items;
        if (branchId != null) {
            items = menuService.searchMenuItems(branchId, query);
        } else {
            items = menuService.searchAllBranches(query);
        }
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/menu/items/{id}")
    @Operation(summary = "Get specific menu item details including variants and addons")
    public ResponseEntity<ApiResponse<MenuItemDto>> getMenuItem(@PathVariable Long id) {
        MenuItemDto item = menuService.getMenuItemById(id);
        return ResponseEntity.ok(ApiResponse.success(item));
    }
}

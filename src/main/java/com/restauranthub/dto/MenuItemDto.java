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
public class MenuItemDto {

    private Long id;
    private Long branchId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private String imageUrl;
    private boolean isVegetarian;
    private boolean isVegan;
    private boolean isGlutenFree;
    private String allergenInfo;
    private Integer prepTimeMinutes;
    private BigDecimal taxRatePercent;
    private boolean isAvailable;
    private Integer quantityInStock;
    private List<VariantDto> variants;
    private List<AddonDto> addons;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VariantDto {
        private Long id;
        private String variantName;
        private BigDecimal priceDelta;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddonDto {
        private Long id;
        private String addonName;
        private BigDecimal price;
    }
}

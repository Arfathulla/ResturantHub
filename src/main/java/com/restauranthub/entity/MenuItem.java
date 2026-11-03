package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    @JsonIgnore
    private RestaurantBranch branch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_vegetarian", nullable = false)
    @Builder.Default
    private boolean isVegetarian = false;

    @Column(name = "is_vegan", nullable = false)
    @Builder.Default
    private boolean isVegan = false;

    @Column(name = "is_gluten_free", nullable = false)
    @Builder.Default
    private boolean isGlutenFree = false;

    @Column(name = "allergen_info", length = 255)
    private String allergenInfo;

    @Column(name = "prep_time_minutes", nullable = false)
    @Builder.Default
    private Integer prepTimeMinutes = 15;

    @Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRatePercent = new BigDecimal("5.00");

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItemVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItemAddon> addons = new ArrayList<>();

    @OneToOne(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private InventoryItem inventoryItem;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "party_halls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyHall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    @JsonIgnore
    private RestaurantBranch branch;

    @Column(name = "hall_name", nullable = false, length = 150)
    private String hallName;

    @Column(nullable = false)
    private Integer capacity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_rental_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRentalPrice;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hourlyRate = new BigDecimal("100.00");

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal depositAmount = new BigDecimal("200.00");

    @Column(name = "min_duration_hours", nullable = false)
    @Builder.Default
    private Integer minDurationHours = 2;

    @Column(name = "max_duration_hours", nullable = false)
    @Builder.Default
    private Integer maxDurationHours = 12;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<HallPackage> packages = new ArrayList<>();

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}

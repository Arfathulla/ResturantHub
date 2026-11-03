package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "hall_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    @JsonIgnore
    private PartyHall hall;

    @Column(name = "package_name", nullable = false, length = 150)
    private String packageName;

    @Column(name = "price_per_guest", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerGuest;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}

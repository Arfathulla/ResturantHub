package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.restauranthub.entity.enums.DiningSection;
import com.restauranthub.entity.enums.TableType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dining_tables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"branch_id", "table_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    @JsonIgnore
    private RestaurantBranch branch;

    @Column(name = "table_number", nullable = false, length = 50)
    private String tableNumber;

    @Column(name = "seating_capacity", nullable = false)
    private Integer seatingCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "table_type", nullable = false, length = 50)
    private TableType tableType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiningSection section;

    @Column(name = "is_accessible", nullable = false)
    @Builder.Default
    private boolean isAccessible = true;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}

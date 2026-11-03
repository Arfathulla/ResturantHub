package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.restauranthub.entity.enums.InventoryMovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @JsonIgnore
    private InventoryItem inventoryItem;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private InventoryMovementType movementType;

    @Column(name = "reference_note", length = 255)
    private String referenceNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

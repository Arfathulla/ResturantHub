package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.restauranthub.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "delivery_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonIgnore
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "current_location", length = 255)
    private String currentLocation;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Column(name = "driver_phone", length = 30)
    private String driverPhone;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "estimated_delivery_minutes")
    private Integer estimatedDeliveryMinutes;

    @Column(name = "status_updated_at", nullable = false)
    @Builder.Default
    private Instant statusUpdatedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

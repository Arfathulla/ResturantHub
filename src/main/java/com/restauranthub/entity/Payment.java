package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.restauranthub.entity.enums.PaymentProvider;
import com.restauranthub.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 64)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_reservation_id")
    private TableReservation tableReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_reservation_id")
    private RoomReservation roomReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_reservation_id")
    private HallReservation hallReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_reservation_id")
    private ParkingReservation parkingReservation;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentProvider provider;

    @Column(name = "gateway_transaction_id", length = 150)
    private String gatewayTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_details", length = 255)
    private String paymentDetails;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Refund> refunds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

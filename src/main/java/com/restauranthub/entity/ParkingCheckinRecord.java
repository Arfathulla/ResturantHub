package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "parking_checkin_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingCheckinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_reservation_id", nullable = false)
    @JsonIgnore
    private ParkingReservation parkingReservation;

    @CreationTimestamp
    @Column(name = "actual_entry_time", nullable = false, updatable = false)
    private Instant actualEntryTime;

    @Column(name = "actual_exit_time")
    private Instant actualExitTime;

    @Column(name = "handled_by_user_id")
    private Long handledByUserId;
}

package com.restauranthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingReservationResponseDto {

    private Long id;
    private String referenceNo;
    private Long branchId;
    private String branchName;
    private Long parkingSlotId;
    private String slotNumber;
    private String vehicleType;
    private boolean isCovered;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalPrice;
    private String status;
    private Instant expiresAt;
    // Note: vehicle registration number is purposely excluded from response to protect personal data
}

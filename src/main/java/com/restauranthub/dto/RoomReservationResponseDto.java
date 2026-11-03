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
public class RoomReservationResponseDto {

    private Long id;
    private String referenceNo;
    private Long branchId;
    private String branchName;
    private Long roomId;
    private String roomName;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationHours;
    private Integer guestCount;
    private BigDecimal totalPrice;
    private BigDecimal depositAmount;
    private String status;
    private Instant expiresAt;
    private String specialRequests;
}

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
public class TableReservationResponseDto {

    private Long id;
    private String referenceNo;
    private Long branchId;
    private String branchName;
    private Long tableId;
    private String tableNumber;
    private Integer seatingCapacity;
    private String section;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer guestCount;
    private String status;
    private BigDecimal depositAmount;
    private boolean depositPaid;
    private Instant expiresAt;
    private String specialRequests;
}

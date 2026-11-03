package com.restauranthub.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableReservationRequestDto {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Long tableId; // If null, system automatically assigns suitable available table

    @NotNull(message = "Reservation date is required")
    @FutureOrPresent(message = "Reservation date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "Guest count must be at least 1")
    @Max(value = 30, message = "Guest count cannot exceed 30 per table reservation")
    private Integer guestCount;

    private String specialRequests;
}

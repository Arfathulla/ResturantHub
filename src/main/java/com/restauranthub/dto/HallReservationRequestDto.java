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
public class HallReservationRequestDto {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Party Hall ID is required")
    private Long hallId;

    private Long packageId; // Optional catering package

    @NotBlank(message = "Event type is required (e.g., Birthday, Corporate, Wedding)")
    private String eventType;

    @NotNull(message = "Reservation date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Duration in hours is required")
    @Min(value = 2, message = "Minimum booking duration is 2 hours")
    @Max(value = 12, message = "Maximum booking duration is 12 hours")
    private Integer durationHours;

    @NotNull(message = "Guest count is required")
    @Min(value = 10, message = "Guest count must be at least 10 for party hall reservations")
    private Integer guestCount;

    private String specialRequests;
}

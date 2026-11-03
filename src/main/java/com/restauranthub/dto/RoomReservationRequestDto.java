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
public class RoomReservationRequestDto {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Dining Room ID is required")
    private Long roomId;

    @NotNull(message = "Reservation date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Duration in hours is required")
    @Min(value = 1, message = "Duration must be at least 1 hour")
    @Max(value = 8, message = "Duration cannot exceed 8 hours")
    private Integer durationHours;

    @NotNull(message = "Guest count is required")
    @Min(value = 1, message = "Guest count must be at least 1")
    private Integer guestCount;

    private String specialRequests;
}

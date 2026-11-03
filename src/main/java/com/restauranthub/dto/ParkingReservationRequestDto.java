package com.restauranthub.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.restauranthub.entity.enums.VehicleType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ParkingReservationRequestDto {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Long parkingSlotId; // Optional; if omitted, automatically allocates suitable slot

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle registration number is required")
    @Size(min = 3, max = 20, message = "Vehicle registration must be between 3 and 20 characters")
    private String vehicleRegistrationNo;

    @NotNull(message = "Reservation date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
}

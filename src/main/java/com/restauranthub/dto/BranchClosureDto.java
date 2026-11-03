package com.restauranthub.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchClosureDto {

    private Long id;

    @NotNull(message = "Closure date is required")
    @FutureOrPresent(message = "Closure date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate closureDate;

    @NotBlank(message = "Reason for closure is required")
    private String reason;
}

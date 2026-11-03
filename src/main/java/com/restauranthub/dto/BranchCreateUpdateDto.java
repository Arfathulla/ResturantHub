package com.restauranthub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchCreateUpdateDto {

    @NotBlank(message = "Branch name is required")
    @Size(max = 150, message = "Branch name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Branch code is required")
    @Size(max = 50, message = "Branch code cannot exceed 50 characters")
    private String code;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9\\-\\s()]{7,20}$", message = "Phone number format is invalid")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    @Builder.Default
    private String timeZone = "America/New_York";

    private String imageUrl;

    @Builder.Default
    private boolean isActive = true;

    private List<OperatingHoursDto> operatingHours;
}

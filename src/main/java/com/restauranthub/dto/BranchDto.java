package com.restauranthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDto {

    private Long id;
    private String name;
    private String code;
    private String description;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String phoneNumber;
    private String email;
    private String timeZone;
    private String imageUrl;
    private boolean isActive;
    private List<OperatingHoursDto> operatingHours;
    private List<BranchClosureDto> upcomingClosures;
}

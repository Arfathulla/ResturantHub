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
public class AuthResponseDto {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresInMs;

    private Long userId;

    private String email;

    private String fullName;

    private List<String> roles;
}

package com.restauranthub.dto.sql;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryRequestDto {

    @NotBlank(message = "SQL query string cannot be empty")
    private String query;

    @Builder.Default
    private Integer maxRows = 500;

    @Builder.Default
    private boolean stopOnError = false;
}

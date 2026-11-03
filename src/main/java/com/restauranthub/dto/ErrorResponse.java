package com.restauranthub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;

    private int status;

    private String errorCode;

    private String message;

    private String path;

    private String correlationId;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private List<ValidationError> validationErrors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidationError {
        private String field;
        private String rejectedValue;
        private String message;
    }
}

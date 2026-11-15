package com.restauranthub.dto.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlBatchResultDto {

    @Builder.Default
    private List<SqlQueryResultDto> results = new ArrayList<>();

    private int totalQueries;
    private int successfulQueries;
    private int failedQueries;
    private long totalExecutionTimeMs;

    @Builder.Default
    private Instant executedAt = Instant.now();
}

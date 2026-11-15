package com.restauranthub.dto.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryResultDto {

    private String query;
    private String queryType; // SELECT, DML (INSERT/UPDATE/DELETE), DDL, OTHER
    
    @Builder.Default
    private List<String> columns = new ArrayList<>();

    @Builder.Default
    private List<String> columnTypes = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> rows = new ArrayList<>();

    private int rowCount;
    private int affectedRows;
    private long executionTimeMs;
    private boolean success;
    private String errorMessage;
    private String sqlState;
    private Integer errorCode;
}

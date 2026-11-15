package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.dto.sql.PredefinedQueryDto;
import com.restauranthub.dto.sql.SqlBatchResultDto;
import com.restauranthub.dto.sql.SqlQueryRequestDto;
import com.restauranthub.dto.sql.TableSchemaDto;
import com.restauranthub.service.sql.SqlQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/admin/sql", "/api/admin/sql"})
@RequiredArgsConstructor
@Tag(name = "Admin SQL Query Interface", description = "Endpoints for executing dynamic SQL queries, scripts, and database schema inspections")
public class AdminSqlRestController {

    private final SqlQueryService sqlQueryService;

    @PostMapping("/execute")
    @Operation(summary = "Execute single or multiple SQL statements against dedicated MySQL database")
    public ResponseEntity<ApiResponse<SqlBatchResultDto>> executeQuery(@Valid @RequestBody SqlQueryRequestDto request) {
        SqlBatchResultDto result = sqlQueryService.executeQuery(
                request.getQuery(),
                request.getMaxRows(),
                request.isStopOnError()
        );
        return ResponseEntity.ok(ApiResponse.success("SQL query executed", result));
    }

    @PostMapping("/execute-all")
    @Operation(summary = "Execute all predefined queries across all 12 platform modules")
    public ResponseEntity<ApiResponse<SqlBatchResultDto>> executeAllQueries() {
        SqlBatchResultDto result = sqlQueryService.executeAllPredefinedQueries();
        return ResponseEntity.ok(ApiResponse.success("All predefined queries executed successfully", result));
    }

    @GetMapping("/tables")
    @Operation(summary = "List all tables with schema details and row counts")
    public ResponseEntity<ApiResponse<List<TableSchemaDto>>> getTables() {
        List<TableSchemaDto> tables = sqlQueryService.getDatabaseSchemaOverview();
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    @GetMapping("/tables/{tableName}")
    @Operation(summary = "Get column specifications and metadata for a specific table")
    public ResponseEntity<ApiResponse<TableSchemaDto>> getTableDetails(@PathVariable String tableName) {
        TableSchemaDto details = sqlQueryService.getTableDetails(tableName);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @GetMapping("/predefined")
    @Operation(summary = "List all predefined SQL queries categorized by module")
    public ResponseEntity<ApiResponse<Map<String, List<PredefinedQueryDto>>>> getPredefinedQueries() {
        Map<String, List<PredefinedQueryDto>> queries = sqlQueryService.getCategorizedPredefinedQueries();
        return ResponseEntity.ok(ApiResponse.success(queries));
    }
}

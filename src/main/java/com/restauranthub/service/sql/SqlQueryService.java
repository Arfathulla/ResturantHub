package com.restauranthub.service.sql;

import com.restauranthub.dto.sql.PredefinedQueryDto;
import com.restauranthub.dto.sql.SqlBatchResultDto;
import com.restauranthub.dto.sql.TableSchemaDto;

import java.util.List;
import java.util.Map;

public interface SqlQueryService {

    /**
     * Executes one or more semicolon-separated SQL statements.
     *
     * @param sql     the SQL string (single statement or multiple statements)
     * @param maxRows maximum number of rows to return for SELECT queries
     * @return batch execution result with detailed metrics for each statement
     */
    SqlBatchResultDto executeQuery(String sql, Integer maxRows);

    /**
     * Executes one or more semicolon-separated SQL statements with stopOnError option.
     */
    SqlBatchResultDto executeQuery(String sql, Integer maxRows, boolean stopOnError);

    /**
     * Retrieves all table names present in the active database.
     */
    List<String> getAllTableNames();

    /**
     * Retrieves schema details (row count and column information) for all tables.
     */
    List<TableSchemaDto> getDatabaseSchemaOverview();

    /**
     * Retrieves column definitions, constraints, and row count for a specific table.
     */
    TableSchemaDto getTableDetails(String tableName);

    /**
     * Retrieves predefined queries categorized by Bhojana Bangalore modules.
     */
    Map<String, List<PredefinedQueryDto>> getCategorizedPredefinedQueries();

    /**
     * Retrieves a flat list of all predefined SQL queries.
     */
    List<PredefinedQueryDto> getAllPredefinedQueries();

    /**
     * Executes all predefined queries across all 12 modules and aggregates the results.
     */
    SqlBatchResultDto executeAllPredefinedQueries();
}

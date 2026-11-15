package com.restauranthub;

import com.restauranthub.dto.sql.PredefinedQueryDto;
import com.restauranthub.dto.sql.SqlBatchResultDto;
import com.restauranthub.dto.sql.SqlQueryResultDto;
import com.restauranthub.dto.sql.TableSchemaDto;
import com.restauranthub.service.sql.SqlQueryService;
import com.restauranthub.service.sql.SqlQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SqlQueryServiceTest {

    @Autowired
    private SqlQueryService sqlQueryService;

    @Test
    @DisplayName("SQL Service: Execute single SELECT query successfully")
    void testExecuteSingleSelectQuery() {
        SqlBatchResultDto batchResult = sqlQueryService.executeQuery("SELECT 1 AS test_col, 'Bhojana' AS project_name;", 100);

        assertNotNull(batchResult);
        assertEquals(1, batchResult.getTotalQueries());
        assertEquals(1, batchResult.getSuccessfulQueries());
        assertEquals(0, batchResult.getFailedQueries());

        SqlQueryResultDto result = batchResult.getResults().get(0);
        assertTrue(result.isSuccess());
        assertEquals("SELECT", result.getQueryType());
        assertEquals(2, result.getColumns().size());
        assertEquals(1, result.getRowCount());
        assertEquals("1", String.valueOf(result.getRows().get(0).get("test_col")));
        assertEquals("Bhojana", result.getRows().get(0).get("project_name"));
    }

    @Test
    @DisplayName("SQL Service: Execute multiple semicolon-separated queries in batch")
    void testExecuteBatchQueries() {
        String script =
                "SELECT 100 AS num;\n" +
                "SELECT 'Second Query' AS msg;\n" +
                "SELECT 'Third Query' AS msg3;";

        SqlBatchResultDto batchResult = sqlQueryService.executeQuery(script, 100);

        assertNotNull(batchResult);
        assertEquals(3, batchResult.getTotalQueries());
        assertEquals(3, batchResult.getSuccessfulQueries());
        assertEquals(0, batchResult.getFailedQueries());
        assertTrue(batchResult.getTotalExecutionTimeMs() >= 0);
    }

    @Test
    @DisplayName("SQL Service: Gracefully captures SQL syntax errors without crashing")
    void testExecuteInvalidSyntaxQuery() {
        SqlBatchResultDto batchResult = sqlQueryService.executeQuery("SELECT * FROM non_existent_dummy_table_xyz_12345;", 100);

        assertNotNull(batchResult);
        assertEquals(1, batchResult.getTotalQueries());
        assertEquals(0, batchResult.getSuccessfulQueries());
        assertEquals(1, batchResult.getFailedQueries());

        SqlQueryResultDto result = batchResult.getResults().get(0);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("SQL Service: Correctly parses SQL script containing comments and quotes")
    void testSplitSqlStatements() {
        String script =
                "-- Line comment with semicolon ; should not split\n" +
                "SELECT 'Hello; World' AS msg;\n" +
                "/* Block comment with ; semicolon */\n" +
                "SELECT 2 AS val;";

        List<String> statements = SqlQueryServiceImpl.splitSqlStatements(script);

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("'Hello; World'"));
        assertTrue(statements.get(1).contains("SELECT 2"));
    }

    @Test
    @DisplayName("SQL Service: Predefined queries cover all 12 modules")
    void testPredefinedQueries() {
        Map<String, List<PredefinedQueryDto>> categorized = sqlQueryService.getCategorizedPredefinedQueries();

        assertNotNull(categorized);
        assertTrue(categorized.size() >= 12, "Should contain predefined queries for at least 12 modules");

        List<PredefinedQueryDto> allQueries = sqlQueryService.getAllPredefinedQueries();
        assertFalse(allQueries.isEmpty());

        for (PredefinedQueryDto query : allQueries) {
            assertNotNull(query.getName());
            assertNotNull(query.getSql());
            assertNotNull(query.getModule());
        }
    }

    @Test
    @DisplayName("SQL Service: Retrieve database table list and schema overview")
    void testGetTableNamesAndSchema() {
        List<String> tables = sqlQueryService.getAllTableNames();
        assertNotNull(tables);

        List<TableSchemaDto> schema = sqlQueryService.getDatabaseSchemaOverview();
        assertNotNull(schema);
        assertEquals(tables.size(), schema.size());
    }
}

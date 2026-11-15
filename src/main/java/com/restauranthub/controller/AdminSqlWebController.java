package com.restauranthub.controller;

import com.restauranthub.dto.sql.SqlBatchResultDto;
import com.restauranthub.dto.sql.SqlOperationDef;
import com.restauranthub.dto.sql.SqlQueryResultDto;
import com.restauranthub.service.sql.DemoDataSeeder;
import com.restauranthub.service.sql.SqlOperationsCatalog;
import com.restauranthub.service.sql.SqlQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping({"/sql-queries", "/sql", "/admin/sql-console", "/admin/sql", "/sql-console"})
@RequiredArgsConstructor
public class AdminSqlWebController {

    private final SqlQueryService sqlQueryService;
    private final DemoDataSeeder demoDataSeeder;

    @GetMapping
    public String showSqlConsole(
            @RequestParam(value = "tab", required = false, defaultValue = "op_27_inner_join") String activeTab,
            @RequestParam(value = "query", required = false) String customQuery,
            Model model) {

        // Auto-seed if database orders are empty
        demoDataSeeder.seedDemoDataIfEmpty();

        // Retrieve requested operation definition (defaulting to operation 27: INNER JOIN)
        SqlOperationDef activeOp = SqlOperationsCatalog.getByKey(activeTab);
        if (activeOp == null) {
            activeOp = SqlOperationsCatalog.getByKey("op_27_inner_join");
            activeTab = "op_27_inner_join";
        }

        // Execute the single active operation directly against MySQL
        // If operation has multiple statements (e.g. DDL/DML followed by a verification SELECT),
        // pick the last successful result that has columns/rows (or the last executed statement).
        SqlQueryResultDto activeResult;
        try {
            SqlBatchResultDto batch = sqlQueryService.executeQuery(activeOp.getSql(), 100, false);
            if (batch.getResults() != null && !batch.getResults().isEmpty()) {
                SqlQueryResultDto selected = null;
                for (SqlQueryResultDto r : batch.getResults()) {
                    if (r.isSuccess() && r.getColumns() != null && !r.getColumns().isEmpty()) {
                        selected = r; // prefer SELECT or DESCRIBE result set
                    }
                }
                activeResult = (selected != null) ? selected : batch.getResults().get(batch.getResults().size() - 1);
            } else {
                activeResult = SqlQueryResultDto.builder()
                        .query(activeOp.getSql())
                        .columns(Collections.emptyList())
                        .rows(Collections.emptyList())
                        .rowCount(0)
                        .executionTimeMs(1)
                        .success(true)
                        .build();
            }
        } catch (Exception e) {
            activeResult = SqlQueryResultDto.builder()
                    .query(activeOp.getSql())
                    .columns(Collections.emptyList())
                    .rows(Collections.emptyList())
                    .rowCount(0)
                    .executionTimeMs(1)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }

        // Categorized operations dictionary for the dropdown: Basic (1-18), Intermediate (19-42), Advanced (43-76)
        Map<String, List<SqlOperationDef>> categorizedOps = SqlOperationsCatalog.getCategorizedOperations();

        model.addAttribute("allOperations", SqlOperationsCatalog.getAllOperations());
        model.addAttribute("categorizedOps", categorizedOps);
        model.addAttribute("activeOp", activeOp);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("activeResult", activeResult);
        model.addAttribute("totalOperationsCount", SqlOperationsCatalog.getAllOperations().size());
        model.addAttribute("tables", sqlQueryService.getAllTableNames());
        model.addAttribute("schemaOverview", sqlQueryService.getDatabaseSchemaOverview());

        // Handle optional custom query if submitted
        if (customQuery != null && !customQuery.trim().isEmpty()) {
            SqlBatchResultDto customBatch = sqlQueryService.executeQuery(customQuery, 100, false);
            model.addAttribute("customResult", customBatch);
            model.addAttribute("customQuery", customQuery);
            model.addAttribute("showCustomResult", true);
        }

        return "admin/sql-console";
    }

    @PostMapping("/seed-demo")
    public String seedDemoData(RedirectAttributes redirectAttributes) {
        try {
            demoDataSeeder.forceSeedDemoData();
            redirectAttributes.addFlashAttribute("successMessage", "Bangalore demo data seeded successfully into MySQL!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error seeding demo data: " + e.getMessage());
        }
        return "redirect:/sql-queries?tab=op_27_inner_join";
    }

    @PostMapping("/execute")
    public String executeCustomQuery(
            @RequestParam("query") String query,
            @RequestParam(value = "maxRows", defaultValue = "100") Integer maxRows,
            @RequestParam(value = "stopOnError", defaultValue = "false") boolean stopOnError,
            Model model) {

        SqlBatchResultDto batchResult = sqlQueryService.executeQuery(query, maxRows, stopOnError);
        model.addAttribute("customResult", batchResult);
        model.addAttribute("customQuery", query);
        model.addAttribute("showCustomResult", true);

        return showSqlConsole("op_27_inner_join", query, model);
    }
}

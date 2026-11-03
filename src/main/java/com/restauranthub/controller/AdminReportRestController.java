package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.dto.DashboardMetricsDto;
import com.restauranthub.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Administrative Reporting", description = "Operational metrics, revenue recognition, and stock shortage alerts")
public class AdminReportRestController {

    private final ReportingService reportingService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get aggregated platform dashboard KPI metrics (Admin only)")
    public ResponseEntity<ApiResponse<DashboardMetricsDto>> getDashboardMetrics() {
        DashboardMetricsDto metrics = reportingService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}

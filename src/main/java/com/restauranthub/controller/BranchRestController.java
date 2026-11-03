package com.restauranthub.controller;

import com.restauranthub.dto.ApiResponse;
import com.restauranthub.dto.BranchClosureDto;
import com.restauranthub.dto.BranchCreateUpdateDto;
import com.restauranthub.dto.BranchDto;
import com.restauranthub.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Restaurant Branches", description = "Endpoints for discovering restaurant locations, operating hours, and admin configuration")
public class BranchRestController {

    private final BranchService branchService;

    @GetMapping("/branches")
    @Operation(summary = "Get list of active restaurant branches with optional search filter")
    public ResponseEntity<ApiResponse<List<BranchDto>>> getBranches(@RequestParam(value = "query", required = false) String query) {
        List<BranchDto> branches = branchService.searchBranches(query);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    @GetMapping("/branches/{id}")
    @Operation(summary = "Get specific restaurant branch details including schedule and closures")
    public ResponseEntity<ApiResponse<BranchDto>> getBranchById(@PathVariable Long id) {
        BranchDto branch = branchService.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.success(branch));
    }

    @PostMapping("/admin/branches")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new restaurant branch (Admin only)")
    public ResponseEntity<ApiResponse<BranchDto>> createBranch(@Valid @RequestBody BranchCreateUpdateDto dto) {
        BranchDto created = branchService.createBranch(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created successfully", created));
    }

    @PutMapping("/admin/branches/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing restaurant branch (Admin only)")
    public ResponseEntity<ApiResponse<BranchDto>> updateBranch(@PathVariable Long id, @Valid @RequestBody BranchCreateUpdateDto dto) {
        BranchDto updated = branchService.updateBranch(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Branch updated successfully", updated));
    }

    @PostMapping("/admin/branches/{id}/closures")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Operation(summary = "Schedule a temporary holiday or maintenance closure for a branch")
    public ResponseEntity<ApiResponse<BranchClosureDto>> addClosure(@PathVariable Long id, @Valid @RequestBody BranchClosureDto dto) {
        BranchClosureDto saved = branchService.addClosure(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Closure registered successfully", saved));
    }
}

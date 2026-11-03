package com.restauranthub.controller;

import com.restauranthub.dto.DashboardMetricsDto;
import com.restauranthub.service.BranchService;
import com.restauranthub.service.ReportingService;
import com.restauranthub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminWebController {

    private final ReportingService reportingService;
    private final BranchService branchService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        DashboardMetricsDto metrics = reportingService.getDashboardMetrics();
        model.addAttribute("metrics", metrics);
        model.addAttribute("branches", branchService.getAllActiveBranches());
        model.addAttribute("users", userService.getAllUsers());
        return "admin/dashboard";
    }
}

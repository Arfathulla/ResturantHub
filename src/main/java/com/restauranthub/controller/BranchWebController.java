package com.restauranthub.controller;

import com.restauranthub.dto.BranchDto;
import com.restauranthub.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchWebController {

    private final BranchService branchService;

    @GetMapping
    public String showBranchesDirectory(@RequestParam(value = "query", required = false) String query, Model model) {
        List<BranchDto> branches = branchService.searchBranches(query);
        model.addAttribute("branches", branches);
        model.addAttribute("searchQuery", query);
        return "branches";
    }

    @GetMapping("/{id}")
    public String showBranchDetails(@PathVariable Long id, Model model) {
        BranchDto branch = branchService.getBranchById(id);
        model.addAttribute("branch", branch);
        return "branch-details";
    }
}

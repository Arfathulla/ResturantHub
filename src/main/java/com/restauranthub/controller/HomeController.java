package com.restauranthub.controller;

import com.restauranthub.dto.BranchDto;
import com.restauranthub.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BranchService branchService;

    @GetMapping("/")
    public String showHomePage(Model model) {
        List<BranchDto> branches = branchService.getAllActiveBranches();
        model.addAttribute("branches", branches);
        return "index";
    }

    @GetMapping("favicon.ico")
    @ResponseBody
    public void returnNoFavicon() {
        // Return clean HTTP 200 with empty body for direct browser favicon requests
    }
}

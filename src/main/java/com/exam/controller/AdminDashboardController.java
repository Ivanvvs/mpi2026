package com.exam.controller;

import com.exam.dto.AdminDashboardResponse;
import com.exam.dto.ConfirmRankUpdatesRequest;
import com.exam.service.AdminDashboardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        return adminDashboardService.getDashboard();
    }

    @GetMapping("/rank-preview")
    public AdminDashboardResponse getRankPreview() {
        return adminDashboardService.getRankPreview();
    }

    @PostMapping("/ranks/confirm")
    public AdminDashboardResponse confirmRankUpdates(@Valid @RequestBody ConfirmRankUpdatesRequest request) {
        return adminDashboardService.confirmRankUpdates(request);
    }
}

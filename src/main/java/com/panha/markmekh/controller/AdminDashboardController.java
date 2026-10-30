package com.panha.markmekh.controller;

import com.panha.markmekh.dto.AdminDashboardSummary;
import com.panha.markmekh.dto.ApiResponse;
import com.panha.markmekh.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<AdminDashboardSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(service.summary()));
    }
}
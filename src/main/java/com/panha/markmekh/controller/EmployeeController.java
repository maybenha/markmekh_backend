package com.panha.markmekh.controller;

import com.panha.markmekh.dto.ApiResponse;
import com.panha.markmekh.dto.EmployeeRequest;
import com.panha.markmekh.dto.EmployeeResponse;
import com.panha.markmekh.exception.BadRequestException;
import com.panha.markmekh.security.SecurityUtils;
import com.panha.markmekh.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ═════════════════════════════════════════════════════════════════════
    // CREATE / LIST / GET
    // ═════════════════════════════════════════════════════════════════════

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Employee created",
                employeeService.create(req)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.list(keyword, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getById(id)));
    }

    // ═════════════════════════════════════════════════════════════════════
    // MY PROFILE (self-service) — salary stripped, bank details included
    // ═════════════════════════════════════════════════════════════════════

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeResponse>> myProfile() {
        EmployeeResponse resp = employeeService.getByUserId(
                SecurityUtils.currentUserId());
        // Salary is only revealed via password on POST /me/salary.
        resp.setBaseSalary(null);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    // ═════════════════════════════════════════════════════════════════════
    // UPDATE MY PROFILE (self-service: bank details & contact info ONLY)
    // 🔒 Uses a dedicated service method — baseSalary and all HR-controlled
    //    fields are never modified or wiped here.
    // ═════════════════════════════════════════════════════════════════════

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateMyProfile(
            @Valid @RequestBody EmployeeRequest req) {
        EmployeeResponse updated = employeeService.updateMyProfile(
                SecurityUtils.currentUserId(), req);
        // Don't leak salary without the password check
        updated.setBaseSalary(null);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", updated));
    }

    // ═════════════════════════════════════════════════════════════════════
    // REVEAL SALARY (requires password)
    // ═════════════════════════════════════════════════════════════════════

    @PostMapping("/me/salary")
    public ResponseEntity<ApiResponse<EmployeeResponse>> revealSalary(
            @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        EmployeeResponse resp = employeeService.getMySalary(
                SecurityUtils.currentUserId(), password);
        return ResponseEntity.ok(ApiResponse.ok("Salary revealed", resp));
    }

    // ═════════════════════════════════════════════════════════════════════
    // UPDATE / STATUS (admin / HR)
    // ═════════════════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Employee updated",
                employeeService.update(id, req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        employeeService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated", null));
    }

    // ═════════════════════════════════════════════════════════════════════
    // UPDATE PROFILE PHOTO (base64)
    // ═════════════════════════════════════════════════════════════════════

    @PatchMapping("/me/profile-photo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateProfilePhoto(
            @RequestBody Map<String, String> body) {
        String dataUrl = body.get("profilePhoto");
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new BadRequestException("profilePhoto is required");
        }
        EmployeeResponse resp = employeeService.updateProfilePhoto(
                SecurityUtils.currentUserId(), dataUrl);
        resp.setBaseSalary(null);
        return ResponseEntity.ok(ApiResponse.ok("Profile photo updated", resp));
    }

    @PatchMapping("/me/cover-photo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateCoverPhoto(
            @RequestBody Map<String, String> body) {
        String dataUrl = body.get("coverPhoto");
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new BadRequestException("coverPhoto is required");
        }
        EmployeeResponse resp = employeeService.updateCoverPhoto(
                SecurityUtils.currentUserId(), dataUrl);
        resp.setBaseSalary(null);
        return ResponseEntity.ok(ApiResponse.ok("Cover photo updated", resp));
    }
}
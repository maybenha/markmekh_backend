package com.panha.markmekh.controller;

import com.panha.markmekh.dto.*;
import com.panha.markmekh.repository.EmployeeRepository;
import com.panha.markmekh.security.SecurityUtils;
import com.panha.markmekh.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmployeeRepository employeeRepository;

    // ⚡ Single constructor — both dependencies injected
    public AuthController(AuthService authService,
                          EmployeeRepository employeeRepository) {
        this.authService = authService;
        this.employeeRepository = employeeRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Login successful", authService.login(req)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGISTER
    // ═══════════════════════════════════════════════════════════════════════
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(ApiResponse.ok(
                "Account created successfully", null));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Token refreshed", authService.refresh(req)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════════════════════
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHANGE PASSWORD
    // ═══════════════════════════════════════════════════════════════════════
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(SecurityUtils.currentUserId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CURRENT USER (/me)
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserDto>> me() {
        var user = SecurityUtils.currentUser();
        var emp = employeeRepository.findByUser_Id(user.getId()).orElse(null);

        return ResponseEntity.ok(ApiResponse.ok(CurrentUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .hasEmployeeProfile(emp != null)
                .employeeCode(emp != null ? emp.getEmployeeCode() : null)
                .fullNameEn(emp != null ? emp.getFullNameEn() : null)
                .fullNameKh(emp != null ? emp.getFullNameKh() : null)
                .phoneNumber(emp != null ? emp.getPhoneNumber() : null)
                .build()));
    }
}
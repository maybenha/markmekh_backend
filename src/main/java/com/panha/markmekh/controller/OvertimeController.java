package com.panha.markmekh.controller;

import com.panha.markmekh.dto.ApiResponse;
import com.panha.markmekh.dto.OvertimeRequestDto;
import com.panha.markmekh.dto.OvertimeResponse;
import com.panha.markmekh.security.SecurityUtils;
import com.panha.markmekh.service.OvertimeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    private final OvertimeService overtimeService;

    public OvertimeController(OvertimeService overtimeService) {
        this.overtimeService = overtimeService;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<OvertimeResponse>> submit(
            @Valid @RequestBody OvertimeRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Submitted",
                overtimeService.submit(SecurityUtils.currentUserId(), dto)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OvertimeResponse>>> myRequests() {
        return ResponseEntity.ok(ApiResponse.ok(
                overtimeService.myRequests(SecurityUtils.currentUserId())));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<List<OvertimeResponse>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok(overtimeService.pending()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<OvertimeResponse>>> all() {
        return ResponseEntity.ok(ApiResponse.ok(overtimeService.all()));
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<OvertimeResponse>> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok("Approved",
                overtimeService.review(id, true, note, SecurityUtils.currentUser())));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<OvertimeResponse>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok("Rejected",
                overtimeService.review(id, false, note, SecurityUtils.currentUser())));
    }
}
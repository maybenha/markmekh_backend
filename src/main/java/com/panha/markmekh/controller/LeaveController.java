package com.panha.markmekh.controller;

import com.panha.markmekh.dto.ApiResponse;
import com.panha.markmekh.dto.LeaveRequestDto;
import com.panha.markmekh.entity.LeaveRequest;
import com.panha.markmekh.security.SecurityUtils;
import com.panha.markmekh.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.panha.markmekh.dto.LeaveResponseDto;
import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<LeaveResponseDto>> submit(
            @Valid @RequestBody LeaveRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Submitted",
                leaveService.submit(SecurityUtils.currentUserId(), dto)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<java.util.List<com.panha.markmekh.dto.LeaveResponseDto>>> myRequests() {
        return ResponseEntity.ok(ApiResponse.ok(
                leaveService.myDtos(SecurityUtils.currentUserId())));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<com.panha.markmekh.dto.LeaveResponseDto>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.pendingDtos()));
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<LeaveResponseDto>> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok("Approved",
                leaveService.review(id, true, note, SecurityUtils.currentUser())));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<LeaveResponseDto>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok("Rejected",
                leaveService.review(id, false, note, SecurityUtils.currentUser())));
    }
}
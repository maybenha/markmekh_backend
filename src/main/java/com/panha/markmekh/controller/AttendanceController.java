package com.panha.markmekh.controller;

import com.panha.markmekh.dto.*;
import com.panha.markmekh.entity.User;
import com.panha.markmekh.security.SecurityUtils;
import com.panha.markmekh.service.AttendanceService;
import com.panha.markmekh.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AttendanceController(AttendanceService attendanceService,
                                AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    // -------------------------------------------------------------------
    // SESSIONS
    // -------------------------------------------------------------------

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Session created",
                attendanceService.createSession(req, SecurityUtils.currentUser())));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> activeSessions() {
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.activeSessions()));
    }

    @PostMapping("/sessions/{id}/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable Long id) {
        attendanceService.revokeSession(id);
        return ResponseEntity.ok(ApiResponse.ok("Session revoked", null));
    }

    /**
     * Super Admin / HR: generate ONE shared QR session that all employees can scan.
     * The session defaults to a 30-minute window if no times are supplied.
     */
    @PostMapping("/sessions/generate-qr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<SessionResponse>> generateQr(
            @Valid @RequestBody CreateSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "QR session generated",
                attendanceService.createSession(req, SecurityUtils.currentUser())
        ));
    }

    // -------------------------------------------------------------------
    // SELF-SERVICE CHECK-IN / CHECK-OUT (JWT-authenticated employees)
    // -------------------------------------------------------------------

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody CheckInRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Checked in",
                attendanceService.checkIn(SecurityUtils.currentUserId(), req)));
    }

    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @Valid @RequestBody CheckInRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Checked out",
                attendanceService.checkOut(SecurityUtils.currentUserId(), req)));
    }

    // -------------------------------------------------------------------
    // QR + CREDENTIALS FLOW (public — no JWT required)
    // -------------------------------------------------------------------

    /**
     * Employee scans the admin's QR code, then submits username + password.
     * Backend verifies credentials, validates the session and geofence,
     * then records attendance on behalf of that employee.
     */
    @PostMapping("/scan-with-credentials")
    public ResponseEntity<ApiResponse<AttendanceResponse>> scanWithCredentials(
            @Valid @RequestBody ScanWithCredentialsRequest req) {

        // 1. Verify identity (username + password)
        User user = authService.verifyCredentials(req.getUsername(), req.getPassword());

        // 2. Build a CheckInRequest with token + location
        CheckInRequest ci = new CheckInRequest();
        ci.setSessionToken(req.getSessionToken());
        ci.setLatitude(req.getLatitude());
        ci.setLongitude(req.getLongitude());
        ci.setMethod(req.getMethod() == null ? "QR" : req.getMethod());

        // 3. Record attendance
        AttendanceResponse resp = attendanceService.checkInByUser(user, ci);
        return ResponseEntity.ok(ApiResponse.ok("Attendance recorded", resp));
    }

    // -------------------------------------------------------------------
    // HISTORY / REPORTS / SUMMARY
    // -------------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> myHistory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.myHistory(SecurityUtils.currentUserId(), from, to)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.summaryForEmployee(SecurityUtils.currentUserId())));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> byDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.byDate(date)));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<com.panha.markmekh.dto.TodayAttendanceDto>>> today() {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.todayList()));
    }
}
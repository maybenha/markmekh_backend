package com.panha.markmekh.service;

import com.panha.markmekh.dto.*;
import com.panha.markmekh.entity.*;
import com.panha.markmekh.exception.BadRequestException;
import com.panha.markmekh.exception.ResourceNotFoundException;
import com.panha.markmekh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final int LATE_GRACE_MINUTES = 10;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceRecordRepository attendanceRepo;
    private final AttendanceSessionRepository sessionRepo;
    private final EmployeeRepository employeeRepo;
    private final BranchRepository branchRepo;
    private final NotificationService notificationService; // NEW

    public AttendanceService(AttendanceRecordRepository attendanceRepo,
                             AttendanceSessionRepository sessionRepo,
                             EmployeeRepository employeeRepo,
                             BranchRepository branchRepo,
                             NotificationService notificationService) { // NEW
        this.attendanceRepo = attendanceRepo;
        this.sessionRepo = sessionRepo;
        this.employeeRepo = employeeRepo;
        this.branchRepo = branchRepo;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------
    // SESSION MANAGEMENT
    // -------------------------------------------------------------------

    @Transactional
    public SessionResponse createSession(CreateSessionRequest req, User creator) {
        Branch branch = null;
        if (req.getBranchId() != null) {
            branch = branchRepo.findById(req.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        }
        LocalDateTime start = req.getStartTime() != null ? req.getStartTime() : LocalDateTime.now();
        LocalDateTime end = req.getExpirationTime() != null
                ? req.getExpirationTime()
                : start.plusMinutes(30);

        AttendanceSession session = AttendanceSession.builder()
                .sessionToken(UUID.randomUUID().toString().replace("-", ""))
                .branch(branch)
                .purpose(req.getPurpose())
                .startTime(start)
                .expirationTime(end)
                .active(true)
                .revoked(false)
                .createdBy(creator)
                .build();
        sessionRepo.save(session);
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> activeSessions() {
        return sessionRepo.findByActiveTrueAndRevokedFalseAndExpirationTimeAfter(LocalDateTime.now())
                .stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(Long id) {
        AttendanceSession session = sessionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        session.setRevoked(true);
        session.setActive(false);
        sessionRepo.save(session);
    }

    // -------------------------------------------------------------------
    // CHECK-IN
    // -------------------------------------------------------------------

    @Transactional
    public AttendanceResponse checkIn(Long userId, CheckInRequest req) {
        Employee employee = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        AttendanceSession session = sessionRepo.findBySessionToken(req.getSessionToken())
                .orElseThrow(() -> new BadRequestException("Invalid session token"));

        validateSession(session);
        validateGeofence(session, req);

        LocalDate today = LocalDate.now();
        AttendanceRecord record = getTodayRecordSafe(employee, today, true);

        if (record.getCheckInTime() != null) {
            String t = record.getCheckInTime().format(HHMM);
            throw new BadRequestException(
                    "You already checked in at " + t + " today. Session: MORNING.");
        }

        record.setCheckInTime(LocalDateTime.now());
        record.setCheckInMethod(req.getMethod() == null ? "QR" : req.getMethod());
        record.setCheckInLatitude(toBigDecimal(req.getLatitude()));
        record.setCheckInLongitude(toBigDecimal(req.getLongitude()));
        record.setStatus("PRESENT");
        record.setLateMinutes(0);
        record.setOvertimeMinutes(0);
        record.setWorkMinutes(0);
        record.setEarlyLeaveMinutes(0);

        LocalDateTime shiftStart = today.atTime(8, 0);
        if (record.getCheckInTime().isAfter(shiftStart.plusMinutes(LATE_GRACE_MINUTES))) {
            long late = Duration.between(shiftStart, record.getCheckInTime()).toMinutes();
            record.setLateMinutes((int) late);
            record.setStatus("LATE");
        }

        attendanceRepo.save(record);

        // 🔔 NOTIFICATIONS START
        String empName = employee.getFullNameEn();
        String timeStr = record.getCheckInTime().format(HHMM);

        // 1. Notify the Employee
        notificationService.notifyUser(
                employee.getUser(),
                "ATTENDANCE_CHECK_IN",
                "Check-In Successful",
                "You checked in successfully at " + timeStr + ".",
                record.getId()
        );

        // 2. Notify Admins / HR
        notificationService.notifyAdmins(
                "ADMIN_ATTENDANCE_ALERT",
                "Employee Checked In",
                empName + " checked in at " + timeStr + ".",
                record.getId()
        );
        // 🔔 NOTIFICATIONS END

        return toAttendanceResponse(record);
    }

    // -------------------------------------------------------------------
    // CHECK-OUT
    // -------------------------------------------------------------------

    @Transactional
    public AttendanceResponse checkOut(Long userId, CheckInRequest req) {
        Employee employee = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        AttendanceSession session = sessionRepo.findBySessionToken(req.getSessionToken())
                .orElseThrow(() -> new BadRequestException("Invalid session token"));

        validateSession(session);
        validateGeofence(session, req);

        LocalDate today = LocalDate.now();
        AttendanceRecord record = getTodayRecordSafe(employee, today, false);
        if (record == null || record.getCheckInTime() == null) {
            throw new BadRequestException(
                    "You must check in first before checking out.");
        }

        if (record.getCheckOutTime() != null) {
            String t = record.getCheckOutTime().format(HHMM);
            throw new BadRequestException(
                    "You already checked out at " + t + " today. Session: MORNING.");
        }

        record.setCheckOutTime(LocalDateTime.now());
        record.setCheckOutMethod(req.getMethod() == null ? "QR" : req.getMethod());
        record.setCheckOutLatitude(toBigDecimal(req.getLatitude()));
        record.setCheckOutLongitude(toBigDecimal(req.getLongitude()));

        long workMinutes = Duration.between(record.getCheckInTime(), record.getCheckOutTime()).toMinutes();
        record.setWorkMinutes((int) workMinutes);

        LocalDateTime shiftEnd = today.atTime(17, 0);
        if (record.getCheckOutTime().isBefore(shiftEnd)) {
            long early = Duration.between(record.getCheckOutTime(), shiftEnd).toMinutes();
            record.setEarlyLeaveMinutes((int) early);
            record.setOvertimeMinutes(0);
        } else if (record.getCheckOutTime().isAfter(shiftEnd)) {
            long ot = Duration.between(shiftEnd, record.getCheckOutTime()).toMinutes();
            record.setOvertimeMinutes((int) ot);
            record.setEarlyLeaveMinutes(0);
        } else {
            record.setEarlyLeaveMinutes(0);
            record.setOvertimeMinutes(0);
        }

        attendanceRepo.save(record);

        // 🔔 NOTIFICATIONS START
        String empName = employee.getFullNameEn();
        String timeStr = record.getCheckOutTime().format(HHMM);
        String workHrs = String.format("%dh %dm", workMinutes / 60, workMinutes % 60);

        // 1. Notify the Employee
        notificationService.notifyUser(
                employee.getUser(),
                "ATTENDANCE_CHECK_OUT",
                "Check-Out Successful",
                "You checked out at " + timeStr + ". Total work: " + workHrs + ".",
                record.getId()
        );

        // 2. Notify Admins / HR
        notificationService.notifyAdmins(
                "ADMIN_ATTENDANCE_ALERT",
                "Employee Checked Out",
                empName + " checked out at " + timeStr + " (" + workHrs + ").",
                record.getId()
        );
        // 🔔 NOTIFICATIONS END

        return toAttendanceResponse(record);
    }

    // -------------------------------------------------------------------
    // SHARED QR + CREDENTIALS FLOW
    // -------------------------------------------------------------------

    /**
     * Check-in triggered by scanning a shared QR session, then proving identity
     * with username/password. Delegates to the standard checkIn flow.
     */
    @Transactional
    public AttendanceResponse checkInByUser(User user, CheckInRequest req) {
        return checkIn(user.getId(), req);
    }

    // -------------------------------------------------------------------
    // SAFE RECORD LOOKUP — handles leftover duplicate rows gracefully
    // -------------------------------------------------------------------

    /**
     * Returns today's attendance record for the employee.
     * If multiple rows exist (data drift), it returns the FIRST one (oldest by id)
     * instead of throwing NonUniqueResultException.
     */
    private AttendanceRecord getTodayRecordSafe(Employee employee, LocalDate date, boolean createIfMissing) {
        List<AttendanceRecord> list =
                attendanceRepo.findAllByEmployee_IdAndAttendanceDate(employee.getId(), date);

        if (list == null || list.isEmpty()) {
            if (!createIfMissing) return null;
            return AttendanceRecord.builder()
                    .employee(employee)
                    .attendanceDate(date)
                    .build();
        }

        // Sort by id ascending to always return the earliest row
        list.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return list.get(0);
    }

    // -------------------------------------------------------------------
    // HISTORY / REPORTS
    // -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AttendanceResponse> myHistory(Long userId, LocalDate from, LocalDate to) {
        Employee employee = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        return attendanceRepo.findByEmployee_IdAndAttendanceDateBetween(employee.getId(), from, to)
                .stream().map(this::toAttendanceResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> byDate(LocalDate date) {
        return attendanceRepo.findByAttendanceDate(date)
                .stream().map(this::toAttendanceResponse).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------
    // TODAY LIST (admin) — null-safe
    // -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TodayAttendanceDto> todayList() {
        LocalDate today = LocalDate.now();
        List<AttendanceRecord> records = attendanceRepo.findByAttendanceDate(today);

        Map<Long, AttendanceRecord> byEmp = new HashMap<>();
        for (AttendanceRecord r : records) {
            if (r.getEmployee() != null) {
                byEmp.put(r.getEmployee().getId(), r);
            }
        }

        List<TodayAttendanceDto> result = new ArrayList<>();
        for (Employee emp : employeeRepo.findAll()) {
            if (!"ACTIVE".equals(emp.getEmploymentStatus())) continue;

            AttendanceRecord r = byEmp.get(emp.getId());

            // Inside the todayList() method, update the builder:

            result.add(TodayAttendanceDto.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(emp.getFullNameEn())
                    .departmentName(emp.getDepartment() != null
                            ? emp.getDepartment().getName() : null)
                    .positionTitle(emp.getPosition() != null
                            ? emp.getPosition().getTitle() : null)
                    .checkInTime(r != null && r.getCheckInTime() != null
                            ? r.getCheckInTime().toString() : null)
                    .checkOutTime(r != null && r.getCheckOutTime() != null
                            ? r.getCheckOutTime().toString() : null)
                    .status(r != null && r.getStatus() != null
                            ? r.getStatus() : "ABSENT")
                    .lateMinutes(safeInt(r != null ? r.getLateMinutes() : null))
                    .workMinutes(safeInt(r != null ? r.getWorkMinutes() : null))
                    .overtimeMinutes(safeInt(r != null ? r.getOvertimeMinutes() : null))
                    // 👇 ADD THIS LINE
                    .profilePhotoUrl(emp.getProfilePhotoUrl())
                    .build());
        }
        return result;
    }

    // -------------------------------------------------------------------
    // DASHBOARD SUMMARY
    // -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardSummary summaryForEmployee(Long userId) {
        Employee employee = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        var records = attendanceRepo.findByEmployee_IdAndAttendanceDateBetween(
                employee.getId(), monthStart, today);

        int present = 0, late = 0, absent = 0, otMinutes = 0;
        for (AttendanceRecord r : records) {
            switch (r.getStatus()) {
                case "PRESENT" -> present++;
                case "LATE" -> late++;
                case "ABSENT" -> absent++;
            }
            otMinutes += safeInt(r.getOvertimeMinutes());
        }

        AttendanceRecord todayRec = records.stream()
                .filter(r -> r.getAttendanceDate().equals(today))
                .findFirst().orElse(null);

        return DashboardSummary.builder()
                .presentDays(present)
                .lateDays(late)
                .absentDays(absent)
                .leaveBalance(employee.getAnnualLeaveBalance() != null
                        ? employee.getAnnualLeaveBalance() : 0)
                .overtimeHours(otMinutes / 60)
                .todayLateMinutes(todayRec != null ? todayRec.getLateMinutes() : null)
                .todayOvertimeMinutes(todayRec != null ? todayRec.getOvertimeMinutes() : null)
                .todayWorkMinutes(todayRec != null ? todayRec.getWorkMinutes() : null)
                .todayCheckInTime(todayRec != null && todayRec.getCheckInTime() != null
                        ? todayRec.getCheckInTime().toString() : null)
                .todayCheckOutTime(todayRec != null && todayRec.getCheckOutTime() != null
                        ? todayRec.getCheckOutTime().toString() : null)
                .todayStatus(todayRec != null ? todayRec.getStatus() : "NOT_CHECKED_IN")
                .employeeName(employee.getFullNameEn())
                .employeeCode(employee.getEmployeeCode())
                .build();
    }

    // -------------------------------------------------------------------
    // VALIDATORS / HELPERS
    // -------------------------------------------------------------------

    private void validateSession(AttendanceSession session) {
        if (!session.isActive()) throw new BadRequestException("Session is not active");
        if (session.isRevoked()) throw new BadRequestException("Session revoked");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) throw new BadRequestException("Session not started");
        if (now.isAfter(session.getExpirationTime())) throw new BadRequestException("Session expired");
    }

    private void validateGeofence(AttendanceSession session, CheckInRequest req) {
        if (session.getBranch() == null) return;
        Branch branch = session.getBranch();
        if (branch.getLatitude() == null || branch.getLongitude() == null) return;
        if (req.getLatitude() == null || req.getLongitude() == null)
            throw new BadRequestException("Location is required");

        double distance = haversine(
                branch.getLatitude().doubleValue(), branch.getLongitude().doubleValue(),
                req.getLatitude().doubleValue(), req.getLongitude().doubleValue()
        );
        int radius = branch.getRadiusMeters() != null ? branch.getRadiusMeters() : 100;
        if (distance > radius) {
            throw new BadRequestException(String.format(
                    "You are outside the allowed area (%.1f m away, max %d m)", distance, radius));
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /**
     * Null-safe conversion of Integer to int. Prevents NPE when
     * JPA columns are NULL and the DTO field is a primitive.
     */
    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * Convert a nullable Double to a BigDecimal for JPA precision columns.
     */
    private BigDecimal toBigDecimal(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value);
    }

    private SessionResponse toSessionResponse(AttendanceSession s) {
        return SessionResponse.builder()
                .id(s.getId())
                .sessionToken(s.getSessionToken())
                .purpose(s.getPurpose())
                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                .startTime(s.getStartTime())
                .expirationTime(s.getExpirationTime())
                .active(s.isActive())
                .revoked(s.isRevoked())
                .build();
    }

    private AttendanceResponse toAttendanceResponse(AttendanceRecord r) {
        return AttendanceResponse.builder()
                .id(r.getId())
                .employeeId(r.getEmployee().getId())
                .employeeName(r.getEmployee().getFullNameEn())
                .attendanceDate(r.getAttendanceDate())
                .checkInTime(r.getCheckInTime())
                .checkOutTime(r.getCheckOutTime())
                .checkInMethod(r.getCheckInMethod())
                .checkOutMethod(r.getCheckOutMethod())
                .status(r.getStatus())
                .lateMinutes(safeInt(r.getLateMinutes()))
                .earlyLeaveMinutes(safeInt(r.getEarlyLeaveMinutes()))
                .overtimeMinutes(safeInt(r.getOvertimeMinutes()))
                .workMinutes(safeInt(r.getWorkMinutes()))
                .note(r.getNote())
                .build();
    }
}
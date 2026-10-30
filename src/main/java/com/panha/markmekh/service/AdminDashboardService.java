package com.panha.markmekh.service;

import com.panha.markmekh.dto.AdminDashboardSummary;
import com.panha.markmekh.entity.AttendanceRecord;
import com.panha.markmekh.entity.Employee;
import com.panha.markmekh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminDashboardService {

    private final EmployeeRepository employeeRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final AttendanceSessionRepository sessionRepo;
    private final LeaveRequestRepository leaveRepo;
    private final OvertimeRequestRepository overtimeRepo;

    public AdminDashboardService(EmployeeRepository employeeRepo,
                                 AttendanceRecordRepository attendanceRepo,
                                 AttendanceSessionRepository sessionRepo,
                                 LeaveRequestRepository leaveRepo,
                                 OvertimeRequestRepository overtimeRepo) {
        this.employeeRepo = employeeRepo;
        this.attendanceRepo = attendanceRepo;
        this.sessionRepo = sessionRepo;
        this.leaveRepo = leaveRepo;
        this.overtimeRepo = overtimeRepo;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummary summary() {
        LocalDate today = LocalDate.now();

        List<Employee> all = employeeRepo.findAll();
        long total = all.size();
        long active = all.stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getEmploymentStatus()))
                .count();

        List<AttendanceRecord> todayRecords = attendanceRepo.findByAttendanceDate(today);

        long present = todayRecords.stream()
                .filter(r -> r.getCheckInTime() != null)
                .count();
        long late = todayRecords.stream()
                .filter(r -> "LATE".equalsIgnoreCase(r.getStatus()))
                .count();
        long absent = todayRecords.stream()
                .filter(r -> "ABSENT".equalsIgnoreCase(r.getStatus()))
                .count();

        long pendingLeaves = leaveRepo.findByStatusOrderByCreatedAtDesc("PENDING").size();
        long pendingOvertimes = overtimeRepo.findByStatusOrderByCreatedAtDesc("PENDING").size();
        long openSessions = sessionRepo
                .findByActiveTrueAndRevokedFalseAndExpirationTimeAfter(LocalDateTime.now())
                .size();

        String todayLabel = today.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));

        return AdminDashboardSummary.builder()
                .totalEmployees(total)
                .activeEmployees(active)
                .presentToday(present)
                .lateToday(late)
                .absentToday(absent)
                .pendingLeaves(pendingLeaves)
                .pendingOvertimes(pendingOvertimes)
                .openSessions(openSessions)
                .todayLabel(todayLabel)
                .build();
    }
}
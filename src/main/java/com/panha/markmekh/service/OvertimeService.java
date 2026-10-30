package com.panha.markmekh.service;

import com.panha.markmekh.dto.OvertimeRequestDto;
import com.panha.markmekh.dto.OvertimeResponse;
import com.panha.markmekh.entity.Employee;
import com.panha.markmekh.entity.OvertimeRequest;
import com.panha.markmekh.entity.User;
import com.panha.markmekh.exception.BadRequestException;
import com.panha.markmekh.exception.ResourceNotFoundException;
import com.panha.markmekh.repository.EmployeeRepository;
import com.panha.markmekh.repository.OvertimeRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OvertimeService {

    private final OvertimeRequestRepository repo;
    private final EmployeeRepository employeeRepo;
    private final NotificationService notifService;

    public OvertimeService(OvertimeRequestRepository repo,
                           EmployeeRepository employeeRepo,
                           NotificationService notifService) {
        this.repo = repo;
        this.employeeRepo = employeeRepo;
        this.notifService = notifService;
    }

    @Transactional
    public OvertimeResponse submit(Long userId, OvertimeRequestDto dto) {
        Employee emp = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        if (!dto.getEndTime().isAfter(dto.getStartTime()))
            throw new BadRequestException("End time must be after start time");

        long minutes = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
        if (minutes <= 0)
            throw new BadRequestException("Overtime must be at least 1 minute");

        OvertimeRequest ot = OvertimeRequest.builder()
                .employee(emp)
                .overtimeDate(dto.getOvertimeDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .totalMinutes((int) minutes)
                .reason(dto.getReason())
                .status("PENDING")
                .build();
        ot = repo.save(ot);

        // 🔔 Notify admins + HR
        String empName = emp.getFullNameEn() != null
                ? emp.getFullNameEn()
                : emp.getEmployeeCode();
        String hours = String.format("%dh %dm", minutes / 60, minutes % 60);
        notifService.notifyAdmins(
                "OVERTIME_REQUEST",
                "New Overtime Request",
                empName + " requested " + hours + " of overtime on " + dto.getOvertimeDate(),
                ot.getId()
        );

        return toResponse(ot);
    }

    @Transactional(readOnly = true)
    public List<OvertimeResponse> myRequests(Long userId) {
        Employee emp = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return repo.findByEmployee_IdOrderByCreatedAtDesc(emp.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OvertimeResponse> pending() {
        return repo.findByStatusOrderByCreatedAtDesc("PENDING")
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OvertimeResponse> all() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public OvertimeResponse review(Long id, boolean approve, String note, User reviewer) {
        OvertimeRequest ot = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime request not found"));
        if (!"PENDING".equals(ot.getStatus()))
            throw new BadRequestException("Request already reviewed");

        ot.setStatus(approve ? "APPROVED" : "REJECTED");
        ot.setReviewedBy(reviewer);
        ot.setReviewNote(note);
        ot.setReviewedAt(LocalDateTime.now());
        ot = repo.save(ot);

        // 🔔 Notify the employee
        Employee emp = ot.getEmployee();
        if (emp != null && emp.getUser() != null) {
            String statusWord = approve ? "approved" : "rejected";
            notifService.notifyUser(
                    emp.getUser(),
                    "OVERTIME_" + ot.getStatus(),
                    "Overtime " + statusWord,
                    "Your overtime request for " + ot.getOvertimeDate()
                            + " has been " + statusWord + "."
                            + (note != null && !note.isEmpty() ? "\nNote: " + note : ""),
                    ot.getId()
            );
        }

        return toResponse(ot);
    }

    // -------------------------------------------------------------------------
    // ENTITY → DTO (reads lazy fields inside the transaction, avoids proxies)
    // -------------------------------------------------------------------------
    private OvertimeResponse toResponse(OvertimeRequest ot) {
        Employee emp = ot.getEmployee();
        return OvertimeResponse.builder()
                .id(ot.getId())
                .employeeId(emp != null ? emp.getId() : null)
                .employeeName(emp != null ? emp.getFullNameEn() : null)
                .employeeCode(emp != null ? emp.getEmployeeCode() : null)
                .overtimeDate(ot.getOvertimeDate())
                .startTime(ot.getStartTime())
                .endTime(ot.getEndTime())
                .totalMinutes(ot.getTotalMinutes())
                .reason(ot.getReason())
                .status(ot.getStatus())
                .reviewNote(ot.getReviewNote())
                .reviewedAt(ot.getReviewedAt())
                .createdAt(ot.getCreatedAt())
                // 👇 ADDED PROFILE PHOTO URL HERE
                .profilePhotoUrl(emp != null ? emp.getProfilePhotoUrl() : null)
                .build();
    }
}
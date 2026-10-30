package com.panha.markmekh.service;

import com.panha.markmekh.dto.LeaveRequestDto;
import com.panha.markmekh.dto.LeaveResponseDto;
import com.panha.markmekh.entity.Employee;
import com.panha.markmekh.entity.LeaveRequest;
import com.panha.markmekh.entity.User;
import com.panha.markmekh.exception.BadRequestException;
import com.panha.markmekh.exception.ResourceNotFoundException;
import com.panha.markmekh.repository.EmployeeRepository;
import com.panha.markmekh.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRepo;
    private final EmployeeRepository employeeRepo;
    private final NotificationService notifService;

    public LeaveService(LeaveRequestRepository leaveRepo,
                        EmployeeRepository employeeRepo,
                        NotificationService notifService) {
        this.leaveRepo = leaveRepo;
        this.employeeRepo = employeeRepo;
        this.notifService = notifService;
    }

    // -------------------------------------------------------------------
    // SUBMIT
    // -------------------------------------------------------------------

    @Transactional
    public LeaveResponseDto submit(Long userId, LeaveRequestDto dto) {
        Employee emp = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        if (dto.getEndDate().isBefore(dto.getStartDate()))
            throw new BadRequestException("End date must be after start date");

        LeaveRequest lr = LeaveRequest.builder()
                .employee(emp)
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .attachmentUrl(dto.getAttachmentUrl())
                .status("PENDING")
                .build();
        lr = leaveRepo.save(lr);

        // 🔔 Notify admins + HR
        String empName = emp.getFullNameEn() != null
                ? emp.getFullNameEn()
                : emp.getEmployeeCode();
        notifService.notifyAdmins(
                "LEAVE_REQUEST",
                "New Leave Request",
                empName + " requested " + dto.getLeaveType() + " leave from "
                        + dto.getStartDate() + " to " + dto.getEndDate(),
                lr.getId()
        );

        return toDto(lr);
    }

    // -------------------------------------------------------------------
    // LISTS
    // -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LeaveRequest> myRequests(Long userId) {
        Employee emp = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return leaveRepo.findByEmployee_IdOrderByCreatedAtDesc(emp.getId());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> pending() {
        return leaveRepo.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<LeaveResponseDto> pendingDtos() {
        return leaveRepo.findByStatusOrderByCreatedAtDesc("PENDING")
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveResponseDto> myDtos(Long userId) {
        Employee emp = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return leaveRepo.findByEmployee_IdOrderByCreatedAtDesc(emp.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------
    // REVIEW
    // -------------------------------------------------------------------

    @Transactional
    public LeaveResponseDto review(Long id, boolean approve, String note, User reviewer) {
        LeaveRequest lr = leaveRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        if (!"PENDING".equals(lr.getStatus()))
            throw new BadRequestException("Request already reviewed");

        lr.setStatus(approve ? "APPROVED" : "REJECTED");
        lr.setReviewedBy(reviewer);
        lr.setReviewNote(note);
        lr.setReviewedAt(LocalDateTime.now());
        lr = leaveRepo.save(lr);

        // 🔔 Notify the employee
        Employee emp = lr.getEmployee();
        if (emp != null && emp.getUser() != null) {
            String word = approve ? "approved" : "rejected";
            notifService.notifyUser(
                    emp.getUser(),
                    "LEAVE_" + lr.getStatus(),
                    "Leave request " + word,
                    "Your " + lr.getLeaveType() + " leave from "
                            + lr.getStartDate() + " to " + lr.getEndDate()
                            + " has been " + word + "."
                            + (note != null && !note.isEmpty() ? "\nNote: " + note : ""),
                    lr.getId()
            );
        }

        return toDto(lr);
    }

    // -------------------------------------------------------------------
    // DTO MAPPER
    // -------------------------------------------------------------------

    private LeaveResponseDto toDto(LeaveRequest lr) {
        Employee emp = lr.getEmployee();
        return LeaveResponseDto.builder()
                .id(lr.getId())
                .employeeId(emp != null ? emp.getId() : null)
                .employeeName(emp != null ? emp.getFullNameEn() : null)
                .employeeCode(emp != null ? emp.getEmployeeCode() : null)
                .departmentName(emp != null && emp.getDepartment() != null
                        ? emp.getDepartment().getName() : null)
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .reason(lr.getReason())
                .attachmentUrl(lr.getAttachmentUrl())
                .status(lr.getStatus())
                .reviewNote(lr.getReviewNote())
                .reviewedAt(lr.getReviewedAt())
                .createdAt(lr.getCreatedAt())
                // 👇 ADDED PROFILE PHOTO URL HERE
                .profilePhotoUrl(emp != null ? emp.getProfilePhotoUrl() : null)
                .build();
    }
}
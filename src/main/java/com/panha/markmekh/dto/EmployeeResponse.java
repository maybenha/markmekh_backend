package com.panha.markmekh.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String username;
    private String email;
    private String fullNameKh;
    private String fullNameEn;
    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;

    // ── ADDED IDs ──
    private Long departmentId;
    private Long positionId;
    private Long branchId;

    private String departmentName;
    private String positionTitle;
    private String branchName;
    private String managerName;
    private String employmentType;
    private String employmentStatus;
    private LocalDate startDate;
    private LocalDate contractEndDate;
    private BigDecimal baseSalary;
    private String bankName;
    private String bankAccount;
    private Integer annualLeaveBalance;
    private String profilePhotoUrl;
    private String coverPhotoUrl;
}
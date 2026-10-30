package com.panha.markmekh.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EmployeeRequest {

    @NotBlank
    private String username;

    @NotBlank @Email
    private String email;

    private String password;

    @NotBlank
    private String fullNameKh;

    @NotBlank
    private String fullNameEn;

    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;

    private Long departmentId;
    private Long positionId;
    private Long branchId;
    private Long managerId;

    private String employmentType;

    // 👇 ADDED THIS FIELD 👇
    private String employmentStatus;

    private LocalDate startDate;
    private LocalDate contractEndDate;

    private BigDecimal baseSalary;
    private String bankAccount;
    private String bankName;
    private String profilePhotoUrl;
    private String coverPhotoUrl;
}
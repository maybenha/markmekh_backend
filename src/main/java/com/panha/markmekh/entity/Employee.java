package com.panha.markmekh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_emp_code", columnList = "employee_code", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Column(name = "full_name_kh", nullable = false, length = 120)
    private String fullNameKh;

    @Column(name = "full_name_en", nullable = false, length = 120)
    private String fullNameEn;

    @Column(name = "profile_photo_url", columnDefinition = "TEXT")
    private String profilePhotoUrl;

    @Column(name = "cover_photo_url", columnDefinition = "TEXT")
    private String coverPhotoUrl;

    @Column(length = 10)
    private String gender;

    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 120)
    private String emergencyContactName;

    @Column(length = 20)
    private String emergencyContactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "employment_type", length = 30)
    private String employmentType;

    @Column(name = "employment_status", length = 30)
    private String employmentStatus = "ACTIVE";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "base_salary", precision = 14, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "bank_account", length = 60)
    private String bankAccount;

    @Column(name = "bank_name", length = 60)
    private String bankName;

    @Column(name = "annual_leave_balance")
    private Integer annualLeaveBalance = 18;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (employeeCode == null) {
            employeeCode = "EMP" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
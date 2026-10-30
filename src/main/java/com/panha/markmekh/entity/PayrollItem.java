package com.panha.markmekh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_items",
        indexes = {
                @Index(name = "idx_payroll_period", columnList = "payroll_period_id"),
                @Index(name = "idx_payroll_employee", columnList = "employee_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_period_id", nullable = false)
    private PayrollPeriod payrollPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ---------- Earnings ----------
    @Column(name = "basic_salary", precision = 14, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "paid_days")
    private Integer paidDays = 0;

    @Column(name = "unpaid_days")
    private Integer unpaidDays = 0;

    @Column(name = "overtime_pay", precision = 14, scale = 2)
    private BigDecimal overtimePay = BigDecimal.ZERO;

    @Column(name = "allowances", precision = 14, scale = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(name = "bonus", precision = 14, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "gross_pay", precision = 14, scale = 2)
    private BigDecimal grossPay = BigDecimal.ZERO;

    // ---------- Deductions ----------
    @Column(name = "tax_deduction", precision = 14, scale = 2)
    private BigDecimal taxDeduction = BigDecimal.ZERO;

    @Column(name = "absent_deduction", precision = 14, scale = 2)
    private BigDecimal absentDeduction = BigDecimal.ZERO;

    @Column(name = "other_deductions", precision = 14, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "net_pay", precision = 14, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    // ---------- Meta ----------
    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, FINALIZED, PAID

    @Column(name = "note", length = 500)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
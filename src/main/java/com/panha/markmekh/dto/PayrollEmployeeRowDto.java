package com.panha.markmekh.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollEmployeeRowDto {
    // Employee
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private String positionTitle;
    private String employmentStatus;

    // Payroll readiness
    private boolean hasPayroll;               // true if a PayrollItem exists
    private Long payrollItemId;               // null if no payroll item
    private Integer paidDays;                 // worked days in current month
    private Integer workDaysSoFar;            // Mon-Fri days from month start to today
    private Integer workDaysInMonth;          // total Mon-Fri days in current month
    private Integer unpaidDays;
    private boolean monthComplete;            // false if we're mid-month
    private String readiness;                 // READY | PARTIAL | NOT_STARTED
    private String warningMessage;            // human-readable warning or null

    // Money (only if hasPayroll)
    private BigDecimal basicSalary;
    private BigDecimal grossPay;
    private BigDecimal netPay;
    private String payrollStatus;             // DRAFT / FINALIZED / PAID / etc.
}
package com.panha.markmekh.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollItemDto {
    private Long id;
    private Long payrollPeriodId;
    private Integer year;
    private Integer month;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private String positionTitle;

    // Inputs (editable)
    private BigDecimal basicSalary;
    private Integer monthlyDivisorHours;      // default 208
    private Integer monthlyDivisorDays;       // default 26
    private BigDecimal allowances;
    private BigDecimal bonus;
    private BigDecimal otherEarnings;
    private BigDecimal taxDeduction;
    private BigDecimal otherDeductions;
    private String note;

    // Auto-calculated from attendance/OT/leave
    private Integer paidDays;
    private Integer unpaidDays;
    private Integer lateMinutes;
    private Integer overtimeDayMinutes;
    private Integer overtimeNightMinutes;

    private BigDecimal overtimePay;
    private BigDecimal absentDeduction;
    private BigDecimal grossPay;
    private BigDecimal netPay;
    private String status;
}
package com.panha.markmekh.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PayrollEditDto {

    private BigDecimal basicSalary;

    private Integer monthlyDivisorHours;

    private Integer monthlyDivisorDays;

    private BigDecimal allowances;

    private BigDecimal bonus;

    private BigDecimal otherEarnings;

    private BigDecimal taxDeduction;

    private BigDecimal otherDeductions;

    private String note;
}
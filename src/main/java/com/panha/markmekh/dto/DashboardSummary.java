package com.panha.markmekh.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardSummary {
    private int presentDays;
    private int lateDays;
    private int absentDays;
    private int leaveBalance;
    private int overtimeHours;
    private Integer todayLateMinutes;
    private Integer todayOvertimeMinutes;
    private Integer todayWorkMinutes;
    private String todayCheckInTime;
    private String todayCheckOutTime;
    private String todayStatus;
    private String employeeName;
    private String employeeCode;
}
package com.panha.markmekh.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TodayAttendanceDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private String positionTitle;
    private String checkInTime;
    private String checkOutTime;
    private String status;
    private Integer lateMinutes;
    private Integer workMinutes;
    private Integer overtimeMinutes;

    // 👇 ADD THIS FIELD
    private String profilePhotoUrl;
}
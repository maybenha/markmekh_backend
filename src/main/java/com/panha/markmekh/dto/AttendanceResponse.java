package com.panha.markmekh.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String checkInMethod;
    private String checkOutMethod;
    private String status;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private Integer overtimeMinutes;
    private Integer workMinutes;
    private String note;
}
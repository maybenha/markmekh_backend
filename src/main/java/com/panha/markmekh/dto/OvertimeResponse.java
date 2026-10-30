package com.panha.markmekh.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OvertimeResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private LocalDate overtimeDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalMinutes;
    private String reason;
    private String status;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private String profilePhotoUrl; // 👈 Added field
}
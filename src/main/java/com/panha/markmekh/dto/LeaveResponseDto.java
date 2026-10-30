package com.panha.markmekh.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String attachmentUrl;
    private String status;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private String profilePhotoUrl; // 👈 Added field
}
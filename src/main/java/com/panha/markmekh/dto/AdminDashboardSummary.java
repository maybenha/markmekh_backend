package com.panha.markmekh.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminDashboardSummary {
    private long totalEmployees;
    private long activeEmployees;
    private long presentToday;
    private long lateToday;
    private long absentToday;
    private long pendingLeaves;
    private long pendingOvertimes;
    private long openSessions;
    private String todayLabel;
}
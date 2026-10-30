package com.panha.markmekh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records", indexes = {
        @Index(name = "idx_att_emp_date", columnList = "employee_id,attendance_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "check_in_method", length = 20)
    private String checkInMethod;

    @Column(name = "check_out_method", length = 20)
    private String checkOutMethod;

    @Column(name = "check_in_latitude", precision = 10, scale = 7)
    private java.math.BigDecimal checkInLatitude;

    @Column(name = "check_in_longitude", precision = 10, scale = 7)
    private java.math.BigDecimal checkInLongitude;

    @Column(name = "check_out_latitude", precision = 10, scale = 7)
    private java.math.BigDecimal checkOutLatitude;

    @Column(name = "check_out_longitude", precision = 10, scale = 7)
    private java.math.BigDecimal checkOutLongitude;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PRESENT";

    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    @Column(name = "early_leave_minutes")
    private Integer earlyLeaveMinutes = 0;

    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes = 0;

    @Column(name = "work_minutes")
    private Integer workMinutes = 0;

    @Column(length = 500)
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
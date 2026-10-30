package com.panha.markmekh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_periods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT"; // DRAFT, CALCULATED, APPROVED, FINALIZED, PUBLISHED, PAID

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
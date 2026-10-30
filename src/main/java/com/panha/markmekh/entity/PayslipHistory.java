package com.panha.markmekh.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payslip_history")
public class PayslipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "date", nullable = false)
    private String date;

    @Lob
    @Column(name = "pdf_data", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] pdfData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public byte[] getPdfData() { return pdfData; }
    public void setPdfData(byte[] pdfData) { this.pdfData = pdfData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
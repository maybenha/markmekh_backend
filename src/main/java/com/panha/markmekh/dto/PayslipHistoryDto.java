package com.panha.markmekh.dto;

public class PayslipHistoryDto {
    private Long id;
    private String employeeName;
    private String employeeCode;
    private String invoiceNumber;
    private String date;

    // Constructors, Getters, Setters
    public PayslipHistoryDto(Long id, String employeeName, String employeeCode, String invoiceNumber, String date) {
        this.id = id;
        this.employeeName = employeeName;
        this.employeeCode = employeeCode;
        this.invoiceNumber = invoiceNumber;
        this.date = date;
    }
    public Long getId() { return id; }
    public String getEmployeeName() { return employeeName; }
    public String getEmployeeCode() { return employeeCode; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getDate() { return date; }
}
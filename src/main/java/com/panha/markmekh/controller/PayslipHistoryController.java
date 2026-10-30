package com.panha.markmekh.controller;

import com.panha.markmekh.dto.PayslipHistoryDto;
import com.panha.markmekh.service.PayslipHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payslips")
public class PayslipHistoryController {

    private final PayslipHistoryService service;

    public PayslipHistoryController(PayslipHistoryService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPayslip(
            Authentication auth,
            @RequestParam("employeeName") String employeeName,
            @RequestParam("employeeCode") String employeeCode,
            @RequestParam("invoiceNumber") String invoiceNumber,
            @RequestParam("date") String date,
            @RequestParam("file") MultipartFile file) {
        try {
            service.savePayslip(auth.getName(), employeeName, employeeCode, invoiceNumber, date, file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<PayslipHistoryDto>> getHistory(Authentication auth) {
        return ResponseEntity.ok(service.getHistory(auth.getName()));
    }

    // 👇 CHANGED: Returns JSON with Base64 string instead of raw PDF bytes
    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> downloadPdf(@PathVariable Long id, Authentication auth) {
        byte[] pdfBytes = service.getPdfBytes(id, auth.getName());
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);

        return ResponseEntity.ok(Map.of("data", base64));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayslip(@PathVariable Long id, Authentication auth) {
        service.deleteHistory(id, auth.getName());
        return ResponseEntity.ok().build();
    }
}
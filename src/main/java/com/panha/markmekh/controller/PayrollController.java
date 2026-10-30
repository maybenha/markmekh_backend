package com.panha.markmekh.controller;

import com.panha.markmekh.dto.*;
import com.panha.markmekh.entity.PayrollPeriod;
import com.panha.markmekh.security.SecurityUtils;
import com.panha.markmekh.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.panha.markmekh.dto.PayrollEmployeeRowDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // -------------------------------------------------------------------
    // PERIODS
    // -------------------------------------------------------------------

    @PostMapping("/periods")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollPeriod>> createPeriod(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok("Period created",
                payrollService.createPeriod(year, month)));
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollPeriod>> calculate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Calculated",
                payrollService.calculate(id)));
    }

    @GetMapping("/periods/{id}/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<PayrollItemDto>>> periodItems(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.periodItems(id)));
    }

    @PostMapping("/periods/{id}/finalize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollPeriod>> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Finalized",
                payrollService.finalize(id)));
    }

    @PostMapping("/periods/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> publish(
            @PathVariable Long id,
            @Valid @RequestBody PublishPayslipDto dto) {
        int sent = payrollService.publish(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(
                "Published to " + sent + " employees",
                Map.of("count", sent)));
    }

    @PostMapping("/periods/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollPeriod>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Paid",
                payrollService.markPaid(id)));
    }

    // -------------------------------------------------------------------
    // ADMIN ITEMS
    // -------------------------------------------------------------------

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<PayrollItemDto>>> allItems() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.allItems()));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollItemDto>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getItemById(id)));
    }

    @PatchMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollItemDto>> editItem(
            @PathVariable Long id,
            @Valid @RequestBody PayrollEditDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Updated",
                payrollService.editItem(id, dto)));
    }

    // -------------------------------------------------------------------
    // EMPLOYEE SELF-SERVICE
    // -------------------------------------------------------------------

    @GetMapping("/me/payslips")
    public ResponseEntity<ApiResponse<List<PayrollItemDto>>> myPayslips() {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.myPayslips(SecurityUtils.currentUserId())));
    }

    @GetMapping("/me/payslips/{id}")
    public ResponseEntity<ApiResponse<PayrollItemDto>> myPayslip(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.getPayslip(SecurityUtils.currentUserId(), id)));
    }

    @GetMapping("/employees/rows")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<List<PayrollEmployeeRowDto>>> employeeRows() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.employeePayrollRows()));
    }

    @PostMapping("/employees/{employeeId}/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollItemDto>> generateForEmployee(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok("Generated",
                payrollService.generateForEmployee(employeeId)));
    }
}
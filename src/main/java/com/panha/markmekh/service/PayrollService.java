package com.panha.markmekh.service;

import com.panha.markmekh.dto.*;
import com.panha.markmekh.entity.*;
import com.panha.markmekh.exception.BadRequestException;
import com.panha.markmekh.exception.ResourceNotFoundException;
import com.panha.markmekh.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    // ─── Defaults ───
    private static final int DEFAULT_DIVISOR_HOURS = 208;   // 26 days × 8h
    private static final int DEFAULT_DIVISOR_DAYS = 26;

    // ─── Cambodia ToS tax (KHR, monthly, resident) ───
    private static final long TAX_B1_MAX = 1_500_000L;   // 0%
    private static final long TAX_B2_MAX = 2_000_000L;   // 5%  d=75,000
    private static final long TAX_B3_MAX = 8_500_000L;   // 10% d=175,000
    private static final long TAX_B4_MAX = 12_500_000L;  // 15% d=600,000
    // > 12,500,000 → 20% d=1,225,000

    private static final BigDecimal KHR_PER_USD = new BigDecimal("4100");

    private final PayrollPeriodRepository periodRepo;
    private final PayrollItemRepository itemRepo;
    private final EmployeeRepository employeeRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final OvertimeRequestRepository overtimeRepo;
    private final NotificationService notifService;

    public PayrollService(PayrollPeriodRepository periodRepo,
                          PayrollItemRepository itemRepo,
                          EmployeeRepository employeeRepo,
                          AttendanceRecordRepository attendanceRepo,
                          OvertimeRequestRepository overtimeRepo,
                          NotificationService notifService) {
        this.periodRepo = periodRepo;
        this.itemRepo = itemRepo;
        this.employeeRepo = employeeRepo;
        this.attendanceRepo = attendanceRepo;
        this.overtimeRepo = overtimeRepo;
        this.notifService = notifService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERIODS
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayrollPeriod createPeriod(int year, int month) {
        periodRepo.findByYearAndMonth(year, month).ifPresent(p -> {
            throw new BadRequestException("Payroll period already exists for "
                    + month + "/" + year);
        });
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        PayrollPeriod period = PayrollPeriod.builder()
                .year(year).month(month).startDate(start).endDate(end)
                .status("DRAFT").build();
        return periodRepo.save(period);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CALCULATE WHOLE PERIOD
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayrollPeriod calculate(Long periodId) {
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found"));

        if (!"DRAFT".equals(period.getStatus())
                && !"CALCULATED".equals(period.getStatus())) {
            throw new BadRequestException("Period already finalized");
        }

        List<PayrollItem> existing = itemRepo.findByPayrollPeriod_Id(periodId);
        if (!existing.isEmpty()) itemRepo.deleteAll(existing);

        for (Employee emp : employeeRepo.findAll()) {
            if (!"ACTIVE".equals(emp.getEmploymentStatus())) continue;
            if (emp.getBaseSalary() == null) continue;
            itemRepo.save(calculateForEmployee(emp, period));
        }

        period.setStatus("CALCULATED");
        return periodRepo.save(period);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CORE CALCULATION FOR ONE EMPLOYEE
    // ═══════════════════════════════════════════════════════════════════════

    private PayrollItem calculateForEmployee(Employee emp, PayrollPeriod period) {
        BigDecimal basic = emp.getBaseSalary();

        // ─── No salary → placeholder row ───
        if (basic == null || basic.compareTo(BigDecimal.ZERO) <= 0) {
            return PayrollItem.builder()
                    .payrollPeriod(period)
                    .employee(emp)
                    .basicSalary(BigDecimal.ZERO)
                    .paidDays(0)
                    .unpaidDays(0)
                    .overtimePay(BigDecimal.ZERO)
                    .allowances(BigDecimal.ZERO)
                    .bonus(BigDecimal.ZERO)
                    .grossPay(BigDecimal.ZERO)
                    .taxDeduction(BigDecimal.ZERO)
                    .absentDeduction(BigDecimal.ZERO)
                    .otherDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.ZERO)
                    .status("DRAFT")
                    .note("⚠ No basic salary configured for this employee. "
                            + "Please set base salary before finalizing payroll.")
                    .build();
        }

        int divisorHours = DEFAULT_DIVISOR_HOURS;
        int divisorDays = DEFAULT_DIVISOR_DAYS;

        BigDecimal hourlyRate = basic.divide(
                BigDecimal.valueOf(divisorHours), 4, RoundingMode.HALF_UP);
        BigDecimal dailyRate = basic.divide(
                BigDecimal.valueOf(divisorDays), 4, RoundingMode.HALF_UP);

        // ─── Attendance records ───
        List<AttendanceRecord> records = attendanceRepo
                .findByEmployee_IdAndAttendanceDateBetween(
                        emp.getId(), period.getStartDate(), period.getEndDate());

        int workDaysInMonth = countWorkDays(period.getStartDate(), period.getEndDate());
        int presentDays = 0;
        int lateMinutes = 0;
        long overtimeDayMinutes = 0;
        long overtimeNightMinutes = 0;

        for (AttendanceRecord r : records) {
            if (r.getCheckInTime() != null) presentDays++;
            if (r.getLateMinutes() != null) lateMinutes += r.getLateMinutes();
            if (r.getOvertimeMinutes() != null && r.getOvertimeMinutes() > 0) {
                LocalDateTime co = r.getCheckOutTime();
                if (co != null && co.toLocalTime().isAfter(LocalTime.of(22, 0))) {
                    overtimeNightMinutes += r.getOvertimeMinutes();
                } else {
                    overtimeDayMinutes += r.getOvertimeMinutes();
                }
            }
        }

        int unpaidDays = Math.max(0, workDaysInMonth - presentDays);

        // ─── Overtime pay ───
        BigDecimal otDayHours = BigDecimal.valueOf(overtimeDayMinutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal otNightHours = BigDecimal.valueOf(overtimeNightMinutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        BigDecimal otDayPay = hourlyRate.multiply(otDayHours)
                .multiply(new BigDecimal("1.5"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal otNightPay = hourlyRate.multiply(otNightHours)
                .multiply(new BigDecimal("2.0"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal otPay = otDayPay.add(otNightPay);

        // ─── Absence deduction ───
        BigDecimal absentDeduction = dailyRate
                .multiply(BigDecimal.valueOf(unpaidDays))
                .setScale(2, RoundingMode.HALF_UP);

        // ─── Defaults ───
        BigDecimal allowances = BigDecimal.ZERO;
        BigDecimal bonus = BigDecimal.ZERO;
        BigDecimal otherDeductions = BigDecimal.ZERO;

        // ─── Gross ───
        BigDecimal gross = basic
                .add(otPay)
                .add(allowances)
                .add(bonus)
                .subtract(absentDeduction);
        if (gross.compareTo(BigDecimal.ZERO) < 0) gross = BigDecimal.ZERO;

        // ─── Tax ───
        BigDecimal taxUsd = calculateCambodiaSalaryTax(gross, KHR_PER_USD);

        // ─── Net ───
        BigDecimal net = gross.subtract(taxUsd).subtract(otherDeductions);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;

        return PayrollItem.builder()
                .payrollPeriod(period)
                .employee(emp)
                .basicSalary(basic)
                .paidDays(presentDays)
                .unpaidDays(unpaidDays)
                .overtimePay(otPay)
                .allowances(allowances)
                .bonus(bonus)
                .grossPay(gross)
                .taxDeduction(taxUsd)
                .absentDeduction(absentDeduction)
                .otherDeductions(otherDeductions)
                .netPay(net)
                .status("DRAFT")
                .note("Auto-calculated: " + presentDays + " present, " + unpaidDays
                        + " unpaid, " + (overtimeDayMinutes + overtimeNightMinutes)
                        + " OT minutes")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CAMBODIA SALARY TAX (ToS)
    // ═══════════════════════════════════════════════════════════════════════

    private BigDecimal calculateCambodiaSalaryTax(BigDecimal monthlyUsd,
                                                  BigDecimal khrPerUsd) {
        if (monthlyUsd == null || monthlyUsd.compareTo(BigDecimal.ZERO) <= 0)
            return BigDecimal.ZERO;

        long taxableKhr = monthlyUsd.multiply(khrPerUsd)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        long rateBps;
        long quickDeduct;
        if (taxableKhr <= TAX_B1_MAX) {
            rateBps = 0;
            quickDeduct = 0;
        } else if (taxableKhr <= TAX_B2_MAX) {
            rateBps = 500;
            quickDeduct = 75_000L;
        } else if (taxableKhr <= TAX_B3_MAX) {
            rateBps = 1000;
            quickDeduct = 175_000L;
        } else if (taxableKhr <= TAX_B4_MAX) {
            rateBps = 1500;
            quickDeduct = 600_000L;
        } else {
            rateBps = 2000;
            quickDeduct = 1_225_000L;
        }

        long taxKhr = (taxableKhr * rateBps) / 10_000L - quickDeduct;
        if (taxKhr < 0) taxKhr = 0;

        return BigDecimal.valueOf(taxKhr)
                .divide(khrPerUsd, 2, RoundingMode.HALF_UP);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COUNT MON–FRI WORKDAYS
    // ═══════════════════════════════════════════════════════════════════════

    private int countWorkDays(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            DayOfWeek dow = cur.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            cur = cur.plusDays(1);
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EDIT ITEM (admin can override any value)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayrollItemDto editItem(Long itemId, PayrollEditDto dto) {
        PayrollItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        if ("PAID".equals(item.getStatus())) {
            throw new BadRequestException("Cannot edit a paid payroll item");
        }

        if (dto.getBasicSalary() != null) item.setBasicSalary(dto.getBasicSalary());
        if (dto.getAllowances() != null) item.setAllowances(dto.getAllowances());
        if (dto.getBonus() != null) item.setBonus(dto.getBonus());
        if (dto.getTaxDeduction() != null) item.setTaxDeduction(dto.getTaxDeduction());
        if (dto.getOtherDeductions() != null) item.setOtherDeductions(dto.getOtherDeductions());
        if (dto.getNote() != null) item.setNote(dto.getNote());

        recalculate(item);
        itemRepo.save(item);
        return toDto(item);
    }

    private void recalculate(PayrollItem item) {
        BigDecimal gross = item.getBasicSalary()
                .add(item.getOvertimePay())
                .add(item.getAllowances())
                .add(item.getBonus())
                .subtract(item.getAbsentDeduction());
        if (gross.compareTo(BigDecimal.ZERO) < 0) gross = BigDecimal.ZERO;

        BigDecimal net = gross
                .subtract(item.getTaxDeduction())
                .subtract(item.getOtherDeductions());
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;

        item.setGrossPay(gross);
        item.setNetPay(net);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS TRANSITIONS
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayrollPeriod finalize(Long periodId) {
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found"));
        if (!"CALCULATED".equals(period.getStatus())
                && !"APPROVED".equals(period.getStatus())) {
            throw new BadRequestException("Calculate first");
        }
        period.setStatus("FINALIZED");
        period.setFinalizedAt(LocalDateTime.now());
        periodRepo.save(period);

        List<PayrollItem> items = itemRepo.findByPayrollPeriod_Id(periodId);
        items.forEach(i -> i.setStatus("FINALIZED"));
        itemRepo.saveAll(items);
        return period;
    }

    @Transactional
    public PayrollPeriod markPaid(Long periodId) {
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found"));
        period.setStatus("PAID");
        period.setPaidAt(LocalDateTime.now());
        periodRepo.save(period);

        List<PayrollItem> items = itemRepo.findByPayrollPeriod_Id(periodId);
        items.forEach(i -> i.setStatus("PAID"));
        itemRepo.saveAll(items);
        return period;
    }

    @Transactional
    public int publish(Long periodId, PublishPayslipDto dto) {
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found"));
        if (!"FINALIZED".equals(period.getStatus())
                && !"PAID".equals(period.getStatus())) {
            throw new BadRequestException("Finalize payroll before publishing");
        }

        List<PayrollItem> items = itemRepo.findByPayrollPeriod_Id(periodId);
        List<PayrollItem> targets;

        switch (dto.getMode()) {
            case "ALL" -> targets = items;
            case "SINGLE" -> {
                if (dto.getEmployeeId() == null)
                    throw new BadRequestException("employeeId is required");
                targets = items.stream()
                        .filter(i -> i.getEmployee().getId().equals(dto.getEmployeeId()))
                        .collect(Collectors.toList());
            }
            case "FILTER" -> {
                String kw = dto.getKeyword() == null ? "" : dto.getKeyword().toLowerCase();
                targets = items.stream()
                        .filter(i -> {
                            String name = i.getEmployee().getFullNameEn() == null
                                    ? "" : i.getEmployee().getFullNameEn().toLowerCase();
                            String code = i.getEmployee().getEmployeeCode() == null
                                    ? "" : i.getEmployee().getEmployeeCode().toLowerCase();
                            return name.contains(kw) || code.contains(kw);
                        })
                        .collect(Collectors.toList());
            }
            default -> throw new BadRequestException("Invalid mode");
        }

        for (PayrollItem item : targets) {
            Employee emp = item.getEmployee();
            if (emp.getUser() != null) {
                notifService.notifyUser(emp.getUser(),
                        "PAYROLL_PUBLISHED",
                        "Payslip available",
                        "Your payslip for " + period.getMonth() + "/" + period.getYear()
                                + " is ready. Net: $" + item.getNetPay(),
                        item.getId());
            }
        }

        period.setStatus("PUBLISHED");
        period.setPublishedAt(LocalDateTime.now());
        periodRepo.save(period);
        return targets.size();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LISTERS
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PayrollItemDto> periodItems(Long periodId) {
        return itemRepo.findByPayrollPeriod_Id(periodId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayrollItemDto> allItems() {
        return itemRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayrollItemDto> myPayslips(Long userId) {
        Employee emp = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return itemRepo.findByEmployee_IdOrderByCreatedAtDesc(emp.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollItemDto getPayslip(Long userId, Long itemId) {
        PayrollItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));
        Employee emp = employeeRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        if (!item.getEmployee().getId().equals(emp.getId()))
            throw new BadRequestException("Access denied");
        return toDto(item);
    }

    @Transactional(readOnly = true)
    public PayrollItemDto getItemById(Long itemId) {
        return toDto(itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found")));
    }

    private PayrollItemDto toDto(PayrollItem i) {
        Employee emp = i.getEmployee();
        return PayrollItemDto.builder()
                .id(i.getId())
                .payrollPeriodId(i.getPayrollPeriod() != null ? i.getPayrollPeriod().getId() : null)
                .year(i.getPayrollPeriod() != null ? i.getPayrollPeriod().getYear() : null)
                .month(i.getPayrollPeriod() != null ? i.getPayrollPeriod().getMonth() : null)
                .employeeId(emp != null ? emp.getId() : null)
                .employeeName(emp != null ? emp.getFullNameEn() : null)
                .employeeCode(emp != null ? emp.getEmployeeCode() : null)
                .departmentName(emp != null && emp.getDepartment() != null
                        ? emp.getDepartment().getName() : null)
                .positionTitle(emp != null && emp.getPosition() != null
                        ? emp.getPosition().getTitle() : null)
                .basicSalary(i.getBasicSalary())
                .monthlyDivisorHours(DEFAULT_DIVISOR_HOURS)
                .monthlyDivisorDays(DEFAULT_DIVISOR_DAYS)
                .paidDays(i.getPaidDays())
                .unpaidDays(i.getUnpaidDays())
                .overtimePay(i.getOvertimePay())
                .allowances(i.getAllowances())
                .bonus(i.getBonus())
                .otherEarnings(BigDecimal.ZERO)
                .grossPay(i.getGrossPay())
                .taxDeduction(i.getTaxDeduction())
                .absentDeduction(i.getAbsentDeduction())
                .otherDeductions(i.getOtherDeductions())
                .netPay(i.getNetPay())
                .status(i.getStatus())
                .note(i.getNote())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EMPLOYEE PAYROLL ROWS (Admin list page)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<PayrollEmployeeRowDto> employeePayrollRows() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        int totalWorkDaysInMonth = countWorkDays(monthStart, monthEnd);
        int workedDaysSoFar = countWorkDays(monthStart,
                today.isBefore(monthEnd) ? today : monthEnd);
        boolean monthComplete = today.isAfter(monthEnd) || today.equals(monthEnd);

        // Load any existing payroll items for this month
        Map<Long, PayrollItem> itemsByEmp = new HashMap<>();
        periodRepo.findByYearAndMonth(year, month).ifPresent(period -> {
            for (PayrollItem it : itemRepo.findByPayrollPeriod_Id(period.getId())) {
                if (it.getEmployee() != null) {
                    itemsByEmp.put(it.getEmployee().getId(), it);
                }
            }
        });

        List<PayrollEmployeeRowDto> result = new ArrayList<>();

        for (Employee emp : employeeRepo.findAll()) {
            if (!"ACTIVE".equals(emp.getEmploymentStatus())) continue;

            PayrollItem item = itemsByEmp.get(emp.getId());
            boolean hasPayroll = item != null;

            // Worked days so far this month
            List<AttendanceRecord> records = attendanceRepo
                    .findByEmployee_IdAndAttendanceDateBetween(
                            emp.getId(), monthStart,
                            today.isBefore(monthEnd) ? today : monthEnd);

            int paidDays = 0;
            for (AttendanceRecord r : records) {
                if (r.getCheckInTime() != null) paidDays++;
            }

            int unpaidDays = Math.max(0, workedDaysSoFar - paidDays);

            // ─── Readiness + warning ───
            String readiness;
            String warning;
            if (emp.getBaseSalary() == null
                    || emp.getBaseSalary().compareTo(BigDecimal.ZERO) <= 0) {
                readiness = "NOT_STARTED";
                warning = "⚠ No basic salary set for this employee. "
                        + "Configure it in their profile before generating a payslip.";
            } else if (hasPayroll) {
                readiness = "READY";
                warning = null;
            } else if (paidDays == 0) {
                readiness = "NOT_STARTED";
                warning = "This employee has no attendance records yet this month.";
            } else if (!monthComplete) {
                readiness = "PARTIAL";
                warning = "This month is not finished. " + paidDays
                        + " of " + workedDaysSoFar
                        + " workdays worked so far. Generating a payslip now may be inaccurate.";
            } else {
                readiness = "READY";
                warning = null;
            }

            result.add(PayrollEmployeeRowDto.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(emp.getFullNameEn())
                    .departmentName(emp.getDepartment() != null
                            ? emp.getDepartment().getName() : null)
                    .positionTitle(emp.getPosition() != null
                            ? emp.getPosition().getTitle() : null)
                    .employmentStatus(emp.getEmploymentStatus())
                    .hasPayroll(hasPayroll)
                    .payrollItemId(item != null ? item.getId() : null)
                    .paidDays(paidDays)
                    .workDaysSoFar(workedDaysSoFar)
                    .workDaysInMonth(totalWorkDaysInMonth)
                    .unpaidDays(unpaidDays)
                    .monthComplete(monthComplete)
                    .readiness(readiness)
                    .warningMessage(warning)
                    .basicSalary(emp.getBaseSalary())
                    .grossPay(item != null ? item.getGrossPay() : null)
                    .netPay(item != null ? item.getNetPay() : null)
                    .payrollStatus(item != null ? item.getStatus() : null)
                    .build());
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GENERATE FOR ONE EMPLOYEE (from Payroll page action)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayrollItemDto generateForEmployee(Long employeeId) {
        Employee emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Guard: refuse to generate payroll for an employee with no salary
        if (emp.getBaseSalary() == null
                || emp.getBaseSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Cannot generate payslip for " + emp.getFullNameEn()
                            + ": no basic salary is configured for this employee. "
                            + "Please set the base salary in their employee profile first.");
        }

        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        PayrollPeriod period = periodRepo.findByYearAndMonth(year, month)
                .orElseGet(() -> {
                    LocalDate start = LocalDate.of(year, month, 1);
                    LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
                    return periodRepo.save(PayrollPeriod.builder()
                            .year(year).month(month)
                            .startDate(start).endDate(end)
                            .status("DRAFT")
                            .build());
                });

        // Regenerate: delete any existing item for this employee+period
        itemRepo.findByPayrollPeriod_Id(period.getId()).stream()
                .filter(i -> i.getEmployee().getId().equals(employeeId))
                .forEach(i -> itemRepo.deleteById(i.getId()));

        PayrollItem item = calculateForEmployee(emp, period);
        itemRepo.save(item); // 👈 Saves to database!
        return toDto(item);
    }
}
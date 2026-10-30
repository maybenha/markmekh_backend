package com.panha.markmekh.config;

import com.panha.markmekh.entity.*;
import com.panha.markmekh.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      EmployeeRepository employeeRepository,
                      DepartmentRepository departmentRepository,
                      PositionRepository positionRepository,
                      BranchRepository branchRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // Seed super admin
        User admin = User.builder()
                .username("superadmin")
                .email("admin@markmekh.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        userRepository.save(admin);

        // HR manager
        User hr = User.builder()
                .username("hrmanager")
                .email("hr@markmekh.com")
                .password(passwordEncoder.encode("hr123456"))
                .role(Role.HR_MANAGER)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        userRepository.save(hr);

        // Employee demo
        User empUser = User.builder()
                .username("employee1")
                .email("emp1@markmekh.com")
                .password(passwordEncoder.encode("emp123456"))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        userRepository.save(empUser);

        // Department
        Department dept = Department.builder()
                .name("Information Technology")
                .description("IT Department")
                .build();
        departmentRepository.save(dept);

        // Position
        Position pos = Position.builder()
                .title("Software Engineer")
                .description("Backend/Frontend developer")
                .build();
        positionRepository.save(pos);

        // Branch
        Branch branch = Branch.builder()
                .name("Head Office - Phnom Penh")
                .address("Phnom Penh, Cambodia")
                .latitude(new BigDecimal("11.5564"))
                .longitude(new BigDecimal("104.9282"))
                .radiusMeters(200)
                .build();
        branchRepository.save(branch);

        // Employee profile linked to employee1
        Employee employee = Employee.builder()
                .user(empUser)
                .employeeCode("EMP0001")
                .fullNameKh("សុខ ដារា")
                .fullNameEn("Sok Dara")
                .gender("MALE")
                .dateOfBirth(LocalDate.of(1995, 5, 12))
                .phoneNumber("012345678")
                .address("Phnom Penh")
                .department(dept)
                .position(pos)
                .branch(branch)
                .employmentType("FULL_TIME")
                .employmentStatus("ACTIVE")
                .startDate(LocalDate.now().minusMonths(6))
                .baseSalary(new BigDecimal("600.00"))
                .annualLeaveBalance(18)
                .build();
        employeeRepository.save(employee);

        // HR Employee profile
        Employee hrEmployee = Employee.builder()
                .user(hr)
                .employeeCode("EMP0002")
                .fullNameKh("លី ម៉ារី")
                .fullNameEn("Ly Mary")
                .gender("FEMALE")
                .phoneNumber("098765432")
                .department(dept)
                .position(pos)
                .branch(branch)
                .employmentType("FULL_TIME")
                .employmentStatus("ACTIVE")
                .startDate(LocalDate.now().minusYears(1))
                .baseSalary(new BigDecimal("900.00"))
                .annualLeaveBalance(18)
                .build();
        employeeRepository.save(hrEmployee);

        System.out.println("========== SEED DATA CREATED ==========");
        System.out.println("superadmin / admin123");
        System.out.println("hrmanager / hr123456");
        System.out.println("employee1 / emp123456");
        System.out.println("=======================================");
    }
}
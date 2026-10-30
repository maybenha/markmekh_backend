package com.panha.markmekh.service;

import com.panha.markmekh.dto.EmployeeRequest;
import com.panha.markmekh.dto.EmployeeResponse;
import com.panha.markmekh.entity.*;
import com.panha.markmekh.exception.BadRequestException;
import com.panha.markmekh.exception.ResourceNotFoundException;
import com.panha.markmekh.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           PositionRepository positionRepository,
                           BranchRepository branchRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest req) {
        // ⚡ Validate password manually since DTO allows it to be optional
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Password is required when creating an employee");
        }
        if (req.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        if (userRepository.existsByUsername(req.getUsername()))
            throw new BadRequestException("Username already exists");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new BadRequestException("Email already exists");

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .accountNonLocked(true)
                .build();
        userRepository.save(user);

        Employee emp = Employee.builder()
                .user(user)
                .fullNameKh(req.getFullNameKh())
                .fullNameEn(req.getFullNameEn())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .phoneNumber(req.getPhoneNumber())
                .address(req.getAddress())
                .emergencyContactName(req.getEmergencyContactName())
                .emergencyContactPhone(req.getEmergencyContactPhone())
                .employmentType(req.getEmploymentType())
                .employmentStatus(req.getEmploymentStatus() != null ? req.getEmploymentStatus() : "ACTIVE")
                .startDate(req.getStartDate())
                .contractEndDate(req.getContractEndDate())
                .baseSalary(req.getBaseSalary())
                .bankAccount(req.getBankAccount())
                .bankName(req.getBankName())
                .employeeCode(generateEmployeeCode())
                .build();

        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            emp.setDepartment(dept);
        }
        if (req.getPositionId() != null) {
            Position pos = positionRepository.findById(req.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
            emp.setPosition(pos);
        }
        if (req.getBranchId() != null) {
            Branch br = branchRepository.findById(req.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            emp.setBranch(br);
        }
        if (req.getManagerId() != null) {
            Employee mgr = employeeRepository.findById(req.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            emp.setManager(mgr);
        }

        employeeRepository.save(emp);
        return toResponse(emp);
    }

    private String generateEmployeeCode() {
        long count = employeeRepository.count() + 1;
        return String.format("EMP%04d", count);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Employee> result = (keyword == null || keyword.isBlank())
                ? employeeRepository.findAll(pageable)
                : employeeRepository.search(keyword, pageable);
        return result.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return toResponse(emp);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getByUserId(Long userId) {
        Employee emp = employeeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return toResponse(emp);
    }

    // ═════════════════════════════════════════════════════════════════════
    // ADMIN / HR UPDATE
    // 🔒 PARTIAL UPDATE: any field that is null in the request is left
    //    unchanged. Saving the form can never wipe salary, bank details,
    //    dates or anything else it did not send.
    // ═════════════════════════════════════════════════════════════════════
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest req) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Required (validated @NotBlank)
        emp.setFullNameKh(req.getFullNameKh());
        emp.setFullNameEn(req.getFullNameEn());

        // Optional — only overwritten when sent
        if (req.getGender() != null) emp.setGender(req.getGender());
        if (req.getDateOfBirth() != null) emp.setDateOfBirth(req.getDateOfBirth());
        if (req.getPhoneNumber() != null) emp.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddress() != null) emp.setAddress(req.getAddress());
        if (req.getEmergencyContactName() != null) emp.setEmergencyContactName(req.getEmergencyContactName());
        if (req.getEmergencyContactPhone() != null) emp.setEmergencyContactPhone(req.getEmergencyContactPhone());
        if (req.getEmploymentType() != null) emp.setEmploymentType(req.getEmploymentType());
        if (req.getEmploymentStatus() != null) emp.setEmploymentStatus(req.getEmploymentStatus());
        if (req.getStartDate() != null) emp.setStartDate(req.getStartDate());
        if (req.getContractEndDate() != null) emp.setContractEndDate(req.getContractEndDate());

        // Salary & bank — never overwritten with null
        if (req.getBaseSalary() != null) emp.setBaseSalary(req.getBaseSalary());
        if (req.getBankAccount() != null) emp.setBankAccount(req.getBankAccount());
        if (req.getBankName() != null) emp.setBankName(req.getBankName());

        if (req.getDepartmentId() != null) {
            emp.setDepartment(departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        }
        if (req.getPositionId() != null) {
            emp.setPosition(positionRepository.findById(req.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Position not found")));
        }
        if (req.getBranchId() != null) {
            emp.setBranch(branchRepository.findById(req.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found")));
        }
        if (req.getManagerId() != null) {
            emp.setManager(employeeRepository.findById(req.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found")));
        }

        employeeRepository.save(emp);
        return toResponse(emp);
    }

    // ═════════════════════════════════════════════════════════════════════
    // 🆕 SELF-SERVICE UPDATE (employee editing their own profile)
    // Only whitelisted fields can change. baseSalary, department,
    // position, branch, manager, status, dates etc. are NEVER touched,
    // so an employee cannot raise their own salary or wipe HR data.
    // ═════════════════════════════════════════════════════════════════════
    @Transactional
    public EmployeeResponse updateMyProfile(Long userId, EmployeeRequest req) {
        Employee emp = employeeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        // Bank details
        if (req.getBankName() != null) {
            emp.setBankName(req.getBankName().trim());
        }
        if (req.getBankAccount() != null) {
            emp.setBankAccount(req.getBankAccount().trim());
        }

        // Optional contact info — only when explicitly provided
        if (req.getPhoneNumber() != null) {
            emp.setPhoneNumber(req.getPhoneNumber());
        }
        if (req.getAddress() != null) {
            emp.setAddress(req.getAddress());
        }
        if (req.getEmergencyContactName() != null) {
            emp.setEmergencyContactName(req.getEmergencyContactName());
        }
        if (req.getEmergencyContactPhone() != null) {
            emp.setEmergencyContactPhone(req.getEmergencyContactPhone());
        }

        employeeRepository.save(emp);
        return toResponse(emp);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        emp.setEmploymentStatus(status);
        employeeRepository.save(emp);
    }

    public EmployeeResponse toResponse(Employee emp) {
        return EmployeeResponse.builder()
                .id(emp.getId())
                .employeeCode(emp.getEmployeeCode())
                .username(emp.getUser() != null ? emp.getUser().getUsername() : null)
                .email(emp.getUser() != null ? emp.getUser().getEmail() : null)
                .fullNameKh(emp.getFullNameKh())
                .fullNameEn(emp.getFullNameEn())
                .gender(emp.getGender())
                .dateOfBirth(emp.getDateOfBirth())
                .phoneNumber(emp.getPhoneNumber())
                .address(emp.getAddress())
                .profilePhotoUrl(emp.getProfilePhotoUrl())
                .coverPhotoUrl(emp.getCoverPhotoUrl())
                .emergencyContactName(emp.getEmergencyContactName())
                .emergencyContactPhone(emp.getEmergencyContactPhone())

                .departmentId(emp.getDepartment() != null ? emp.getDepartment().getId() : null)
                .positionId(emp.getPosition() != null ? emp.getPosition().getId() : null)
                .branchId(emp.getBranch() != null ? emp.getBranch().getId() : null)

                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : null)
                .positionTitle(emp.getPosition() != null ? emp.getPosition().getTitle() : null)
                .branchName(emp.getBranch() != null ? emp.getBranch().getName() : null)
                .managerName(emp.getManager() != null ? emp.getManager().getFullNameEn() : null)
                .employmentType(emp.getEmploymentType())
                .employmentStatus(emp.getEmploymentStatus())
                .startDate(emp.getStartDate())
                .contractEndDate(emp.getContractEndDate())
                .bankName(emp.getBankName())
                .bankAccount(emp.getBankAccount())
                .annualLeaveBalance(emp.getAnnualLeaveBalance())
                .baseSalary(emp.getBaseSalary())
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getMySalary(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Incorrect password");
        }
        Employee emp = employeeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return toResponse(emp); // full response: baseSalary + bankName + bankAccount
    }

    @Transactional
    public EmployeeResponse updateProfilePhoto(Long userId, String dataUrl) {
        Employee emp = employeeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        emp.setProfilePhotoUrl(dataUrl);
        employeeRepository.save(emp);
        return toResponse(emp);
    }

    @Transactional
    public EmployeeResponse updateCoverPhoto(Long userId, String dataUrl) {
        Employee emp = employeeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        emp.setCoverPhotoUrl(dataUrl);
        employeeRepository.save(emp);
        return toResponse(emp);
    }
}
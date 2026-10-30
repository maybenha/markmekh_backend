package com.panha.markmekh.controller;

import com.panha.markmekh.dto.ApiResponse;
import com.panha.markmekh.repository.DepartmentRepository;
import com.panha.markmekh.repository.PositionRepository;
import com.panha.markmekh.repository.BranchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {

    private final DepartmentRepository departmentRepo;
    private final PositionRepository positionRepo;
    private final BranchRepository branchRepo;

    public MasterDataController(DepartmentRepository departmentRepo,
                                PositionRepository positionRepo,
                                BranchRepository branchRepo) {
        this.departmentRepo = departmentRepo;
        this.positionRepo = positionRepo;
        this.branchRepo = branchRepo;
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<?>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.ok("Success", departmentRepo.findAll()));
    }

    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<?>> getPositions() {
        return ResponseEntity.ok(ApiResponse.ok("Success", positionRepo.findAll()));
    }

    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<?>> getBranches() {
        return ResponseEntity.ok(ApiResponse.ok("Success", branchRepo.findAll()));
    }
}
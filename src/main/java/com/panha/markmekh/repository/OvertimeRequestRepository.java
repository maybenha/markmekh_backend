package com.panha.markmekh.repository;

import com.panha.markmekh.entity.OvertimeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, Long> {
    List<OvertimeRequest> findByEmployee_IdOrderByCreatedAtDesc(Long employeeId);
    List<OvertimeRequest> findByStatusOrderByCreatedAtDesc(String status);
}
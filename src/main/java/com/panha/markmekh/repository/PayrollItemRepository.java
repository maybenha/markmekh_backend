package com.panha.markmekh.repository;

import com.panha.markmekh.entity.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {
    List<PayrollItem> findByPayrollPeriod_Id(Long periodId);
    List<PayrollItem> findByEmployee_IdOrderByCreatedAtDesc(Long employeeId);
}
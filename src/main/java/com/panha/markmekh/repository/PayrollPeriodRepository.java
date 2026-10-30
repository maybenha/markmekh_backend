package com.panha.markmekh.repository;

import com.panha.markmekh.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {
    Optional<PayrollPeriod> findByYearAndMonth(Integer year, Integer month);
}
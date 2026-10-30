package com.panha.markmekh.repository;

import com.panha.markmekh.entity.PayslipHistory;
import com.panha.markmekh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipHistoryRepository extends JpaRepository<PayslipHistory, Long> {

    List<PayslipHistory> findByUserOrderByCreatedAtDesc(User user);

    // 👇 NEW: Fetch Payslip AND its User in one query
    @Query("SELECT p FROM PayslipHistory p JOIN FETCH p.user WHERE p.id = :id")
    Optional<PayslipHistory> findByIdWithUser(@Param("id") Long id);
}
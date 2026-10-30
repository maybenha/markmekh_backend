package com.panha.markmekh.repository;

import com.panha.markmekh.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByUser_Id(Long userId);

    boolean existsByEmployeeCode(String employeeCode);

    @Query("""
        SELECT e FROM Employee e
        WHERE LOWER(e.fullNameEn) LIKE LOWER(CONCAT('%', :kw, '%'))
           OR LOWER(e.fullNameKh) LIKE LOWER(CONCAT('%', :kw, '%'))
           OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :kw, '%'))
    """)
    Page<Employee> search(@Param("kw") String keyword, Pageable pageable);
}
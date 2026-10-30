package com.panha.markmekh.repository;

import com.panha.markmekh.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByEmployee_IdAndAttendanceDate(Long employeeId, LocalDate date);
    List<AttendanceRecord> findByEmployee_IdAndAttendanceDateBetween(Long employeeId, LocalDate from, LocalDate to);
    List<AttendanceRecord> findByAttendanceDate(LocalDate date);
    List<AttendanceRecord> findAllByEmployee_IdAndAttendanceDate(Long employeeId, LocalDate date);
}
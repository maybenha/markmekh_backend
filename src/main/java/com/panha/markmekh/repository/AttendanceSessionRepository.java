package com.panha.markmekh.repository;

import com.panha.markmekh.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findBySessionToken(String token);
    List<AttendanceSession> findByActiveTrueAndRevokedFalseAndExpirationTimeAfter(LocalDateTime now);
}
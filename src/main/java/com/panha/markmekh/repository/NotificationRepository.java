package com.panha.markmekh.repository;

import com.panha.markmekh.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndReadFalse(Long userId);
}
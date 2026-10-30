package com.panha.markmekh.repository;

import com.panha.markmekh.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    boolean existsByTitle(String title);
}
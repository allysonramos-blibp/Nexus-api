package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserId(Long userId);
    List<Review> findByUserIdAndDataAgendadaAndConcluidaFalse(Long userId, LocalDate dataAgendada);
}

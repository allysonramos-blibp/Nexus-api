package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findByUserId(Long userId);

    /** Usado para checar posse de uma imagem antes de servi-la (ver WorkoutService.getImageStream). */
    boolean existsByUserIdAndImagemUrlEndingWith(Long userId, String suffix);
}
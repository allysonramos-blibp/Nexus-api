package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByUserIdIsNull();
    List<Exercise> findByUserId(Long userId);
}

package com.nexus.nexus_api.repository;



import com.nexus.nexus_api.model.WorkoutGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutGoalRepository extends JpaRepository<WorkoutGoal, Long> {
    Optional<WorkoutGoal> findByUserId(Long userId);
}

package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM StudyPlan sp WHERE sp.id = :id")
    void deleteByIdCustom(@org.springframework.data.repository.query.Param("id") Long id);
}

package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByStudyPlanId(Long studyPlanId);
    long countByStudyPlanId(Long studyPlanId);
    List<Subject> findByIdInAndStudyPlanUserId(List<Long> ids, Long userId);
}

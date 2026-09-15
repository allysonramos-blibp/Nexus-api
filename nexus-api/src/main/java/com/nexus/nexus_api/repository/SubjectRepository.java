package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByStudyPlanId(Long studyPlanId);
    long countByStudyPlanId(Long studyPlanId);
    List<Subject> findByIdInAndStudyPlanUserId(List<Long> ids, Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Subject s WHERE s.id IN (:ids)")
    void deleteByIdIn(@org.springframework.data.repository.query.Param("ids") List<Long> ids);
}

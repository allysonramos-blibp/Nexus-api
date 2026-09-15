package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectId(Long subjectId);
    long countBySubjectStudyPlanId(Long studyPlanId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Topic t WHERE t.id IN (:ids)")
    void deleteByIdIn(@org.springframework.data.repository.query.Param("ids") List<Long> ids);
}

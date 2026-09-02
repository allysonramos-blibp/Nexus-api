package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectId(Long subjectId);
    long countBySubjectStudyPlanId(Long studyPlanId);
}

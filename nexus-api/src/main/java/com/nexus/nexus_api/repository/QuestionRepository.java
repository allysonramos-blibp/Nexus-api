package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTopicId(Long topicId);

    long countByTopicSubjectStudyPlanId(Long studyPlanId);

    /** Sorteio de questões dentre as matérias selecionadas, usado na criação de simulados. */
    @Query(value = "SELECT q.* FROM questions q " +
            "JOIN topics t ON q.topic_id = t.id " +
            "WHERE t.subject_id IN (:subjectIds) " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomBySubjectIds(@Param("subjectIds") List<Long> subjectIds, @Param("limit") int limit);
}

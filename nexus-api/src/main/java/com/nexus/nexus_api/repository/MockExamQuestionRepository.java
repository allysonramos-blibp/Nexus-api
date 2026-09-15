package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.MockExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MockExamQuestionRepository extends JpaRepository<MockExamQuestion, Long> {
    List<MockExamQuestion> findByMockExamIdOrderByOrdemAsc(Long mockExamId);

    void deleteByMockExamId(Long mockExamId);

    @Modifying
    @Query("DELETE FROM MockExamQuestion meq WHERE meq.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);

    @Modifying
    @Query("DELETE FROM MockExamQuestion meq WHERE meq.question.id IN (:questionIds)")
    void deleteByQuestionIdIn(@Param("questionIds") List<Long> questionIds);
}

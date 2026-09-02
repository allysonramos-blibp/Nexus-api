package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.dto.SubjectPerformanceDto;
import com.nexus.nexus_api.dto.TopicPerformanceDto;
import com.nexus.nexus_api.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByUserId(Long userId);

    List<Answer> findByMockExamId(Long mockExamId);

    long countByUserId(Long userId);

    long countByUserIdAndCorretaTrue(Long userId);

    long countByUserIdAndRespondidoEmBetween(Long userId, LocalDateTime inicio, LocalDateTime fim);

    long countByUserIdAndCorretaTrueAndRespondidoEmBetween(Long userId, LocalDateTime inicio, LocalDateTime fim);

    /** Quantas vezes o usuário já respondeu a esta questão (para numerar a tentativa atual). */
    long countByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("SELECT new com.nexus.nexus_api.dto.SubjectPerformanceDto(" +
            "s.id, s.nome, COUNT(a), SUM(CASE WHEN a.correta = true THEN 1L ELSE 0L END)) " +
            "FROM Answer a " +
            "JOIN a.question q " +
            "JOIN q.topic t " +
            "JOIN t.subject s " +
            "WHERE a.user.id = :userId " +
            "GROUP BY s.id, s.nome")
    List<SubjectPerformanceDto> findPerformanceBySubject(@Param("userId") Long userId);

    @Query("SELECT new com.nexus.nexus_api.dto.TopicPerformanceDto(" +
            "t.id, t.nome, COUNT(a), SUM(CASE WHEN a.correta = true THEN 1L ELSE 0L END)) " +
            "FROM Answer a " +
            "JOIN a.question q " +
            "JOIN q.topic t " +
            "WHERE a.user.id = :userId " +
            "GROUP BY t.id, t.nome")
    List<TopicPerformanceDto> findPerformanceByTopic(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT a.question.id) FROM Answer a " +
            "WHERE a.user.id = :userId AND a.correta = true AND a.question.topic.subject.studyPlan.id = :planId")
    long countDistinctCorrectQuestionsByPlan(@Param("userId") Long userId, @Param("planId") Long planId);
}

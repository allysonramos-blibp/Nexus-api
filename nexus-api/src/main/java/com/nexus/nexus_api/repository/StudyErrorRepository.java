package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.StudyError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyErrorRepository extends JpaRepository<StudyError, Long> {
    List<StudyError> findByUserId(Long userId);
    Optional<StudyError> findByUserIdAndQuestionId(Long userId, Long questionId);
    List<StudyError> findByQuestionId(Long questionId);
    List<StudyError> findByQuestionIdIn(List<Long> questionIds);

    @Modifying
    @Query("DELETE FROM StudyError se WHERE se.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") Long questionId);

    @Modifying
    @Query("DELETE FROM StudyError se WHERE se.question.id IN (:questionIds)")
    void deleteByQuestionIdIn(@Param("questionIds") List<Long> questionIds);

    List<StudyError> findByUserIdAndResolvidoFalseAndProximaRevisaoLessThanEqual(Long userId, LocalDate data);
    long countByUserIdAndResolvidoFalseAndProximaRevisaoLessThanEqual(Long userId, LocalDate data);
}

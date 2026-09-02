package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.StudyError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyErrorRepository extends JpaRepository<StudyError, Long> {
    List<StudyError> findByUserId(Long userId);

    Optional<StudyError> findByUserIdAndQuestionId(Long userId, Long questionId);

    List<StudyError> findByUserIdAndResolvidoFalseAndProximaRevisaoLessThanEqual(Long userId, LocalDate data);

    long countByUserIdAndResolvidoFalseAndProximaRevisaoLessThanEqual(Long userId, LocalDate data);
}

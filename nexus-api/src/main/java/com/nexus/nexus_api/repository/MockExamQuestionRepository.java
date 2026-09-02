package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.MockExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockExamQuestionRepository extends JpaRepository<MockExamQuestion, Long> {
    List<MockExamQuestion> findByMockExamIdOrderByOrdemAsc(Long mockExamId);
    void deleteByMockExamId(Long mockExamId);
}

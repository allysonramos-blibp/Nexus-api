package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockExamRepository extends JpaRepository<MockExam, Long> {
    List<MockExam> findByUserId(Long userId);
}

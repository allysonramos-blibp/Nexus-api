package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MockExamRepository extends JpaRepository<MockExam, Long> {
    List<MockExam> findByUserId(Long userId);

    List<MockExam> findByStudyPlanId(Long studyPlanId);

    @Modifying
    @Query(value = "DELETE FROM mock_exam_subjects WHERE subject_id = :subjectId", nativeQuery = true)
    void deleteMockExamSubjectsBySubjectId(@Param("subjectId") Long subjectId);

    @Modifying
    @Query(value = "DELETE FROM mock_exam_subjects WHERE subject_id IN (:subjectIds)", nativeQuery = true)
    void deleteMockExamSubjectsBySubjectIdIn(@Param("subjectIds") List<Long> subjectIds);

    @Modifying
    @Query(value = "DELETE FROM mock_exam_subjects WHERE mock_exam_id IN (:mockExamIds)", nativeQuery = true)
    void deleteMockExamSubjectsByMockExamIdIn(@Param("mockExamIds") List<Long> mockExamIds);

    @Modifying
    @Query("DELETE FROM MockExam me WHERE me.studyPlan.id = :studyPlanId")
    void deleteByStudyPlanId(@Param("studyPlanId") Long studyPlanId);
}

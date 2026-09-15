package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserId(Long userId);
    List<Review> findByUserIdAndDataAgendadaAndConcluidaFalse(Long userId, LocalDate dataAgendada);

    @Modifying
    @Query("DELETE FROM Review r WHERE r.topic.id = :topicId")
    void deleteByTopicId(@Param("topicId") Long topicId);

    @Modifying
    @Query("DELETE FROM Review r WHERE r.topic.id IN (:topicIds)")
    void deleteByTopicIdIn(@Param("topicIds") List<Long> topicIds);

    @Modifying
    @Query("DELETE FROM Review r WHERE r.studyError.id = :studyErrorId")
    void deleteByStudyErrorId(@Param("studyErrorId") Long studyErrorId);

    @Modifying
    @Query("DELETE FROM Review r WHERE r.studyError.id IN (:studyErrorIds)")
    void deleteByStudyErrorIdIn(@Param("studyErrorIds") List<Long> studyErrorIds);
}

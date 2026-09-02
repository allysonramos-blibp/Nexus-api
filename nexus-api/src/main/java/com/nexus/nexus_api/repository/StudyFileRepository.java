package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.StudyFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyFileRepository extends JpaRepository<StudyFile, Long> {
    List<StudyFile> findByUserIdOrderByDataUploadDesc(Long userId);
}
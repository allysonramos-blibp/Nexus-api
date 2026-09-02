package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.StudyNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {
    List<StudyNote> findByUserIdOrderByAtualizadoEmDesc(Long userId);
}
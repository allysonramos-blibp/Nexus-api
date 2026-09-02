package com.nexus.nexus_api.service;


import com.nexus.nexus_api.dto.StudyNoteRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.StudyNote;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.StudyNoteRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyNoteService {

    private final StudyNoteRepository studyNoteRepository;
    private final UserRepository userRepository;

    public StudyNote create(StudyNoteRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        StudyNote note = StudyNote.builder()
                .titulo(request.titulo())
                .conteudo(request.conteudo())
                .atualizadoEm(LocalDateTime.now())
                .user(user)
                .build();

        return studyNoteRepository.save(note);
    }

    public StudyNote update(Long id, StudyNoteRequest request) {
        StudyNote existing = studyNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada com ID: " + id));

        SecurityUtils.assertOwnership(existing.getUser().getId());

        existing.setTitulo(request.titulo());
        existing.setConteudo(request.conteudo());
        existing.setAtualizadoEm(LocalDateTime.now());

        return studyNoteRepository.save(existing);
    }

    public List<StudyNote> listByUser(Long userId) {
        SecurityUtils.assertOwnership(userId);
        return studyNoteRepository.findByUserIdOrderByAtualizadoEmDesc(userId);
    }

    public void delete(Long id) {
        StudyNote existing = studyNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada com ID: " + id));

        SecurityUtils.assertOwnership(existing.getUser().getId());

        studyNoteRepository.delete(existing);
    }
}

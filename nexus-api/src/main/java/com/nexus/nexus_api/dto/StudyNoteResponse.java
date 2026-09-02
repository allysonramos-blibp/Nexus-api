package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.StudyNote;

import java.time.LocalDateTime;

public record StudyNoteResponse(
        Long id,
        String titulo,
        String conteudo,
        LocalDateTime atualizadoEm,
        Long userId
) {
    public static StudyNoteResponse from(StudyNote note) {
        return new StudyNoteResponse(
                note.getId(),
                note.getTitulo(),
                note.getConteudo(),
                note.getAtualizadoEm(),
                note.getUser().getId()
        );
    }
}

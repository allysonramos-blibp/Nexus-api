package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.ErrorReason;
import com.nexus.nexus_api.model.StudyError;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudyErrorResponse(
        Long id,
        Long questionId,
        String enunciadoQuestao,
        Long answerId,
        ErrorReason motivo,
        String observacao,
        LocalDateTime criadoEm,
        LocalDate proximaRevisao,
        Boolean resolvido
) {
    public static StudyErrorResponse from(StudyError studyError) {
        return new StudyErrorResponse(
                studyError.getId(),
                studyError.getQuestion().getId(),
                studyError.getQuestion().getEnunciado(),
                studyError.getAnswer() != null ? studyError.getAnswer().getId() : null,
                studyError.getMotivo(),
                studyError.getAnotacao(),
                studyError.getCriadoEm(),
                studyError.getProximaRevisao(),
                studyError.getResolvido()
        );
    }
}

package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.MockExam;
import com.nexus.nexus_api.model.MockExamStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MockExamResponse(
        Long id,
        String titulo,
        LocalDate dataRealizacao,
        MockExamStatus status,
        Integer duracaoMinutos,
        Integer totalQuestoes,
        Integer acertos,
        BigDecimal notaObtida,
        BigDecimal percentual,
        LocalDateTime iniciadoEm,
        LocalDateTime finalizadoEm,
        Long studyPlanId,
        List<String> materias
) {
    public static MockExamResponse from(MockExam exam) {
        BigDecimal percentual = BigDecimal.ZERO;
        if (exam.getAcertos() != null && exam.getTotalQuestoes() != null && exam.getTotalQuestoes() > 0) {
            percentual = BigDecimal.valueOf(exam.getAcertos())
                    .divide(BigDecimal.valueOf(exam.getTotalQuestoes()), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }

        return new MockExamResponse(
                exam.getId(),
                exam.getTitulo(),
                exam.getDataRealizacao(),
                exam.getStatus(),
                exam.getDuracaoMinutos(),
                exam.getTotalQuestoes(),
                exam.getAcertos(),
                exam.getNotaObtida(),
                percentual,
                exam.getIniciadoEm(),
                exam.getFinalizadoEm(),
                exam.getStudyPlan() != null ? exam.getStudyPlan().getId() : null,
                exam.getSubjects().stream().map(com.nexus.nexus_api.model.Subject::getNome).toList()
        );
    }
}

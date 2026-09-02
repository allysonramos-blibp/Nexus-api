package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.QuestionDifficulty;

import java.util.List;

/**
 * DTO de saída de questão. O gabarito e a explicação só são preenchidos quando
 * {@code incluirGabarito} for true — usado para esconder a resposta certa enquanto
 * um simulado ainda está em andamento (ver MockExamService).
 */
public record QuestionResponse(
        Long id,
        Integer numero,
        String enunciado,
        List<String> alternativas,
        QuestionDifficulty dificuldade,
        String gabarito,
        String explicacao,
        String banca,
        Integer ano,
        Long topicId,
        Long subjectId,
        String subjectNome
) {
    public static QuestionResponse from(Question question) {
        return build(question, true);
    }

    public static QuestionResponse fromWithoutGabarito(Question question) {
        return build(question, false);
    }

    private static QuestionResponse build(Question question, boolean incluirGabarito) {
        return new QuestionResponse(
                question.getId(),
                question.getNumero(),
                question.getEnunciado(),
                question.getAlternativas(),
                question.getDificuldade(),
                incluirGabarito ? question.getGabarito() : null,
                incluirGabarito ? question.getExplicacao() : null,
                question.getBanca(),
                question.getAno(),
                question.getTopic().getId(),
                question.getTopic().getSubject().getId(),
                question.getTopic().getSubject().getNome()
        );
    }
}

package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Answer;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        Long questionId,
        String respostaEscolhida,
        Boolean correta,
        Integer tempoSegundos,
        Integer numeroTentativa,
        LocalDateTime respondidoEm,
        Long mockExamId
) {
    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getRespostaEscolhida(),
                answer.getCorreta(),
                answer.getTempoSegundos(),
                answer.getNumeroTentativa(),
                answer.getRespondidoEm(),
                answer.getMockExam() != null ? answer.getMockExam().getId() : null
        );
    }
}

package com.nexus.nexus_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AnswerRequest(

        @NotNull(message = "O id da questão é obrigatório.")
        Long questionId,

        @NotBlank(message = "A resposta escolhida é obrigatória.")
        String respostaEscolhida,

        @PositiveOrZero(message = "O tempo de resposta não pode ser negativo.")
        Integer tempoSegundos,

        /** Preenchido apenas quando a resposta faz parte de um simulado. */
        Long mockExamId
) {}

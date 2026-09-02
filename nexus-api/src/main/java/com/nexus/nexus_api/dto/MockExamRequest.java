package com.nexus.nexus_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MockExamRequest(

        @NotBlank(message = "O título do simulado é obrigatório.")
        String titulo,

        /** Opcional — um simulado pode ser avulso, sem plano associado. */
        Long studyPlanId,

        @NotEmpty(message = "Selecione ao menos uma matéria.")
        List<Long> subjectIds,

        @Positive(message = "A quantidade de questões deve ser maior que zero.")
        int quantidadeQuestoes,

        Integer duracaoMinutos
) {}

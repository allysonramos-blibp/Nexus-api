package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.QuestionDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionRequest(

        Integer numero,

        @NotBlank(message = "O enunciado é obrigatório.")
        String enunciado,

        @NotEmpty(message = "Informe ao menos uma alternativa.")
        List<String> alternativas,

        QuestionDifficulty dificuldade,

        @NotBlank(message = "O gabarito é obrigatório.")
        String gabarito,

        String explicacao,

        String banca,

        Integer ano
) {}

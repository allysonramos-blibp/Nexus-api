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

        /** Dica de pegadinha típica da banca para esse tipo de questão (opcional, gerada pela IA na importação). */
        String pegadinha,

        /**
         * Sugestões da IA de disciplina/assunto ao extrair de um PDF — apenas informativas,
         * usadas pelo frontend para pré-selecionar/criar o Subject/Topic antes do POST bulk.
         * Não são persistidas em Question (a relação real é via Topic -> Subject).
         */
        String disciplinaSugerida,

        String assuntoSugerido,

        String banca,

        Integer ano
) {}
